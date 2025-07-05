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

    // Manual builder method
    public static PermissionDtoBuilder builder() {
        return new PermissionDtoBuilder();
    }

    // Manual builder class
    public static class PermissionDtoBuilder {
        private Long id;
        private String name;
        private String description;
        private Permission.ResourceType resourceType;
        private Permission.ActionType action;

        public PermissionDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PermissionDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PermissionDtoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PermissionDtoBuilder resourceType(Permission.ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public PermissionDtoBuilder action(Permission.ActionType action) {
            this.action = action;
            return this;
        }

        public PermissionDto build() {
            PermissionDto dto = new PermissionDto();
            dto.id = this.id;
            dto.name = this.name;
            dto.description = this.description;
            dto.resourceType = this.resourceType;
            dto.action = this.action;
            return dto;
        }
    }

    // Manual getter for id field
    public Long getId() {
        return id;
    }

    // Manual getter for name field
    public String getName() {
        return name;
    }

    // Manual getter for description field
    public String getDescription() {
        return description;
    }

    // Manual getter for resourceType field
    public Permission.ResourceType getResourceType() {
        return resourceType;
    }

    // Manual getter for action field
    public Permission.ActionType getAction() {
        return action;
    }
}
