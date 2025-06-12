package com.codebridge.server.controller;

import com.codebridge.server.config.AIDbAgentServiceConfigProperties;
import com.codebridge.server.dto.ai.ClientNaturalLanguageQueryRequest;
import com.codebridge.server.dto.ai.ClientNaturalLanguageQueryResponse;
import com.codebridge.server.dto.ai.PluginNaturalLanguageQueryRequest;
import com.codebridge.server.exception.AccessDeniedException; // Assuming this exists from previous setups
import com.codebridge.server.service.ServerAccessControlService;
// DbSessionProxyController is not directly injected for this flow, as we require dbSessionToken from plugin
// import com.codebridge.server.controller.DbSessionProxyController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai-db-proxy")
public class AIDbAgentProxyController {

    private static final Logger logger = LoggerFactory.getLogger(AIDbAgentProxyController.class);

    private final RestTemplate restTemplate;
    private final ServerAccessControlService serverAccessControlService;
    // DbSessionProxyController not needed if plugin provides dbSessionToken
    private final AIDbAgentServiceConfigProperties aiDbAgentServiceConfigProperties;

    public AIDbAgentProxyController(RestTemplate restTemplate,
                                    ServerAccessControlService serverAccessControlService,
                                    AIDbAgentServiceConfigProperties aiDbAgentServiceConfigProperties) {
        this.restTemplate = restTemplate;
        this.serverAccessControlService = serverAccessControlService;
        this.aiDbAgentServiceConfigProperties = aiDbAgentServiceConfigProperties;
    }

    private UUID getPlatformUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Authentication principal not found or username is null.");
        }
        return UUID.fromString(authentication.getName());
    }

    private String extractUserJwt(Authentication authentication) {
        // This depends on how JWT is stored in Authentication.
        // If using Spring's default JwtAuthenticationToken, it's authentication.getToken().getTokenValue()
        // For simplicity, assuming it can be retrieved if needed.
        // This is the User JWT, not the dbSessionToken.
        if (authentication.getCredentials() instanceof String) {
            return (String) authentication.getCredentials();
        }
        // Placeholder if using a different Authentication object type (e.g. AbstractOAuth2TokenAuthenticationToken)
        // Object token = authentication.getToken(); if (token instanceof Jwt) return ((Jwt) token).getTokenValue();
        logger.warn("Could not extract User JWT from Authentication object of type: {}", authentication.getClass().getName());
        return null;
    }


    @PostMapping("/query")
    public ResponseEntity<ClientNaturalLanguageQueryResponse> handleNaturalLanguageQuery(
            @Valid @RequestBody PluginNaturalLanguageQueryRequest pluginRequest,
            Authentication authentication,
            HttpServletRequest httpServletRequest) { // To get original Authorization header

        UUID platformUserId = getPlatformUserId(authentication);
        logger.info("User {} querying DB alias '{}' with NLQ: '{}' using session token: {}",
            platformUserId, pluginRequest.getDbConnectionAlias(), pluginRequest.getNaturalLanguageQuery(), pluginRequest.getDbSessionToken());

        // 1. Authorization: For this version, we rely on the dbSessionToken being valid for the user.
        // A deeper authorization would be to check if platformUserId has rights to dbConnectionAlias
        // *before* proxying, but that requires linking dbConnectionAlias to a ServerUser grant or similar.
        // The current simplified approach is that if the user has a valid dbSessionToken for this alias,
        // they are authorized for operations via that token. AIDbAgentService and SessionService will validate the token.
        // serverAccessControlService.checkUserAccessToDbAlias(platformUserId, pluginRequest.getDbConnectionAlias()); // Conceptual

        // 2. Ensure dbSessionToken is provided (as per Worker Decision in prompt)
        if (pluginRequest.getDbSessionToken() == null || pluginRequest.getDbSessionToken().isBlank()) {
            throw new IllegalArgumentException("Active dbSessionToken is required to query the AI DB Agent.");
        }

        // 3. Prepare request for AIDbAgentService
        ClientNaturalLanguageQueryRequest agentRequest = new ClientNaturalLanguageQueryRequest(
            pluginRequest.getDbSessionToken(),
            pluginRequest.getNaturalLanguageQuery(),
            pluginRequest.getDbConnectionAlias() // Pass along for context
        );

        // 4. Call AIDbAgentService
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Propagate the original User JWT to AIDbAgentService
        String userJwt = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (userJwt != null && userJwt.startsWith("Bearer ")) {
            headers.set("Authorization", userJwt);
        } else {
            logger.warn("User JWT not found in original request to AIDbAgentProxyController. AIDbAgentService might deny if it requires User JWT.");
            // Depending on AIDbAgentService's security, this might be an error.
            // For now, proceed, assuming AIDbAgentService might allow if dbSessionToken is sufficient for its auth.
        }


        HttpEntity<ClientNaturalLanguageQueryRequest> entity = new HttpEntity<>(agentRequest, headers);
        String aiDbAgentServiceUrl = aiDbAgentServiceConfigProperties.getAiDbAgentService() + "/query";

        try {
            ResponseEntity<ClientNaturalLanguageQueryResponse> response = restTemplate.exchange(
                aiDbAgentServiceUrl,
                HttpMethod.POST,
                entity,
                ClientNaturalLanguageQueryResponse.class
            );
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            logger.error("Error from AIDbAgentService: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            // Forward the error structure if possible, or create a new one
            ClientNaturalLanguageQueryResponse errorResponse = new ClientNaturalLanguageQueryResponse();
            errorResponse.setProcessingError("Failed to process query via AI DB Agent: " + e.getResponseBodyAsString());
             // Attempt to parse error response from AIDbAgentService if it has a known structure
            return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
        } catch (Exception e) {
            logger.error("Generic error proxying to AIDbAgentService: {}", e.getMessage(), e);
            ClientNaturalLanguageQueryResponse errorResponse = new ClientNaturalLanguageQueryResponse();
            errorResponse.setProcessingError("An unexpected error occurred while proxying to AI DB Agent.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
