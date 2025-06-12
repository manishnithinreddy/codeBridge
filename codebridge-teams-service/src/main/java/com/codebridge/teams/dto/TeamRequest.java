package com.codebridge.teams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
    
    @Size(max = 255)
    private String description;
    
    @NotNull
    private Long organizationId;
    
    private Long parentTeamId;
}

