package com.codebridge.usermanagement.profile.service;

import com.codebridge.usermanagement.common.exception.ResourceNotFoundException;
import com.codebridge.usermanagement.profile.model.UserProfile;
import com.codebridge.usermanagement.profile.model.UserStatus;
import com.codebridge.usermanagement.profile.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for user profile operations.
 */
@Service
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;

    @Autowired
    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Get all user profiles.
     *
     * @return list of user profiles
     */
    public List<UserProfile> getAllUserProfiles() {
        return userProfileRepository.findAll();
    }

    /**
     * Get a user profile by ID.
     *
     * @param id the user profile ID
     * @return the user profile
     * @throws ResourceNotFoundException if the user profile is not found
     */
    public UserProfile getUserProfileById(UUID id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "id", id));
    }

    /**
     * Get a user profile by user ID.
     *
     * @param userId the user ID
     * @return the user profile
     * @throws ResourceNotFoundException if the user profile is not found
     */
    public UserProfile getUserProfileByUserId(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "userId", userId));
    }

    /**
     * Create a new user profile.
     *
     * @param userProfile the user profile to create
     * @return the created user profile
     */
    @Transactional
    public UserProfile createUserProfile(UserProfile userProfile) {
        if (userProfile.getId() == null) {
            userProfile.setId(UUID.randomUUID());
        }
        
        if (userProfile.getStatus() == null) {
            userProfile.setStatus(UserStatus.OFFLINE);
        }
        
        logger.info("Creating user profile for user ID: {}", userProfile.getUserId());
        return userProfileRepository.save(userProfile);
    }

    /**
     * Update a user profile.
     *
     * @param id the user profile ID
     * @param userProfileDetails the user profile details
     * @return the updated user profile
     * @throws ResourceNotFoundException if the user profile is not found
     */
    @Transactional
    public UserProfile updateUserProfile(UUID id, UserProfile userProfileDetails) {
        UserProfile userProfile = getUserProfileById(id);
        
        userProfile.setDisplayName(userProfileDetails.getDisplayName());
        userProfile.setBio(userProfileDetails.getBio());
        userProfile.setAvatarUrl(userProfileDetails.getAvatarUrl());
        userProfile.setLocation(userProfileDetails.getLocation());
        userProfile.setCompany(userProfileDetails.getCompany());
        userProfile.setWebsite(userProfileDetails.getWebsite());
        userProfile.setGitlabUsername(userProfileDetails.getGitlabUsername());
        userProfile.setGithubUsername(userProfileDetails.getGithubUsername());
        userProfile.setSlackUsername(userProfileDetails.getSlackUsername());
        userProfile.setTimezone(userProfileDetails.getTimezone());
        userProfile.setLocale(userProfileDetails.getLocale());
        userProfile.setPhoneNumber(userProfileDetails.getPhoneNumber());
        
        logger.info("Updating user profile for user ID: {}", userProfile.getUserId());
        return userProfileRepository.save(userProfile);
    }

    /**
     * Update a user's status.
     *
     * @param userId the user ID
     * @param status the new status
     * @return the updated user profile
     * @throws ResourceNotFoundException if the user profile is not found
     */
    @Transactional
    public UserProfile updateUserStatus(UUID userId, UserStatus status) {
        UserProfile userProfile = getUserProfileByUserId(userId);
        userProfile.setStatus(status);
        userProfile.setLastActiveAt(LocalDateTime.now());
        
        logger.info("Updating status for user ID: {} to {}", userId, status);
        return userProfileRepository.save(userProfile);
    }

    /**
     * Update a user's security settings.
     *
     * @param userId the user ID
     * @param emailVerified the email verified status
     * @param phoneVerified the phone verified status
     * @param twoFactorEnabled the two-factor authentication status
     * @return the updated user profile
     * @throws ResourceNotFoundException if the user profile is not found
     */
    @Transactional
    public UserProfile updateSecuritySettings(UUID userId, boolean emailVerified, boolean phoneVerified, boolean twoFactorEnabled) {
        UserProfile userProfile = getUserProfileByUserId(userId);
        userProfile.setEmailVerified(emailVerified);
        userProfile.setPhoneVerified(phoneVerified);
        userProfile.setTwoFactorEnabled(twoFactorEnabled);
        
        logger.info("Updating security settings for user ID: {}", userId);
        return userProfileRepository.save(userProfile);
    }

    /**
     * Delete a user profile.
     *
     * @param id the user profile ID
     * @return true if the user profile was deleted, false otherwise
     */
    @Transactional
    public boolean deleteUserProfile(UUID id) {
        Optional<UserProfile> userProfile = userProfileRepository.findById(id);
        if (userProfile.isPresent()) {
            userProfileRepository.delete(userProfile.get());
            logger.info("Deleted user profile with ID: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Get user profiles by status.
     *
     * @param status the user status
     * @return list of user profiles
     */
    public List<UserProfile> getUserProfilesByStatus(UserStatus status) {
        return userProfileRepository.findByStatus(status);
    }

    /**
     * Get active user profiles.
     *
     * @param since the time since users were last active
     * @return list of active user profiles
     */
    public List<UserProfile> getActiveUserProfiles(LocalDateTime since) {
        return userProfileRepository.findByLastActiveAtAfter(since);
    }

    /**
     * Get user profiles by email verification status.
     *
     * @param verified the verification status
     * @return list of user profiles
     */
    public List<UserProfile> getUserProfilesByEmailVerified(boolean verified) {
        return userProfileRepository.findByEmailVerified(verified);
    }

    /**
     * Get user profiles by two-factor authentication status.
     *
     * @param enabled the two-factor authentication status
     * @return list of user profiles
     */
    public List<UserProfile> getUserProfilesByTwoFactorEnabled(boolean enabled) {
        return userProfileRepository.findByTwoFactorEnabled(enabled);
    }
}
