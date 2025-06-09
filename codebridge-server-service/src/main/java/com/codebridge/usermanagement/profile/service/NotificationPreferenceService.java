package com.codebridge.usermanagement.profile.service;

import com.codebridge.usermanagement.profile.model.NotificationPreference;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing user notification preferences.
 */
@Service
public class NotificationPreferenceService {

    // In-memory storage for notification preferences (would be replaced with database in production)
    private final Map<String, NotificationPreference> preferences = new ConcurrentHashMap<>();

    /**
     * Gets a user's notification preference for a specific event type.
     * If no preference exists, creates a default one.
     *
     * @param userId the user ID
     * @param eventType the event type
     * @return the notification preference
     */
    public NotificationPreference getUserNotificationPreference(UUID userId, String eventType) {
        String key = userId.toString() + ":" + eventType;
        
        // Return existing preference if found
        if (preferences.containsKey(key)) {
            return preferences.get(key);
        }
        
        // Create default preference if not found
        NotificationPreference defaultPreference = new NotificationPreference(userId, eventType);
        preferences.put(key, defaultPreference);
        return defaultPreference;
    }

    /**
     * Updates a user's notification preference.
     *
     * @param preference the notification preference to update
     * @return the updated notification preference
     */
    public NotificationPreference updateNotificationPreference(NotificationPreference preference) {
        String key = preference.getUserId().toString() + ":" + preference.getEventType();
        preferences.put(key, preference);
        return preference;
    }

    /**
     * Deletes a user's notification preference.
     *
     * @param userId the user ID
     * @param eventType the event type
     */
    public void deleteNotificationPreference(UUID userId, String eventType) {
        String key = userId.toString() + ":" + eventType;
        preferences.remove(key);
    }
}

