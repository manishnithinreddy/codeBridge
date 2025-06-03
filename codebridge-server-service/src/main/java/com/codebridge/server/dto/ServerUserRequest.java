package com.codebridge.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class ServerUserRequest {

    @NotNull(message = "Platform User ID to be granted access cannot be null")
    private UUID platformUserId; // The user to grant access to

    @NotBlank(message = "Remote username for the user cannot be blank")
    @Size(max = 255)
    private String remoteUsernameForUser;

    // Optional: if this user should use a specific SSH key different from server's default
    private UUID sshKeyIdForUser;

    // serverId will be typically from path variable in controller

    // Getters and Setters
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
