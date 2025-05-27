package com.codebridge.usermanagement.auth.repository;

import com.codebridge.usermanagement.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Role entity operations.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find a role by name.
     *
     * @param name The role name
     * @return The role
     */
    Optional<Role> findByName(String name);

    /**
     * Check if a role exists by name.
     *
     * @param name The role name
     * @return True if exists, false otherwise
     */
    boolean existsByName(String name);
}

