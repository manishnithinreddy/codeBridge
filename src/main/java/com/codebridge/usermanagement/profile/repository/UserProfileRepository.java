package com.codebridge.usermanagement.profile.repository;

import com.codebridge.usermanagement.profile.model.UserProfile;
import com.codebridge.usermanagement.profile.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserProfile entity operations.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Find a user profile by user ID.
     *
     * @param userId the user ID
     * @return the user profile if found
     */
    Optional<UserProfile> findByUserId(UUID userId);

    /**
     * Find user profiles by status.
     *
     * @param status the user status
     * @return list of user profiles
     */
    List<UserProfile> findByStatus(UserStatus status);

    /**
     * Find user profiles by last active time after a specific date.
     *
     * @param dateTime the date time
     * @return list of user profiles
     */
    List<UserProfile> findByLastActiveAtAfter(LocalDateTime dateTime);

    /**
     * Find user profiles by email verification status.
     *
     * @param verified the verification status
     * @return list of user profiles
     */
    List<UserProfile> findByEmailVerified(boolean verified);

    /**
     * Find user profiles by two-factor authentication status.
     *
     * @param enabled the two-factor authentication status
     * @return list of user profiles
     */
    List<UserProfile> findByTwoFactorEnabled(boolean enabled);
}
