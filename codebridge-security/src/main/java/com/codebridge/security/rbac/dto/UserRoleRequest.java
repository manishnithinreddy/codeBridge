package com.codebridge.security.rbac.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user role requests.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Role ID is required")
    private Long roleId;

    // Manual getters
    public Long getUserId() {
        return userId;
    }

    public Long getRoleId() {
        return roleId;
    }
}
