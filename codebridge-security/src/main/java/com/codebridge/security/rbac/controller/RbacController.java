package com.codebridge.security.rbac.controller;

import com.codebridge.security.rbac.dto.PermissionDto;
import com.codebridge.security.rbac.dto.RoleDto;
import com.codebridge.security.rbac.dto.RolePermissionRequest;
import com.codebridge.security.rbac.dto.UserRoleRequest;
import com.codebridge.security.rbac.model.Permission;
import com.codebridge.security.rbac.model.Role;
import com.codebridge.security.rbac.service.RbacService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Controller for RBAC endpoints.
 */
@RestController
@RequestMapping("/api/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;

    /**
     * Gets all roles.
     *
     * @return The roles
     */
    @GetMapping("/roles")
    public ResponseEntity<Iterable<RoleDto>> getAllRoles() {
        Iterable<Role> roles = rbacService.getAllRoles();
        Iterable<RoleDto> roleDtos = StreamSupport.stream(roles.spliterator(), false)
                .map(this::convertToRoleDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roleDtos);
    }

    /**
     * Gets a role by ID.
     *
     * @param id The role ID
     * @return The role
     */
    @GetMapping("/roles/{id}")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable Long id) {
        return rbacService.getRoleByName(id.toString())
                .map(this::convertToRoleDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a role.
     *
     * @param roleDto The role DTO
     * @return The created role
     */
    @PostMapping("/roles")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody RoleDto roleDto) {
        Role role = convertToRole(roleDto);
        Role createdRole = rbacService.createRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToRoleDto(createdRole));
    }

    /**
     * Updates a role.
     *
     * @param id The role ID
     * @param roleDto The role DTO
     * @return The updated role
     */
    @PutMapping("/roles/{id}")
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDto roleDto) {
        if (!id.equals(roleDto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        
        Role role = convertToRole(roleDto);
        Role updatedRole = rbacService.updateRole(role);
        return ResponseEntity.ok(convertToRoleDto(updatedRole));
    }

    /**
     * Deletes a role.
     *
     * @param id The role ID
     * @return The response entity
     */
    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets all permissions.
     *
     * @return The permissions
     */
    @GetMapping("/permissions")
    public ResponseEntity<Iterable<PermissionDto>> getAllPermissions() {
        Iterable<Permission> permissions = rbacService.getAllPermissions();
        Iterable<PermissionDto> permissionDtos = StreamSupport.stream(permissions.spliterator(), false)
                .map(this::convertToPermissionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissionDtos);
    }

    /**
     * Gets a permission by ID.
     *
     * @param id The permission ID
     * @return The permission
     */
    @GetMapping("/permissions/{id}")
    public ResponseEntity<PermissionDto> getPermissionById(@PathVariable Long id) {
        return rbacService.getPermissionByName(id.toString())
                .map(this::convertToPermissionDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a permission.
     *
     * @param permissionDto The permission DTO
     * @return The created permission
     */
    @PostMapping("/permissions")
    public ResponseEntity<PermissionDto> createPermission(@Valid @RequestBody PermissionDto permissionDto) {
        Permission permission = convertToPermission(permissionDto);
        Permission createdPermission = rbacService.createPermission(permission);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToPermissionDto(createdPermission));
    }

    /**
     * Assigns a role to a user.
     *
     * @param request The user role request
     * @return The response entity
     */
    @PostMapping("/users/roles")
    public ResponseEntity<Void> assignRoleToUser(@Valid @RequestBody UserRoleRequest request) {
        rbacService.assignRoleToUser(request.getUserId(), request.getRoleId());
        return ResponseEntity.ok().build();
    }

    /**
     * Removes a role from a user.
     *
     * @param request The user role request
     * @return The response entity
     */
    @DeleteMapping("/users/roles")
    public ResponseEntity<Void> removeRoleFromUser(@Valid @RequestBody UserRoleRequest request) {
        rbacService.removeRoleFromUser(request.getUserId(), request.getRoleId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a permission to a role.
     *
     * @param request The role permission request
     * @return The response entity
     */
    @PostMapping("/roles/permissions")
    public ResponseEntity<Void> addPermissionToRole(@Valid @RequestBody RolePermissionRequest request) {
        rbacService.addPermissionToRole(request.getRoleId(), request.getPermissionId());
        return ResponseEntity.ok().build();
    }

    /**
     * Removes a permission from a role.
     *
     * @param request The role permission request
     * @return The response entity
     */
    @DeleteMapping("/roles/permissions")
    public ResponseEntity<Void> removePermissionFromRole(@Valid @RequestBody RolePermissionRequest request) {
        rbacService.removePermissionFromRole(request.getRoleId(), request.getPermissionId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets all permissions for a user.
     *
     * @param userId The user ID
     * @return The permissions
     */
    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<Set<PermissionDto>> getUserPermissions(@PathVariable Long userId) {
        Set<Permission> permissions = rbacService.getUserPermissions(userId);
        Set<PermissionDto> permissionDtos = permissions.stream()
                .map(this::convertToPermissionDto)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(permissionDtos);
    }

    /**
     * Gets all roles for a user.
     *
     * @param userId The user ID
     * @return The roles
     */
    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<Set<RoleDto>> getUserRoles(@PathVariable Long userId) {
        Set<Role> roles = rbacService.getUserRoles(userId);
        Set<RoleDto> roleDtos = roles.stream()
                .map(this::convertToRoleDto)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(roleDtos);
    }

    /**
     * Converts a role to a role DTO.
     *
     * @param role The role
     * @return The role DTO
     */
    private RoleDto convertToRoleDto(Role role) {
        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .parentId(role.getParent() != null ? role.getParent().getId() : null)
                .permissionIds(role.getPermissions().stream()
                        .map(Permission::getId)
                        .collect(Collectors.toSet()))
                .build();
    }

    /**
     * Converts a role DTO to a role.
     *
     * @param roleDto The role DTO
     * @return The role
     */
    private Role convertToRole(RoleDto roleDto) {
        Role role = new Role();
        role.setId(roleDto.getId());
        role.setName(roleDto.getName());
        role.setDescription(roleDto.getDescription());
        
        // Parent and permissions are handled separately in the service
        
        return role;
    }

    /**
     * Converts a permission to a permission DTO.
     *
     * @param permission The permission
     * @return The permission DTO
     */
    private PermissionDto convertToPermissionDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .resourceType(permission.getResourceType())
                .action(permission.getAction())
                .build();
    }

    /**
     * Converts a permission DTO to a permission.
     *
     * @param permissionDto The permission DTO
     * @return The permission
     */
    private Permission convertToPermission(PermissionDto permissionDto) {
        return Permission.builder()
                .id(permissionDto.getId())
                .name(permissionDto.getName())
                .description(permissionDto.getDescription())
                .resourceType(permissionDto.getResourceType())
                .action(permissionDto.getAction())
                .build();
    }
}

