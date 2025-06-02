package com.codebridge.apitest.dto;

import com.codebridge.apitester.model.enums.SharePermissionLevel; // Enum from apitester.model
import java.time.LocalDateTime;
import java.util.UUID;

public class ShareGrantResponse {
    private UUID id; // ShareGrant ID
    private UUID projectId;
    private String projectName;
    private UUID granteeUserId;
    private SharePermissionLevel permissionLevel;
    private UUID grantedByUserId;
    private LocalDateTime createdAt;

    // Constructors
    public ShareGrantResponse() {
    }

    public ShareGrantResponse(UUID id, UUID projectId, String projectName, UUID granteeUserId,
                              SharePermissionLevel permissionLevel, UUID grantedByUserId, LocalDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.granteeUserId = granteeUserId;
        this.permissionLevel = permissionLevel;
        this.grantedByUserId = grantedByUserId;
        this.createdAt = createdAt;
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
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
}
