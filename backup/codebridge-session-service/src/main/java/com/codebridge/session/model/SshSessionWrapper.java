package com.codebridge.session.model;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session; // JSch Session
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class SshSessionWrapper {
    private static final Logger logger = LoggerFactory.getLogger(SshSessionWrapper.class);

    private final Session jschSession;
    private final SessionKey sessionKey; // Link back to the key that identifies this session
    private volatile long lastAccessedTime; // Epoch millis, volatile for thread safety
    // Add inUse flag for connection pooling
    private final AtomicBoolean inUse;
    // Add connection timeout
    private final int connectionTimeoutMs = 30000; // 30 seconds connection timeout

    public SshSessionWrapper(Session jschSession, SessionKey sessionKey) {
        this.jschSession = jschSession;
        this.sessionKey = sessionKey;
        this.lastAccessedTime = Instant.now().toEpochMilli();
        // Initialize inUse flag
        this.inUse = new AtomicBoolean(false);
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
        
        // Configure session with enhanced properties
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no"); // For testing - in production, handle host keys properly
        // Add additional configuration for better stability
        config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
        config.put("ConnectTimeout", String.valueOf(connectionTimeoutMs));
        config.put("TCPKeepAlive", "yes");
        config.put("ServerAliveInterval", "60"); // Send keep-alive every 60 seconds
        session.setConfig(config);
        
        this.jschSession = session;
        this.sessionKey = null; // Will be set later when added to the manager
        this.lastAccessedTime = Instant.now().toEpochMilli();
        // Initialize inUse flag
        this.inUse = new AtomicBoolean(false);
    }

    public void connect() throws JSchException {
        if (jschSession != null && !jschSession.isConnected()) {
            // Add timeout to connect method
            jschSession.connect(connectionTimeoutMs);
            logger.debug("SSH session connected to {}:{}", jschSession.getHost(), jschSession.getPort());
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
            logger.debug("SSH session disconnected from {}:{}", jschSession.getHost(), jschSession.getPort());
        }
    }
    
    // Add methods for connection pooling
    public boolean isInUse() {
        return inUse.get();
    }
    
    public void setInUse(boolean inUse) {
        this.inUse.set(inUse);
    }
    
    public boolean tryAcquire() {
        return inUse.compareAndSet(false, true);
    }
    
    public void release() {
        inUse.set(false);
        updateLastAccessedTime();
    }
}
