package com.codebridge.session.model;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session; // JSch Session
import java.time.Instant;
import java.util.Properties;

public class SshSessionWrapper {
    private final Session jschSession;
    private final SessionKey sessionKey; // Link back to the key that identifies this session
    private volatile long lastAccessedTime; // Epoch millis, volatile for thread safety

    public SshSessionWrapper(Session jschSession, SessionKey sessionKey) {
        this.jschSession = jschSession;
        this.sessionKey = sessionKey;
        this.lastAccessedTime = Instant.now().toEpochMilli();
    }
    
    public SshSessionWrapper(String host, int port, String username, String password, String privateKey) throws JSchException {
        JSch jsch = new JSch();
        
        // Set up private key if provided
        if (privateKey != null && !privateKey.isEmpty()) {
            // In a real implementation, you might need to save the key to a temp file
            // or use JSch's addIdentity with byte array
            jsch.addIdentity("temp-key", privateKey.getBytes(), null, null);
        }
        
        Session session = jsch.getSession(username, host, port);
        
        // Set password if provided
        if (password != null && !password.isEmpty()) {
            session.setPassword(password);
        }
        
        // Configure session
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no"); // For testing - in production, handle host keys properly
        session.setConfig(config);
        
        this.jschSession = session;
        this.sessionKey = null; // Will be set later when added to the manager
        this.lastAccessedTime = Instant.now().toEpochMilli();
    }

    public void connect() throws JSchException {
        if (jschSession != null && !jschSession.isConnected()) {
            jschSession.connect();
        }
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

