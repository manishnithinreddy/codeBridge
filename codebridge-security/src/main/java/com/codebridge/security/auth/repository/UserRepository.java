package com.codebridge.security.auth.repository;

import com.codebridge.security.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by username.
     *
     * @param username The username
     * @return The user, if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by email.
     *
     * @param email The email
     * @return The user, if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a username exists.
     *
     * @param username The username
     * @return True if the username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Checks if an email exists.
     *
     * @param email The email
     * @return True if the email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Increments the failed login attempts for a user.
     *
     * @param username The username
     * @return The number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.username = :username")
    int incrementFailedAttempts(@Param("username") String username);

    /**
     * Resets the failed login attempts for a user.
     *
     * @param username The username
     * @return The number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = 0 WHERE u.username = :username")
    int resetFailedAttempts(@Param("username") String username);

    /**
     * Locks a user account.
     *
     * @param username The username
     * @param lockoutTime The lockout time
     * @return The number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = false, u.lockoutTime = :lockoutTime WHERE u.username = :username")
    int lockUser(@Param("username") String username, @Param("lockoutTime") LocalDateTime lockoutTime);

    /**
     * Unlocks a user account.
     *
     * @param username The username
     * @return The number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = true, u.lockoutTime = null, u.failedAttempts = 0 WHERE u.username = :username")
    int unlockUser(@Param("username") String username);

    /**
     * Finds users with expired credentials.
     *
     * @param expiryDate The expiry date
     * @return The users with expired credentials
     */
    @Query("SELECT u FROM User u WHERE u.lastPasswordChange < :expiryDate AND u.credentialsNonExpired = true")
    Iterable<User> findUsersWithExpiredCredentials(@Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Expires a user's credentials.
     *
     * @param userId The user ID
     * @return The number of rows affected
     */
    @Modifying
    @Query("UPDATE User u SET u.credentialsNonExpired = false WHERE u.id = :userId")
    int expireUserCredentials(@Param("userId") Long userId);
}

