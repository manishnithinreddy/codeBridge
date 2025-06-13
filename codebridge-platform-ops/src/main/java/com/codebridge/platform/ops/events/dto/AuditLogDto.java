package com.codebridge.platform.ops.events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private Long userId;
    private String username;
    private Long organizationId;
    private String organizationName;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> details;
    private LocalDateTime timestamp;
}

