package com.codebridge.session.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class SshSessionServiceApiInitRequest {

    @NotNull(message = "Platform User ID cannot be null.")
    private UUID platformUserId;

    @NotNull(message = "Server ID cannot be null.")
    private UUID serverId;

    @Valid
    @NotNull(message = "Connection details must be provided.")
    private UserProvidedConnectionDetails connectionDetails;

    // Default constructor for Jackson
    public SshSessionServiceApiInitRequest() {
    }

    public SshSessionServiceApiInitRequest(UUID platformUserId, UUID serverId, UserProvidedConnectionDetails connectionDetails) {
        this.platformUserId = platformUserId;
        this.serverId = serverId;
        this.connectionDetails = connectionDetails;
    }

    // Getters and Setters
    public UUID getPlatformUserId() {
        return platformUserId;
    }

    public void setPlatformUserId(UUID platformUserId) {
        this.platformUserId = platformUserId;
    }

    public UUID getServerId() {
        return serverId;
    }

    public void setServerId(UUID serverId) {
        this.serverId = serverId;
    }

    public UserProvidedConnectionDetails getConnectionDetails() {
        return connectionDetails;
    }

    public void setConnectionDetails(UserProvidedConnectionDetails connectionDetails) {
        this.connectionDetails = connectionDetails;
    }
}
