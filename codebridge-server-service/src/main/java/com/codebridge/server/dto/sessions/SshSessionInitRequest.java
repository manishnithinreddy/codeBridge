package com.codebridge.server.dto.sessions;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class SshSessionInitRequest {

    @NotNull(message = "Server ID cannot be null.")
    private UUID serverId;

    public UUID getServerId() {
        return serverId;
    }

    public void setServerId(UUID serverId) {
        this.serverId = serverId;
    }
}
