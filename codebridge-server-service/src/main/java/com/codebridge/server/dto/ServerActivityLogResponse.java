package com.codebridge.server.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ServerActivityLogResponse {
    private UUID id;
    private UUID serverId;
    private String serverName; // Convenience
    private UUID platformUserId;
    private String platformUsername; // Convenience for display
    private String action;
    private String details;
    private String status;
    private String errorMessage;
    private String ipAddress; // Added for IP tracking
    private String userAgent; // Added for client context
    private LocalDateTime timestamp;

    // Constructor, Getters, Setters
    public ServerActivityLogResponse() {
    }

    public ServerActivityLogResponse(UUID id, UUID serverId, String serverName, UUID platformUserId,
                                   String platformUsername, String action, String details, String status,
                                   String errorMessage, String ipAddress, String userAgent, LocalDateTime timestamp) {
        this.id = id;
        this.serverId = serverId;
        this.serverName = serverName;
        this.platformUserId = platformUserId;
        this.platformUsername = platformUsername;
        this.action = action;
        this.details = details;
        this.status = status;
        this.errorMessage = errorMessage;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getServerId() {
        return serverId;
    }

    public void setServerId(UUID serverId) {
        this.serverId = serverId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public UUID getPlatformUserId() {
        return platformUserId;
    }

    public void setPlatformUserId(UUID platformUserId) {
        this.platformUserId = platformUserId;
    }

    public String getPlatformUsername() {
        return platformUsername;
    }

    public void setPlatformUsername(String platformUsername) {
        this.platformUsername = platformUsername;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
