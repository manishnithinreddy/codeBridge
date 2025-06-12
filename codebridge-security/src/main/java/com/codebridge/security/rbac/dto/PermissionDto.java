package com.codebridge.security.rbac.dto;

import com.codebridge.security.rbac.model.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for permissions.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDto {

    private Long id;

    @NotBlank(message = "Permission name is required")
    private String name;

    private String description;

    @NotNull(message = "Resource type is required")
    private Permission.ResourceType resourceType;

    @NotNull(message = "Action is required")
    private Permission.ActionType action;
}

