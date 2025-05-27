package com.codebridge.usermanagement.profile.controller;

import com.codebridge.usermanagement.profile.model.UserProfile;
import com.codebridge.usermanagement.profile.model.UserStatus;
import com.codebridge.usermanagement.profile.service.UserProfileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for user profile operations.
 */
@RestController
@RequestMapping("/profiles")
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    private final UserProfileService userProfileService;

    @Autowired
    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * Get all user profiles.
     *
     * @return list of user profiles
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfile>> getAllUserProfiles() {
        logger.info("Getting all user profiles");
        List<UserProfile> userProfiles = userProfileService.getAllUserProfiles();
        return ResponseEntity.ok(userProfiles);
    }

    /**
     * Get a user profile by ID.
     *
     * @param id the user profile ID
     * @return the user profile
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserProfile> getUserProfileById(@PathVariable UUID id) {
        logger.info("Getting user profile by ID: {}", id);
        UserProfile userProfile = userProfileService.getUserProfileById(id);
        return ResponseEntity.ok(userProfile);
    }

    /**
     * Get a user profile by user ID.
     *
     * @param userId the user ID
     * @return the user profile
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserProfile> getUserProfileByUserId(@PathVariable UUID userId) {
        logger.info("Getting user profile by user ID: {}", userId);
        UserProfile userProfile = userProfileService.getUserProfileByUserId(userId);
        return ResponseEntity.ok(userProfile);
    }

    /**
     * Create a new user profile.
     *
     * @param userProfile the user profile to create
     * @return the created user profile
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfile> createUserProfile(@Valid @RequestBody UserProfile userProfile) {
        logger.info("Creating user profile for user ID: {}", userProfile.getUserId());
        UserProfile createdUserProfile = userProfileService.createUserProfile(userProfile);
        return new ResponseEntity<>(createdUserProfile, HttpStatus.CREATED);
    }

    /**
     * Update a user profile.
     *
     * @param id the user profile ID
     * @param userProfileDetails the user profile details
     * @return the updated user profile
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserProfile> updateUserProfile(@PathVariable UUID id, @Valid @RequestBody UserProfile userProfileDetails) {
        logger.info("Updating user profile with ID: {}", id);
        UserProfile updatedUserProfile = userProfileService.updateUserProfile(id, userProfileDetails);
        return ResponseEntity.ok(updatedUserProfile);
    }

    /**
     * Update a user's status.
     *
     * @param userId the user ID
     * @param status the new status
     * @return the updated user profile
     */
    @PatchMapping("/user/{userId}/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserProfile> updateUserStatus(@PathVariable UUID userId, @RequestParam UserStatus status) {
        logger.info("Updating status for user ID: {} to {}", userId, status);
        UserProfile updatedUserProfile = userProfileService.updateUserStatus(userId, status);
        return ResponseEntity.ok(updatedUserProfile);
    }

    /**
     * Update a user's security settings.
     *
     * @param userId the user ID
     * @param emailVerified the email verified status
     * @param phoneVerified the phone verified status
     * @param twoFactorEnabled the two-factor authentication status
     * @return the updated user profile
     */
    @PatchMapping("/user/{userId}/security")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserProfile> updateSecuritySettings(
            @PathVariable UUID userId,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) Boolean phoneVerified,
            @RequestParam(required = false) Boolean twoFactorEnabled) {
        logger.info("Updating security settings for user ID: {}", userId);
        
        // Get current values if not provided
        UserProfile currentProfile = userProfileService.getUserProfileByUserId(userId);
        boolean newEmailVerified = emailVerified != null ? emailVerified : currentProfile.isEmailVerified();
        boolean newPhoneVerified = phoneVerified != null ? phoneVerified : currentProfile.isPhoneVerified();
        boolean newTwoFactorEnabled = twoFactorEnabled != null ? twoFactorEnabled : currentProfile.isTwoFactorEnabled();
        
        UserProfile updatedUserProfile = userProfileService.updateSecuritySettings(
                userId, newEmailVerified, newPhoneVerified, newTwoFactorEnabled);
        return ResponseEntity.ok(updatedUserProfile);
    }

    /**
     * Delete a user profile.
     *
     * @param id the user profile ID
     * @return no content if the user profile was deleted
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserProfile(@PathVariable UUID id) {
        logger.info("Deleting user profile with ID: {}", id);
        boolean deleted = userProfileService.deleteUserProfile(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get user profiles by status.
     *
     * @param status the user status
     * @return list of user profiles
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfile>> getUserProfilesByStatus(@PathVariable UserStatus status) {
        logger.info("Getting user profiles by status: {}", status);
        List<UserProfile> userProfiles = userProfileService.getUserProfilesByStatus(status);
        return ResponseEntity.ok(userProfiles);
    }

    /**
     * Get active user profiles.
     *
     * @param since the time since users were last active
     * @return list of active user profiles
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfile>> getActiveUserProfiles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        logger.info("Getting active user profiles since: {}", since);
        List<UserProfile> userProfiles = userProfileService.getActiveUserProfiles(since);
        return ResponseEntity.ok(userProfiles);
    }

    /**
     * Get user profiles by email verification status.
     *
     * @param verified the verification status
     * @return list of user profiles
     */
    @GetMapping("/email-verified")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfile>> getUserProfilesByEmailVerified(@RequestParam boolean verified) {
        logger.info("Getting user profiles by email verification status: {}", verified);
        List<UserProfile> userProfiles = userProfileService.getUserProfilesByEmailVerified(verified);
        return ResponseEntity.ok(userProfiles);
    }

    /**
     * Get user profiles by two-factor authentication status.
     *
     * @param enabled the two-factor authentication status
     * @return list of user profiles
     */
    @GetMapping("/two-factor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfile>> getUserProfilesByTwoFactorEnabled(@RequestParam boolean enabled) {
        logger.info("Getting user profiles by two-factor authentication status: {}", enabled);
        List<UserProfile> userProfiles = userProfileService.getUserProfilesByTwoFactorEnabled(enabled);
        return ResponseEntity.ok(userProfiles);
    }
}
