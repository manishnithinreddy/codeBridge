package com.codebridge.auth.repository;

import com.codebridge.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Role entities.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Finds a role by name.
     *
     * @param name the role name
     * @return the role, if found
     */
    Optional<Role> findByName(String name);
}

