package com.codebridge.server.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ServerUserResponse {

    private UUID id;
    private UUID serverId;
    private String serverName; // For convenience
    private UUID platformUserId;
    private String remoteUsernameForUser;
    private UUID sshKeyIdForUser; // ID of the SshKey used by this user for this server
    private String sshKeyNameForUser; // Name of the SshKey, for convenience
    private UUID accessGrantedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getServerId() {
        return serverId;
    }

    public void setServerId(UUID serverId) {
        this.serverId = serverId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
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

    public String getSshKeyNameForUser() {
        return sshKeyNameForUser;
    }

    public void setSshKeyNameForUser(String sshKeyNameForUser) {
        this.sshKeyNameForUser = sshKeyNameForUser;
    }

    public UUID getAccessGrantedBy() {
        return accessGrantedBy;
    }

    public void setAccessGrantedBy(UUID accessGrantedBy) {
        this.accessGrantedBy = accessGrantedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
