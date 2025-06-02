package com.codebridge.session.dto; // Adapted package

public class KeepAliveResponse {
    private String sessionToken;
    private long expiresInMs;

    public KeepAliveResponse(String sessionToken, long expiresInMs) {
        this.sessionToken = sessionToken;
        this.expiresInMs = expiresInMs;
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
}
