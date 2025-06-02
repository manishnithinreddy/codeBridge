package com.codebridge.server.dto.client;

import java.util.UUID;

// DTO for calling SessionService's /lifecycle/ssh/init endpoint
public class SshSessionServiceInitRequestDto {
    private UUID platformUserId;
    private UUID serverId;
    private ClientUserProvidedConnectionDetails connectionDetails;

    public SshSessionServiceInitRequestDto(UUID platformUserId, UUID serverId, ClientUserProvidedConnectionDetails connectionDetails) {
        this.platformUserId = platformUserId;
        this.serverId = serverId;
        this.connectionDetails = connectionDetails;
    }

    // Getters - Setters might not be needed if only used for sending
    public UUID getPlatformUserId() { return platformUserId; }
    public UUID getServerId() { return serverId; }
    public ClientUserProvidedConnectionDetails getConnectionDetails() { return connectionDetails; }


    public void setPlatformUserId(UUID platformUserId) { this.platformUserId = platformUserId; }
    public void setServerId(UUID serverId) { this.serverId = serverId; }
    public void setConnectionDetails(ClientUserProvidedConnectionDetails connectionDetails) { this.connectionDetails = connectionDetails; }
}
