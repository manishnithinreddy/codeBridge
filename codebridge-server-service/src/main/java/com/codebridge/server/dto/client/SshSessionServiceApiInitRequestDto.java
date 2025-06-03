package com.codebridge.server.dto.client;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// DTO for calling SessionService's SSH init endpoint
public class SshSessionServiceApiInitRequestDto {

    @NotNull
    private UUID platformUserId;

    @NotNull
    private UUID serverId; // The target server resource ID

    @NotNull
    private ClientUserProvidedConnectionDetails connectionDetails;

    // Constructors, Getters, Setters
    public SshSessionServiceApiInitRequestDto() {}

    public SshSessionServiceApiInitRequestDto(UUID platformUserId, UUID serverId, ClientUserProvidedConnectionDetails connectionDetails) {
        this.platformUserId = platformUserId;
        this.serverId = serverId;
        this.connectionDetails = connectionDetails;
    }

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

    public ClientUserProvidedConnectionDetails getConnectionDetails() {
        return connectionDetails;
    }

    public void setConnectionDetails(ClientUserProvidedConnectionDetails connectionDetails) {
        this.connectionDetails = connectionDetails;
    }
}
