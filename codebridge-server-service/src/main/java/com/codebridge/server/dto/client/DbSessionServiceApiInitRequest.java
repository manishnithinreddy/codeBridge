package com.codebridge.server.dto.client;

import com.codebridge.server.dto.sessions.DbSessionCredentials; // Existing DTO in server-service
import java.util.UUID;

// DTO for calling SessionService's /lifecycle/db/init endpoint
public class DbSessionServiceApiInitRequest {
    private UUID platformUserId;
    private String dbConnectionAlias;
    private DbSessionCredentials credentials;

    public DbSessionServiceApiInitRequest(UUID platformUserId, String dbConnectionAlias, DbSessionCredentials credentials) {
        this.platformUserId = platformUserId;
        this.dbConnectionAlias = dbConnectionAlias;
        this.credentials = credentials;
    }

    // Getters (setters not strictly necessary if only used for sending)
    public UUID getPlatformUserId() {
        return platformUserId;
    }

    public String getDbConnectionAlias() {
        return dbConnectionAlias;
    }

    public DbSessionCredentials getCredentials() {
        return credentials;
    }

    // Setters if needed for construction patterns
    public void setPlatformUserId(UUID platformUserId) {
        this.platformUserId = platformUserId;
    }

    public void setDbConnectionAlias(String dbConnectionAlias) {
        this.dbConnectionAlias = dbConnectionAlias;
    }

    public void setCredentials(DbSessionCredentials credentials) {
        this.credentials = credentials;
    }
}
