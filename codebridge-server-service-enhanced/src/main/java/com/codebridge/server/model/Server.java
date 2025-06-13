package com.codebridge.server.model;

import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.model.enums.ServerCloudProvider;
import com.codebridge.server.model.enums.ServerStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "servers")
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String hostname;

    @NotNull
    @Min(1)
    @Max(65535)
    @Column(nullable = false)
    private Integer port;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String remoteUsername;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServerAuthProvider authProvider;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String password; // Placeholder for encrypted password, nullable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ssh_key_id")
    private SshKey sshKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServerStatus status;

    @Size(max = 255)
    private String operatingSystem;

    @Enumerated(EnumType.STRING) // Nullable by default
    private ServerCloudProvider cloudProvider;

    @NotNull
    @Column(nullable = false, updatable = false)
    private UUID userId; // Owner of this server entry

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Server() {
        this.status = ServerStatus.UNKNOWN; // Default status
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return Objects.equals(id, server.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Server{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", hostname='" + hostname + '\'' +
               ", port=" + port +
               ", userId=" + userId +
               ", status=" + status +
               '}';
    }
}
