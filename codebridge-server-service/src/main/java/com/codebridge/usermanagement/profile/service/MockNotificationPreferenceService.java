package com.codebridge.usermanagement.profile.service;

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
}

