package com.codebridge.security.repository;

import com.codebridge.core.repository.BaseRepository;
import com.codebridge.security.model.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends BaseRepository<User, String> {

    /**
     * Finds a user by username.
     *
     * @param username the username
     * @return the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by username and active status.
     *
     * @param username the username
     * @return the user if found
     */
    Optional<User> findByUsernameAndActiveTrue(String username);

    /**
     * Finds a user by ID and active status.
     *
     * @param id the user ID
     * @return the user if found
     */
    Optional<User> findByIdAndActiveTrue(String id);

    /**
     * Finds a user by email.
     *
     * @param email the email
     * @return the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given username.
     *
     * @param username the username
     * @return true if the user exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user exists with the given email.
     *
     * @param email the email
     * @return true if the user exists, false otherwise
     */
    boolean existsByEmail(String email);
}

