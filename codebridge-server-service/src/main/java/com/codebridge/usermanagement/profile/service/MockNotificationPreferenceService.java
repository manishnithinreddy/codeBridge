package com.codebridge.usermanagement.profile.service;

import com.codebridge.usermanagement.profile.model.NotificationPreference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Mock implementation of NotificationPreferenceService for testing
 */
@Service
@Profile("test")
public class MockNotificationPreferenceService {
    
    /**
     * Checks if security notifications are enabled for a user.
     *
     * @param userId the user ID
     * @return true if enabled, false otherwise
     */
    public boolean isSecurityNotificationEnabled(UUID userId) {
        // Always return true for testing
        return true;
    }
    
    /**
     * Checks if server status notifications are enabled for a user.
     *
     * @param userId the user ID
     * @return true if enabled, false otherwise
     */
    public boolean isServerStatusNotificationEnabled(UUID userId) {
        // Always return true for testing
        return true;
    }
    
    /**
     * Gets a user's notification preference for a specific event type.
     *
     * @param userId the user ID
     * @param notificationType the notification type
     * @return the notification preference
     */
    public NotificationPreference getUserNotificationPreference(UUID userId, String notificationType) {
        // Return "enabled" for all notification types in test mode
        return new NotificationPreference("enabled");
    }
}

