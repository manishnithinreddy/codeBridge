package com.codebridge.usermanagement.auth.repository;

import com.codebridge.usermanagement.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by email.
     *
     * @param email The email
     * @return The user
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists by email.
     *
     * @param email The email
     * @return True if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find all users with a specific role.
     *
     * @param roleName The role name
     * @return The users
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(String roleName);

    /**
     * Find all users created after a specific date.
     *
     * @param date The date
     * @return The users
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find all users that have not logged in since a specific date.
     *
     * @param date The date
     * @return The users
     */
    List<User> findByLastLoginAtBeforeOrLastLoginAtIsNull(LocalDateTime date);
}

