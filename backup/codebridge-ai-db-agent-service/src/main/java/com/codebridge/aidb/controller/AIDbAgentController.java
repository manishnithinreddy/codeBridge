package com.codebridge.aidb.controller;

import com.codebridge.aidb.dto.NaturalLanguageQueryRequest;
import com.codebridge.aidb.dto.NaturalLanguageQueryResponse;
import com.codebridge.aidb.service.AIDbQueryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // For platformUserId
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai-db-agent")
public class AIDbAgentController {

    private static final Logger logger = LoggerFactory.getLogger(AIDbAgentController.class);
    private final AIDbQueryService aiDbQueryService;

    public AIDbAgentController(AIDbQueryService aiDbQueryService) {
        this.aiDbQueryService = aiDbQueryService;
    }

    private UUID getPlatformUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            // This should be handled by Spring Security if endpoint is secured
            logger.warn("Attempted to get platformUserId but Authentication principal was null.");
            return null; // Or throw, depending on whether platformUserId is strictly required vs. best-effort for logging
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            logger.error("Could not parse platformUserId from authentication principal name: {}", authentication.getName(), e);
            return null; // Or throw
        }
    }

    @PostMapping("/query")
    public Mono<ResponseEntity<NaturalLanguageQueryResponse>> processNaturalLanguageQuery(
            @Valid @RequestBody NaturalLanguageQueryRequest nlQueryRequest,
            Authentication authentication) { // Inject Authentication to get platformUserId

        UUID platformUserId = getPlatformUserId(authentication);
        // PlatformUserId can be used for logging, auditing, or potentially passed to services if needed for specific logic
        logger.info("Received NLQ request from user (ID placeholder: {}), session token: {}, alias: {}",
                    platformUserId != null ? platformUserId : "N/A",
                    nlQueryRequest.getDbSessionToken(),
                    nlQueryRequest.getDbConnectionAliasOrId());

        return aiDbQueryService.processQuery(
                nlQueryRequest.getDbSessionToken(),
                nlQueryRequest.getNaturalLanguageQuery(),
                platformUserId // Pass platformUserId for logging/auditing within the service
            )
            .map(response -> {
                if (response.getProcessingError() != null || response.getAiError() != null ||
                    (response.getSqlExecutionResult() != null && response.getSqlExecutionResult().getError() != null)) {
                    // Consider mapping different internal errors to appropriate HTTP status codes
                    // For now, sending 200 OK with error details in the body.
                    // Could also be ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response) or similar.
                    return ResponseEntity.ok(response);
                }
                return ResponseEntity.ok(response);
            })
            .defaultIfEmpty(ResponseEntity.status(500).build()); // Should not happen if service always returns a response object
    }
}
