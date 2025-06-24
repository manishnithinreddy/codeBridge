package com.codebridge.session.dto;

import java.io.Serializable;
import java.util.UUID;

// Using a record for conciseness
public record SshSessionMetadata(
    UUID platformUserId,
    UUID serverId,
    String sessionToken, // The JWT associated with this session
    long createdAt,      // Epoch millis
    long lastAccessedTime, // Epoch millis
    long expiresAt,      // Epoch millis (calculated based on timeout)
    String hostingInstanceId, // ID of the SessionService instance managing this session
    String sshHost,      // SSH host for this session
    String sshUsername   // SSH username for this session
) implements Serializable {
}

