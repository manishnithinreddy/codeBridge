package com.codebridge.server.model;

/**
 * Enum for notification channels.
 */
public enum NotificationChannel {
    /**
     * Email notifications.
     */
    EMAIL,
    
    /**
     * Push notifications (mobile, desktop).
     */
    PUSH,
    
    /**
     * In-app notifications.
     */
    IN_APP,
    
    /**
     * Slack notifications.
     */
    SLACK
}

