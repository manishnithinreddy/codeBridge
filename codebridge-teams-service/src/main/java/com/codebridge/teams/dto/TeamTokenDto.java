package com.codebridge.teams.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for TeamToken entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamTokenDto {

    private UUID id;

    @NotNull(message = "Team ID is required")
    private UUID teamId;

    @NotBlank(message = "Token name is required")
    private String name;

    // Only included in responses when a new token is created
    private String tokenValue;

    private LocalDateTime expiresAt;

    private boolean active;

    private UUID createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

