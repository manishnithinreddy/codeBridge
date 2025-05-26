package com.codebridge.teams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Team entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDto {

    private UUID id;

    @NotBlank(message = "Team name is required")
    @Size(min = 3, max = 50, message = "Team name must be between 3 and 50 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private UUID ownerId;

    private UUID parentTeamId;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

