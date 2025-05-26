package com.codebridge.core.dto;

import com.codebridge.core.model.AuditLog.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDto {
    
    private UUID id;
    
    private UUID userId;
    
    private String username;
    
    private UUID teamId;
    
    private String teamName;
    
    private ActionType actionType;
    
    private String entityType;
    
    private String entityId;
    
    private String actionDetails;
    
    private String ipAddress;
    
    private String userAgent;
    
    private String status;
    
    private Integer statusCode;
    
    private String errorMessage;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private LocalDateTime updatedAt;
    
    private String updatedBy;
    
    private Long version;
}

