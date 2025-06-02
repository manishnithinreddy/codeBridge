package com.codebridge.apitester.model;

import com.codebridge.apitester.model.enums.SharePermissionLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "share_grants",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "grantee_user_id"}, name = "uk_project_grantee_user"))
public class ShareGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull
    @Column(name = "grantee_user_id", nullable = false)
    private UUID granteeUserId; // The user with whom the project is shared

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SharePermissionLevel permissionLevel;

    @NotNull
    @Column(name = "granted_by_user_id", nullable = false, updatable = false)
    private UUID grantedByUserId; // The platformUserId of who granted this share

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public ShareGrant() {
    }

    public ShareGrant(Project project, UUID granteeUserId, SharePermissionLevel permissionLevel, UUID grantedByUserId) {
        this.project = project;
        this.granteeUserId = granteeUserId;
        this.permissionLevel = permissionLevel;
        this.grantedByUserId = grantedByUserId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public UUID getGranteeUserId() {
        return granteeUserId;
    }

    public void setGranteeUserId(UUID granteeUserId) {
        this.granteeUserId = granteeUserId;
    }

    public SharePermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(SharePermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public UUID getGrantedByUserId() {
        return grantedByUserId;
    }

    public void setGrantedByUserId(UUID grantedByUserId) {
        this.grantedByUserId = grantedByUserId;
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

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShareGrant that = (ShareGrant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ShareGrant{" +
               "id=" + id +
               ", projectId=" + (project != null ? project.getId() : null) +
               ", granteeUserId=" + granteeUserId +
               ", permissionLevel=" + permissionLevel +
               ", grantedByUserId=" + grantedByUserId +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
}
