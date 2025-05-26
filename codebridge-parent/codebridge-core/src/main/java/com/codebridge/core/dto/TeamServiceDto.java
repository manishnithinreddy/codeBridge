package com.codebridge.core.dto;

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
public class TeamServiceDto {
    
    private UUID id;
    
    @NotNull(message = "Team ID is required")
    private UUID teamId;
    
    @NotNull(message = "Service ID is required")
    private UUID serviceId;
    
    private String displayName;
    
    private Integer displayOrder;
    
    private boolean enabled;
    
    private String configJson;
    
    private String credentialsJson;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private LocalDateTime updatedAt;
    
    private String updatedBy;
    
    private Long version;
}

