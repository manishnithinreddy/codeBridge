package com.codebridge.session.dto; // Adapted package name

import com.codebridge.session.model.SessionKey; // Adapted import for SessionKey
import java.io.Serializable;
import java.util.Objects;

public class SshSessionMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private SessionKey sessionKey;
    private long createdAt;
    private long lastAccessedTime;
    private String jwtToken;
    private String applicationInstanceId;

    // Default constructor for deserialization
    public SshSessionMetadata() {}

    public SshSessionMetadata(SessionKey sessionKey, long createdAt, long lastAccessedTime, String jwtToken, String applicationInstanceId) {
        this.sessionKey = sessionKey;
        this.createdAt = createdAt;
        this.lastAccessedTime = lastAccessedTime;
        this.jwtToken = jwtToken;
        this.applicationInstanceId = applicationInstanceId;
    }

    // Getters
    public SessionKey getSessionKey() {
        return sessionKey;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public String getApplicationInstanceId() {
        return applicationInstanceId;
    }

    // Setters
    public void setSessionKey(SessionKey sessionKey) {
        this.sessionKey = sessionKey;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public void setApplicationInstanceId(String applicationInstanceId) {
        this.applicationInstanceId = applicationInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SshSessionMetadata that = (SshSessionMetadata) o;
        return createdAt == that.createdAt &&
               lastAccessedTime == that.lastAccessedTime &&
               Objects.equals(sessionKey, that.sessionKey) &&
               Objects.equals(jwtToken, that.jwtToken) &&
               Objects.equals(applicationInstanceId, that.applicationInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionKey, createdAt, lastAccessedTime, jwtToken, applicationInstanceId);
    }

    @Override
    public String toString() {
        return "SshSessionMetadata{" +
               "sessionKey=" + sessionKey +
               ", createdAt=" + createdAt +
               ", lastAccessedTime=" + lastAccessedTime +
               ", jwtToken='" + (jwtToken != null ? "[PRESENT]" : "[NULL]") + '\'' +
               ", applicationInstanceId='" + applicationInstanceId + '\'' +
               '}';
    }
}
