package com.codebridge.session.service.connection;

import com.codebridge.session.dto.UserProvidedConnectionDetails;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.enums.ServerAuthProvider;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A connection pool for SSH connections that manages connection lifecycle,
 * reuse, and monitoring. This helps prepare for scaling to large numbers
 * of concurrent connections.
 */
@Component
public class SshConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(SshConnectionPool.class);
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 15000; // 15 seconds
    
    private final JSch jsch;
    private final int maxConnectionsPerInstance;
    private final long connectionIdleTimeoutMs;
    private final Semaphore connectionSemaphore;
    private final Map<SessionKey, PooledConnection> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger activeConnectionCount = new AtomicInteger(0);
    
    // Metrics
    private final MeterRegistry meterRegistry;
    private final Timer connectionCreationTimer;
    private final Counter connectionCreationCounter;
    private final Counter connectionReuseCounter;
    private final Counter connectionTimeoutCounter;
    private final Counter connectionErrorCounter;
    
    @Autowired
    public SshConnectionPool(
            JSch jsch,
            @Value("${codebridge.session.ssh.maxConnectionsPerInstance:1000}") int maxConnectionsPerInstance,
            @Value("${codebridge.session.ssh.connectionIdleTimeoutMs:300000}") long connectionIdleTimeoutMs,
            MeterRegistry meterRegistry) {
        this.jsch = jsch;
        this.maxConnectionsPerInstance = maxConnectionsPerInstance;
        this.connectionIdleTimeoutMs = connectionIdleTimeoutMs;
        this.connectionSemaphore = new Semaphore(maxConnectionsPerInstance, true);
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.connectionCreationTimer = Timer.builder("ssh.connection.creation.time")
                .description("Time taken to create a new SSH connection")
                .register(meterRegistry);
        this.connectionCreationCounter = Counter.builder("ssh.connection.creation.count")
                .description("Number of new SSH connections created")
                .register(meterRegistry);
        this.connectionReuseCounter = Counter.builder("ssh.connection.reuse.count")
                .description("Number of times SSH connections were reused")
                .register(meterRegistry);
        this.connectionTimeoutCounter = Counter.builder("ssh.connection.timeout.count")
                .description("Number of SSH connections that timed out")
                .register(meterRegistry);
        this.connectionErrorCounter = Counter.builder("ssh.connection.error.count")
                .description("Number of SSH connection errors")
                .register(meterRegistry);
        
        // Register gauge for active connection count
        meterRegistry.gauge("ssh.connection.active.count", activeConnectionCount);
    }
    
    /**
     * Acquires an SSH connection for the given session key and connection details.
     * Will reuse an existing connection if available and valid, or create a new one if needed.
     *
     * @param sessionKey The unique key identifying this session
     * @param connDetails Connection details for creating a new connection if needed
     * @return A JSch Session that is connected and ready to use
     * @throws JSchException If there's an error creating or connecting the session
     * @throws InterruptedException If the thread is interrupted while waiting for a connection
     */
    public Session acquireConnection(SessionKey sessionKey, UserProvidedConnectionDetails connDetails) 
            throws JSchException, InterruptedException {
        
        // First check if we have an existing valid connection
        PooledConnection existingConn = activeConnections.get(sessionKey);
        if (existingConn != null && existingConn.isValid()) {
            logger.debug("Reusing existing connection for session key: {}", sessionKey);
            existingConn.updateLastAccessTime();
            connectionReuseCounter.increment();
            return existingConn.getSession();
        }
        
        // If we reach here, we need a new connection
        // Try to acquire a permit from the semaphore with timeout
        boolean acquired = connectionSemaphore.tryAcquire(30, TimeUnit.SECONDS);
        if (!acquired) {
            logger.error("Failed to acquire connection permit after 30 seconds. Max connections: {}, Current active: {}", 
                    maxConnectionsPerInstance, activeConnectionCount.get());
            throw new JSchException("Connection pool exhausted. Too many concurrent connections.");
        }
        
        try {
            // Create a new connection
            Timer.Sample sample = Timer.start(meterRegistry);
            Session session = createJschSession(connDetails);
            session.connect(DEFAULT_CONNECT_TIMEOUT_MS);
            sample.stop(connectionCreationTimer);
            
            // Update metrics and pool state
            connectionCreationCounter.increment();
            activeConnectionCount.incrementAndGet();
            
            // Store in the pool
            PooledConnection newConn = new PooledConnection(session, sessionKey);
            activeConnections.put(sessionKey, newConn);
            
            logger.info("Created new SSH connection for session key: {}", sessionKey);
            return session;
        } catch (JSchException e) {
            // Release the permit if connection creation fails
            connectionSemaphore.release();
            activeConnectionCount.decrementAndGet();
            connectionErrorCounter.increment();
            logger.error("Error creating SSH connection for session key {}: {}", sessionKey, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Releases a connection back to the pool. The connection may be closed if it's
     * no longer needed or kept alive for future reuse.
     *
     * @param sessionKey The session key associated with the connection
     * @param forceClose Whether to force close the connection instead of returning it to the pool
     */
    public void releaseConnection(SessionKey sessionKey, boolean forceClose) {
        PooledConnection conn = activeConnections.get(sessionKey);
        if (conn == null) {
            logger.debug("No connection found to release for session key: {}", sessionKey);
            return;
        }
        
        if (forceClose || !conn.isValid()) {
            closeConnection(sessionKey);
        } else {
            // Update last access time for reuse
            conn.updateLastAccessTime();
            logger.debug("Connection released back to pool for session key: {}", sessionKey);
        }
    }
    
    /**
     * Closes and removes a connection from the pool.
     *
     * @param sessionKey The session key associated with the connection to close
     */
    public void closeConnection(SessionKey sessionKey) {
        PooledConnection conn = activeConnections.remove(sessionKey);
        if (conn != null) {
            try {
                conn.getSession().disconnect();
                logger.info("Closed SSH connection for session key: {}", sessionKey);
            } finally {
                connectionSemaphore.release();
                activeConnectionCount.decrementAndGet();
            }
        }
    }
    
    /**
     * Performs cleanup of idle connections that have exceeded the idle timeout.
     * This should be called periodically by a scheduled task.
     */
    public void cleanupIdleConnections() {
        long now = Instant.now().toEpochMilli();
        int closedCount = 0;
        
        for (Map.Entry<SessionKey, PooledConnection> entry : activeConnections.entrySet()) {
            PooledConnection conn = entry.getValue();
            if (now - conn.getLastAccessTime() > connectionIdleTimeoutMs) {
                closeConnection(entry.getKey());
                connectionTimeoutCounter.increment();
                closedCount++;
            }
        }
        
        if (closedCount > 0) {
            logger.info("Cleaned up {} idle SSH connections", closedCount);
        }
    }
    
    /**
     * Creates a new JSch session with the provided connection details.
     *
     * @param connDetails Connection details for creating the session
     * @return A new JSch Session (not yet connected)
     * @throws JSchException If there's an error creating the session
     */
    private Session createJschSession(UserProvidedConnectionDetails connDetails) throws JSchException {
        if (connDetails.getAuthProvider() == ServerAuthProvider.SSH_KEY) {
            if (!StringUtils.hasText(connDetails.getDecryptedPrivateKey())) {
                throw new JSchException("Private key is required for SSH key authentication but was not provided.");
            }
            String keyName = StringUtils.hasText(connDetails.getSshKeyName()) ? connDetails.getSshKeyName() : "user-key";
            jsch.addIdentity(keyName, connDetails.getDecryptedPrivateKey().getBytes(), null, null);
        }
        
        Session session = jsch.getSession(connDetails.getUsername(), connDetails.getHostname(), connDetails.getPort());
        if (connDetails.getAuthProvider() == ServerAuthProvider.PASSWORD) {
            if (!StringUtils.hasText(connDetails.getDecryptedPassword())) {
                throw new JSchException("Password is required for password authentication but was not provided.");
            }
            session.setPassword(connDetails.getDecryptedPassword());
        }
        
        Properties config = new Properties();
        config.put("PreferredAuthentications", "publickey,password");
        
        // Set more aggressive timeouts for better resource management
        config.put("ServerAliveInterval", "60"); // Send keepalive every 60 seconds
        config.put("ServerAliveCountMax", "3");  // Disconnect after 3 failed keepalives
        config.put("ConnectTimeout", String.valueOf(DEFAULT_CONNECT_TIMEOUT_MS));
        
        session.setConfig(config);
        return session;
    }
    
    /**
     * Gets the current number of active connections in the pool.
     *
     * @return The number of active connections
     */
    public int getActiveConnectionCount() {
        return activeConnectionCount.get();
    }
    
    /**
     * Gets the maximum number of connections allowed per instance.
     *
     * @return The maximum connection limit
     */
    public int getMaxConnectionsPerInstance() {
        return maxConnectionsPerInstance;
    }
    
    /**
     * Gets the number of available connection slots.
     *
     * @return The number of available connections
     */
    public int getAvailableConnections() {
        return connectionSemaphore.availablePermits();
    }
    
    /**
     * Inner class representing a connection in the pool.
     */
    private static class PooledConnection {
        private final Session session;
        private final SessionKey sessionKey;
        private long lastAccessTime;
        
        public PooledConnection(Session session, SessionKey sessionKey) {
            this.session = session;
            this.sessionKey = sessionKey;
            this.lastAccessTime = Instant.now().toEpochMilli();
        }
        
        public Session getSession() {
            return session;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        public void updateLastAccessTime() {
            this.lastAccessTime = Instant.now().toEpochMilli();
        }
        
        public boolean isValid() {
            return session != null && session.isConnected();
        }
    }
}

