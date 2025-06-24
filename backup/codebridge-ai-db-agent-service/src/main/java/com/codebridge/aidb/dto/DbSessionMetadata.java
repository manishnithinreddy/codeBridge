package com.codebridge.aidb.dto;

import com.codebridge.aidb.model.SessionKey;
import java.io.Serializable;
import java.util.UUID; // For platformUserId from SessionKey

// Using a record for conciseness
public record DbSessionMetadata(
    SessionKey sessionKey, // Contains platformUserId, resourceId (dbAlias hash), sessionType
    long createdAt,        // Epoch millis
    long lastAccessedTime, // Epoch millis
    long expiresAt,        // Epoch millis (calculated based on timeout)
    String sessionToken,   // The JWT associated with this session
    String hostingInstanceId, // ID of the SessionService instance managing this session
    String dbType,         // String representation of DbType enum
    String dbHost,
    String dbName,
    String dbUsername      // The username used for this specific DB connection
) implements Serializable {
    // Convenience getter for platformUserId from SessionKey
    public UUID platformUserId() {
        return sessionKey != null ? sessionKey.platformUserId() : null;
    }
    // Convenience getter for resourceId (dbAlias hash) from SessionKey
    public UUID resourceId() {
        return sessionKey != null ? sessionKey.resourceId() : null;
    }
}

