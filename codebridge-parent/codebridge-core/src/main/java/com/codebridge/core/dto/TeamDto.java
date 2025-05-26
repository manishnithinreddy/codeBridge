package com.codebridge.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDto {
    
    private UUID id;
    
    @NotBlank(message = "Team name is required")
    @Size(min = 3, max = 50, message = "Team name must be between 3 and 50 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;
    
    private UUID ownerId;
    
    private UUID parentTeamId;
    
    private boolean active;
    
    private Set<TeamSummaryDto> childTeams = new HashSet<>();
    
    private Set<TeamServiceDto> teamServices = new HashSet<>();
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private LocalDateTime updatedAt;
    
    private String updatedBy;
    
    private Long version;
}

