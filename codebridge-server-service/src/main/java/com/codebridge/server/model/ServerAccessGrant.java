package com.codebridge.server.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a user's access grant to a server
 */
@Entity
@Table(name = "server_access_grants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"server_id", "platform_user_id"}, name = "uk_server_user"))
public class ServerAccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private UUID serverId;

    @Column(name = "platform_user_id", nullable = false)
    private UUID platformUserId;

    @Column(name = "access_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel;

    @Column(name = "granted_by", nullable = false)
    private UUID grantedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Enum for access levels
    public enum AccessLevel {
        OWNER,      // Full access, can grant/revoke access to others
        ADMIN,      // Full access, can't grant/revoke
        OPERATOR,   // Can execute commands, upload/download files
        VIEWER      // Can only view files, no execution or modification
    }

    // Default constructor
    public ServerAccessGrant() {
    }

    // Constructor with required fields
    public ServerAccessGrant(UUID serverId, UUID platformUserId, AccessLevel accessLevel, UUID grantedBy) {
        this.serverId = serverId;
        this.platformUserId = platformUserId;
        this.accessLevel = accessLevel;
        this.grantedBy = grantedBy;
    }

    // Constructor with all fields
    public ServerAccessGrant(UUID serverId, UUID platformUserId, AccessLevel accessLevel, 
                            UUID grantedBy, LocalDateTime expiresAt) {
        this.serverId = serverId;
        this.platformUserId = platformUserId;
        this.accessLevel = accessLevel;
        this.grantedBy = grantedBy;
        this.expiresAt = expiresAt;
    }

    // Getters and setters
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

    public UUID getPlatformUserId() {
        return platformUserId;
    }

    public void setPlatformUserId(UUID platformUserId) {
        this.platformUserId = platformUserId;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
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
        ServerAccessGrant that = (ServerAccessGrant) o;
        return Objects.equals(serverId, that.serverId) &&
                Objects.equals(platformUserId, that.platformUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverId, platformUserId);
    }
}

