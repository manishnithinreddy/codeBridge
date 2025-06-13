package com.codebridge.session.service;

import com.codebridge.session.dto.SshSessionCredentials;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.SshSessionWrapper;
import com.jcraft.jsch.JSchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a pool of SSH connections for improved performance and resource management.
 */
@Component
public class SshConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(SshConnectionPool.class);
    
    private final ConcurrentHashMap<SessionKey, ConcurrentHashMap<String, SshSessionWrapper>> connectionPools = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SessionKey, AtomicInteger> activeConnectionCounts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService healthCheckExecutor = Executors.newScheduledThreadPool(1);
    
    // Default pool configuration
    private final int maxConnectionsPerSession = 5;
    private final int healthCheckIntervalSeconds = 60;
    private final int connectionIdleTimeoutSeconds = 300; // 5 minutes
    
    public SshConnectionPool() {
        // Start periodic health check for all connections
        healthCheckExecutor.scheduleAtFixedRate(this::performHealthCheck, 
                healthCheckIntervalSeconds, healthCheckIntervalSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * Gets an available SSH connection from the pool or creates a new one if needed.
     * 
     * @param sessionKey The session key identifying the user and server
     * @param credentials The SSH credentials to use if a new connection is needed
     * @return An SSH session wrapper with an active connection
     * @throws RemoteOperationException if connection cannot be established
     */
    public SshSessionWrapper getConnection(SessionKey sessionKey, SshSessionCredentials credentials) {
        // Get or create the connection pool for this session
        ConcurrentHashMap<String, SshSessionWrapper> sessionPool = connectionPools.computeIfAbsent(
                sessionKey, k -> new ConcurrentHashMap<>());
        
        // Get or create the connection counter for this session
        AtomicInteger connectionCount = activeConnectionCounts.computeIfAbsent(
                sessionKey, k -> new AtomicInteger(0));
        
        // Try to find an available connection in the pool
        Optional<Map.Entry<String, SshSessionWrapper>> availableConnection = sessionPool.entrySet().stream()
                .filter(entry -> entry.getValue().isConnected() && !entry.getValue().isInUse())
                .findFirst();
        
        if (availableConnection.isPresent()) {
            SshSessionWrapper wrapper = availableConnection.get().getValue();
            wrapper.setInUse(true);
            wrapper.updateLastAccessedTime();
            logger.debug("Reusing existing SSH connection from pool for session key: {}", sessionKey);
            return wrapper;
        }
        
        // Check if we can create a new connection
        if (connectionCount.get() >= maxConnectionsPerSession) {
            logger.warn("Maximum SSH connections reached for session key: {}", sessionKey);
            throw new RemoteOperationException("Maximum SSH connections reached. Please try again later.");
        }
        
        // Create a new connection
        try {
            SshSessionWrapper wrapper = new SshSessionWrapper(
                    credentials.getHost(), 
                    credentials.getPort(), 
                    credentials.getUsername(), 
                    credentials.getPassword(), 
                    credentials.getPrivateKey());
            
            wrapper.connect();
            wrapper.setInUse(true);
            
            String connectionId = "conn-" + System.currentTimeMillis() + "-" + connectionCount.incrementAndGet();
            sessionPool.put(connectionId, wrapper);
            
            logger.info("Created new SSH connection in pool for session key: {}, connection ID: {}", 
                    sessionKey, connectionId);
            
            return wrapper;
        } catch (JSchException e) {
            logger.error("Failed to create new SSH connection for session key: {}", sessionKey, e);
            throw new RemoteOperationException("SSH connection failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Returns a connection to the pool, marking it as available for reuse.
     * 
     * @param sessionKey The session key
     * @param wrapper The SSH session wrapper to return to the pool
     */
    public void returnConnection(SessionKey sessionKey, SshSessionWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        
        ConcurrentHashMap<String, SshSessionWrapper> sessionPool = connectionPools.get(sessionKey);
        if (sessionPool != null) {
            // Find the connection ID for this wrapper
            Optional<String> connectionId = sessionPool.entrySet().stream()
                    .filter(entry -> entry.getValue() == wrapper)
                    .map(Map.Entry::getKey)
                    .findFirst();
            
            connectionId.ifPresent(id -> {
                wrapper.setInUse(false);
                wrapper.updateLastAccessedTime();
                logger.debug("Returned SSH connection to pool for session key: {}, connection ID: {}", 
                        sessionKey, id);
            });
        }
    }
    
    /**
     * Closes and removes all connections for a specific session.
     * 
     * @param sessionKey The session key to clean up
     */
    public void closeAllConnections(SessionKey sessionKey) {
        ConcurrentHashMap<String, SshSessionWrapper> sessionPool = connectionPools.remove(sessionKey);
        if (sessionPool != null) {
            sessionPool.forEach((id, wrapper) -> {
                wrapper.disconnect();
                logger.debug("Closed SSH connection for session key: {}, connection ID: {}", sessionKey, id);
            });
            
            activeConnectionCounts.remove(sessionKey);
            logger.info("Closed all SSH connections for session key: {}", sessionKey);
        }
    }
    
    /**
     * Performs a health check on all connections in the pool.
     * Removes idle or disconnected connections.
     */
    private void performHealthCheck() {
        logger.debug("Performing health check on SSH connection pools");
        long now = System.currentTimeMillis();
        
        connectionPools.forEach((sessionKey, sessionPool) -> {
            sessionPool.entrySet().removeIf(entry -> {
                SshSessionWrapper wrapper = entry.getValue();
                String connectionId = entry.getKey();
                
                // Check if connection is disconnected
                if (!wrapper.isConnected()) {
                    logger.debug("Removing disconnected SSH connection: {}", connectionId);
                    wrapper.disconnect(); // Ensure cleanup
                    return true;
                }
                
                // Check if connection is idle for too long
                if (!wrapper.isInUse() && 
                        (now - wrapper.getLastAccessedTime() > connectionIdleTimeoutSeconds * 1000)) {
                    logger.debug("Removing idle SSH connection: {}", connectionId);
                    wrapper.disconnect();
                    return true;
                }
                
                return false;
            });
            
            // Update the connection count
            activeConnectionCounts.get(sessionKey).set(sessionPool.size());
        });
        
        // Remove empty pools
        connectionPools.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    
    /**
     * Shuts down the connection pool and releases resources.
     */
    public void shutdown() {
        logger.info("Shutting down SSH connection pool");
        
        // Close all connections
        connectionPools.forEach((sessionKey, sessionPool) -> {
            sessionPool.forEach((id, wrapper) -> wrapper.disconnect());
        });
        
        connectionPools.clear();
        activeConnectionCounts.clear();
        
        // Shutdown the health check executor
        healthCheckExecutor.shutdown();
        try {
            if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                healthCheckExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

