package com.codebridge.session.model; // Adapted package name

import java.util.UUID;

/**
 * Represents a unique key for a session.
 *
 * @param userId The ID of the user associated with the session.
 * @param resourceId The ID of the resource (e.g., serverId) the session is for.
 * @param resourceType The type of the resource (e.g., "SSH", "DB").
 */
public record SessionKey(
    UUID userId,
    UUID resourceId,
    String resourceType
) {
    // equals(), hashCode(), and constructor are automatically generated for records.
}
