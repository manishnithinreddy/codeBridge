package com.codebridge.server.sessions;

import com.codebridge.server.config.DbSessionConfigProperties;
import com.codebridge.server.config.JwtConfigProperties;
import com.codebridge.server.dto.sessions.*;
import com.codebridge.server.model.enums.DbType;
import com.codebridge.server.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary; // If this should be the primary for DbSessionWrapper
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service("dbSessionManager")
// @Primary // Uncomment if this becomes the primary SessionManager<DbSessionWrapper>
public class RedisDbSessionManagerImpl implements SessionManager<DbSessionWrapper> {

    private static final Logger log = LoggerFactory.getLogger(RedisDbSessionManagerImpl.class);

    private final RedisTemplate<String, SessionKey> sessionKeyRedisTemplate; // Shared with SSH for token->SessionKey
    private final RedisTemplate<String, DbSessionMetadata> dbSessionMetadataRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final DbSessionConfigProperties dbSessionConfigProperties;
    private final JwtConfigProperties jwtConfigProperties;

    private final ConcurrentMap<SessionKey, DbSessionWrapper> localActiveDbSessions = new ConcurrentHashMap<>();
    private final String applicationInstanceId;

    // JDBC connection timeout (for establishing the actual DB connection)
    private static final int DB_CONNECT_TIMEOUT_SECONDS = 15;


    public RedisDbSessionManagerImpl(
            RedisTemplate<String, SessionKey> sessionKeyRedisTemplate, // Reusing the sessionKeyRedisTemplate
            RedisTemplate<String, DbSessionMetadata> dbSessionMetadataRedisTemplate,
            JwtTokenProvider jwtTokenProvider,
            DbSessionConfigProperties dbSessionConfigProperties,
            JwtConfigProperties jwtConfigProperties,
            @Value("${spring.application.name}:${server.port:unknown}") String appInstanceIdBase) {
        this.sessionKeyRedisTemplate = sessionKeyRedisTemplate;
        this.dbSessionMetadataRedisTemplate = dbSessionMetadataRedisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.dbSessionConfigProperties = dbSessionConfigProperties;
        this.jwtConfigProperties = jwtConfigProperties;
        this.applicationInstanceId = appInstanceIdBase + ":" + UUID.randomUUID().toString().substring(0,8);
        log.info("RedisDbSessionManagerImpl initialized with applicationInstanceId: {}", this.applicationInstanceId);
    }

    // --- Helper methods for Redis keys ---
    private String dbTokenRedisKey(String token) {
        // Prefix to distinguish from SSH tokens if the same Redis instance/template for tokens is used.
        // Or, ensure tokens are globally unique (e.g. JWTs from same provider are fine).
        // For SessionKey itself in Redis, SessionKey contains resourceType="DB"
        return "db:session:token:" + token;
    }

    private String dbSessionMetadataRedisKey(SessionKey key) {
        return "db:session:metadata:" + key.userId() + ":" + key.resourceId() + ":" + key.resourceType();
    }

    // --- Lifecycle Methods ---

    public SessionResponse initDbSession(UUID platformUserId, String dbConnectionAlias, DbSessionCredentials credentials) {
        if (credentials == null) {
            throw new IllegalArgumentException("Database credentials must be provided.");
        }
        // Use a UUID derived from the user-provided alias for resourceId.
        // This makes the resourceId stable for a given alias by a user.
        UUID resourceId = UUID.nameUUIDFromBytes((platformUserId.toString() + ":" + dbConnectionAlias).getBytes(StandardCharsets.UTF_8));
        SessionKey sessionKey = new SessionKey(platformUserId, resourceId, "DB:" + credentials.getDbType().name());

        log.info("Attempting to initialize DB session for key: {}, alias: {}", sessionKey, dbConnectionAlias);

        Supplier<DbSessionWrapper> dbSessionFactory = () -> {
            Connection connection;
            String jdbcUrl = constructJdbcUrl(credentials);
            try {
                // Load driver explicitly if needed, though modern JDBC drivers often register via SPI
                // Class.forName(getDriverClassName(credentials.getDbType()));
                log.debug("Attempting to connect to DB via JDBC URL: {} with user: {}", jdbcUrl, credentials.getUsername());
                DriverManager.setLoginTimeout(DB_CONNECT_TIMEOUT_SECONDS);
                connection = DriverManager.getConnection(jdbcUrl, credentials.getUsername(), credentials.getPassword());
                log.info("DB connection established for key: {} (alias: {}) on instance {}", sessionKey, dbConnectionAlias, applicationInstanceId);
                return new DbSessionWrapper(sessionKey, connection, credentials.getDbType());
            } catch (SQLException e) {
                log.error("SQLException during DB connection for key {} (alias: {}): {}", sessionKey, dbConnectionAlias, e.getMessage(), e);
                throw new RuntimeException("Failed to create DB session: " + e.getMessage(), e);
            }
            // catch (ClassNotFoundException e) {
            //     log.error("JDBC Driver not found for DbType {}: {}", credentials.getDbType(), e.getMessage());
            //     throw new RuntimeException("JDBC Driver not found: " + e.getMessage(), e);
            // }
        };

        try {
            DbSessionWrapper sessionWrapper = this.createSession(sessionKey, dbSessionFactory);
            String jwtToken = jwtTokenProvider.generateToken(sessionKey); // SessionKey includes resourceType
            long currentTime = System.currentTimeMillis();

            DbSessionMetadata metadata = new DbSessionMetadata(
                sessionKey, currentTime, currentTime, jwtToken, this.applicationInstanceId,
                credentials.getDbType().name(), credentials.getHost(), credentials.getDatabaseName(), credentials.getUsername()
            );

            // Store JWT -> SessionKey mapping
            sessionKeyRedisTemplate.opsForValue().set(
                dbTokenRedisKey(jwtToken), // Use db specific token key
                sessionKey,
                jwtConfigProperties.getExpirationMs(),
                TimeUnit.MILLISECONDS
            );
             log.debug("Stored DB JWT-to-SessionKey mapping in Redis for token linked to key {}", sessionKey);

            // Store DbSessionMetadata
            dbSessionMetadataRedisTemplate.opsForValue().set(
                dbSessionMetadataRedisKey(sessionKey),
                metadata,
                dbSessionConfigProperties.getTimeoutMs(),
                TimeUnit.MILLISECONDS
            );
            log.info("Initialized and stored DB session metadata in Redis for key: {}, token: {}, instanceId: {}",
                     sessionKey, jwtToken, this.applicationInstanceId);

            return new SessionResponse(
                jwtToken,
                jwtConfigProperties.getExpirationMs(),
                platformUserId,
                resourceId, // Return the generated resourceId
                sessionKey.resourceType() // "DB:" + credentials.getDbType().name()
            );

        } catch (Exception e) {
            log.error("Failed to initialize and store DB session for user {}, alias {}: {}", platformUserId, dbConnectionAlias, e.getMessage(), e);
            throw new RuntimeException("DB Session initialization and storage failed: " + e.getMessage(), e);
        }
    }

    public Optional<KeepAliveResponse> keepAliveDbSession(String sessionToken) {
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        if (keyOpt.isEmpty() || !keyOpt.get().resourceType().startsWith("DB:")) {
            log.warn("Keepalive failed: Invalid, expired, or non-DB JWT token: {}", sessionToken);
            sessionKeyRedisTemplate.delete(dbTokenRedisKey(sessionToken));
            return Optional.empty();
        }
        SessionKey sessionKey = keyOpt.get();

        DbSessionMetadata metadata = dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(sessionKey));
        if (metadata == null) {
            log.warn("Keepalive failed: No DB session metadata in Redis for key {} (from token {}). Cleaning token.",
                     sessionKey, sessionToken);
            sessionKeyRedisTemplate.delete(dbTokenRedisKey(sessionToken));
            return Optional.empty();
        }

        metadata.setLastAccessedTime(System.currentTimeMillis());
        String newJwtToken = jwtTokenProvider.generateToken(sessionKey);
        metadata.setJwtToken(newJwtToken);

        dbSessionMetadataRedisTemplate.opsForValue().set(
            dbSessionMetadataRedisKey(sessionKey), metadata,
            dbSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS
        );

        sessionKeyRedisTemplate.delete(dbTokenRedisKey(sessionToken)); // Delete old
        sessionKeyRedisTemplate.opsForValue().set(
            dbTokenRedisKey(newJwtToken), sessionKey,
            jwtConfigProperties.getExpirationMs(), TimeUnit.MILLISECONDS
        );
        log.info("DB Session Keepalive successful for key: {}. New JWT. Expected on instance: {}",
                 sessionKey, metadata.getApplicationInstanceId());

        DbSessionWrapper localWrapper = localActiveDbSessions.get(sessionKey);
        if (localWrapper != null) {
            try {
                if (!localWrapper.isValid(2)) { // Check if local connection is still valid
                     log.warn("Local DB session for key {} found invalid during keepalive. Closing and removing.", sessionKey);
                     localWrapper.closeConnection();
                     localActiveDbSessions.remove(sessionKey);
                } else {
                    localWrapper.updateLastAccessedTime();
                    log.debug("Updated lastAccessedTime for local DbSessionWrapper for key {} during keepalive.", sessionKey);
                }
            } catch (Exception e) {
                 log.error("Error validating local DB session during keepalive for key {}: {}", sessionKey, e.getMessage(), e);
                 localWrapper.closeConnection(); // Attempt to close on error
                 localActiveDbSessions.remove(sessionKey);
            }
        }
        return Optional.of(new KeepAliveResponse(newJwtToken, jwtConfigProperties.getExpirationMs()));
    }

    public void releaseDbSession(String sessionToken) {
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        sessionKeyRedisTemplate.delete(dbTokenRedisKey(sessionToken)); // Always delete token mapping

        if (keyOpt.isEmpty() || !keyOpt.get().resourceType().startsWith("DB:")) {
            log.warn("Release attempt with invalid, expired, or non-DB JWT token: {}. Token mapping deleted.", sessionToken);
            return;
        }
        SessionKey sessionKey = keyOpt.get();
        log.info("Attempting to release DB session for key: {} (derived from token {})", sessionKey, sessionToken);
        releaseSessionInternal(sessionKey, false);
    }

    // --- SessionManager<DbSessionWrapper> Implementation ---

    @Override
    public DbSessionWrapper createSession(SessionKey key, Supplier<DbSessionWrapper> sessionFactory) throws Exception {
        log.info("Attempting to create local DB Connection for key: {} on instance {}", key, applicationInstanceId);
        DbSessionWrapper oldLocalSession = localActiveDbSessions.remove(key);
        if (oldLocalSession != null) {
            log.warn("Removed pre-existing local DB session for key {} before creating new one. Closing old connection.", key);
            oldLocalSession.closeConnection();
        }
        DbSessionWrapper newWrapper = sessionFactory.get(); // This connects the DB
        if (newWrapper == null || !newWrapper.isValid(2)) {
             if(newWrapper != null) newWrapper.closeConnection(); // cleanup if invalid
            log.error("DB Session factory created an invalid or null session for key {}.", key);
            throw new RuntimeException("DB Session factory created an invalid session for key: " + key);
        }
        localActiveDbSessions.put(key, newWrapper);
        log.info("Successfully created and cached local DB Connection for key: {} on instance {}", key, applicationInstanceId);
        return newWrapper;
    }

    @Override
    public Optional<DbSessionWrapper> getSession(SessionKey key) {
        if (key == null || !key.resourceType().startsWith("DB:")) return Optional.empty();

        DbSessionWrapper localWrapper = localActiveDbSessions.get(key);
        if (localWrapper != null) {
            try {
                if (!localWrapper.isValid(2)) { // Check validity with a short timeout
                    log.warn("Local DB session for key {} found invalid. Releasing.", key);
                    releaseSessionInternal(key, true); // Attempt full cleanup
                    return Optional.empty();
                }
                localWrapper.updateLastAccessedTime();
                DbSessionMetadata metadata = dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(key));
                if (metadata != null) {
                    metadata.setLastAccessedTime(localWrapper.getLastAccessedTime());
                    dbSessionMetadataRedisTemplate.opsForValue().set(
                        dbSessionMetadataRedisKey(key), metadata,
                        dbSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS);
                    log.debug("Local DB session found and metadata updated in Redis for key: {}", key);
                } else {
                    log.warn("Local DB session for key {} exists, but metadata missing in Redis. Recreating. Instance: {}", key, applicationInstanceId);
                     // Recreate metadata (token might be missing here or hard to find without scanning)
                    DbSessionMetadata newMeta = new DbSessionMetadata(key, localWrapper.getCreatedAt(), localWrapper.getLastAccessedTime(),
                                                                  null, applicationInstanceId, localWrapper.getDbType().name(),
                                                                  extractHostFromConnection(localWrapper.getConnection()),
                                                                  extractDbNameFromConnection(localWrapper.getConnection()),
                                                                  extractUserFromConnection(localWrapper.getConnection()));
                    dbSessionMetadataRedisTemplate.opsForValue().set(dbSessionMetadataRedisKey(key), newMeta, dbSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS);
                }
                return Optional.of(localWrapper);
            } catch (SQLException e) {
                 log.error("SQL Exception while validating local DB session for key {}. Releasing.", key, e);
                 releaseSessionInternal(key, true);
                 return Optional.empty();
            }
        }

        DbSessionMetadata metadata = dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(key));
        if (metadata != null) {
            log.warn("DB Session metadata in Redis for key {}, but no local Connection on instance {}. Expected on instance {}.",
                     key, this.applicationInstanceId, metadata.getApplicationInstanceId());
            if ((System.currentTimeMillis() - metadata.getLastAccessedTime()) > dbSessionConfigProperties.getTimeoutMs()) {
                log.warn("Stale DB session metadata in Redis for key {}. Instance {} may have died. Cleaning up.", key, metadata.getApplicationInstanceId());
                releaseSessionInternal(key, true);
            }
        } else {
            log.debug("No local DB session and no metadata in Redis for key: {}", key);
        }
        return Optional.empty();
    }

    @Override
    public boolean releaseSession(SessionKey key) {
        if (key == null || !key.resourceType().startsWith("DB:")) return false;
        return releaseSessionInternal(key, true);
    }

    private boolean releaseSessionInternal(SessionKey key, boolean deleteTokenFromMetadata) {
        if (key == null) return false;
        DbSessionWrapper localWrapper = localActiveDbSessions.remove(key);
        boolean wasLocal = (localWrapper != null);
        if (wasLocal) {
            log.info("Releasing local DB connection for key: {} on instance {}", key, applicationInstanceId);
            localWrapper.closeConnection();
        }
        DbSessionMetadata metadata = dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(key));
        if (metadata != null) {
            dbSessionMetadataRedisTemplate.delete(dbSessionMetadataRedisKey(key));
            log.debug("Deleted DB session metadata from Redis for key: {}", key);
            if (deleteTokenFromMetadata && metadata.getJwtToken() != null) {
                sessionKeyRedisTemplate.delete(dbTokenRedisKey(metadata.getJwtToken())); // Use dbTokenRedisKey
                log.debug("Deleted DB JWT mapping from Redis for token associated with key {} (from metadata)", key);
            }
        }
        return wasLocal;
    }

    @Override
    public void cleanupExpiredSessions() {
        log.info("Starting expired local DB session cleanup on instance {}...", applicationInstanceId);
        long timeoutMs = dbSessionConfigProperties.getTimeoutMs();
        long currentTime = System.currentTimeMillis();
        int expiredCount = 0;

        Iterator<Map.Entry<SessionKey, DbSessionWrapper>> iterator = localActiveDbSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SessionKey, DbSessionWrapper> entry = iterator.next();
            SessionKey key = entry.getKey();
            DbSessionWrapper wrapper = entry.getValue();
            boolean isValid = false;
            try {
                isValid = wrapper.isValid(2);
            } catch (Exception e) {
                log.error("Error validating DB session during cleanup for key {}: {}", key, e.getMessage(), e);
            }

            if (!isValid || (currentTime - wrapper.getLastAccessedTime()) > timeoutMs) {
                log.info("Local DB session for key: {} expired or invalid (last_accessed: {}ms ago, valid: {}). Closing and removing.",
                         key, (currentTime - wrapper.getLastAccessedTime()), isValid);
                wrapper.closeConnection();
                iterator.remove();
                DbSessionMetadata metadata = dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(key));
                if (metadata != null) {
                    dbSessionMetadataRedisTemplate.delete(dbSessionMetadataRedisKey(key));
                    if (metadata.getJwtToken() != null) {
                        sessionKeyRedisTemplate.delete(dbTokenRedisKey(metadata.getJwtToken()));
                    }
                }
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            log.info("Local DB session cleanup on instance {} removed {} sessions.", applicationInstanceId, expiredCount);
        } else {
            log.debug("Local DB session cleanup on instance {}: No sessions expired.", applicationInstanceId);
        }
    }

    @Override
    public void storeSession(SessionKey key, DbSessionWrapper sessionWrapper) {
        if (key == null || sessionWrapper == null || !key.resourceType().startsWith("DB:")) return;
        localActiveDbSessions.put(key, sessionWrapper);
        log.debug("Stored/Updated local DB session for key: {} on instance {}", key, applicationInstanceId);

        DbSessionMetadata metadata = dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(key));
        if (metadata != null) {
            if (!this.applicationInstanceId.equals(metadata.getApplicationInstanceId())) {
                 log.warn("StoreSession (DB) on instance {} for session whose metadata indicates owner {}. Key {}. Overwriting owner.",
                         this.applicationInstanceId, metadata.getApplicationInstanceId(), key);
                metadata.setApplicationInstanceId(this.applicationInstanceId);
            }
            metadata.setLastAccessedTime(sessionWrapper.getLastAccessedTime());
            // JWT in metadata is updated by init/keepalive.
            dbSessionMetadataRedisTemplate.opsForValue().set(
                dbSessionMetadataRedisKey(key), metadata,
                dbSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS);
        } else {
            log.warn("No Redis metadata for key {} during storeSession (DB). Creating. Instance: {}", key, applicationInstanceId);
            // Token might be hard to find here without scanning or if wrapper doesn't hold it
            DbSessionMetadata newMeta = new DbSessionMetadata(key, sessionWrapper.getCreatedAt(), sessionWrapper.getLastAccessedTime(),
                                                              null, this.applicationInstanceId, sessionWrapper.getDbType().name(),
                                                              extractHostFromConnection(sessionWrapper.getConnection()),
                                                              extractDbNameFromConnection(sessionWrapper.getConnection()),
                                                              extractUserFromConnection(sessionWrapper.getConnection()));
            dbSessionMetadataRedisTemplate.opsForValue().set(dbSessionMetadataRedisKey(key), newMeta, dbSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS);
        }
    }

    // --- JDBC URL and Driver Helper ---
    private String constructJdbcUrl(DbSessionCredentials credentials) {
        // Basic URL construction, might need more specific logic per DbType for parameters
        String baseUrl;
        switch (credentials.getDbType()) {
            case POSTGRESQL:
                baseUrl = "jdbc:postgresql://" + credentials.getHost() + ":" + credentials.getPort() + "/" + credentials.getDatabaseName();
                break;
            case MYSQL:
                baseUrl = "jdbc:mysql://" + credentials.getHost() + ":" + credentials.getPort() + "/" + credentials.getDatabaseName();
                break;
            case MARIADB:
                baseUrl = "jdbc:mariadb://" + credentials.getHost() + ":" + credentials.getPort() + "/" + credentials.getDatabaseName();
                break;
            case SQLSERVER:
                baseUrl = "jdbc:sqlserver://" + credentials.getHost() + ":" + credentials.getPort() + ";databaseName=" + credentials.getDatabaseName();
                break;
            case ORACLE: // Example: jdbc:oracle:thin:@//host:port/serviceName or SID
                baseUrl = "jdbc:oracle:thin:@//" + credentials.getHost() + ":" + credentials.getPort() + "/" + credentials.getDatabaseName();
                break;
            default:
                throw new IllegalArgumentException("Unsupported DbType for JDBC URL construction: " + credentials.getDbType());
        }
        StringBuilder jdbcUrl = new StringBuilder(baseUrl);
        if (credentials.isSslEnabled()) {
            jdbcUrl.append(credentials.getDbType() == DbType.MYSQL || credentials.getDbType() == DbType.MARIADB || credentials.getDbType() == DbType.POSTGRESQL ? "?sslmode=require" : ";encrypt=true"); // Example SSL params
        }
        if (credentials.getAdditionalProperties() != null && !credentials.getAdditionalProperties().isEmpty()) {
            boolean firstParam = !jdbcUrl.toString().contains("?");
            for (Map.Entry<String, String> entry : credentials.getAdditionalProperties().entrySet()) {
                jdbcUrl.append(firstParam ? "?" : "&");
                firstParam = false;
                jdbcUrl.append(entry.getKey()).append("=").append(entry.getValue()); // Basic, consider URL encoding for values
            }
        }
        return jdbcUrl.toString();
    }

    // Helper methods to extract info from connection (best effort, might not always work or be accurate)
    private String extractHostFromConnection(Connection conn) {
        try { return conn != null ? new java.net.URI(conn.getMetaData().getURL().substring(5)).getHost() : null; } // Remove "jdbc:"
        catch (Exception e) { return null; }
    }
     private String extractDbNameFromConnection(Connection conn) {
        try {String url = conn.getMetaData().getURL(); String[] parts = url.split("/"); return parts.length > 0 ? parts[parts.length-1].split("\\?")[0] : null; }
        catch (Exception e) { return null; }
    }
    private String extractUserFromConnection(Connection conn) {
        try { return conn != null ? conn.getMetaData().getUserName() : null; }
        catch (Exception e) { return null; }
    }

    // Optional: Get JDBC driver class name if Class.forName() is used
    // private String getDriverClassName(DbType dbType) { ... }
}
