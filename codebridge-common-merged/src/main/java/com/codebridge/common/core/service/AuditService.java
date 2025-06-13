package com.codebridge.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for logging audit events.
 */
@Service
@Slf4j
public class AuditService {

    private final WebClient.Builder webClientBuilder;

    public AuditService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Logs a request to the audit service.
     *
     * @param auditId The audit ID
     * @param serviceName The name of the service
     * @param path The path of the request
     * @param method The HTTP method
     * @param userId The ID of the user making the request
     */
    public void logRequest(String auditId, String serviceName, String path, String method, String userId) {
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("auditId", auditId);
        auditEvent.put("timestamp", LocalDateTime.now().toString());
        auditEvent.put("type", "REQUEST");
        auditEvent.put("serviceName", serviceName);
        auditEvent.put("path", path);
        auditEvent.put("method", method);
        auditEvent.put("userId", userId);
        
        // Log locally for now
        log.info("Audit event: {}", auditEvent);
        
        // In a real implementation, we would send this to a dedicated audit service
        // sendToAuditService(auditEvent).subscribe();
    }

    /**
     * Logs a response to the audit service.
     *
     * @param auditId The audit ID
     * @param serviceName The name of the service
     * @param status The status of the response
     */
    public void logResponse(String auditId, String serviceName, String status) {
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("auditId", auditId);
        auditEvent.put("timestamp", LocalDateTime.now().toString());
        auditEvent.put("type", "RESPONSE");
        auditEvent.put("serviceName", serviceName);
        auditEvent.put("status", status);
        
        // Log locally for now
        log.info("Audit event: {}", auditEvent);
        
        // In a real implementation, we would send this to a dedicated audit service
        // sendToAuditService(auditEvent).subscribe();
    }

    /**
     * Logs an error to the audit service.
     *
     * @param auditId The audit ID
     * @param serviceName The name of the service
     * @param errorMessage The error message
     */
    public void logError(String auditId, String serviceName, String errorMessage) {
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("auditId", auditId);
        auditEvent.put("timestamp", LocalDateTime.now().toString());
        auditEvent.put("type", "ERROR");
        auditEvent.put("serviceName", serviceName);
        auditEvent.put("errorMessage", errorMessage);
        
        // Log locally for now
        log.error("Audit event: {}", auditEvent);
        
        // In a real implementation, we would send this to a dedicated audit service
        // sendToAuditService(auditEvent).subscribe();
    }

    /**
     * Sends an audit event to the audit service.
     *
     * @param auditEvent The audit event to send
     * @return A Mono that completes when the event is sent
     */
    private Mono<Void> sendToAuditService(Map<String, Object> auditEvent) {
        return webClientBuilder.build()
                .post()
                .uri("lb://audit-service/api/audit")
                .bodyValue(auditEvent)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    log.error("Failed to send audit event", e);
                    return Mono.empty();
                });
    }
}

