package com.codebridge.usermanagement.profile.model;

import java.util.Map;
import java.util.UUID;

/**
 * Model class representing a user's notification preferences for a specific event type.
 */
public class NotificationPreference {
    private UUID id;
    private UUID userId;
    private String eventType;
    private boolean emailEnabled;
    private boolean pushEnabled;
    private boolean inAppEnabled;
    private boolean slackEnabled;
    private String customChannel;
    private Map<String, Object> customChannelConfig;

    /**
     * Default constructor.
     */
    public NotificationPreference() {
        // Default constructor
        this.id = UUID.randomUUID();
        this.emailEnabled = true;
        this.pushEnabled = true;
        this.inAppEnabled = true;
        this.slackEnabled = false;
    }

    /**
     * Constructor with userId and eventType.
     *
     * @param userId the user ID
     * @param eventType the event type
     */
    public NotificationPreference(UUID userId, String eventType) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.eventType = eventType;
        // Default to all channels enabled
        this.emailEnabled = true;
        this.pushEnabled = true;
        this.inAppEnabled = true;
        this.slackEnabled = false;
        this.customChannel = null;
        this.customChannelConfig = null;
    }

    /**
     * Constructor with status.
     *
     * @param status the status (e.g., "enabled", "disabled")
     */
    public NotificationPreference(String status) {
        this.id = UUID.randomUUID();
        this.userId = UUID.randomUUID(); // Dummy user ID
        this.eventType = "DEFAULT";
        // Set channels based on status
        boolean enabled = "enabled".equalsIgnoreCase(status);
        this.emailEnabled = enabled;
        this.pushEnabled = enabled;
        this.inAppEnabled = enabled;
        this.slackEnabled = false;
        this.customChannel = null;
        this.customChannelConfig = null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public boolean isSlackEnabled() {
        return slackEnabled;
    }

    public void setSlackEnabled(boolean slackEnabled) {
        this.slackEnabled = slackEnabled;
    }

    public String getCustomChannel() {
        return customChannel;
    }

    public void setCustomChannel(String customChannel) {
        this.customChannel = customChannel;
    }

    public Map<String, Object> getCustomChannelConfig() {
        return customChannelConfig;
    }

    public void setCustomChannelConfig(Map<String, Object> customChannelConfig) {
        this.customChannelConfig = customChannelConfig;
    }
}

