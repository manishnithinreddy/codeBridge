package com.codebridge.aidb.dto;

import java.io.Serializable;

// Using a record for conciseness
public record SessionResponse(
    String sessionToken, // The JWT issued by SessionService
    String type,         // e.g., "SSH", "DB_POSTGRESQL"
    String status,       // e.g., "ACTIVE", "INITIALIZING"
    long createdAt,      // Epoch millis
    long expiresAt       // Epoch millis
) implements Serializable {
}

