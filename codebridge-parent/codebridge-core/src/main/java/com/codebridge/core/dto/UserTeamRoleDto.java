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
public class UserTeamRoleDto {
    
    private UUID id;
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    private String username;
    
    @NotNull(message = "Team ID is required")
    private UUID teamId;
    
    private String teamName;
    
    @NotNull(message = "Role ID is required")
    private UUID roleId;
    
    private String roleName;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private LocalDateTime updatedAt;
    
    private String updatedBy;
    
    private Long version;
}

