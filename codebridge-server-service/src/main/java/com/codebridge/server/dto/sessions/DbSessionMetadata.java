package com.codebridge.server.dto.sessions;

import com.codebridge.server.sessions.SessionKey;
import java.io.Serializable;
import java.util.Objects;

public class DbSessionMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private SessionKey sessionKey;
    private long createdAt;
    private long lastAccessedTime;
    private String jwtToken; // The currently active JWT for this session
    private String applicationInstanceId; // Identifier for the app instance holding the live DB connection

    // DB specific info for metadata
    private String dbType; // From DbType.name()
    private String dbHost;
    private String dbName;
    private String dbUsername;

    // Default constructor for deserialization
    public DbSessionMetadata() {}

    public DbSessionMetadata(SessionKey sessionKey, long createdAt, long lastAccessedTime, String jwtToken,
                             String applicationInstanceId, String dbType, String dbHost, String dbName, String dbUsername) {
        this.sessionKey = sessionKey;
        this.createdAt = createdAt;
        this.lastAccessedTime = lastAccessedTime;
        this.jwtToken = jwtToken;
        this.applicationInstanceId = applicationInstanceId;
        this.dbType = dbType;
        this.dbHost = dbHost;
        this.dbName = dbName;
        this.dbUsername = dbUsername;
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

    public String getDbType() {
        return dbType;
    }

    public String getDbHost() {
        return dbHost;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUsername() {
        return dbUsername;
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

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DbSessionMetadata that = (DbSessionMetadata) o;
        return createdAt == that.createdAt &&
               lastAccessedTime == that.lastAccessedTime &&
               Objects.equals(sessionKey, that.sessionKey) &&
               Objects.equals(jwtToken, that.jwtToken) &&
               Objects.equals(applicationInstanceId, that.applicationInstanceId) &&
               Objects.equals(dbType, that.dbType) &&
               Objects.equals(dbHost, that.dbHost) &&
               Objects.equals(dbName, that.dbName) &&
               Objects.equals(dbUsername, that.dbUsername);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionKey, createdAt, lastAccessedTime, jwtToken, applicationInstanceId,
                            dbType, dbHost, dbName, dbUsername);
    }

    @Override
    public String toString() {
        return "DbSessionMetadata{" +
               "sessionKey=" + sessionKey +
               ", createdAt=" + createdAt +
               ", lastAccessedTime=" + lastAccessedTime +
               ", jwtToken='" + (jwtToken != null ? "[PRESENT]" : "[NULL]") + '\'' +
               ", applicationInstanceId='" + applicationInstanceId + '\'' +
               ", dbType='" + dbType + '\'' +
               ", dbHost='" + dbHost + '\'' +
               ", dbName='" + dbName + '\'' +
               ", dbUsername='" + dbUsername + '\'' +
               '}';
    }
}
