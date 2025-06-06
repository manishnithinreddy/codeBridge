package com.codebridge.session.model;

import com.jcraft.jsch.Session; // JSch Session
import java.time.Instant;

public class SshSessionWrapper {
    private final Session jschSession;
    private final SessionKey sessionKey; // Link back to the key that identifies this session
    private volatile long lastAccessedTime; // Epoch millis, volatile for thread safety

    public SshSessionWrapper(Session jschSession, SessionKey sessionKey) {
        this.jschSession = jschSession;
        this.sessionKey = sessionKey;
        this.lastAccessedTime = Instant.now().toEpochMilli();
    }

    public Session getJschSession() {
        return jschSession;
    }

    public SessionKey getSessionKey() {
        return sessionKey;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void updateLastAccessedTime() {
        this.lastAccessedTime = Instant.now().toEpochMilli();
    }

    public boolean isConnected() {
        return this.jschSession != null && this.jschSession.isConnected();
    }

    public void disconnect() {
        if (isConnected()) {
            this.jschSession.disconnect();
        }
    }
}
