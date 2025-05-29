package com.codebridge.server.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "server_users")
public class ServerUser {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Column(nullable = false)
    private UUID platformUserId; // Represents the user in CodeBridge platform

    @Column(nullable = false)
    private String remoteUsernameForUser; // e.g., dev-user1 on the remote server

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ssh_key_id") // Nullable if password auth for this user or key is managed externally
    private SshKey sshKeyForUser;

    @Column(nullable = false, updatable = false)
    private UUID accessGrantedBy; // PlatformUserId of admin/owner

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
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

    public SshKey getSshKeyForUser() {
        return sshKeyForUser;
    }

    public void setSshKeyForUser(SshKey sshKeyForUser) {
        this.sshKeyForUser = sshKeyForUser;
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
