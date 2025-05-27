package com.codebridge.usermanagement.repository;

import com.codebridge.core.repository.BaseRepository;
import com.codebridge.usermanagement.model.FeatureFlag;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for FeatureFlag entity operations.
 */
@Repository
public interface FeatureFlagRepository extends BaseRepository<FeatureFlag, UUID> {

    /**
     * Find a feature flag by key.
     *
     * @param key the feature flag key
     * @return the feature flag if found
     */
    Optional<FeatureFlag> findByKey(String key);

    /**
     * Find all enabled feature flags.
     *
     * @param enabled the enabled status
     * @return list of enabled feature flags
     */
    List<FeatureFlag> findByEnabled(boolean enabled);

    /**
     * Find all global feature flags.
     *
     * @param isGlobal the global status
     * @return list of global feature flags
     */
    List<FeatureFlag> findByIsGlobal(boolean isGlobal);

    /**
     * Find all feature flags for a specific team.
     *
     * @param teamId the team ID
     * @return list of team feature flags
     */
    List<FeatureFlag> findByTeamId(UUID teamId);

    /**
     * Find all enabled feature flags for a specific team.
     *
     * @param teamId the team ID
     * @param enabled the enabled status
     * @return list of enabled team feature flags
     */
    List<FeatureFlag> findByTeamIdAndEnabled(UUID teamId, boolean enabled);

    /**
     * Find a specific feature flag for a team.
     *
     * @param teamId the team ID
     * @param key the feature flag key
     * @return the feature flag if found
     */
    Optional<FeatureFlag> findByTeamIdAndKey(UUID teamId, String key);

    /**
     * Find all feature flags that expire after a specific date.
     *
     * @param dateTime the date time
     * @return list of feature flags
     */
    List<FeatureFlag> findByExpiresAtAfter(LocalDateTime dateTime);

    /**
     * Find all feature flags that expire before a specific date.
     *
     * @param dateTime the date time
     * @return list of feature flags
     */
    List<FeatureFlag> findByExpiresAtBefore(LocalDateTime dateTime);

    /**
     * Find all feature flags with a specific rollout percentage.
     *
     * @param rolloutPercentage the rollout percentage
     * @return list of feature flags
     */
    List<FeatureFlag> findByRolloutPercentage(int rolloutPercentage);
}

