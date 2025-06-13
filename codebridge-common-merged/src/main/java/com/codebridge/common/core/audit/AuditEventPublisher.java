package com.codebridge.core.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Publisher for audit events.
 * Sends audit events to Kafka for processing by the audit service.
 */
@Component
public class AuditEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventPublisher.class);
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String AUDIT_EVENT_BINDING = "auditEvent-out-0";

    private final StreamBridge streamBridge;
    private final String serviceName;

    public AuditEventPublisher(StreamBridge streamBridge, String serviceName) {
        this.streamBridge = streamBridge;
        this.serviceName = serviceName;
    }

    /**
     * Publishes an audit event.
     *
     * @param type the event type
     * @param path the request path
     * @param method the HTTP method
     * @param userId the user ID
     * @param teamId the team ID
     * @param status the status
     * @param requestBody the request body
     * @param responseBody the response body
     * @param metadata additional metadata
     */
    public void publishAuditEvent(
            String type,
            String path,
            String method,
            UUID userId,
            UUID teamId,
            String status,
            String requestBody,
            String responseBody,
            Map<String, Object> metadata) {
        
        try {
            AuditEvent auditEvent = new AuditEvent();
            auditEvent.setAuditId(UUID.randomUUID().toString());
            auditEvent.setTimestamp(LocalDateTime.now());
            auditEvent.setType(type);
            auditEvent.setServiceName(serviceName);
            auditEvent.setPath(path);
            auditEvent.setMethod(method);
            auditEvent.setUserId(userId);
            auditEvent.setTeamId(teamId);
            auditEvent.setStatus(status);
            auditEvent.setRequestBody(requestBody);
            auditEvent.setResponseBody(responseBody);
            auditEvent.setMetadata(metadata);
            
            // Add correlation ID if available
            String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
            if (correlationId != null) {
                if (metadata != null) {
                    metadata.put("correlationId", correlationId);
                }
            }
            
            streamBridge.send(AUDIT_EVENT_BINDING, auditEvent);
            
            logger.debug("Published audit event: {}", auditEvent.getAuditId());
        } catch (Exception e) {
            logger.error("Failed to publish audit event", e);
        }
    }

    /**
     * Publishes an error audit event.
     *
     * @param type the event type
     * @param path the request path
     * @param method the HTTP method
     * @param userId the user ID
     * @param teamId the team ID
     * @param errorMessage the error message
     * @param requestBody the request body
     * @param metadata additional metadata
     */
    public void publishErrorEvent(
            String type,
            String path,
            String method,
            UUID userId,
            UUID teamId,
            String errorMessage,
            String requestBody,
            Map<String, Object> metadata) {
        
        try {
            AuditEvent auditEvent = new AuditEvent();
            auditEvent.setAuditId(UUID.randomUUID().toString());
            auditEvent.setTimestamp(LocalDateTime.now());
            auditEvent.setType(type);
            auditEvent.setServiceName(serviceName);
            auditEvent.setPath(path);
            auditEvent.setMethod(method);
            auditEvent.setUserId(userId);
            auditEvent.setTeamId(teamId);
            auditEvent.setStatus("ERROR");
            auditEvent.setErrorMessage(errorMessage);
            auditEvent.setRequestBody(requestBody);
            auditEvent.setMetadata(metadata);
            
            // Add correlation ID if available
            String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
            if (correlationId != null) {
                if (metadata != null) {
                    metadata.put("correlationId", correlationId);
                }
            }
            
            streamBridge.send(AUDIT_EVENT_BINDING, auditEvent);
            
            logger.debug("Published error audit event: {}", auditEvent.getAuditId());
        } catch (Exception e) {
            logger.error("Failed to publish error audit event", e);
        }
    }
}

