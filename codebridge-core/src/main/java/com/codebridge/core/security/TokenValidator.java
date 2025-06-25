package com.codebridge.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service to validate JWT tokens against Keycloak's token introspection endpoint.
 */
@Component
@Slf4j
public class TokenValidator {

    private final WebClient webClient;
    private final String keycloakIntrospectionUrl;
    private final String clientId;
    private final String clientSecret;

    public TokenValidator(
            WebClient.Builder webClientBuilder,
            @Value("${keycloak.auth-server-url}/realms/${keycloak.realm}/protocol/openid-connect/token/introspect") String keycloakIntrospectionUrl,
            @Value("${keycloak.resource}") String clientId,
            @Value("${keycloak.credentials.secret}") String clientSecret) {
        this.webClient = webClientBuilder.build();
        this.keycloakIntrospectionUrl = keycloakIntrospectionUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Validates a token against Keycloak and extracts user information.
     *
     * @param token The JWT token to validate
     * @return A Mono containing user information if valid, or empty if invalid
     */
    public Mono<UserInfo> validateToken(String token) {
        return webClient.post()
                .uri(keycloakIntrospectionUrl)
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .bodyValue("token=" + token + "&token_type_hint=access_token")
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    log.error("Error validating token: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Token validation failed"));
                })
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    Boolean active = (Boolean) response.get("active");
                    if (active != null && active) {
                        String userId = (String) response.get("sub");
                        String username = (String) response.get("preferred_username");
                        String email = (String) response.get("email");
                        
                        // Extract roles from resource_access.{client-id}.roles
                        Map<String, Object> resourceAccess = (Map<String, Object>) response.get("resource_access");
                        List<String> roles = Collections.emptyList();
                        if (resourceAccess != null && resourceAccess.containsKey(clientId)) {
                            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
                            if (clientAccess != null && clientAccess.containsKey("roles")) {
                                roles = (List<String>) clientAccess.get("roles");
                            }
                        }
                        
                        // Extract active team ID from custom claim if present
                        String activeTeamId = null;
                        if (response.containsKey("active_team_id")) {
                            activeTeamId = (String) response.get("active_team_id");
                        }
                        
                        return Mono.just(new UserInfo(userId, username, email, roles, activeTeamId));
                    } else {
                        return Mono.empty();
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error processing token validation response", e);
                    return Mono.empty();
                });
    }
}
