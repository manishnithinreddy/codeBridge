package com.codebridge.usermanagement.profile.service;

import com.codebridge.usermanagement.profile.model.NotificationPreference;
import com.codebridge.usermanagement.profile.repository.NotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing user notification preferences.
 */
@Service
public class NotificationPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceService.class);

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    public NotificationPreferenceService(NotificationPreferenceRepository notificationPreferenceRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    /**
     * Gets a user's notification preference for a specific event type.
     * If no preference exists, a default preference is created and returned.
     *
     * @param userId the user ID
     * @param eventType the event type
     * @return the notification preference
     */
    @Transactional
    public NotificationPreference getUserNotificationPreference(UUID userId, String eventType) {
        return notificationPreferenceRepository.findByUserIdAndEventType(userId, eventType)
                .orElseGet(() -> createDefaultPreference(userId, eventType));
    }

    /**
     * Gets all notification preferences for a user.
     *
     * @param userId the user ID
     * @return the list of notification preferences
     */
    @Transactional(readOnly = true)
    public List<NotificationPreference> getUserNotificationPreferences(UUID userId) {
        return notificationPreferenceRepository.findByUserId(userId);
    }

    /**
     * Updates a user's notification preference.
     *
     * @param preference the notification preference to update
     * @return the updated notification preference
     */
    @Transactional
    public NotificationPreference updateNotificationPreference(NotificationPreference preference) {
        // Ensure the preference exists
        NotificationPreference existingPreference = getUserNotificationPreference(
                preference.getUserId(), preference.getEventType());
        
        // Update the preference
        existingPreference.setEmailEnabled(preference.isEmailEnabled());
        existingPreference.setPushEnabled(preference.isPushEnabled());
        existingPreference.setInAppEnabled(preference.isInAppEnabled());
        existingPreference.setSlackEnabled(preference.isSlackEnabled());
        existingPreference.setCustomChannel(preference.getCustomChannel());
        existingPreference.setCustomChannelConfig(preference.getCustomChannelConfig());
        
        return notificationPreferenceRepository.save(existingPreference);
    }

    /**
     * Creates a default notification preference for a user and event type.
     *
     * @param userId the user ID
     * @param eventType the event type
     * @return the created notification preference
     */
    private NotificationPreference createDefaultPreference(UUID userId, String eventType) {
        NotificationPreference preference = new NotificationPreference(userId, eventType);
        
        // Set default values
        preference.setEmailEnabled(true);
        preference.setPushEnabled(true);
        preference.setInAppEnabled(true);
        preference.setSlackEnabled(false);
        
        logger.info("Creating default notification preference for user {} and event type {}", userId, eventType);
        return notificationPreferenceRepository.save(preference);
    }
}

