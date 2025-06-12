package com.codebridge.security.rbac.repository;

import com.codebridge.security.rbac.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Permission entities.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Finds a permission by name.
     *
     * @param name The permission name
     * @return The permission, if found
     */
    Optional<Permission> findByName(String name);

    /**
     * Checks if a permission name exists.
     *
     * @param name The permission name
     * @return True if the permission name exists, false otherwise
     */
    boolean existsByName(String name);
}

