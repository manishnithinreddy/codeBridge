package com.codebridge.security.rbac.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Permission entity for role-based access control.
 */
@Entity
@Table(name = "permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType action;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Resource types for permissions.
     */
    public enum ResourceType {
        USER,
        ROLE,
        PROJECT,
        REPOSITORY,
        BUILD,
        DEPLOYMENT,
        ENVIRONMENT,
        API_KEY,
        AUDIT_LOG,
        SYSTEM_SETTING,
        TEAM,
        ORGANIZATION
    }

    /**
     * Action types for permissions.
     */
    public enum ActionType {
        CREATE,
        READ,
        UPDATE,
        DELETE,
        EXECUTE,
        MANAGE,
        APPROVE
    }

    /**
     * Creates a permission name from resource type and action.
     *
     * @param resourceType The resource type
     * @param action The action
     * @return The permission name
     */
    public static String createPermissionName(ResourceType resourceType, ActionType action) {
        return resourceType.name() + "_" + action.name();
    }
}

