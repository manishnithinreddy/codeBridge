package com.codebridge.server.model;

import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.model.enums.ServerCloudProvider;
import com.codebridge.server.model.enums.ServerStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "servers")
public class Server {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String hostname;

    @Column(nullable = false)
    private Integer port = 22;

    @Column(nullable = false)
    private String remoteUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServerAuthProvider authProvider;

    @Column(nullable = true) // Nullable because authProvider might be SSH_KEY
    private String password; // Encrypted

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ssh_key_id", nullable = true) // Nullable if authProvider is PASSWORD
    private SshKey sshKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServerStatus status;

    @Column(nullable = true)
    private String operatingSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ServerCloudProvider cloudProvider;

    @Column(nullable = false, updatable = false)
    private UUID userId; // User who registered this server

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ServerStatus.UNKNOWN;
        }
        if (port == null) {
            port = 22;
        }
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getRemoteUsername() {
        return remoteUsername;
    }

    public void setRemoteUsername(String remoteUsername) {
        this.remoteUsername = remoteUsername;
    }

    public ServerAuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(ServerAuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public SshKey getSshKey() {
        return sshKey;
    }

    public void setSshKey(SshKey sshKey) {
        this.sshKey = sshKey;
    }

    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public ServerCloudProvider getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(ServerCloudProvider cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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
