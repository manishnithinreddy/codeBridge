package com.codebridge.usermanagement.profile.service;

import com.codebridge.usermanagement.profile.model.NotificationPreference;
import java.util.UUID;

/**
 * Service for managing user notification preferences
 */
public interface NotificationPreferenceService {
    
    /**
     * Check if a user has enabled security notifications
     * @param userId The user ID
     * @return true if security notifications are enabled, false otherwise
     */
    boolean isSecurityNotificationEnabled(UUID userId);
    
    /**
     * Check if a user has enabled server status notifications
     * @param userId The user ID
     * @return true if server status notifications are enabled, false otherwise
     */
    boolean isServerStatusNotificationEnabled(UUID userId);
    
    /**
     * Get a user's notification preference for a specific type
     * @param userId The user ID
     * @param notificationType The notification type
     * @return The notification preference object
     */
    NotificationPreference getUserNotificationPreference(UUID userId, String notificationType);
}

