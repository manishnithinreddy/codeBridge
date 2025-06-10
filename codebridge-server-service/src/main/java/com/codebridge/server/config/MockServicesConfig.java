package com.codebridge.server.config;

import com.codebridge.usermanagement.profile.model.NotificationPreference;
import com.codebridge.usermanagement.profile.service.NotificationPreferenceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration class for mock services.
 * This provides mock implementations of external services for testing and development.
 */
@Configuration
public class MockServicesConfig {

    /**
     * Creates a mock NotificationPreferenceService.
     *
     * @return the mock service
     */
    @Bean
    @Primary
    public NotificationPreferenceService notificationPreferenceService() {
        return new NotificationPreferenceService();
    }
}

