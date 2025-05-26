package com.codebridge.teams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for TeamService entity.
 */
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

    @NotBlank(message = "Access level is required")
    private String accessLevel;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

