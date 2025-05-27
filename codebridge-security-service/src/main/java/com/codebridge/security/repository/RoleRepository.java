package com.codebridge.security.repository;

import com.codebridge.core.repository.BaseRepository;
import com.codebridge.security.model.Role;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Role entity operations.
 */
@Repository
public interface RoleRepository extends BaseRepository<Role, UUID> {

    /**
     * Finds a role by name.
     *
     * @param name the role name
     * @return the role if found
     */
    Optional<Role> findByName(String name);
}

