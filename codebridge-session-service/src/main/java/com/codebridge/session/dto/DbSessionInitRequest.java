package com.codebridge.session.dto; // Adapted package

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID; // Import for UUID

// Assuming DbSessionCredentials will also be in com.codebridge.session.dto
// If it's defined elsewhere in this module, the import will be different.

public class DbSessionInitRequest {

    @NotNull(message = "Platform User ID cannot be null.") // Added platformUserId
    private UUID platformUserId;

    @NotBlank(message = "Database connection alias (dbConnectionAlias) cannot be blank.")
    private String dbConnectionAlias;

    @Valid
    @NotNull(message = "Database credentials must be provided.")
    private DbSessionCredentials credentials;

    // Default constructor for Jackson
    public DbSessionInitRequest() {}

    public DbSessionInitRequest(UUID platformUserId, String dbConnectionAlias, DbSessionCredentials credentials) {
        this.platformUserId = platformUserId;
        this.dbConnectionAlias = dbConnectionAlias;
        this.credentials = credentials;
    }

    // Getters and Setters
    public UUID getPlatformUserId() {
        return platformUserId;
    }

    public void setPlatformUserId(UUID platformUserId) {
        this.platformUserId = platformUserId;
    }

    public String getDbConnectionAlias() {
        return dbConnectionAlias;
    }

    public void setDbConnectionAlias(String dbConnectionAlias) {
        this.dbConnectionAlias = dbConnectionAlias;
    }

    public DbSessionCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(DbSessionCredentials credentials) {
        this.credentials = credentials;
    }
}
