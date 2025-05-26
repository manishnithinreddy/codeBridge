package com.codebridge.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for AuditLog entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {

    private UUID id;
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
    private LocalDateTime createdAt;
}

