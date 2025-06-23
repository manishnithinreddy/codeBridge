package com.codebridge.apitest.model;

import com.codebridge.apitest.util.EncryptedStringConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity for project-level authentication tokens.
 * These tokens are used for auto-injection into API requests.
 */
@Entity
@Table(name = "project_tokens")
public class ProjectToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String token;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Column(name = "token_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private Long revokedBy;

    @NotNull
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // For backward compatibility with code that expects active field
    @Transient
    private Boolean active;

    public enum TokenType {
        API_KEY,
        OAUTH_TOKEN,
        BEARER_TOKEN,
        BASIC_AUTH,
        CUSTOM
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // For backward compatibility with code that uses setTokenValue
    public void setTokenValue(String tokenValue) {
        this.token = tokenValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Long getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(Long revokedBy) {
        this.revokedBy = revokedBy;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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

    /**
     * Check if the token is active (not revoked and not expired).
     *
     * @return true if the token is active, false otherwise
     */
    public Boolean getActive() {
        LocalDateTime now = LocalDateTime.now();
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }

    /**
     * Set the active status of the token.
     * If setting to false, this will set the revokedAt timestamp to now.
     * If setting to true, this will clear the revokedAt timestamp.
     *
     * @param active the active status to set
     */
    public void setActive(Boolean active) {
        if (active != null) {
            if (!active) {
                this.revokedAt = LocalDateTime.now();
            } else {
                this.revokedAt = null;
            }
        }
        this.active = active;
    }

    /**
     * Get the project ID.
     *
     * @return the project ID
     */
    public Long getProjectId() {
        return project != null ? project.getId() : null;
    }

    /**
     * Set the project ID by setting the project.
     *
     * @param projectId the project ID to set
     */
    public void setProjectId(Long projectId) {
        if (projectId != null) {
            Project newProject = new Project();
            newProject.setId(projectId);
            this.project = newProject;
        } else {
            this.project = null;
        }
    }

    // Equals and HashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectToken that = (ProjectToken) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

