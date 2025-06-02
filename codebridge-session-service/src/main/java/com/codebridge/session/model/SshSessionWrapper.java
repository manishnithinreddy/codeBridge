package com.codebridge.session.model; // Adapted package name

import com.jcraft.jsch.Session;
import com.codebridge.session.model.SessionKey; // Adapted import for SessionKey
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshSessionWrapper {

    private static final Logger log = LoggerFactory.getLogger(SshSessionWrapper.class);

    private final com.jcraft.jsch.Session jschSession;
    private final long createdAt;
    private volatile long lastAccessedTime;
    private final SessionKey sessionKey;

    /**
     * Constructs a new SshSessionWrapper.
     *
     * @param sessionKey The key associated with this session.
     * @param jschSession The active JSch session.
     */
    public SshSessionWrapper(SessionKey sessionKey, com.jcraft.jsch.Session jschSession) {
        if (sessionKey == null) {
            throw new IllegalArgumentException("SessionKey cannot be null.");
        }
        if (jschSession == null) {
            throw new IllegalArgumentException("JSch Session cannot be null.");
        }
        this.sessionKey = sessionKey;
        this.jschSession = jschSession;
        this.createdAt = System.currentTimeMillis();
        this.lastAccessedTime = this.createdAt;
    }

    /**
     * Gets the underlying JSch session.
     * @return The JSch session.
     */
    public com.jcraft.jsch.Session getJschSession() {
        return jschSession;
    }

    /**
     * Gets the session key associated with this session.
     * @return The session key.
     */
    public SessionKey getSessionKey() {
        return sessionKey;
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
     * Disconnects the SSH session if it is active.
     */
    public void disconnect() {
        if (jschSession != null && jschSession.isConnected()) {
            log.info("Disconnecting SSH session for key: {}", sessionKey);
            try {
                jschSession.disconnect();
                log.info("SSH session disconnected successfully for key: {}", sessionKey);
            } catch (Exception e) {
                log.error("Error while disconnecting SSH session for key: {}: {}", sessionKey, e.getMessage(), e);
            }
        } else {
            log.debug("SSH session for key: {} was already disconnected or not established.", sessionKey);
        }
    }

    /**
     * Checks if the underlying JSch session is connected.
     * @return true if connected, false otherwise.
     */
    public boolean isConnected() {
        return jschSession != null && jschSession.isConnected();
    }
}
