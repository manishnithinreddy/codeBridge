package com.codebridge.server.dto.logging;

import java.io.Serializable;
import java.util.UUID;

// Using a record for conciseness, implements Serializable for AMQP
public record LogEventMessage(
        UUID platformUserId,
        String action,
        UUID serverId, // Nullable
        String details,
        String status,
        String errorMessage, // Nullable
        String ipAddress, // Added for IP tracking
        String userAgent, // Added for additional client context
        long timestamp // Epoch millis
) implements Serializable {
    // No explicit constructor or getters needed for records
}
