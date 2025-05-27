package com.codebridge.usermanagement.repository;

import com.codebridge.core.repository.BaseRepository;
import com.codebridge.usermanagement.model.UserPreference;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserPreference entity operations.
 */
@Repository
public interface UserPreferenceRepository extends BaseRepository<UserPreference, UUID> {

    /**
     * Find all preferences for a user.
     *
     * @param userId the user ID
     * @return list of user preferences
     */
    List<UserPreference> findByUserId(UUID userId);

    /**
     * Find all preferences for a user in a specific category.
     *
     * @param userId the user ID
     * @param category the category
     * @return list of user preferences
     */
    List<UserPreference> findByUserIdAndCategory(UUID userId, String category);

    /**
     * Find a specific preference for a user.
     *
     * @param userId the user ID
     * @param key the preference key
     * @return the user preference if found
     */
    Optional<UserPreference> findByUserIdAndKey(UUID userId, String key);

    /**
     * Find a specific preference for a user in a specific category.
     *
     * @param userId the user ID
     * @param category the category
     * @param key the preference key
     * @return the user preference if found
     */
    Optional<UserPreference> findByUserIdAndCategoryAndKey(UUID userId, String category, String key);

    /**
     * Delete all preferences for a user.
     *
     * @param userId the user ID
     * @return the number of deleted preferences
     */
    int deleteByUserId(UUID userId);

    /**
     * Delete all preferences for a user in a specific category.
     *
     * @param userId the user ID
     * @param category the category
     * @return the number of deleted preferences
     */
    int deleteByUserIdAndCategory(UUID userId, String category);
}

