package com.codebridge.auth.repository;

import com.codebridge.auth.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for UserRole entities.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    /**
     * Finds all user roles by user ID.
     *
     * @param userId the user ID
     * @return the list of user roles
     */
    List<UserRole> findByUserId(UUID userId);
}

