package com.codebridge.teams.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for TeamMember entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMemberDto {

    private UUID id;

    @NotNull(message = "Team ID is required")
    private UUID teamId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Role ID is required")
    private UUID roleId;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

