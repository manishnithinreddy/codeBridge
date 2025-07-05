package com.codebridge.security.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO for roles.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleDto {

    private Long id;

    @NotBlank(message = "Role name is required")
    private String name;

    private String description;

    private Long parentId;

    private Set<Long> permissionIds = new HashSet<>();

    // Manual getter for id field
    public Long getId() {
        return id;
    }

    // Manual getter for description field
    public String getDescription() {
        return description;
    }

    // Manual getter for name field
    public String getName() {
        return name;
    }

    // Manual getter for parentId field
    public Long getParentId() {
        return parentId;
    }

    // Manual getter for permissionIds field
    public Set<Long> getPermissionIds() {
        return permissionIds;
    }

    // Manual builder method
    public static RoleDtoBuilder builder() {
        return new RoleDtoBuilder();
    }

    // Manual builder class
    public static class RoleDtoBuilder {
        private Long id;
        private String name;
        private String description;
        private Long parentId;
        private Set<Long> permissionIds = new HashSet<>();

        public RoleDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public RoleDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RoleDtoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public RoleDtoBuilder parentId(Long parentId) {
            this.parentId = parentId;
            return this;
        }

        public RoleDtoBuilder permissionIds(Set<Long> permissionIds) {
            this.permissionIds = permissionIds;
            return this;
        }

        public RoleDto build() {
            RoleDto roleDto = new RoleDto();
            roleDto.id = this.id;
            roleDto.name = this.name;
            roleDto.description = this.description;
            roleDto.parentId = this.parentId;
            roleDto.permissionIds = this.permissionIds;
            return roleDto;
        }
    }
}
