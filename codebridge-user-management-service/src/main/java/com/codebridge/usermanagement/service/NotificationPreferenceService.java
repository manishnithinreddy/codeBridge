package com.codebridge.usermanagement.service;

import com.codebridge.usermanagement.exception.ResourceNotFoundException;
import com.codebridge.usermanagement.model.NotificationPreference;
import com.codebridge.usermanagement.repository.NotificationPreferenceRepository;
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
 * Service for notification preference operations.
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
     * Get all notification preferences for a user.
     *
     * @param userId the user ID
     * @return list of notification preferences
     */
    public List<NotificationPreference> getUserNotificationPreferences(UUID userId) {
        return notificationPreferenceRepository.findByUserId(userId);
    }

    /**
     * Get a specific notification preference for a user.
     *
     * @param userId the user ID
     * @param eventType the event type
     * @return the notification preference
     * @throws ResourceNotFoundException if the preference is not found
     */
    public NotificationPreference getUserNotificationPreference(UUID userId, String eventType) {
        return notificationPreferenceRepository.findByUserIdAndEventType(userId, eventType)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationPreference", "eventType", eventType));
    }

    /**
     * Create a new notification preference.
     *
     * @param notificationPreference the notification preference to create
     * @return the created notification preference
     */
    @Transactional
    public NotificationPreference createNotificationPreference(NotificationPreference notificationPreference) {
        if (notificationPreference.getId() == null) {
            notificationPreference.setId(UUID.randomUUID());
        }
        
        logger.info("Creating notification preference for user ID: {}, event type: {}", 
                notificationPreference.getUserId(), notificationPreference.getEventType());
        return notificationPreferenceRepository.save(notificationPreference);
    }

    /**
     * Update a notification preference.
     *
     * @param id the notification preference ID
     * @param notificationPreferenceDetails the notification preference details
     * @return the updated notification preference
     * @throws ResourceNotFoundException if the preference is not found
     */
    @Transactional
    public NotificationPreference updateNotificationPreference(UUID id, NotificationPreference notificationPreferenceDetails) {
        NotificationPreference notificationPreference = notificationPreferenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationPreference", "id", id));
        
        notificationPreference.setEmailEnabled(notificationPreferenceDetails.isEmailEnabled());
        notificationPreference.setPushEnabled(notificationPreferenceDetails.isPushEnabled());
        notificationPreference.setInAppEnabled(notificationPreferenceDetails.isInAppEnabled());
        notificationPreference.setSlackEnabled(notificationPreferenceDetails.isSlackEnabled());
        notificationPreference.setCustomChannel(notificationPreferenceDetails.getCustomChannel());
        notificationPreference.setCustomChannelConfig(notificationPreferenceDetails.getCustomChannelConfig());
        
        logger.info("Updating notification preference for user ID: {}, event type: {}", 
                notificationPreference.getUserId(), notificationPreference.getEventType());
        return notificationPreferenceRepository.save(notificationPreference);
    }

    /**
     * Set a notification preference.
     *
     * @param userId the user ID
     * @param eventType the event type
     * @param emailEnabled the email enabled status
     * @param pushEnabled the push enabled status
     * @param inAppEnabled the in-app enabled status
     * @param slackEnabled the Slack enabled status
     * @return the updated or created notification preference
     */
    @Transactional
    public NotificationPreference setNotificationPreference(UUID userId, String eventType, 
                                                           boolean emailEnabled, boolean pushEnabled, 
                                                           boolean inAppEnabled, boolean slackEnabled) {
        Optional<NotificationPreference> existingPreference = notificationPreferenceRepository.findByUserIdAndEventType(userId, eventType);
        
        if (existingPreference.isPresent()) {
            NotificationPreference notificationPreference = existingPreference.get();
            notificationPreference.setEmailEnabled(emailEnabled);
            notificationPreference.setPushEnabled(pushEnabled);
            notificationPreference.setInAppEnabled(inAppEnabled);
            notificationPreference.setSlackEnabled(slackEnabled);
            
            logger.info("Updating notification preference for user ID: {}, event type: {}", userId, eventType);
            return notificationPreferenceRepository.save(notificationPreference);
        } else {
            NotificationPreference notificationPreference = new NotificationPreference();
            notificationPreference.setId(UUID.randomUUID());
            notificationPreference.setUserId(userId);
            notificationPreference.setEventType(eventType);
            notificationPreference.setEmailEnabled(emailEnabled);
            notificationPreference.setPushEnabled(pushEnabled);
            notificationPreference.setInAppEnabled(inAppEnabled);
            notificationPreference.setSlackEnabled(slackEnabled);
            
            logger.info("Creating notification preference for user ID: {}, event type: {}", userId, eventType);
            return notificationPreferenceRepository.save(notificationPreference);
        }
    }

    /**
     * Set a custom channel for a notification preference.
     *
     * @param userId the user ID
     * @param eventType the event type
     * @param customChannel the custom channel
     * @param customChannelConfig the custom channel configuration
     * @return the updated or created notification preference
     */
    @Transactional
    public NotificationPreference setCustomChannel(UUID userId, String eventType, 
                                                 String customChannel, String customChannelConfig) {
        NotificationPreference notificationPreference = getUserNotificationPreference(userId, eventType);
        notificationPreference.setCustomChannel(customChannel);
        notificationPreference.setCustomChannelConfig(customChannelConfig);
        
        logger.info("Setting custom channel for user ID: {}, event type: {}", userId, eventType);
        return notificationPreferenceRepository.save(notificationPreference);
    }

    /**
     * Delete a notification preference.
     *
     * @param id the notification preference ID
     * @return true if the preference was deleted, false otherwise
     */
    @Transactional
    public boolean deleteNotificationPreference(UUID id) {
        Optional<NotificationPreference> notificationPreference = notificationPreferenceRepository.findById(id);
        if (notificationPreference.isPresent()) {
            notificationPreferenceRepository.delete(notificationPreference.get());
            logger.info("Deleted notification preference with ID: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Delete all notification preferences for a user.
     *
     * @param userId the user ID
     * @return the number of deleted preferences
     */
    @Transactional
    public int deleteAllUserNotificationPreferences(UUID userId) {
        int count = notificationPreferenceRepository.deleteByUserId(userId);
        logger.info("Deleted {} notification preferences for user ID: {}", count, userId);
        return count;
    }

    /**
     * Get notification preferences as a map.
     *
     * @param userId the user ID
     * @return map of event types to notification preferences
     */
    public Map<String, NotificationPreference> getNotificationPreferencesAsMap(UUID userId) {
        List<NotificationPreference> preferences = notificationPreferenceRepository.findByUserId(userId);
        return preferences.stream()
                .collect(Collectors.toMap(NotificationPreference::getEventType, preference -> preference));
    }

    /**
     * Get notification preferences for a specific channel.
     *
     * @param userId the user ID
     * @param emailEnabled the email enabled status
     * @return list of notification preferences
     */
    public List<NotificationPreference> getEmailNotificationPreferences(UUID userId, boolean emailEnabled) {
        return notificationPreferenceRepository.findByUserIdAndEmailEnabled(userId, emailEnabled);
    }

    /**
     * Get push notification preferences.
     *
     * @param userId the user ID
     * @param pushEnabled the push enabled status
     * @return list of notification preferences
     */
    public List<NotificationPreference> getPushNotificationPreferences(UUID userId, boolean pushEnabled) {
        return notificationPreferenceRepository.findByUserIdAndPushEnabled(userId, pushEnabled);
    }

    /**
     * Get in-app notification preferences.
     *
     * @param userId the user ID
     * @param inAppEnabled the in-app enabled status
     * @return list of notification preferences
     */
    public List<NotificationPreference> getInAppNotificationPreferences(UUID userId, boolean inAppEnabled) {
        return notificationPreferenceRepository.findByUserIdAndInAppEnabled(userId, inAppEnabled);
    }

    /**
     * Get Slack notification preferences.
     *
     * @param userId the user ID
     * @param slackEnabled the Slack enabled status
     * @return list of notification preferences
     */
    public List<NotificationPreference> getSlackNotificationPreferences(UUID userId, boolean slackEnabled) {
        return notificationPreferenceRepository.findByUserIdAndSlackEnabled(userId, slackEnabled);
    }

    /**
     * Set multiple notification preferences at once.
     *
     * @param userId the user ID
     * @param preferences map of event types to channel settings
     * @return list of updated or created notification preferences
     */
    @Transactional
    public List<NotificationPreference> setNotificationPreferences(UUID userId, 
                                                                 Map<String, Map<String, Boolean>> preferences) {
        return preferences.entrySet().stream()
                .map(entry -> {
                    String eventType = entry.getKey();
                    Map<String, Boolean> channels = entry.getValue();
                    
                    boolean emailEnabled = channels.getOrDefault("email", false);
                    boolean pushEnabled = channels.getOrDefault("push", false);
                    boolean inAppEnabled = channels.getOrDefault("inApp", false);
                    boolean slackEnabled = channels.getOrDefault("slack", false);
                    
                    return setNotificationPreference(userId, eventType, emailEnabled, pushEnabled, inAppEnabled, slackEnabled);
                })
                .collect(Collectors.toList());
    }
}

