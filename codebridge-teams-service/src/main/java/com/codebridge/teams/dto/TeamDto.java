package com.codebridge.teams.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Team information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDto {
    private Long id;
    private String name;
    private String description;
    private Long organizationId;
    private String organizationName;
    private int memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

