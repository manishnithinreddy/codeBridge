package com.codebridge.security.rbac.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for role permission requests.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RolePermissionRequest {

    @NotNull(message = "Role ID is required")
    private Long roleId;

    @NotNull(message = "Permission ID is required")
    private Long permissionId;
}

