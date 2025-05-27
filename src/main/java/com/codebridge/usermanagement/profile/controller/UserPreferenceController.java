package com.codebridge.usermanagement.profile.controller;

import com.codebridge.usermanagement.common.model.PreferenceType;
import com.codebridge.usermanagement.profile.model.UserPreference;
import com.codebridge.usermanagement.profile.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user preference operations.
 */
@RestController
@RequestMapping("/preferences")
public class UserPreferenceController {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferenceController.class);

    private final UserPreferenceService userPreferenceService;

    @Autowired
    public UserPreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    /**
     * Get all preferences for a user.
     *
     * @param userId the user ID
     * @return list of user preferences
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<UserPreference>> getUserPreferences(@PathVariable UUID userId) {
        logger.info("Getting preferences for user ID: {}", userId);
        List<UserPreference> preferences = userPreferenceService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Get all preferences for a user in a specific category.
     *
     * @param userId the user ID
     * @param category the category
     * @return list of user preferences
     */
    @GetMapping("/user/{userId}/category/{category}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<UserPreference>> getUserPreferencesByCategory(
            @PathVariable UUID userId, @PathVariable String category) {
        logger.info("Getting preferences for user ID: {} in category: {}", userId, category);
        List<UserPreference> preferences = userPreferenceService.getUserPreferencesByCategory(userId, category);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Get a specific preference for a user.
     *
     * @param userId the user ID
     * @param key the preference key
     * @return the user preference
     */
    @GetMapping("/user/{userId}/key/{key}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserPreference> getUserPreference(@PathVariable UUID userId, @PathVariable String key) {
        logger.info("Getting preference for user ID: {}, key: {}", userId, key);
        UserPreference preference = userPreferenceService.getUserPreference(userId, key);
        return ResponseEntity.ok(preference);
    }

    /**
     * Get a specific preference for a user in a specific category.
     *
     * @param userId the user ID
     * @param category the category
     * @param key the preference key
     * @return the user preference
     */
    @GetMapping("/user/{userId}/category/{category}/key/{key}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserPreference> getUserPreference(
            @PathVariable UUID userId, @PathVariable String category, @PathVariable String key) {
        logger.info("Getting preference for user ID: {}, category: {}, key: {}", userId, category, key);
        UserPreference preference = userPreferenceService.getUserPreference(userId, category, key);
        return ResponseEntity.ok(preference);
    }

    /**
     * Create a new user preference.
     *
     * @param userPreference the user preference to create
     * @return the created user preference
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserPreference> createUserPreference(@Valid @RequestBody UserPreference userPreference) {
        logger.info("Creating preference for user ID: {}, key: {}", userPreference.getUserId(), userPreference.getKey());
        UserPreference createdPreference = userPreferenceService.createUserPreference(userPreference);
        return new ResponseEntity<>(createdPreference, HttpStatus.CREATED);
    }

    /**
     * Update a user preference.
     *
     * @param id the user preference ID
     * @param userPreferenceDetails the user preference details
     * @return the updated user preference
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserPreference> updateUserPreference(
            @PathVariable UUID id, @Valid @RequestBody UserPreference userPreferenceDetails) {
        logger.info("Updating preference with ID: {}", id);
        UserPreference updatedPreference = userPreferenceService.updateUserPreference(id, userPreferenceDetails);
        return ResponseEntity.ok(updatedPreference);
    }

    /**
     * Set a user preference.
     *
     * @param userId the user ID
     * @param category the category
     * @param key the preference key
     * @param value the preference value
     * @param type the preference type
     * @param description the preference description
     * @return the updated or created user preference
     */
    @PutMapping("/user/{userId}/category/{category}/key/{key}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<UserPreference> setUserPreference(
            @PathVariable UUID userId,
            @PathVariable String category,
            @PathVariable String key,
            @RequestParam String value,
            @RequestParam(required = false) PreferenceType type,
            @RequestParam(required = false) String description) {
        logger.info("Setting preference for user ID: {}, category: {}, key: {}", userId, category, key);
        
        // Default to STRING type if not provided
        PreferenceType preferenceType = type != null ? type : PreferenceType.STRING;
        
        UserPreference preference = userPreferenceService.setUserPreference(
                userId, category, key, value, preferenceType, description);
        return ResponseEntity.ok(preference);
    }

    /**
     * Delete a user preference.
     *
     * @param id the user preference ID
     * @return no content if the preference was deleted
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserPreference(@PathVariable UUID id) {
        logger.info("Deleting preference with ID: {}", id);
        boolean deleted = userPreferenceService.deleteUserPreference(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Delete a specific preference for a user.
     *
     * @param userId the user ID
     * @param key the preference key
     * @return no content if the preference was deleted
     */
    @DeleteMapping("/user/{userId}/key/{key}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserPreference(@PathVariable UUID userId, @PathVariable String key) {
        logger.info("Deleting preference for user ID: {}, key: {}", userId, key);
        boolean deleted = userPreferenceService.deleteUserPreference(userId, key);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Delete all preferences for a user.
     *
     * @param userId the user ID
     * @return the number of deleted preferences
     */
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Integer> deleteAllUserPreferences(@PathVariable UUID userId) {
        logger.info("Deleting all preferences for user ID: {}", userId);
        int count = userPreferenceService.deleteAllUserPreferences(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Delete all preferences for a user in a specific category.
     *
     * @param userId the user ID
     * @param category the category
     * @return the number of deleted preferences
     */
    @DeleteMapping("/user/{userId}/category/{category}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Integer> deleteUserPreferencesByCategory(
            @PathVariable UUID userId, @PathVariable String category) {
        logger.info("Deleting preferences for user ID: {} in category: {}", userId, category);
        int count = userPreferenceService.deleteUserPreferencesByCategory(userId, category);
        return ResponseEntity.ok(count);
    }

    /**
     * Get user preferences as a map.
     *
     * @param userId the user ID
     * @return map of preference keys to values
     */
    @GetMapping("/user/{userId}/map")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getUserPreferencesAsMap(@PathVariable UUID userId) {
        logger.info("Getting preferences as map for user ID: {}", userId);
        Map<String, String> preferences = userPreferenceService.getUserPreferencesAsMap(userId);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Get user preferences in a specific category as a map.
     *
     * @param userId the user ID
     * @param category the category
     * @return map of preference keys to values
     */
    @GetMapping("/user/{userId}/category/{category}/map")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getUserPreferencesAsMap(
            @PathVariable UUID userId, @PathVariable String category) {
        logger.info("Getting preferences as map for user ID: {} in category: {}", userId, category);
        Map<String, String> preferences = userPreferenceService.getUserPreferencesAsMap(userId, category);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Set multiple user preferences at once.
     *
     * @param userId the user ID
     * @param category the category
     * @param preferences map of preference keys to values
     * @param type the preference type
     * @return list of updated or created user preferences
     */
    @PutMapping("/user/{userId}/category/{category}/batch")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<UserPreference>> setUserPreferences(
            @PathVariable UUID userId,
            @PathVariable String category,
            @RequestBody Map<String, String> preferences,
            @RequestParam(required = false) PreferenceType type) {
        logger.info("Setting multiple preferences for user ID: {} in category: {}", userId, category);
        
        // Default to STRING type if not provided
        PreferenceType preferenceType = type != null ? type : PreferenceType.STRING;
        
        List<UserPreference> updatedPreferences = userPreferenceService.setUserPreferences(
                userId, category, preferences, preferenceType);
        return ResponseEntity.ok(updatedPreferences);
    }
}
