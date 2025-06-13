package com.codebridge.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Global filter that validates Keycloak JWT tokens and extracts user information
 * to pass to downstream microservices.
 */
@Component
@Slf4j
public class KeycloakAuthFilter implements GlobalFilter, Ordered {

    private final TokenValidator tokenValidator;
    private final List<String> openEndpoints = List.of(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
    );

    public KeycloakAuthFilter(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for open endpoints
        if (isOpenEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Extract the Authorization header
        List<String> authHeaders = request.getHeaders().getOrEmpty("Authorization");
        if (authHeaders.isEmpty() || !authHeaders.get(0).startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeaders.get(0).substring(7);
        
        // Validate the token
        return tokenValidator.validateToken(token)
                .flatMap(userInfo -> {
                    if (userInfo == null) {
                        log.warn("Invalid token");
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }

                    // Add user information to headers for downstream services
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", userInfo.getUserId())
                            .header("X-User-Name", userInfo.getUsername())
                            .header("X-User-Email", userInfo.getEmail())
                            .header("X-User-Roles", String.join(",", userInfo.getRoles()))
                            .header("X-Team-Id", userInfo.getActiveTeamId())
                            .header("X-Audit-Id", UUID.randomUUID().toString())
                            .build();

                    log.debug("User {} authenticated successfully", userInfo.getUsername());
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .onErrorResume(e -> {
                    log.error("Error validating token", e);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    private boolean isOpenEndpoint(String path) {
        return openEndpoints.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

