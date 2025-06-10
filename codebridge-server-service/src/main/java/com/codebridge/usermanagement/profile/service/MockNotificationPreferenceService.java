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
public class MockNotificationPreferenceService implements NotificationPreferenceService {
    
    @Override
    public boolean isSecurityNotificationEnabled(UUID userId) {
        // Always return true for testing
        return true;
    }
    
    @Override
    public boolean isServerStatusNotificationEnabled(UUID userId) {
        // Always return true for testing
        return true;
    }
    
    @Override
    public NotificationPreference getUserNotificationPreference(UUID userId, String notificationType) {
        // Return "enabled" for all notification types in test mode
        return new NotificationPreference("enabled");
    }
}

