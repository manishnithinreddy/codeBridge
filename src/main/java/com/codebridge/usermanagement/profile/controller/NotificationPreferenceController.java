package com.codebridge.usermanagement.profile.controller;

import com.codebridge.usermanagement.profile.model.NotificationPreference;
import com.codebridge.usermanagement.profile.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for notification preference operations.
 */
@RestController
@RequestMapping("/notification-preferences")
public class NotificationPreferenceController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceController.class);

    private final NotificationPreferenceService notificationPreferenceService;

    @Autowired
    public NotificationPreferenceController(NotificationPreferenceService notificationPreferenceService) {
        this.notificationPreferenceService = notificationPreferenceService;
    }

    /**
     * Get all notification preferences for a user.
     *
     * @param userId the user ID
     * @return list of notification preferences
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationPreference>> getUserNotificationPreferences(@PathVariable UUID userId) {
        logger.info("Getting notification preferences for user ID: {}", userId);
        List<NotificationPreference> preferences = notificationPreferenceService.getUserNotificationPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Get a specific notification preference for a user.
     *
     * @param userId the user ID
     * @param eventType the event type
     * @return the notification preference
     */
    @GetMapping("/user/{userId}/event/{eventType}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<NotificationPreference> getUserNotificationPreference(
            @PathVariable UUID userId, @PathVariable String eventType) {
        logger.info("Getting notification preference for user ID: {}, event type: {}", userId, eventType);
        NotificationPreference preference = notificationPreferenceService.getUserNotificationPreference(userId, eventType);
        return ResponseEntity.ok(preference);
    }

    /**
     * Create a new notification preference.
     *
     * @param notificationPreference the notification preference to create
     * @return the created notification preference
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<NotificationPreference> createNotificationPreference(
            @Valid @RequestBody NotificationPreference notificationPreference) {
        logger.info("Creating notification preference for user ID: {}, event type: {}", 
                notificationPreference.getUserId(), notificationPreference.getEventType());
        NotificationPreference createdPreference = notificationPreferenceService.createNotificationPreference(notificationPreference);
        return new ResponseEntity<>(createdPreference, HttpStatus.CREATED);
    }

    /**
     * Update a notification preference.
     *
     * @param id the notification preference ID
     * @param notificationPreferenceDetails the notification preference details
     * @return the updated notification preference
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<NotificationPreference> updateNotificationPreference(
            @PathVariable UUID id, @Valid @RequestBody NotificationPreference notificationPreferenceDetails) {
        logger.info("Updating notification preference with ID: {}", id);
        NotificationPreference updatedPreference = notificationPreferenceService.updateNotificationPreference(
                id, notificationPreferenceDetails);
        return ResponseEntity.ok(updatedPreference);
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
    @PutMapping("/user/{userId}/event/{eventType}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<NotificationPreference> setNotificationPreference(
            @PathVariable UUID userId,
            @PathVariable String eventType,
            @RequestParam(required = false) Boolean emailEnabled,
            @RequestParam(required = false) Boolean pushEnabled,
            @RequestParam(required = false) Boolean inAppEnabled,
            @RequestParam(required = false) Boolean slackEnabled) {
        logger.info("Setting notification preference for user ID: {}, event type: {}", userId, eventType);
        
        // Default to false if not provided
        boolean email = emailEnabled != null ? emailEnabled : false;
        boolean push = pushEnabled != null ? pushEnabled : false;
        boolean inApp = inAppEnabled != null ? inAppEnabled : false;
        boolean slack = slackEnabled != null ? slackEnabled : false;
        
        NotificationPreference preference = notificationPreferenceService.setNotificationPreference(
                userId, eventType, email, push, inApp, slack);
        return ResponseEntity.ok(preference);
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
    @PutMapping("/user/{userId}/event/{eventType}/custom-channel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<NotificationPreference> setCustomChannel(
            @PathVariable UUID userId,
            @PathVariable String eventType,
            @RequestParam String customChannel,
            @RequestParam(required = false) String customChannelConfig) {
        logger.info("Setting custom channel for user ID: {}, event type: {}", userId, eventType);
        NotificationPreference preference = notificationPreferenceService.setCustomChannel(
                userId, eventType, customChannel, customChannelConfig);
        return ResponseEntity.ok(preference);
    }

    /**
     * Delete a notification preference.
     *
     * @param id the notification preference ID
     * @return no content if the preference was deleted
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNotificationPreference(@PathVariable UUID id) {
        logger.info("Deleting notification preference with ID: {}", id);
        boolean deleted = notificationPreferenceService.deleteNotificationPreference(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Delete all notification preferences for a user.
     *
     * @param userId the user ID
     * @return the number of deleted preferences
     */
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Integer> deleteAllUserNotificationPreferences(@PathVariable UUID userId) {
        logger.info("Deleting all notification preferences for user ID: {}", userId);
        int count = notificationPreferenceService.deleteAllUserNotificationPreferences(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get notification preferences as a map.
     *
     * @param userId the user ID
     * @return map of event types to notification preferences
     */
    @GetMapping("/user/{userId}/map")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, NotificationPreference>> getNotificationPreferencesAsMap(@PathVariable UUID userId) {
        logger.info("Getting notification preferences as map for user ID: {}", userId);
        Map<String, NotificationPreference> preferences = notificationPreferenceService.getNotificationPreferencesAsMap(userId);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Get notification preferences for a specific channel.
     *
     * @param userId the user ID
     * @param emailEnabled the email enabled status
     * @return list of notification preferences
     */
    @GetMapping("/user/{userId}/email")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationPreference>> getEmailNotificationPreferences(
            @PathVariable UUID userId, @RequestParam boolean emailEnabled) {
        logger.info("Getting email notification preferences for user ID: {}, enabled: {}", userId, emailEnabled);
        List<NotificationPreference> preferences = notificationPreferenceService.getEmailNotificationPreferences(
                userId, emailEnabled);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Get push notification preferences.
     *
     * @param userId the user ID
     * @param pushEnabled the push enabled status
     * @return list of notification preferences
     */
    @GetMapping("/user/{userId}/push")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationPreference>> getPushNotificationPreferences(
            @PathVariable UUID userId, @RequestParam boolean pushEnabled) {
        logger.info("Getting push notification preferences for user ID: {}, enabled: {}", userId, pushEnabled);
        List<NotificationPreference> preferences = notificationPreferenceService.getPushNotificationPreferences(
                userId, pushEnabled);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Get in-app notification preferences.
     *
     * @param userId the user ID
     * @param inAppEnabled the in-app enabled status
     * @return list of notification preferences
     */
    @GetMapping("/user/{userId}/in-app")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationPreference>> getInAppNotificationPreferences(
            @PathVariable UUID userId, @RequestParam boolean inAppEnabled) {
        logger.info("Getting in-app notification preferences for user ID: {}, enabled: {}", userId, inAppEnabled);
        List<NotificationPreference> preferences = notificationPreferenceService.getInAppNotificationPreferences(
                userId, inAppEnabled);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Get Slack notification preferences.
     *
     * @param userId the user ID
     * @param slackEnabled the Slack enabled status
     * @return list of notification preferences
     */
    @GetMapping("/user/{userId}/slack")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationPreference>> getSlackNotificationPreferences(
            @PathVariable UUID userId, @RequestParam boolean slackEnabled) {
        logger.info("Getting Slack notification preferences for user ID: {}, enabled: {}", userId, slackEnabled);
        List<NotificationPreference> preferences = notificationPreferenceService.getSlackNotificationPreferences(
                userId, slackEnabled);
        return ResponseEntity.ok(preferences);
    }

    /**
     * Set multiple notification preferences at once.
     *
     * @param userId the user ID
     * @param preferences map of event types to channel settings
     * @return list of updated or created notification preferences
     */
    @PutMapping("/user/{userId}/batch")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationPreference>> setNotificationPreferences(
            @PathVariable UUID userId, @RequestBody Map<String, Map<String, Boolean>> preferences) {
        logger.info("Setting multiple notification preferences for user ID: {}", userId);
        List<NotificationPreference> updatedPreferences = notificationPreferenceService.setNotificationPreferences(
                userId, preferences);
        return ResponseEntity.ok(updatedPreferences);
    }
}
