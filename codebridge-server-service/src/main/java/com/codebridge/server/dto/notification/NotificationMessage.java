package com.codebridge.server.dto.notification;

import java.util.Map;
import java.util.UUID;

/**
 * DTO for notification messages.
 */
public class NotificationMessage {

    private UUID id;
    private UUID userId;
    private String eventType;
    private String title;
    private String content;
    private Map<String, Object> details;
    private long timestamp;

    /**
     * Default constructor.
     */
    public NotificationMessage() {
    }

    /**
     * Constructor with all fields.
     *
     * @param id the notification ID
     * @param userId the user ID
     * @param eventType the event type
     * @param title the notification title
     * @param content the notification content
     * @param details additional details
     * @param timestamp the timestamp
     */
    public NotificationMessage(UUID id, UUID userId, String eventType, String title, 
                              String content, Map<String, Object> details, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.eventType = eventType;
        this.title = title;
        this.content = content;
        this.details = details;
        this.timestamp = timestamp;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

