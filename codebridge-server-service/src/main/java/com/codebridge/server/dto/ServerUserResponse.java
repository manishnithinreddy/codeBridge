package com.codebridge.server.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ServerUserResponse {
    private UUID id; // ID of the ServerUser link record
    private UUID serverId;
    private String serverName; // Convenience
    private UUID platformUserId; // The user who has access
    private String remoteUsernameForUser;
    private UUID sshKeyIdForUser; // ID of specific key, if any
    private String sshKeyNameForUser; // Name of specific key, if any
    private UUID accessGrantedBy; // Platform user who granted this
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt; // Expiration date for time-limited access
    private String accessLevel; // Access level (OWNER, ADMIN, OPERATOR, VIEWER)
    private boolean isActive; // Whether the access is currently active

    // Constructor, Getters, Setters
    public ServerUserResponse() {
    }

    public ServerUserResponse(UUID id, UUID serverId, String serverName, UUID platformUserId,
                          String remoteUsernameForUser, UUID sshKeyIdForUser, String sshKeyNameForUser,
                          UUID accessGrantedBy, LocalDateTime createdAt, LocalDateTime updatedAt,
                          LocalDateTime expiresAt, String accessLevel, boolean isActive) {
        this.id = id;
        this.serverId = serverId;
        this.serverName = serverName;
        this.platformUserId = platformUserId;
        this.remoteUsernameForUser = remoteUsernameForUser;
        this.sshKeyIdForUser = sshKeyIdForUser;
        this.sshKeyNameForUser = sshKeyNameForUser;
        this.accessGrantedBy = accessGrantedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
        this.accessLevel = accessLevel;
        this.isActive = isActive;
    }

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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
