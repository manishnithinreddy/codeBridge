package com.codebridge.security.rbac.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Role entity for role-based access control.
 */
@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Role parent;

    @OneToMany(mappedBy = "parent")
    private Set<Role> children = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Adds a permission to this role.
     *
     * @param permission The permission to add
     */
    public void addPermission(Permission permission) {
        permissions.add(permission);
    }

    /**
     * Removes a permission from this role.
     *
     * @param permission The permission to remove
     */
    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    /**
     * Adds a child role to this role.
     *
     * @param child The child role to add
     */
    public void addChild(Role child) {
        children.add(child);
        child.setParent(this);
    }

    /**
     * Removes a child role from this role.
     *
     * @param child The child role to remove
     */
    public void removeChild(Role child) {
        children.remove(child);
        child.setParent(null);
    }

    /**
     * Gets all permissions, including inherited permissions from parent roles.
     *
     * @return The complete set of permissions
     */
    public Set<Permission> getAllPermissions() {
        Set<Permission> allPermissions = new HashSet<>(permissions);
        
        // Add parent permissions recursively
        if (parent != null) {
            allPermissions.addAll(parent.getAllPermissions());
        }
        
        return allPermissions;
    }

    // Manual getter for name field (Lombok might not be generating properly)
    public String getName() {
        return name;
    }

    // Manual getter for description field
    public String getDescription() {
        return description;
    }

    // Manual setter for parent field
    public void setParent(Role parent) {
        this.parent = parent;
    }

    // Manual getter for id field
    public Long getId() {
        return id;
    }

    // Manual getter for parent field
    public Role getParent() {
        return parent;
    }

    // Manual getter for permissions field
    public Set<Permission> getPermissions() {
        return permissions;
    }

    // Manual setter for id field
    public void setId(Long id) {
        this.id = id;
    }

    // Manual setter for description field
    public void setDescription(String description) {
        this.description = description;
    }

    // Manual setter for name field
    public void setName(String name) {
        this.name = name;
    }
}
