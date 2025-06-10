package com.codebridge.usermanagement.profile.model;

import java.util.UUID;

/**
 * Model class for user notification preferences
 */
public class NotificationPreference {
    private UUID id;
    private UUID userId;
    private String notificationType;
    private String value;
    
    public NotificationPreference() {
    }
    
    public NotificationPreference(String value) {
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
}

