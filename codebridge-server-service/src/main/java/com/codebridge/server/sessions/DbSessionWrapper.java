package com.codebridge.server.sessions;

import com.codebridge.server.model.enums.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
// import java.util.Map; // For future aiContextCache

public class DbSessionWrapper {

    private static final Logger log = LoggerFactory.getLogger(DbSessionWrapper.class);

    private final Connection connection;
    private final SessionKey sessionKey;
    private final DbType dbType;
    private final long createdAt;
    private volatile long lastAccessedTime;

    // Future placeholder:
    // private Map<String, Object> aiContextCache;

    /**
     * Constructs a new DbSessionWrapper.
     *
     * @param sessionKey The key identifying this session.
     * @param connection The active JDBC connection.
     * @param dbType     The type of the database.
     */
    public DbSessionWrapper(SessionKey sessionKey, Connection connection, DbType dbType) {
        if (sessionKey == null) {
            throw new IllegalArgumentException("SessionKey cannot be null.");
        }
        if (connection == null) {
            throw new IllegalArgumentException("JDBC Connection cannot be null.");
        }
        if (dbType == null) {
            throw new IllegalArgumentException("DbType cannot be null.");
        }

        this.sessionKey = sessionKey;
        this.connection = connection;
        this.dbType = dbType;
        this.createdAt = System.currentTimeMillis();
        this.lastAccessedTime = this.createdAt;
    }

    /**
     * Gets the underlying JDBC connection.
     * @return The JDBC connection.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Gets the session key associated with this session.
     * @return The session key.
     */
    public SessionKey getSessionKey() {
        return sessionKey;
    }

    /**
     * Gets the type of the database for this session.
     * @return The DbType.
     */
    public DbType getDbType() {
        return dbType;
    }

    /**
     * Gets the timestamp when this session was created.
     * @return The creation timestamp in milliseconds.
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the timestamp when this session was last accessed.
     * @return The last access timestamp in milliseconds.
     */
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    /**
     * Updates the last accessed time to the current time.
     */
    public void updateLastAccessedTime() {
        this.lastAccessedTime = System.currentTimeMillis();
    }

    /**
     * Closes the JDBC connection if it is active.
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    log.info("Closing DB connection for session key: {}", sessionKey);
                    connection.close();
                    log.info("DB connection closed successfully for session key: {}", sessionKey);
                } else {
                    log.debug("DB connection for session key: {} was already closed.", sessionKey);
                }
            } catch (SQLException e) {
                log.error("Error while closing DB connection for session key: {}: {}", sessionKey, e.getMessage(), e);
            }
        }
    }

    /**
     * Checks if the underlying JDBC connection is valid.
     *
     * @param timeoutSeconds The time in seconds to wait for the database operation used to validate the connection.
     * @return true if the connection is valid, false otherwise.
     */
    public boolean isValid(int timeoutSeconds) {
        if (connection == null) {
            return false;
        }
        try {
            return connection.isValid(timeoutSeconds);
        } catch (SQLException e) {
            log.error("SQLException while checking DB connection validity for session key {}: {}", sessionKey, e.getMessage(), e);
            return false;
        }
    }
}
