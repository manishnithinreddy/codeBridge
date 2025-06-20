package com.codebridge.apitest.model;

import com.codebridge.apitest.model.enums.SharePermissionLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity for project sharing grants.
 * Represents access permissions granted to users for projects.
 */
@Entity
@Table(name = "share_grants",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "grantee_user_id"}, name = "uk_project_grantee_user"))
public class ShareGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull
    @Column(name = "grantee_user_id", nullable = false)
    private Long granteeUserId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false)
    private SharePermissionLevel permissionLevel;

    @NotNull
    @Column(name = "granter_user_id", nullable = false, updatable = false)
    private Long granterUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Long getGranteeUserId() {
        return granteeUserId;
    }

    public void setGranteeUserId(Long granteeUserId) {
        this.granteeUserId = granteeUserId;
    }

    public SharePermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(SharePermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public Long getGranterUserId() {
        return granterUserId;
    }

    public void setGranterUserId(Long granterUserId) {
        this.granterUserId = granterUserId;
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

    // Equals and HashCode

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
}

