package com.codebridge.usermanagement.profile.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Entity representing notification preferences.
 */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private boolean emailEnabled;

    @Column(nullable = false)
    private boolean pushEnabled;

    @Column(nullable = false)
    private boolean inAppEnabled;

    @Column(nullable = false)
    private boolean slackEnabled;

    @Column
    private String customChannel;

    @Column
    private String customChannelConfig;

    public NotificationPreference() {
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

    public String getCustomChannelConfig() {
        return customChannelConfig;
    }

    public void setCustomChannelConfig(String customChannelConfig) {
        this.customChannelConfig = customChannelConfig;
    }
}
