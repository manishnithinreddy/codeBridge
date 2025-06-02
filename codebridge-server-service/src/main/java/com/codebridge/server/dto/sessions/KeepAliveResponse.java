package com.codebridge.server.dto.sessions;

public class KeepAliveResponse {
    private String sessionToken;
    private long expiresInMs; // Represents the total configured timeout, indicating the session was refreshed to this validity period

    public KeepAliveResponse(String sessionToken, long expiresInMs) {
        this.sessionToken = sessionToken;
        this.expiresInMs = expiresInMs;
    }

    // Getters and Setters (or use Lombok @Data)
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
