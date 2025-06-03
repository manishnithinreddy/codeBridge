package com.codebridge.session.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

public class DbSessionInitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Platform User ID cannot be null")
    private UUID platformUserId;

    @NotBlank(message = "Database connection alias cannot be blank")
    private String dbConnectionAlias; 

    @NotNull(message = "Database credentials cannot be null")
    @Valid // Enable validation for nested DbSessionCredentials fields
    private DbSessionCredentials credentials;
    
    // Optional: If this DB session is related to a managed Server entity in ServerService,
    // its ID could be passed for context or logging.
    private UUID serverId; 

    // Constructors
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

    public UUID getServerId() {
        return serverId;
    }

    public void setServerId(UUID serverId) {
        this.serverId = serverId;
    }
}
