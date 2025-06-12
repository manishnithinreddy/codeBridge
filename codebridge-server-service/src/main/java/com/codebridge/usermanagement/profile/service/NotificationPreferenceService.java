package com.codebridge.usermanagement.profile.service;

import com.codebridge.usermanagement.profile.model.NotificationPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing user notification preferences.
 * This is a mock implementation that stores preferences in memory.
 * In a real implementation, this would use a database repository.
 */
@Service
public class NotificationPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceService.class);
    
    // In-memory store for notification preferences (userId -> eventType -> preference)
    private final Map<UUID, Map<String, NotificationPreference>> userPreferences = new ConcurrentHashMap<>();

    /**
     * Checks if security notifications are enabled for a user.
     *
     * @param userId the user ID
     * @return true if enabled, false otherwise
     */
    public boolean isSecurityNotificationEnabled(UUID userId) {
        // Default to true for security notifications
        return true;
    }
    
    /**
     * Checks if server status notifications are enabled for a user.
     *
     * @param userId the user ID
     * @return true if enabled, false otherwise
     */
    public boolean isServerStatusNotificationEnabled(UUID userId) {
        // Default to true for server status notifications
        return true;
    }

    /**
     * Gets a user's notification preference for a specific event type.
     * If no preference exists, creates a default preference.
     *
     * @param userId the user ID
     * @param eventType the event type
     * @return the notification preference
     */
    public NotificationPreference getUserNotificationPreference(UUID userId, String eventType) {
        // Get or create user's preferences map
        Map<String, NotificationPreference> preferences = userPreferences.computeIfAbsent(userId, k -> new HashMap<>());
        
        // Get or create preference for this event type
        return preferences.computeIfAbsent(eventType, k -> {
            logger.info("Creating default notification preference for user {} and event type {}", userId, eventType);
            return new NotificationPreference(userId, eventType);
        });
    }

    /**
     * Updates a user's notification preference.
     *
     * @param preference the updated preference
     * @return the updated preference
     */
    public NotificationPreference updateNotificationPreference(NotificationPreference preference) {
        UUID userId = preference.getUserId();
        String eventType = preference.getEventType();
        
        // Get or create user's preferences map
        Map<String, NotificationPreference> preferences = userPreferences.computeIfAbsent(userId, k -> new HashMap<>());
        
        // Update preference
        preferences.put(eventType, preference);
        logger.info("Updated notification preference for user {} and event type {}", userId, eventType);
        
        return preference;
    }

    /**
     * Deletes a user's notification preference for a specific event type.
     *
     * @param userId the user ID
     * @param eventType the event type
     */
    public void deleteNotificationPreference(UUID userId, String eventType) {
        Map<String, NotificationPreference> preferences = userPreferences.get(userId);
        if (preferences != null) {
            preferences.remove(eventType);
            logger.info("Deleted notification preference for user {} and event type {}", userId, eventType);
        }
    }

    /**
     * Deletes all notification preferences for a user.
     *
     * @param userId the user ID
     */
    public void deleteAllUserNotificationPreferences(UUID userId) {
        userPreferences.remove(userId);
        logger.info("Deleted all notification preferences for user {}", userId);
    }
}

