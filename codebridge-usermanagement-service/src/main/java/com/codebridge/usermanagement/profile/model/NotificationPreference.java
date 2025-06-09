package com.codebridge.usermanagement.profile.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Entity for user notification preferences.
 */
@Entity
@Table(name = "notification_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_type"}))
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Size(max = 100)
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "slack_enabled", nullable = false)
    private boolean slackEnabled = false;

    @Size(max = 100)
    @Column(name = "custom_channel")
    private String customChannel;

    @Column(name = "custom_channel_config", columnDefinition = "TEXT")
    private String customChannelConfig;

    // Default constructor
    public NotificationPreference() {
    }

    // Constructor with required fields
    public NotificationPreference(UUID userId, String eventType) {
        this.userId = userId;
        this.eventType = eventType;
    }

    // Getters and setters
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

    public String getCustomChannelConfig() {
        return customChannelConfig;
    }

    public void setCustomChannelConfig(String customChannelConfig) {
        this.customChannelConfig = customChannelConfig;
    }
}

