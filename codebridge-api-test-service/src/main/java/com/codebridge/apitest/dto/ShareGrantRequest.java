package com.codebridge.apitest.dto;

import com.codebridge.apitest.model.enums.SharePermissionLevel; // Updated to use apitest.model.enums
import jakarta.validation.constraints.NotNull;

public class ShareGrantRequest {

    @NotNull(message = "Grantee User ID cannot be null")
    private Long granteeUserId;

    @NotNull(message = "Permission level cannot be null")
    private SharePermissionLevel permissionLevel;

    // Getters and Setters
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
    
    // For backward compatibility
    public Long getUserId() {
        return granteeUserId;
    }

    public void setUserId(Long userId) {
        this.granteeUserId = userId;
    }
}

