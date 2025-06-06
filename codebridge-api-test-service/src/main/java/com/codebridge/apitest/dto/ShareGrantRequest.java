package com.codebridge.apitest.dto;

import com.codebridge.apitest.model.enums.SharePermissionLevel; // Updated to use apitest.model.enums
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ShareGrantRequest {

    @NotNull(message = "Grantee User ID cannot be null")
    private UUID granteeUserId;

    @NotNull(message = "Permission level cannot be null")
    private SharePermissionLevel permissionLevel;

    // Getters and Setters
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
}
