package com.codebridge.session.service;

import com.codebridge.session.dto.DbSessionCredentials;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.DbSessionWrapper;
import com.codebridge.session.model.enums.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a pool of database connections for improved performance and resource management.
 */
@Component
public class DbConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(DbConnectionPool.class);
    
    private final ConcurrentHashMap<SessionKey, ConcurrentHashMap<String, DbSessionWrapper>> connectionPools = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SessionKey, AtomicInteger> activeConnectionCounts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService healthCheckExecutor = Executors.newScheduledThreadPool(1);
    
    // Default pool configuration
    private final int maxConnectionsPerSession = 5;
    private final int healthCheckIntervalSeconds = 60;
    private final int connectionIdleTimeoutSeconds = 300; // 5 minutes
    private final int connectionValidationTimeoutSeconds = 5;
    
    public DbConnectionPool() {
        // Start periodic health check for all connections
        healthCheckExecutor.scheduleAtFixedRate(this::performHealthCheck, 
                healthCheckIntervalSeconds, healthCheckIntervalSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * Gets an available database connection from the pool or creates a new one if needed.
     * 
     * @param sessionKey The session key identifying the user and database
     * @param credentials The database credentials to use if a new connection is needed
     * @return A database session wrapper with an active connection
     * @throws RemoteOperationException if connection cannot be established
     */
    public DbSessionWrapper getConnection(SessionKey sessionKey, DbSessionCredentials credentials) {
        // Get or create the connection pool for this session
        ConcurrentHashMap<String, DbSessionWrapper> sessionPool = connectionPools.computeIfAbsent(
                sessionKey, k -> new ConcurrentHashMap<>());
        
        // Get or create the connection counter for this session
        AtomicInteger connectionCount = activeConnectionCounts.computeIfAbsent(
                sessionKey, k -> new AtomicInteger(0));
        
        // Try to find an available connection in the pool
        Optional<Map.Entry<String, DbSessionWrapper>> availableConnection = sessionPool.entrySet().stream()
                .filter(entry -> entry.getValue().isValid(connectionValidationTimeoutSeconds) && !entry.getValue().isInUse())
                .findFirst();
        
        if (availableConnection.isPresent()) {
            DbSessionWrapper wrapper = availableConnection.get().getValue();
            wrapper.setInUse(true);
            wrapper.updateLastAccessedTime();
            logger.debug("Reusing existing DB connection from pool for session key: {}", sessionKey);
            return wrapper;
        }
        
        // Check if we can create a new connection
        if (connectionCount.get() >= maxConnectionsPerSession) {
            logger.warn("Maximum DB connections reached for session key: {}", sessionKey);
            throw new RemoteOperationException("Maximum database connections reached. Please try again later.");
        }
        
        // Create a new connection
        try {
            Connection connection = createJdbcConnection(credentials);
            DbSessionWrapper wrapper = new DbSessionWrapper(connection, sessionKey, credentials.getDbType());
            wrapper.setInUse(true);
            
            String connectionId = "conn-" + System.currentTimeMillis() + "-" + connectionCount.incrementAndGet();
            sessionPool.put(connectionId, wrapper);
            
            logger.info("Created new DB connection in pool for session key: {}, connection ID: {}", 
                    sessionKey, connectionId);
            
            return wrapper;
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Failed to create new DB connection for session key: {}", sessionKey, e);
            throw new RemoteOperationException("Database connection failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Returns a connection to the pool, marking it as available for reuse.
     * 
     * @param sessionKey The session key
     * @param wrapper The database session wrapper to return to the pool
     */
    public void returnConnection(SessionKey sessionKey, DbSessionWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        
        ConcurrentHashMap<String, DbSessionWrapper> sessionPool = connectionPools.get(sessionKey);
        if (sessionPool != null) {
            // Find the connection ID for this wrapper
            Optional<String> connectionId = sessionPool.entrySet().stream()
                    .filter(entry -> entry.getValue() == wrapper)
                    .map(Map.Entry::getKey)
                    .findFirst();
            
            connectionId.ifPresent(id -> {
                wrapper.setInUse(false);
                wrapper.updateLastAccessedTime();
                logger.debug("Returned DB connection to pool for session key: {}, connection ID: {}", 
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
        ConcurrentHashMap<String, DbSessionWrapper> sessionPool = connectionPools.remove(sessionKey);
        if (sessionPool != null) {
            sessionPool.forEach((id, wrapper) -> {
                wrapper.closeConnection();
                logger.debug("Closed DB connection for session key: {}, connection ID: {}", sessionKey, id);
            });
            
            activeConnectionCounts.remove(sessionKey);
            logger.info("Closed all DB connections for session key: {}", sessionKey);
        }
    }
    
    /**
     * Performs a health check on all connections in the pool.
     * Removes idle or invalid connections.
     */
    private void performHealthCheck() {
        logger.debug("Performing health check on DB connection pools");
        long now = System.currentTimeMillis();
        
        connectionPools.forEach((sessionKey, sessionPool) -> {
            sessionPool.entrySet().removeIf(entry -> {
                DbSessionWrapper wrapper = entry.getValue();
                String connectionId = entry.getKey();
                
                // Check if connection is invalid
                if (!wrapper.isValid(connectionValidationTimeoutSeconds)) {
                    logger.debug("Removing invalid DB connection: {}", connectionId);
                    wrapper.closeConnection(); // Ensure cleanup
                    return true;
                }
                
                // Check if connection is idle for too long
                if (!wrapper.isInUse() && 
                        (now - wrapper.getLastAccessedTime() > connectionIdleTimeoutSeconds * 1000)) {
                    logger.debug("Removing idle DB connection: {}", connectionId);
                    wrapper.closeConnection();
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
     * Creates a JDBC connection based on the provided credentials.
     */
    private Connection createJdbcConnection(DbSessionCredentials creds) throws SQLException, ClassNotFoundException {
        // Basic JDBC URL construction. Production systems might use a more robust factory.
        String jdbcUrl = switch (creds.getDbType()) {
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s", creds.getHost(), creds.getPort(), creds.getDatabaseName());
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s", creds.getHost(), creds.getPort(), creds.getDatabaseName());
            case MARIADB -> String.format("jdbc:mariadb://%s:%d/%s", creds.getHost(), creds.getPort(), creds.getDatabaseName());
            case SQLSERVER -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s", creds.getHost(), creds.getPort(), creds.getDatabaseName());
            // Oracle has more complex URL usually: jdbc:oracle:thin:@//host:port/serviceName
            // case ORACLE -> String.format("jdbc:oracle:thin:@//%s:%d/%s", creds.getHost(), creds.getPort(), creds.getDatabaseName());
            default -> throw new UnsupportedOperationException("DB Type not supported for direct JDBC URL construction: " + creds.getDbType());
        };

        Properties props = new Properties();
        props.setProperty("user", creds.getUsername());
        props.setProperty("password", creds.getPassword());
        
        // Add connection pooling properties
        props.setProperty("autoReconnect", "true");
        props.setProperty("connectTimeout", String.valueOf(30000)); // 30 seconds
        
        if (creds.isSslEnabled()) {
            // Basic SSL, might need more properties depending on driver and server config
            props.setProperty("ssl", "true");
            props.setProperty("sslmode", "prefer"); // Or "require", "verify-full", etc.
        }
        if (creds.getAdditionalProperties() != null) {
            creds.getAdditionalProperties().forEach(props::setProperty);
        }

        // Ensure driver is loaded (optional for modern JDBC, but good practice for some environments)
        loadDriverIfNeeded(creds.getDbType());

        return DriverManager.getConnection(jdbcUrl, props);
    }
    
    /**
     * Loads the JDBC driver class if needed.
     */
    private void loadDriverIfNeeded(DbType dbType) throws ClassNotFoundException {
        String driverClassName = switch (dbType) {
            case POSTGRESQL -> "org.postgresql.Driver";
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MARIADB -> "org.mariadb.jdbc.Driver";
            case SQLSERVER -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            // case ORACLE -> "oracle.jdbc.OracleDriver";
            default -> null;
        };
        
        if (driverClassName != null) {
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                logger.error("JDBC driver not found: {}", driverClassName, e);
                throw e;
            }
        }
    }
    
    /**
     * Shuts down the connection pool and releases resources.
     */
    public void shutdown() {
        logger.info("Shutting down DB connection pool");
        
        // Close all connections
        connectionPools.forEach((sessionKey, sessionPool) -> {
            sessionPool.forEach((id, wrapper) -> wrapper.closeConnection());
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

