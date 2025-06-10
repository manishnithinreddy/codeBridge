package com.codebridge.session.dto;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

public class SshSessionServiceApiInitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    private UUID platformUserId;

    @NotNull
    private UUID serverId; // The target server resource ID in ServerService's context

    @NotNull
    private UserProvidedConnectionDetails connectionDetails;

    // Constructors
    public SshSessionServiceApiInitRequest() {}

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
