package com.codebridge.usermanagement.repository;

import com.codebridge.core.repository.BaseRepository;
import com.codebridge.usermanagement.model.UserProfile;
import com.codebridge.usermanagement.model.UserStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserProfile entity operations.
 */
@Repository
public interface UserProfileRepository extends BaseRepository<UserProfile, UUID> {

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

