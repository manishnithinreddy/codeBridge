package com.codebridge.usermanagement.service;

import com.codebridge.usermanagement.exception.ResourceNotFoundException;
import com.codebridge.usermanagement.model.PreferenceType;
import com.codebridge.usermanagement.model.UserPreference;
import com.codebridge.usermanagement.repository.UserPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for user preference operations.
 */
@Service
public class UserPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferenceService.class);

    private final UserPreferenceRepository userPreferenceRepository;

    @Autowired
    public UserPreferenceService(UserPreferenceRepository userPreferenceRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
    }

    /**
     * Get all preferences for a user.
     *
     * @param userId the user ID
     * @return list of user preferences
     */
    public List<UserPreference> getUserPreferences(UUID userId) {
        return userPreferenceRepository.findByUserId(userId);
    }

    /**
     * Get all preferences for a user in a specific category.
     *
     * @param userId the user ID
     * @param category the category
     * @return list of user preferences
     */
    public List<UserPreference> getUserPreferencesByCategory(UUID userId, String category) {
        return userPreferenceRepository.findByUserIdAndCategory(userId, category);
    }

    /**
     * Get a specific preference for a user.
     *
     * @param userId the user ID
     * @param key the preference key
     * @return the user preference
     * @throws ResourceNotFoundException if the preference is not found
     */
    public UserPreference getUserPreference(UUID userId, String key) {
        return userPreferenceRepository.findByUserIdAndKey(userId, key)
                .orElseThrow(() -> new ResourceNotFoundException("UserPreference", "key", key));
    }

    /**
     * Get a specific preference for a user in a specific category.
     *
     * @param userId the user ID
     * @param category the category
     * @param key the preference key
     * @return the user preference
     * @throws ResourceNotFoundException if the preference is not found
     */
    public UserPreference getUserPreference(UUID userId, String category, String key) {
        return userPreferenceRepository.findByUserIdAndCategoryAndKey(userId, category, key)
                .orElseThrow(() -> new ResourceNotFoundException("UserPreference", "key", key));
    }

    /**
     * Create a new user preference.
     *
     * @param userPreference the user preference to create
     * @return the created user preference
     */
    @Transactional
    public UserPreference createUserPreference(UserPreference userPreference) {
        if (userPreference.getId() == null) {
            userPreference.setId(UUID.randomUUID());
        }
        
        logger.info("Creating user preference for user ID: {}, key: {}", userPreference.getUserId(), userPreference.getKey());
        return userPreferenceRepository.save(userPreference);
    }

    /**
     * Update a user preference.
     *
     * @param id the user preference ID
     * @param userPreferenceDetails the user preference details
     * @return the updated user preference
     * @throws ResourceNotFoundException if the preference is not found
     */
    @Transactional
    public UserPreference updateUserPreference(UUID id, UserPreference userPreferenceDetails) {
        UserPreference userPreference = userPreferenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserPreference", "id", id));
        
        userPreference.setValue(userPreferenceDetails.getValue());
        userPreference.setDescription(userPreferenceDetails.getDescription());
        userPreference.setType(userPreferenceDetails.getType());
        
        logger.info("Updating user preference for user ID: {}, key: {}", userPreference.getUserId(), userPreference.getKey());
        return userPreferenceRepository.save(userPreference);
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
    @Transactional
    public UserPreference setUserPreference(UUID userId, String category, String key, String value, PreferenceType type, String description) {
        Optional<UserPreference> existingPreference = userPreferenceRepository.findByUserIdAndCategoryAndKey(userId, category, key);
        
        if (existingPreference.isPresent()) {
            UserPreference userPreference = existingPreference.get();
            userPreference.setValue(value);
            userPreference.setType(type);
            userPreference.setDescription(description);
            
            logger.info("Updating user preference for user ID: {}, key: {}", userId, key);
            return userPreferenceRepository.save(userPreference);
        } else {
            UserPreference userPreference = new UserPreference();
            userPreference.setId(UUID.randomUUID());
            userPreference.setUserId(userId);
            userPreference.setCategory(category);
            userPreference.setKey(key);
            userPreference.setValue(value);
            userPreference.setType(type);
            userPreference.setDescription(description);
            
            logger.info("Creating user preference for user ID: {}, key: {}", userId, key);
            return userPreferenceRepository.save(userPreference);
        }
    }

    /**
     * Delete a user preference.
     *
     * @param id the user preference ID
     * @return true if the preference was deleted, false otherwise
     */
    @Transactional
    public boolean deleteUserPreference(UUID id) {
        Optional<UserPreference> userPreference = userPreferenceRepository.findById(id);
        if (userPreference.isPresent()) {
            userPreferenceRepository.delete(userPreference.get());
            logger.info("Deleted user preference with ID: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Delete a specific preference for a user.
     *
     * @param userId the user ID
     * @param key the preference key
     * @return true if the preference was deleted, false otherwise
     */
    @Transactional
    public boolean deleteUserPreference(UUID userId, String key) {
        Optional<UserPreference> userPreference = userPreferenceRepository.findByUserIdAndKey(userId, key);
        if (userPreference.isPresent()) {
            userPreferenceRepository.delete(userPreference.get());
            logger.info("Deleted user preference for user ID: {}, key: {}", userId, key);
            return true;
        }
        return false;
    }

    /**
     * Delete all preferences for a user.
     *
     * @param userId the user ID
     * @return the number of deleted preferences
     */
    @Transactional
    public int deleteAllUserPreferences(UUID userId) {
        int count = userPreferenceRepository.deleteByUserId(userId);
        logger.info("Deleted {} preferences for user ID: {}", count, userId);
        return count;
    }

    /**
     * Delete all preferences for a user in a specific category.
     *
     * @param userId the user ID
     * @param category the category
     * @return the number of deleted preferences
     */
    @Transactional
    public int deleteUserPreferencesByCategory(UUID userId, String category) {
        int count = userPreferenceRepository.deleteByUserIdAndCategory(userId, category);
        logger.info("Deleted {} preferences for user ID: {} in category: {}", count, userId, category);
        return count;
    }

    /**
     * Get user preferences as a map.
     *
     * @param userId the user ID
     * @return map of preference keys to values
     */
    public Map<String, String> getUserPreferencesAsMap(UUID userId) {
        List<UserPreference> preferences = userPreferenceRepository.findByUserId(userId);
        return preferences.stream()
                .collect(Collectors.toMap(UserPreference::getKey, UserPreference::getValue));
    }

    /**
     * Get user preferences in a specific category as a map.
     *
     * @param userId the user ID
     * @param category the category
     * @return map of preference keys to values
     */
    public Map<String, String> getUserPreferencesAsMap(UUID userId, String category) {
        List<UserPreference> preferences = userPreferenceRepository.findByUserIdAndCategory(userId, category);
        return preferences.stream()
                .collect(Collectors.toMap(UserPreference::getKey, UserPreference::getValue));
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
    @Transactional
    public List<UserPreference> setUserPreferences(UUID userId, String category, Map<String, String> preferences, PreferenceType type) {
        return preferences.entrySet().stream()
                .map(entry -> setUserPreference(userId, category, entry.getKey(), entry.getValue(), type, null))
                .collect(Collectors.toList());
    }
}

