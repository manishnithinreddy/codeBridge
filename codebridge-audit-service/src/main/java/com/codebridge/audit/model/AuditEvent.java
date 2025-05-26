package com.codebridge.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Data class representing an audit event.
 * Used for receiving audit events from other services.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    private String auditId;
    private LocalDateTime timestamp;
    private String type;
    private String serviceName;
    private String path;
    private String method;
    private UUID userId;
    private UUID teamId;
    private String status;
    private String errorMessage;
    private String requestBody;
    private String responseBody;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> metadata;
}

