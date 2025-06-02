package com.codebridge.server.dto.logging;

import java.io.Serializable;
import java.util.UUID;

// Using record for simplicity, ensures it's a plain data carrier and easily serializable to JSON.
// For Java serialization, records are inherently Serializable if their components are.
public record LogEventMessage(
    UUID platformUserId,
    String action,
    UUID serverId, // Nullable
    String details,
    String status,
    String errorMessage, // Nullable
    long timestamp // Epoch milliseconds
) implements Serializable {
    // No explicit serialVersionUID needed for records with default serialization,
    // but can be added if custom serialization is ever introduced.
    // private static final long serialVersionUID = 1L;
}
