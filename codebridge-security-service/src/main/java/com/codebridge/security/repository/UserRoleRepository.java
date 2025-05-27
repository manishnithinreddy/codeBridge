package com.codebridge.security.repository;

import com.codebridge.core.repository.BaseRepository;
import com.codebridge.security.model.UserRole;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for UserRole entity operations.
 */
@Repository
public interface UserRoleRepository extends BaseRepository<UserRole, UUID> {

    /**
     * Finds all user roles for a user.
     *
     * @param userId the user ID
     * @return list of user roles
     */
    List<UserRole> findByUserId(UUID userId);

    /**
     * Finds all user roles for a role.
     *
     * @param roleId the role ID
     * @return list of user roles
     */
    List<UserRole> findByRoleId(UUID roleId);

    /**
     * Deletes all user roles for a user.
     *
     * @param userId the user ID
     * @return the number of deleted user roles
     */
    int deleteByUserId(UUID userId);
}

