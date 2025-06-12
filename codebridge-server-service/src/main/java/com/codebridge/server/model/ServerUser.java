package com.codebridge.server.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "server_users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"server_id", "platform_user_id"}, name = "uk_server_platform_user")
})
public class ServerUser { // Links platform users to servers for team/shared access

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @NotNull
    @Column(name = "platform_user_id", nullable = false)
    private UUID platformUserId; // The CodeBridge platform user ID

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String remoteUsernameForUser; // The username on the remote server for this platform user

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ssh_key_id_for_user") // Nullable, if user uses server's default key or password
    private SshKey sshKeyForUser; // Specific SSH key for this user on this server

    @NotNull
    @Column(nullable = false, updatable = false)
    private UUID accessGrantedBy; // PlatformUser ID who granted this access

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Optional expiration date for time-limited access
    
    @Column(name = "access_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private ServerAccessGrant.AccessLevel accessLevel = ServerAccessGrant.AccessLevel.OPERATOR; // Default to OPERATOR
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // Whether this access grant is currently active

    // Constructors
    public ServerUser() {
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public ServerAccessGrant.AccessLevel getAccessLevel() {
        return accessLevel;
    }
    
    public void setAccessLevel(ServerAccessGrant.AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    /**
     * Check if this access grant is currently valid (active and not expired)
     * @return true if the grant is valid, false otherwise
     */
    @Transient
    public boolean isValid() {
        return isActive && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerUser that = (ServerUser) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ServerUser{" +
               "id=" + id +
               ", serverId=" + (server != null ? server.getId() : null) +
               ", platformUserId=" + platformUserId +
               ", remoteUsernameForUser='" + remoteUsernameForUser + '\'' +
               '}';
    }
}
