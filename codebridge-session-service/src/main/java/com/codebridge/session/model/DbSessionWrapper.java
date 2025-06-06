package com.codebridge.session.model;

import com.codebridge.session.model.enums.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

public class DbSessionWrapper {
    private static final Logger logger = LoggerFactory.getLogger(DbSessionWrapper.class);

    private final Connection connection;
    private final SessionKey sessionKey; // Links to platformUserId, resourceId (e.g. db alias ID), sessionType
    private final DbType dbType;
    private final long createdAt; // Epoch millis
    private volatile long lastAccessedTime; // Epoch millis

    public DbSessionWrapper(Connection connection, SessionKey sessionKey, DbType dbType) {
        this.connection = connection;
        this.sessionKey = sessionKey;
        this.dbType = dbType;
        this.createdAt = Instant.now().toEpochMilli();
        this.lastAccessedTime = this.createdAt;
    }

    public Connection getConnection() {
        return connection;
    }

    public SessionKey getSessionKey() {
        return sessionKey;
    }

    public DbType getDbType() {
        return dbType;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void updateLastAccessedTime() {
        this.lastAccessedTime = Instant.now().toEpochMilli();
    }

    public boolean isValid(int timeoutSeconds) {
        if (connection == null) {
            return false;
        }
        try {
            return connection.isValid(timeoutSeconds);
        } catch (SQLException e) {
            logger.warn("Error validating DB connection for session {}: {}", sessionKey, e.getMessage());
            return false;
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.info("DB connection closed for session key: {}", sessionKey);
                }
            } catch (SQLException e) {
                logger.error("Error closing DB connection for session key {}: {}", sessionKey, e.getMessage(), e);
            }
        }
    }
}
