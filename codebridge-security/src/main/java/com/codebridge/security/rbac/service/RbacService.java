package com.codebridge.security.rbac.service;

import com.codebridge.security.audit.AuditLogger;
import com.codebridge.security.auth.model.User;
import com.codebridge.security.auth.repository.UserRepository;
import com.codebridge.security.rbac.model.Permission;
import com.codebridge.security.rbac.model.Role;
import com.codebridge.security.rbac.repository.PermissionRepository;
import com.codebridge.security.rbac.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for role-based access control.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    /**
     * Checks if a user has a permission.
     *
     * @param user The user
     * @param permissionName The permission name
     * @return True if the user has the permission, false otherwise
     */
    @Cacheable(value = "userPermissions", key = "#user.id + '_' + #permissionName")
    public boolean hasPermission(User user, String permissionName) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(permissionName));
    }

    /**
     * Checks if a user has a role.
     *
     * @param user The user
     * @param roleName The role name
     * @return True if the user has the role, false otherwise
     */
    @Cacheable(value = "userRoles", key = "#user.id + '_' + #roleName")
    public boolean hasRole(User user, String roleName) {
        String roleAuthority = "ROLE_" + roleName;
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(roleAuthority));
    }

    /**
     * Checks if the current user has a permission.
     *
     * @param permissionName The permission name
     * @return True if the current user has the permission, false otherwise
     */
    public boolean currentUserHasPermission(String permissionName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(permissionName));
    }

    /**
     * Checks if the current user has a role.
     *
     * @param roleName The role name
     * @return True if the current user has the role, false otherwise
     */
    public boolean currentUserHasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String roleAuthority = "ROLE_" + roleName;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(roleAuthority));
    }

    /**
     * Enforces that the current user has a permission.
     *
     * @param permissionName The permission name
     * @throws AccessDeniedException If the current user does not have the permission
     */
    public void enforcePermission(String permissionName) {
        if (!currentUserHasPermission(permissionName)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            // Log access denied event
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("username", username);
            metadata.put("permissionName", permissionName);
            
            auditLogger.logSecurityEvent(
                    "ACCESS_DENIED",
                    "User does not have required permission",
                    metadata
            );
            
            throw new AccessDeniedException("User does not have permission: " + permissionName);
        }
    }

    /**
     * Enforces that the current user has a role.
     *
     * @param roleName The role name
     * @throws AccessDeniedException If the current user does not have the role
     */
    public void enforceRole(String roleName) {
        if (!currentUserHasRole(roleName)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            // Log access denied event
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("username", username);
            metadata.put("roleName", roleName);
            
            auditLogger.logSecurityEvent(
                    "ACCESS_DENIED",
                    "User does not have required role",
                    metadata
            );
            
            throw new AccessDeniedException("User does not have role: " + roleName);
        }
    }

    /**
     * Gets all roles.
     *
     * @return The roles
     */
    public Iterable<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Gets a role by name.
     *
     * @param name The role name
     * @return The role, if found
     */
    public Optional<Role> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    /**
     * Gets all permissions.
     *
     * @return The permissions
     */
    public Iterable<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    /**
     * Gets a permission by name.
     *
     * @param name The permission name
     * @return The permission, if found
     */
    public Optional<Permission> getPermissionByName(String name) {
        return permissionRepository.findByName(name);
    }

    /**
     * Creates a role.
     *
     * @param role The role
     * @return The created role
     */
    @Transactional
    public Role createRole(Role role) {
        // Enforce permission
        enforcePermission(Permission.createPermissionName(
                Permission.ResourceType.ROLE, Permission.ActionType.CREATE));
        
        Role savedRole = roleRepository.save(role);
        
        // Log role creation
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "system";
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        metadata.put("roleName", role.getName());
        metadata.put("roleId", savedRole.getId());
        
        auditLogger.logSecurityEvent(
                "ROLE_CREATED",
                "Role created",
                metadata
        );
        
        return savedRole;
    }

    /**
     * Updates a role.
     *
     * @param role The role
     * @return The updated role
     */
    @Transactional
    @CacheEvict(value = {"userRoles", "userPermissions"}, allEntries = true)
    public Role updateRole(Role role) {
        // Enforce permission
        enforcePermission(Permission.createPermissionName(
                Permission.ResourceType.ROLE, Permission.ActionType.UPDATE));
        
        Role savedRole = roleRepository.save(role);
        
        // Log role update
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "system";
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        metadata.put("roleName", role.getName());
        metadata.put("roleId", savedRole.getId());
        
        auditLogger.logSecurityEvent(
                "ROLE_UPDATED",
                "Role updated",
                metadata
        );
        
        return savedRole;
    }

    /**
     * Deletes a role.
     *
     * @param roleId The role ID
     */
    @Transactional
    @CacheEvict(value = {"userRoles", "userPermissions"}, allEntries = true)
    public void deleteRole(Long roleId) {
        // Enforce permission
        enforcePermission(Permission.createPermissionName(
                Permission.ResourceType.ROLE, Permission.ActionType.DELETE));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        // Log role deletion
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "system";
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        metadata.put("roleName", role.getName());
        metadata.put("roleId", role.getId());
        
        auditLogger.logSecurityEvent(
                "ROLE_DELETED",
                "Role deleted",
                metadata
        );
        
        roleRepository.deleteById(roleId);
    }

    /**
     * Creates a permission.
     *
     * @param permission The permission
     * @return The created permission
     */
    @Transactional
    public Permission createPermission(Permission permission) {
        // Enforce permission
        enforcePermission(Permission.createPermissionName(
                Permission.ResourceType.ROLE, Permission.ActionType.MANAGE));
        
        Permission savedPermission = permissionRepository.save(permission);
        
        // Log permission creation
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "system";
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        metadata.put("permissionName", permission.getName());
        metadata.put("permissionId", savedPermission.getId());
        
        auditLogger.logSecurityEvent(
                "PERMISSION_CREATED",
                "Permission created",
                metadata
        );
        
        return savedPermission;
    }

    /**
     * Assigns a role to a user.
     *
     * @param userId The user ID
     * @param roleId The role ID
     */
    @Transactional
    @CacheEvict(value = {"userRoles", "userPermissions"}, allEntries = true)
    public void assignRoleToUser(Long userId, Long roleId) {
        // Enforce permission
        enforcePermission(Permission.createPermissionName(
                Permission.ResourceType.USER, Permission.ActionType.UPDATE));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        user.getRoles().add(role);
        userRepository.save(user);
        
        // Log role assignment
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "system";
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        metadata.put("targetUsername", user.getUsername());
        metadata.put("targetUserId", user.getId());
        metadata.put("roleName", role.getName());
        metadata.put("roleId", role.getId());
        
        auditLogger.logSecurityEvent(
                "ROLE_ASSIGNED",
                "Role assigned to user",
                metadata
        );
    }

    /**
     * Removes a role from a user.
     *
     * @param userId The user ID
     * @param roleId The role ID
     */
    @Transactional
    @CacheEvict(value = {"userRoles", "userPermissions"}, allEntries = true)
    public void removeRoleFromUser(Long userId, Long roleId) {
        // Enforce permission
        enforcePermission(Permission.createPermissionName(
                Permission.ResourceType.USER, Permission.ActionType.UPDATE));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        user.getRoles().remove(role);
        userRepository.save(user);
        
        // Log role removal
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "system";
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        metadata.put("targetUsername", user.getUsername());
        metadata.put("targetUserId", user.getId());
        metadata.put("roleName", role.getName());
        metadata.put("roleId", role.getId());
        
        auditLogger.logSecurityEvent(
                "ROLE_REMOVED",
                "Role removed from user",
                metadata
        );
    }

    /**
     * Adds a permission to a role.
     *
     * @param roleId The role ID
     * @param permissionId The permission ID
     */
    @Transactional
    @CacheEvict(value = {"userRoles", "userPermissions"}, allEntries = true)
    public void addPermissionToRole(Long roleId, Long permissionId) {
        // Enforce permission
        enforcePermission(Permission.createPermissionName(
                Permission.ResourceType.ROLE, Permission.ActionType.UPDATE));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));
        
        role.addPermission(permission);
        roleRepository.save(role);
        
        // Log permission addition
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "system";
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        metadata.put("roleName", role.getName());
        metadata.put("roleId", role.getId());
        metadata.put("permissionName", permission.getName());
        metadata.put("permissionId", permission.getId());
        
        auditLogger.logSecurityEvent(
                "PERMISSION_ADDED_TO_ROLE",
                "Permission added to role",
                metadata
        );
    }

    /**
     * Removes a permission from a role.
     *
     * @param roleId The role ID
     * @param permissionId The permission ID
     */
    @Transactional
    @CacheEvict(value = {"userRoles", "userPermissions"}, allEntries = true)
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        // Enforce permission
        enforcePermission(Permission.createPermissionName(
                Permission.ResourceType.ROLE, Permission.ActionType.UPDATE));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));
        
        role.removePermission(permission);
        roleRepository.save(role);
        
        // Log permission removal
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "system";
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        metadata.put("roleName", role.getName());
        metadata.put("roleId", role.getId());
        metadata.put("permissionName", permission.getName());
        metadata.put("permissionId", permission.getId());
        
        auditLogger.logSecurityEvent(
                "PERMISSION_REMOVED_FROM_ROLE",
                "Permission removed from role",
                metadata
        );
    }

    /**
     * Gets all permissions for a user.
     *
     * @param userId The user ID
     * @return The permissions
     */
    @Cacheable(value = "userAllPermissions", key = "#userId")
    public Set<Permission> getUserPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return user.getRoles().stream()
                .flatMap(role -> role.getAllPermissions().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Gets all roles for a user.
     *
     * @param userId The user ID
     * @return The roles
     */
    @Cacheable(value = "userAllRoles", key = "#userId")
    public Set<Role> getUserRoles(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return user.getRoles();
    }
}

