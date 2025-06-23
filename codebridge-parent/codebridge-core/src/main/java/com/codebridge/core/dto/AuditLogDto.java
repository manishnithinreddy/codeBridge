package com.codebridge.core.dto;

import com.codebridge.core.model.AuditLog.ActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    
    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;
    
    private UUID userId;
    
    private String ipAddress;
    
    private String userAgent;
    
    @NotNull(message = "Action type is required")
    private ActionType actionType;
    
    private String entityType;
    
    private String entityId;
    
    private String details;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private LocalDateTime updatedAt;
    
    private String updatedBy;
    
    private Long version;
}

