package com.codebridge.server.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// DTO for calling SessionService's DB init endpoint
public class DbSessionServiceApiInitRequestDto {

    @NotNull
    private UUID platformUserId;

    @NotBlank // User-defined alias for this DB connection profile
    private String dbConnectionAlias;

    @NotNull
    private DbSessionCredentials credentials;

    // Optional: If this DB session is tied to a specific managed Server entity in ServerService
    private UUID serverId;

    // Constructors, Getters, Setters
    public DbSessionServiceApiInitRequestDto() {}

    public DbSessionServiceApiInitRequestDto(UUID platformUserId, String dbConnectionAlias, DbSessionCredentials credentials) {
        this.platformUserId = platformUserId;
        this.dbConnectionAlias = dbConnectionAlias;
        this.credentials = credentials;
    }

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
