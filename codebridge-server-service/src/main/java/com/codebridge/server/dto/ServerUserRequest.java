package com.codebridge.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class ServerUserRequest {

    @NotNull(message = "Server ID cannot be null")
    private UUID serverId;

    @NotNull(message = "Platform User ID cannot be null")
    private UUID platformUserId;

    @NotBlank(message = "Remote username for the user cannot be blank")
    @Size(max = 255, message = "Remote username cannot exceed 255 characters")
    private String remoteUsernameForUser;

    private UUID sshKeyIdForUser; // Optional, if user will use a specific SSH key for this server access

    // accessGrantedBy will be set by the system from the authenticated user context

    // Getters and Setters
    public UUID getServerId() {
        return serverId;
    }

    public void setServerId(UUID serverId) {
        this.serverId = serverId;
    }

    public UUID getPlatformUserId() {
        return platformUserId;
    }

    public void setPlatformUserId(UUID platformUserId) {
        this.platformUserId = platformUserId;
    }

    public String getRemoteUsernameForUser() {
        return remoteUsernameForUser;
    }

    public void setRemoteUsernameForUser(String remoteUsernameForUser) {
        this.remoteUsernameForUser = remoteUsernameForUser;
    }

    public UUID getSshKeyIdForUser() {
        return sshKeyIdForUser;
    }

    public void setSshKeyIdForUser(UUID sshKeyIdForUser) {
        this.sshKeyIdForUser = sshKeyIdForUser;
    }
}
