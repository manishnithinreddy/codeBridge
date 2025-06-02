package com.codebridge.session.dto; // Adapted package

import java.util.UUID;

public class SessionResponse {
    private String sessionToken;
    private long expiresInMs;
    private UUID userId;
    private UUID resourceId;
    private String resourceType;

    public SessionResponse(String sessionToken, long expiresInMs, UUID userId, UUID resourceId, String resourceType) {
        this.sessionToken = sessionToken;
        this.expiresInMs = expiresInMs;
        this.userId = userId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }

    // Getters and Setters
    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public void setExpiresInMs(long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
