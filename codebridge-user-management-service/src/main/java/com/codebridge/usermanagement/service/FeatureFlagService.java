package com.codebridge.usermanagement.service;

import com.codebridge.usermanagement.exception.ResourceNotFoundException;
import com.codebridge.usermanagement.model.FeatureFlag;
import com.codebridge.usermanagement.repository.FeatureFlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for feature flag operations.
 */
@Service
public class FeatureFlagService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagService.class);

    private final FeatureFlagRepository featureFlagRepository;

    @Autowired
    public FeatureFlagService(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    /**
     * Get all feature flags.
     *
     * @return list of feature flags
     */
    public List<FeatureFlag> getAllFeatureFlags() {
        return featureFlagRepository.findAll();
    }

    /**
     * Get a feature flag by ID.
     *
     * @param id the feature flag ID
     * @return the feature flag
     * @throws ResourceNotFoundException if the feature flag is not found
     */
    public FeatureFlag getFeatureFlagById(UUID id) {
        return featureFlagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", "id", id));
    }

    /**
     * Get a feature flag by key.
     *
     * @param key the feature flag key
     * @return the feature flag
     * @throws ResourceNotFoundException if the feature flag is not found
     */
    public FeatureFlag getFeatureFlagByKey(String key) {
        return featureFlagRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", "key", key));
    }

    /**
     * Get all enabled feature flags.
     *
     * @return list of enabled feature flags
     */
    public List<FeatureFlag> getEnabledFeatureFlags() {
        return featureFlagRepository.findByEnabled(true);
    }

    /**
     * Get all global feature flags.
     *
     * @return list of global feature flags
     */
    public List<FeatureFlag> getGlobalFeatureFlags() {
        return featureFlagRepository.findByIsGlobal(true);
    }

    /**
     * Get all feature flags for a specific team.
     *
     * @param teamId the team ID
     * @return list of team feature flags
     */
    public List<FeatureFlag> getTeamFeatureFlags(UUID teamId) {
        return featureFlagRepository.findByTeamId(teamId);
    }

    /**
     * Get all enabled feature flags for a specific team.
     *
     * @param teamId the team ID
     * @return list of enabled team feature flags
     */
    public List<FeatureFlag> getEnabledTeamFeatureFlags(UUID teamId) {
        return featureFlagRepository.findByTeamIdAndEnabled(teamId, true);
    }

    /**
     * Get a specific feature flag for a team.
     *
     * @param teamId the team ID
     * @param key the feature flag key
     * @return the feature flag
     * @throws ResourceNotFoundException if the feature flag is not found
     */
    public FeatureFlag getTeamFeatureFlag(UUID teamId, String key) {
        return featureFlagRepository.findByTeamIdAndKey(teamId, key)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", "key", key));
    }

    /**
     * Create a new feature flag.
     *
     * @param featureFlag the feature flag to create
     * @return the created feature flag
     */
    @Transactional
    public FeatureFlag createFeatureFlag(FeatureFlag featureFlag) {
        if (featureFlag.getId() == null) {
            featureFlag.setId(UUID.randomUUID());
        }
        
        logger.info("Creating feature flag with key: {}", featureFlag.getKey());
        return featureFlagRepository.save(featureFlag);
    }

    /**
     * Update a feature flag.
     *
     * @param id the feature flag ID
     * @param featureFlagDetails the feature flag details
     * @return the updated feature flag
     * @throws ResourceNotFoundException if the feature flag is not found
     */
    @Transactional
    public FeatureFlag updateFeatureFlag(UUID id, FeatureFlag featureFlagDetails) {
        FeatureFlag featureFlag = getFeatureFlagById(id);
        
        featureFlag.setEnabled(featureFlagDetails.isEnabled());
        featureFlag.setDescription(featureFlagDetails.getDescription());
        featureFlag.setGlobal(featureFlagDetails.isGlobal());
        featureFlag.setTeamId(featureFlagDetails.getTeamId());
        featureFlag.setRules(featureFlagDetails.getRules());
        featureFlag.setExpiresAt(featureFlagDetails.getExpiresAt());
        featureFlag.setRolloutPercentage(featureFlagDetails.getRolloutPercentage());
        
        logger.info("Updating feature flag with key: {}", featureFlag.getKey());
        return featureFlagRepository.save(featureFlag);
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
    @Transactional
    public FeatureFlag setGlobalFeatureFlag(String key, boolean enabled, String description, 
                                          int rolloutPercentage, LocalDateTime expiresAt) {
        Optional<FeatureFlag> existingFlag = featureFlagRepository.findByKey(key);
        
        if (existingFlag.isPresent()) {
            FeatureFlag featureFlag = existingFlag.get();
            featureFlag.setEnabled(enabled);
            featureFlag.setDescription(description);
            featureFlag.setGlobal(true);
            featureFlag.setTeamId(null);
            featureFlag.setRolloutPercentage(rolloutPercentage);
            featureFlag.setExpiresAt(expiresAt);
            
            logger.info("Updating global feature flag with key: {}", key);
            return featureFlagRepository.save(featureFlag);
        } else {
            FeatureFlag featureFlag = new FeatureFlag();
            featureFlag.setId(UUID.randomUUID());
            featureFlag.setKey(key);
            featureFlag.setEnabled(enabled);
            featureFlag.setDescription(description);
            featureFlag.setGlobal(true);
            featureFlag.setTeamId(null);
            featureFlag.setRolloutPercentage(rolloutPercentage);
            featureFlag.setExpiresAt(expiresAt);
            
            logger.info("Creating global feature flag with key: {}", key);
            return featureFlagRepository.save(featureFlag);
        }
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
    @Transactional
    public FeatureFlag setTeamFeatureFlag(UUID teamId, String key, boolean enabled, String description, 
                                        int rolloutPercentage, LocalDateTime expiresAt) {
        Optional<FeatureFlag> existingFlag = featureFlagRepository.findByTeamIdAndKey(teamId, key);
        
        if (existingFlag.isPresent()) {
            FeatureFlag featureFlag = existingFlag.get();
            featureFlag.setEnabled(enabled);
            featureFlag.setDescription(description);
            featureFlag.setGlobal(false);
            featureFlag.setRolloutPercentage(rolloutPercentage);
            featureFlag.setExpiresAt(expiresAt);
            
            logger.info("Updating team feature flag with key: {} for team ID: {}", key, teamId);
            return featureFlagRepository.save(featureFlag);
        } else {
            FeatureFlag featureFlag = new FeatureFlag();
            featureFlag.setId(UUID.randomUUID());
            featureFlag.setKey(key);
            featureFlag.setEnabled(enabled);
            featureFlag.setDescription(description);
            featureFlag.setGlobal(false);
            featureFlag.setTeamId(teamId);
            featureFlag.setRolloutPercentage(rolloutPercentage);
            featureFlag.setExpiresAt(expiresAt);
            
            logger.info("Creating team feature flag with key: {} for team ID: {}", key, teamId);
            return featureFlagRepository.save(featureFlag);
        }
    }

    /**
     * Enable a feature flag.
     *
     * @param id the feature flag ID
     * @return the updated feature flag
     * @throws ResourceNotFoundException if the feature flag is not found
     */
    @Transactional
    public FeatureFlag enableFeatureFlag(UUID id) {
        FeatureFlag featureFlag = getFeatureFlagById(id);
        featureFlag.setEnabled(true);
        
        logger.info("Enabling feature flag with key: {}", featureFlag.getKey());
        return featureFlagRepository.save(featureFlag);
    }

    /**
     * Disable a feature flag.
     *
     * @param id the feature flag ID
     * @return the updated feature flag
     * @throws ResourceNotFoundException if the feature flag is not found
     */
    @Transactional
    public FeatureFlag disableFeatureFlag(UUID id) {
        FeatureFlag featureFlag = getFeatureFlagById(id);
        featureFlag.setEnabled(false);
        
        logger.info("Disabling feature flag with key: {}", featureFlag.getKey());
        return featureFlagRepository.save(featureFlag);
    }

    /**
     * Delete a feature flag.
     *
     * @param id the feature flag ID
     * @return true if the feature flag was deleted, false otherwise
     */
    @Transactional
    public boolean deleteFeatureFlag(UUID id) {
        Optional<FeatureFlag> featureFlag = featureFlagRepository.findById(id);
        if (featureFlag.isPresent()) {
            featureFlagRepository.delete(featureFlag.get());
            logger.info("Deleted feature flag with ID: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Check if a feature flag is enabled.
     *
     * @param key the feature flag key
     * @return true if the feature flag is enabled, false otherwise
     */
    public boolean isFeatureFlagEnabled(String key) {
        Optional<FeatureFlag> featureFlag = featureFlagRepository.findByKey(key);
        return featureFlag.isPresent() && featureFlag.get().isEnabled();
    }

    /**
     * Check if a team feature flag is enabled.
     *
     * @param teamId the team ID
     * @param key the feature flag key
     * @return true if the feature flag is enabled, false otherwise
     */
    public boolean isTeamFeatureFlagEnabled(UUID teamId, String key) {
        Optional<FeatureFlag> featureFlag = featureFlagRepository.findByTeamIdAndKey(teamId, key);
        return featureFlag.isPresent() && featureFlag.get().isEnabled();
    }

    /**
     * Get feature flags as a map.
     *
     * @return map of feature flag keys to enabled status
     */
    public Map<String, Boolean> getFeatureFlagsAsMap() {
        List<FeatureFlag> featureFlags = featureFlagRepository.findAll();
        return featureFlags.stream()
                .collect(Collectors.toMap(FeatureFlag::getKey, FeatureFlag::isEnabled));
    }

    /**
     * Get global feature flags as a map.
     *
     * @return map of feature flag keys to enabled status
     */
    public Map<String, Boolean> getGlobalFeatureFlagsAsMap() {
        List<FeatureFlag> featureFlags = featureFlagRepository.findByIsGlobal(true);
        return featureFlags.stream()
                .collect(Collectors.toMap(FeatureFlag::getKey, FeatureFlag::isEnabled));
    }

    /**
     * Get team feature flags as a map.
     *
     * @param teamId the team ID
     * @return map of feature flag keys to enabled status
     */
    public Map<String, Boolean> getTeamFeatureFlagsAsMap(UUID teamId) {
        List<FeatureFlag> featureFlags = featureFlagRepository.findByTeamId(teamId);
        return featureFlags.stream()
                .collect(Collectors.toMap(FeatureFlag::getKey, FeatureFlag::isEnabled));
    }

    /**
     * Clean up expired feature flags.
     *
     * @return the number of deleted feature flags
     */
    @Transactional
    public int cleanupExpiredFeatureFlags() {
        List<FeatureFlag> expiredFlags = featureFlagRepository.findByExpiresAtBefore(LocalDateTime.now());
        featureFlagRepository.deleteAll(expiredFlags);
        
        logger.info("Cleaned up {} expired feature flags", expiredFlags.size());
        return expiredFlags.size();
    }
}

