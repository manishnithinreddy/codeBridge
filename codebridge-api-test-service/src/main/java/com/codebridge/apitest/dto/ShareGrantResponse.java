package com.codebridge.apitest.dto;

import com.codebridge.apitest.model.enums.SharePermissionLevel; // Updated to use apitest.model.enums
import java.time.LocalDateTime;

public class ShareGrantResponse {
    private Long id; // ShareGrant ID
    private Long projectId;
    private String projectName;
    private Long userId;
    private SharePermissionLevel permissionLevel;
    private Long createdBy;
    private LocalDateTime createdAt;

    // Constructors
    public ShareGrantResponse() {
    }

    public ShareGrantResponse(Long id, Long projectId, String projectName, Long userId,
                              SharePermissionLevel permissionLevel, Long createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.userId = userId;
        this.permissionLevel = permissionLevel;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SharePermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(SharePermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
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
    
    // New methods for compatibility with updated field names
    public Long getGranteeUserId() {
        return userId;
    }

    public void setGranteeUserId(Long granteeUserId) {
        this.userId = granteeUserId;
    }

    public Long getGrantedByUserId() {
        return createdBy;
    }

    public void setGrantedByUserId(Long grantedByUserId) {
        this.createdBy = grantedByUserId;
    }
}

