package com.codebridge.apitest.model;

import com.codebridge.apitest.util.EncryptedStringConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

/**
 * Entity for project-level authentication tokens.
 * These tokens are used for auto-injection into API requests.
 */
@Entity
@Table(name = "project_tokens")
public class ProjectToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID projectId;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String tokenType; // "Bearer", "Basic", "ApiKey", "OAuth2"

    @NotBlank
    @Column(nullable = false)
    @Convert(converter = EncryptedStringConverter.class) // Encrypt token values in database
    private String tokenValue;

    @Column
    private String headerName; // e.g., "Authorization" for Bearer tokens, or custom header name for API keys

    @Column
    private String parameterName; // For tokens passed as query parameters or form fields

    @Column
    private String tokenLocation; // "header", "query", "cookie", "body"

    @Column
    private LocalDateTime expiresAt;

    @Column
    private String refreshUrl;

    @Column
    @Lob
    private String refreshData; // JSON data for refresh requests

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean autoRefresh;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private UUID createdBy;

    // Constructors
    public ProjectToken() {
    }

    public ProjectToken(UUID projectId, String name, String tokenType, String tokenValue, 
                        String headerName, boolean active, UUID createdBy) {
        this.projectId = projectId;
        this.name = name;
        this.tokenType = tokenType;
        this.tokenValue = tokenValue;
        this.headerName = headerName;
        this.tokenLocation = "header"; // Default to header
        this.active = active;
        this.autoRefresh = false;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getTokenLocation() {
        return tokenLocation;
    }

    public void setTokenLocation(String tokenLocation) {
        this.tokenLocation = tokenLocation;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRefreshUrl() {
        return refreshUrl;
    }

    public void setRefreshUrl(String refreshUrl) {
        this.refreshUrl = refreshUrl;
    }

    public String getRefreshData() {
        return refreshData;
    }

    public void setRefreshData(String refreshData) {
        this.refreshData = refreshData;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    // equals, hashCode, toString
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

    @Override
    public String toString() {
        return "ProjectToken{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", name='" + name + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", headerName='" + headerName + '\'' +
                ", tokenLocation='" + tokenLocation + '\'' +
                ", active=" + active +
                ", autoRefresh=" + autoRefresh +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
