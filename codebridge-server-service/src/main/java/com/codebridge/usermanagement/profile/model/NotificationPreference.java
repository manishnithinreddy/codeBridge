package com.codebridge.usermanagement.profile.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Model class for user notification preferences
 */
public class NotificationPreference {
    private UUID id;
    private UUID userId;
    private String notificationType;
    private String value;
    private Map<String, String> channels;
    private Map<String, String> customChannelConfig;
    
    public NotificationPreference() {
        this.channels = new HashMap<>();
        this.customChannelConfig = new HashMap<>();
    }
    
    public NotificationPreference(String value) {
        this();
        this.value = value;
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
    
    public String getNotificationType() {
        return notificationType;
    }
    
    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public Map<String, String> getChannels() {
        return channels;
    }
    
    public void setChannels(Map<String, String> channels) {
        this.channels = channels;
    }
    
    public Map<String, String> getCustomChannelConfig() {
        return customChannelConfig;
    }
    
    public void setCustomChannelConfig(Map<String, String> customChannelConfig) {
        this.customChannelConfig = customChannelConfig;
    }
}

