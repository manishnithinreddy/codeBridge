package com.codebridge.server.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for team-based server access.
 */
public record TeamServerAccessDTO(
        Long id,
        UUID serverId,
        UUID teamId,
        String accessLevel,
        UUID createdBy,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

