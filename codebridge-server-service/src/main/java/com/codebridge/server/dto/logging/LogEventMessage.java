package com.codebridge.server.dto.logging;

import java.io.Serializable;
import java.util.UUID;

/**
 * Message sent to the activity log queue.
 */
public class LogEventMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID platformUserId;
    private String action;
    private UUID serverId;
    private String details;
    private String status;
    private String errorMessage;
    private String ipAddress;
    private String userAgent;
    private long timestamp;

    // Default constructor for serialization
    public LogEventMessage() {
    }

    public LogEventMessage(UUID platformUserId, String action, UUID serverId, String details, String status, 
                          String errorMessage, String ipAddress, String userAgent, long timestamp) {
        this.platformUserId = platformUserId;
        this.action = action;
        this.serverId = serverId;
        this.details = details;
        this.status = status;
        this.errorMessage = errorMessage;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public UUID getPlatformUserId() {
        return platformUserId;
    }

    public void setPlatformUserId(UUID platformUserId) {
        this.platformUserId = platformUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public UUID getServerId() {
        return serverId;
    }

    public void setServerId(UUID serverId) {
        this.serverId = serverId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

