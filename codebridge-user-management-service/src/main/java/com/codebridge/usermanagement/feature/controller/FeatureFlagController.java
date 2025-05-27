package com.codebridge.usermanagement.feature.controller;

import com.codebridge.usermanagement.feature.model.FeatureFlag;
import com.codebridge.usermanagement.feature.service.FeatureFlagService;
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
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for feature flag operations.
 */
@RestController
@RequestMapping("/feature-flags")
public class FeatureFlagController {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagController.class);

    private final FeatureFlagService featureFlagService;

    @Autowired
    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    /**
     * Get all feature flags.
     *
     * @return list of feature flags
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<FeatureFlag>> getAllFeatureFlags() {
        logger.info("Getting all feature flags");
        List<FeatureFlag> featureFlags = featureFlagService.getAllFeatureFlags();
        return ResponseEntity.ok(featureFlags);
    }

    /**
     * Get a feature flag by ID.
     *
     * @param id the feature flag ID
     * @return the feature flag
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FeatureFlag> getFeatureFlagById(@PathVariable UUID id) {
        logger.info("Getting feature flag by ID: {}", id);
        FeatureFlag featureFlag = featureFlagService.getFeatureFlagById(id);
        return ResponseEntity.ok(featureFlag);
    }

    /**
     * Get a feature flag by key.
     *
     * @param key the feature flag key
     * @return the feature flag
     */
    @GetMapping("/key/{key}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FeatureFlag> getFeatureFlagByKey(@PathVariable String key) {
        logger.info("Getting feature flag by key: {}", key);
        FeatureFlag featureFlag = featureFlagService.getFeatureFlagByKey(key);
        return ResponseEntity.ok(featureFlag);
    }

    /**
     * Get all enabled feature flags.
     *
     * @return list of enabled feature flags
     */
    @GetMapping("/enabled")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<FeatureFlag>> getEnabledFeatureFlags() {
        logger.info("Getting enabled feature flags");
        List<FeatureFlag> featureFlags = featureFlagService.getEnabledFeatureFlags();
        return ResponseEntity.ok(featureFlags);
    }

    /**
     * Get all global feature flags.
     *
     * @return list of global feature flags
     */
    @GetMapping("/global")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<FeatureFlag>> getGlobalFeatureFlags() {
        logger.info("Getting global feature flags");
        List<FeatureFlag> featureFlags = featureFlagService.getGlobalFeatureFlags();
        return ResponseEntity.ok(featureFlags);
    }

    /**
     * Get all feature flags for a specific team.
     *
     * @param teamId the team ID
     * @return list of team feature flags
     */
    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<FeatureFlag>> getTeamFeatureFlags(@PathVariable UUID teamId) {
        logger.info("Getting feature flags for team ID: {}", teamId);
        List<FeatureFlag> featureFlags = featureFlagService.getTeamFeatureFlags(teamId);
        return ResponseEntity.ok(featureFlags);
    }

    /**
     * Get all enabled feature flags for a specific team.
     *
     * @param teamId the team ID
     * @return list of enabled team feature flags
     */
    @GetMapping("/team/{teamId}/enabled")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<FeatureFlag>> getEnabledTeamFeatureFlags(@PathVariable UUID teamId) {
        logger.info("Getting enabled feature flags for team ID: {}", teamId);
        List<FeatureFlag> featureFlags = featureFlagService.getEnabledTeamFeatureFlags(teamId);
        return ResponseEntity.ok(featureFlags);
    }

    /**
     * Get a specific feature flag for a team.
     *
     * @param teamId the team ID
     * @param key the feature flag key
     * @return the feature flag
     */
    @GetMapping("/team/{teamId}/key/{key}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<FeatureFlag> getTeamFeatureFlag(@PathVariable UUID teamId, @PathVariable String key) {
        logger.info("Getting feature flag for team ID: {}, key: {}", teamId, key);
        FeatureFlag featureFlag = featureFlagService.getTeamFeatureFlag(teamId, key);
        return ResponseEntity.ok(featureFlag);
    }

    /**
     * Create a new feature flag.
     *
     * @param featureFlag the feature flag to create
     * @return the created feature flag
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeatureFlag> createFeatureFlag(@Valid @RequestBody FeatureFlag featureFlag) {
        logger.info("Creating feature flag with key: {}", featureFlag.getKey());
        FeatureFlag createdFeatureFlag = featureFlagService.createFeatureFlag(featureFlag);
        return new ResponseEntity<>(createdFeatureFlag, HttpStatus.CREATED);
    }

    /**
     * Update a feature flag.
     *
     * @param id the feature flag ID
     * @param featureFlagDetails the feature flag details
     * @return the updated feature flag
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeatureFlag> updateFeatureFlag(
            @PathVariable UUID id, @Valid @RequestBody FeatureFlag featureFlagDetails) {
        logger.info("Updating feature flag with ID: {}", id);
        FeatureFlag updatedFeatureFlag = featureFlagService.updateFeatureFlag(id, featureFlagDetails);
        return ResponseEntity.ok(updatedFeatureFlag);
    }

    /**
     * Set a global feature flag.
     *
     * @param key the feature flag key
     * @param enabled the enabled status
     * @param description the description
     * @param rolloutPercentage the rollout percentage
     * @param expiresAt the expiration date
     * @return the updated or created feature flag
     */
    @PutMapping("/global/key/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeatureFlag> setGlobalFeatureFlag(
            @PathVariable String key,
            @RequestParam boolean enabled,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "100") int rolloutPercentage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt) {
        logger.info("Setting global feature flag with key: {}", key);
        FeatureFlag featureFlag = featureFlagService.setGlobalFeatureFlag(
                key, enabled, description, rolloutPercentage, expiresAt);
        return ResponseEntity.ok(featureFlag);
    }

    /**
     * Set a team feature flag.
     *
     * @param teamId the team ID
     * @param key the feature flag key
     * @param enabled the enabled status
     * @param description the description
     * @param rolloutPercentage the rollout percentage
     * @param expiresAt the expiration date
     * @return the updated or created feature flag
     */
    @PutMapping("/team/{teamId}/key/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeatureFlag> setTeamFeatureFlag(
            @PathVariable UUID teamId,
            @PathVariable String key,
            @RequestParam boolean enabled,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "100") int rolloutPercentage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt) {
        logger.info("Setting team feature flag with key: {} for team ID: {}", key, teamId);
        FeatureFlag featureFlag = featureFlagService.setTeamFeatureFlag(
                teamId, key, enabled, description, rolloutPercentage, expiresAt);
        return ResponseEntity.ok(featureFlag);
    }

    /**
     * Enable a feature flag.
     *
     * @param id the feature flag ID
     * @return the updated feature flag
     */
    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeatureFlag> enableFeatureFlag(@PathVariable UUID id) {
        logger.info("Enabling feature flag with ID: {}", id);
        FeatureFlag featureFlag = featureFlagService.enableFeatureFlag(id);
        return ResponseEntity.ok(featureFlag);
    }

    /**
     * Disable a feature flag.
     *
     * @param id the feature flag ID
     * @return the updated feature flag
     */
    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeatureFlag> disableFeatureFlag(@PathVariable UUID id) {
        logger.info("Disabling feature flag with ID: {}", id);
        FeatureFlag featureFlag = featureFlagService.disableFeatureFlag(id);
        return ResponseEntity.ok(featureFlag);
    }

    /**
     * Delete a feature flag.
     *
     * @param id the feature flag ID
     * @return no content if the feature flag was deleted
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFeatureFlag(@PathVariable UUID id) {
        logger.info("Deleting feature flag with ID: {}", id);
        boolean deleted = featureFlagService.deleteFeatureFlag(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Check if a feature flag is enabled.
     *
     * @param key the feature flag key
     * @return true if the feature flag is enabled, false otherwise
     */
    @GetMapping("/key/{key}/is-enabled")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> isFeatureFlagEnabled(@PathVariable String key) {
        logger.info("Checking if feature flag with key: {} is enabled", key);
        boolean isEnabled = featureFlagService.isFeatureFlagEnabled(key);
        return ResponseEntity.ok(isEnabled);
    }

    /**
     * Check if a team feature flag is enabled.
     *
     * @param teamId the team ID
     * @param key the feature flag key
     * @return true if the feature flag is enabled, false otherwise
     */
    @GetMapping("/team/{teamId}/key/{key}/is-enabled")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> isTeamFeatureFlagEnabled(@PathVariable UUID teamId, @PathVariable String key) {
        logger.info("Checking if feature flag with key: {} for team ID: {} is enabled", key, teamId);
        boolean isEnabled = featureFlagService.isTeamFeatureFlagEnabled(teamId, key);
        return ResponseEntity.ok(isEnabled);
    }

    /**
     * Get feature flags as a map.
     *
     * @return map of feature flag keys to enabled status
     */
    @GetMapping("/map")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> getFeatureFlagsAsMap() {
        logger.info("Getting feature flags as map");
        Map<String, Boolean> featureFlags = featureFlagService.getFeatureFlagsAsMap();
        return ResponseEntity.ok(featureFlags);
    }

    /**
     * Get global feature flags as a map.
     *
     * @return map of feature flag keys to enabled status
     */
    @GetMapping("/global/map")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> getGlobalFeatureFlagsAsMap() {
        logger.info("Getting global feature flags as map");
        Map<String, Boolean> featureFlags = featureFlagService.getGlobalFeatureFlagsAsMap();
        return ResponseEntity.ok(featureFlags);
    }

    /**
     * Get team feature flags as a map.
     *
     * @param teamId the team ID
     * @return map of feature flag keys to enabled status
     */
    @GetMapping("/team/{teamId}/map")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> getTeamFeatureFlagsAsMap(@PathVariable UUID teamId) {
        logger.info("Getting feature flags as map for team ID: {}", teamId);
        Map<String, Boolean> featureFlags = featureFlagService.getTeamFeatureFlagsAsMap(teamId);
        return ResponseEntity.ok(featureFlags);
    }

    /**
     * Clean up expired feature flags.
     *
     * @return the number of deleted feature flags
     */
    @DeleteMapping("/cleanup-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> cleanupExpiredFeatureFlags() {
        logger.info("Cleaning up expired feature flags");
        int count = featureFlagService.cleanupExpiredFeatureFlags();
        return ResponseEntity.ok(count);
    }
}
