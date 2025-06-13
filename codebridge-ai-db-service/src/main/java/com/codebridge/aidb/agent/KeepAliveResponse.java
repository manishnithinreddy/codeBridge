package com.codebridge.aidb.agent;

import java.io.Serializable;

// Using a record
public record KeepAliveResponse(
    String sessionToken, // Potentially refreshed token
    String status,       // e.g., "ACTIVE", "EXPIRED"
    long expiresAt       // Epoch millis for new expiry
) implements Serializable {
}

