package com.codebridge.session.service;

import com.codebridge.session.config.DbSessionConfigProperties;
import com.codebridge.session.config.JwtConfigProperties;
import com.codebridge.session.dto.DbSessionCredentials;
import com.codebridge.session.dto.DbSessionMetadata;
import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.enums.DbType;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.sessions.DbSessionWrapper; // Corrected import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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

@Service("dbSessionManager") // Using the qualifier from the plan
public class DbSessionLifecycleManager { // Not implementing SessionManager<DbSessionWrapper> directly yet
                                      // to match SshSessionLifecycleManager style first

    private static final Logger log = LoggerFactory.getLogger(DbSessionLifecycleManager.class);

    private final RedisTemplate<String, SessionKey> sessionKeyRedisTemplate;
    private final RedisTemplate<String, DbSessionMetadata> dbSessionMetadataRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final DbSessionConfigProperties dbSessionConfigProperties;
    private final JwtConfigProperties jwtConfigProperties;
    private final ApplicationInstanceIdProvider applicationInstanceIdProvider;

    private final ConcurrentMap<SessionKey, DbSessionWrapper> localActiveDbSessions = new ConcurrentHashMap<>();
    private final String applicationInstanceId;

    private static final int DB_CONNECT_TIMEOUT_SECONDS = 15;

    public DbSessionLifecycleManager(
            @Qualifier("sessionKeyRedisTemplate") RedisTemplate<String, SessionKey> sessionKeyRedisTemplate,
            @Qualifier("dbSessionMetadataRedisTemplate") RedisTemplate<String, DbSessionMetadata> dbSessionMetadataRedisTemplate,
            JwtTokenProvider jwtTokenProvider,
            DbSessionConfigProperties dbSessionConfigProperties,
            JwtConfigProperties jwtConfigProperties,
            ApplicationInstanceIdProvider applicationInstanceIdProvider) {
        this.sessionKeyRedisTemplate = sessionKeyRedisTemplate;
        this.dbSessionMetadataRedisTemplate = dbSessionMetadataRedisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.dbSessionConfigProperties = dbSessionConfigProperties;
        this.jwtConfigProperties = jwtConfigProperties;
        this.applicationInstanceIdProvider = applicationInstanceIdProvider;
        this.applicationInstanceId = this.applicationInstanceIdProvider.getInstanceId() + ":db"; // Append type
        log.info("DbSessionLifecycleManager initialized with applicationInstanceId: {}", this.applicationInstanceId);
    }

    // --- Redis Key Helpers ---
    private String dbTokenRedisKey(String token) {
        return "db:token:" + token; // Differentiated from ssh:token
    }

    private String dbSessionMetadataRedisKey(SessionKey key) {
        return "db:meta:" + key.userId().toString() + ":" + key.resourceId().toString() + ":" + key.resourceType();
    }

    // --- Public Lifecycle Methods ---

    public SessionResponse initDbSession(UUID platformUserId, String dbConnectionAlias, DbSessionCredentials credentials) {
        if (credentials == null) {
            throw new IllegalArgumentException("Database credentials must be provided.");
        }
        UUID resourceId = UUID.nameUUIDFromBytes((platformUserId.toString() + ":" + dbConnectionAlias).getBytes(StandardCharsets.UTF_8));
        String resourceType = "DB:" + credentials.getDbType().name();
        SessionKey sessionKey = new SessionKey(platformUserId, resourceId, resourceType);

        log.info("Initializing DB session for key: {}, alias: {} on instance {}", sessionKey, dbConnectionAlias, applicationInstanceId);
        forceReleaseDbSessionByKey(sessionKey, false); // Clean start

        Supplier<DbSessionWrapper> dbSessionFactory = () -> {
            Connection connection;
            String jdbcUrl = constructJdbcUrl(credentials);
            try {
                // Consider pre-loading drivers if Class.forName is needed for some older JDBCs
                // DriverManager.setLoginTimeout(DB_CONNECT_TIMEOUT_SECONDS); // Set on DriverManager globally
                log.debug("Attempting DB connection: {} user: {}", jdbcUrl, credentials.getUsername());
                connection = DriverManager.getConnection(jdbcUrl, credentials.getUsername(), credentials.getPassword());
                log.info("DB connection established for key {} (alias: {}) on instance {}", sessionKey, dbConnectionAlias, applicationInstanceId);
                return new DbSessionWrapper(sessionKey, connection, credentials.getDbType());
            } catch (SQLException e) {
                log.error("SQLException for key {} (alias: {}), JDBC URL {}: {}", sessionKey, dbConnectionAlias, jdbcUrl, e.getMessage(), e);
                throw new RuntimeException("Failed to create DB session via JDBC: " + e.getMessage(), e);
            }
        };

        DbSessionWrapper sessionWrapper;
        try {
            sessionWrapper = dbSessionFactory.get();
            if (!sessionWrapper.isValid(DB_CONNECT_TIMEOUT_SECONDS > 0 ? DB_CONNECT_TIMEOUT_SECONDS : 2)) { // Check validity after connection
                throw new RuntimeException("Established DB connection is invalid.");
            }
            localActiveDbSessions.put(sessionKey, sessionWrapper);
            log.info("Local DB session created and cached for key {}", sessionKey);
        } catch (Exception e) {
            log.error("Failed to create and store local DB session for key {}: {}", sessionKey, e.getMessage(), e);
            throw e;
        }

        String jwtToken = jwtTokenProvider.generateToken(sessionKey);
        long currentTime = System.currentTimeMillis();
        DbSessionMetadata metadata = new DbSessionMetadata(
            sessionKey, currentTime, currentTime, jwtToken, this.applicationInstanceId,
            credentials.getDbType().name(), credentials.getHost(), credentials.getDatabaseName(), credentials.getUsername()
        );

        try {
            sessionKeyRedisTemplate.opsForValue().set(
                dbTokenRedisKey(jwtToken), sessionKey,
                jwtConfigProperties.getExpirationMs(), TimeUnit.MILLISECONDS);
            dbSessionMetadataRedisTemplate.opsForValue().set(
                dbSessionMetadataRedisKey(sessionKey), metadata,
                dbSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            log.info("DB session metadata and token mapping stored in Redis for key {}", sessionKey);
        } catch (Exception e) {
            log.error("Failed to store DB session state in Redis for key {}. Cleaning up local session.", sessionKey, e);
            forceReleaseDbSessionByKey(sessionKey, false);
            throw new RuntimeException("Failed to store DB session state in Redis: " + e.getMessage(), e);
        }

        return new SessionResponse(jwtToken, jwtConfigProperties.getExpirationMs(), platformUserId, resourceId, resourceType);
    }

    public Optional<KeepAliveResponse> keepAliveDbSession(String sessionToken) {
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        if (keyOpt.isEmpty() || !keyOpt.get().resourceType().startsWith("DB:")) {
            log.warn("Keepalive failed: Invalid, expired, or non-DB JWT token: {}", sessionToken);
            sessionKeyRedisTemplate.delete(dbTokenRedisKey(sessionToken));
            return Optional.empty();
        }
        SessionKey sessionKey = keyOpt.get();

        String metadataRedisKey = dbSessionMetadataRedisKey(sessionKey);
        DbSessionMetadata metadata = dbSessionMetadataRedisTemplate.opsForValue().get(metadataRedisKey);

        if (metadata == null) {
            log.warn("Keepalive failed: No DB session metadata in Redis for key {} (token {}). Cleaning token.", sessionKey, sessionToken);
            sessionKeyRedisTemplate.delete(dbTokenRedisKey(sessionToken));
            return Optional.empty();
        }

        metadata.setLastAccessedTime(System.currentTimeMillis());
        String newJwtToken = jwtTokenProvider.generateToken(sessionKey);
        metadata.setJwtToken(newJwtToken); // Update to the new active token
        metadata.setApplicationInstanceId(this.applicationInstanceId); // This instance takes/confirms ownership

        dbSessionMetadataRedisTemplate.opsForValue().set(
            metadataRedisKey, metadata,
            dbSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS);

        sessionKeyRedisTemplate.delete(dbTokenRedisKey(sessionToken)); // Delete old
        sessionKeyRedisTemplate.opsForValue().set(
            dbTokenRedisKey(newJwtToken), sessionKey,
            jwtConfigProperties.getExpirationMs(), TimeUnit.MILLISECONDS);
        log.info("DB Session Keepalive for key {}. New JWT. Hosting instance: {}", sessionKey, this.applicationInstanceId);

        DbSessionWrapper localWrapper = localActiveDbSessions.get(sessionKey);
        if (localWrapper != null) {
            try {
                if (localWrapper.isValid(2)) {
                    localWrapper.updateLastAccessedTime();
                    log.debug("Updated lastAccessedTime for local DbSessionWrapper for key {} during keepalive.", sessionKey);
                } else {
                    log.warn("Local DB session for key {} found invalid during keepalive. Releasing.", sessionKey);
                    forceReleaseDbSessionByKey(sessionKey, true); // true: token was just refreshed, ensure old one from meta is gone
                    return Optional.empty(); // Session was bad, effectively not kept alive
                }
            } catch (Exception e) {
                 log.error("Error validating/updating local DB session during keepalive for key {}: {}. Releasing.", sessionKey, e.getMessage(), e);
                 forceReleaseDbSessionByKey(sessionKey, true);
                 return Optional.empty();
            }
        } else {
            log.info("No local DB session for key {} on instance {} during keepalive, though metadata now points here.",
                     sessionKey, this.applicationInstanceId);
        }
        return Optional.of(new KeepAliveResponse(newJwtToken, jwtConfigProperties.getExpirationMs()));
    }

    public void releaseDbSession(String sessionToken) {
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        sessionKeyRedisTemplate.delete(dbTokenRedisKey(sessionToken)); // Always delete token mapping

        if (keyOpt.isEmpty() || !keyOpt.get().resourceType().startsWith("DB:")) {
            log.warn("Release attempt with invalid, expired, or non-DB token: {}. Token mapping deleted.", sessionToken);
            return;
        }
        SessionKey sessionKey = keyOpt.get();
        log.info("Attempting to release DB session for key: {} (from token {})", sessionKey, sessionToken);
        forceReleaseDbSessionByKey(sessionKey, false); // false: token already deleted by this method
    }

    private void forceReleaseDbSessionByKey(SessionKey key, boolean wasTokenBasedRelease) {
        if (key == null) return;
        log.info("Force releasing DB session for key: {} on instance {}", key, applicationInstanceId);
        DbSessionWrapper localWrapper = localActiveDbSessions.remove(key);
        if (localWrapper != null) {
            log.debug("Closing local DB connection for key {}", key);
            localWrapper.closeConnection();
        }

        String metadataRedisKey = dbSessionMetadataRedisKey(key);
        DbSessionMetadata metadata = null;
        if (wasTokenBasedRelease) { // If true, need metadata to find the token if it wasn't the one passed
             metadata = dbSessionMetadataRedisTemplate.opsForValue().get(metadataRedisKey);
        }
        dbSessionMetadataRedisTemplate.delete(metadataRedisKey);
        log.debug("Deleted DB session metadata from Redis for key {}", key);

        if (wasTokenBasedRelease && metadata != null && metadata.getJwtToken() != null) {
            sessionKeyRedisTemplate.delete(dbTokenRedisKey(metadata.getJwtToken()));
            log.debug("Deleted DB JWT mapping from Redis for token in metadata associated with key {}", key);
        }
    }

    @Scheduled(fixedDelayString = "${codebridge.session.db.timeout-ms:300000}", initialDelay = 150000)
    public void cleanupExpiredDbSessions() {
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
                log.warn("Exception validating DB connection for key {} during cleanup: {}", key, e.getMessage());
            }

            if (!isValid || (currentTime - wrapper.getLastAccessedTime()) > timeoutMs) {
                log.info("Local DB session for key: {} expired/invalid (last_accessed: {}ms ago, valid: {}). Cleaning up.",
                         key, (currentTime - wrapper.getLastAccessedTime()), isValid);
                iterator.remove();
                forceReleaseDbSessionByKey(key, true); // true: ensure token from metadata is cleaned
                expiredCount++;
            }
        }
         if (expiredCount > 0) {
            log.info("Local DB session cleanup on instance {} removed {} sessions.", applicationInstanceId, expiredCount);
        } else {
            log.debug("Local DB session cleanup on instance {}: No sessions expired or found invalid.", applicationInstanceId);
        }
    }

    // --- Helper methods for operational endpoints ---
    public Optional<DbSessionWrapper> getLocalSession(SessionKey key) {
        if (key == null || !key.resourceType().startsWith("DB:")) return Optional.empty();
        DbSessionWrapper wrapper = localActiveDbSessions.get(key);
        if (wrapper != null) {
            try {
                if (wrapper.isValid(2)) {
                    // wrapper.updateLastAccessedTime(); // Done by updateSessionAccessTime called by controller
                    return Optional.of(wrapper);
                } else {
                    log.warn("Local DB session for key {} is invalid. Releasing.", key);
                    forceReleaseDbSessionByKey(key, true);
                    return Optional.empty();
                }
            } catch (Exception e) {
                 log.error("Error validating local DB session for key {}: {}. Releasing.", key, e.getMessage(), e);
                 forceReleaseDbSessionByKey(key, true);
                 return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Optional<DbSessionMetadata> getSessionMetadata(SessionKey key) {
        if (key == null || !key.resourceType().startsWith("DB:")) return Optional.empty();
        return Optional.ofNullable(dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(key)));
    }

    public void updateSessionAccessTime(SessionKey key) {
        if (key == null || !key.resourceType().startsWith("DB:")) return;

        DbSessionWrapper localWrapper = localActiveDbSessions.get(key);
        long newLastAccessedTime = System.currentTimeMillis();

        if (localWrapper != null) { // No is Valid check here, just update time if it exists
            localWrapper.updateLastAccessedTime();
            newLastAccessedTime = localWrapper.getLastAccessedTime();
            log.debug("Updated local DbSessionWrapper lastAccessTime for key {}", key);
        }

        DbSessionMetadata metadata = dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(key));
        if (metadata != null) {
            metadata.setLastAccessedTime(newLastAccessedTime);
            metadata.setApplicationInstanceId(this.applicationInstanceId); // Mark this instance as current host
            dbSessionMetadataRedisTemplate.opsForValue().set(
                dbSessionMetadataRedisKey(key), metadata,
                dbSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            log.debug("Updated lastAccessedTime and hostingInstanceId in Redis DB metadata for key {}", key);
        } else {
            log.warn("Attempted to update access time for DB key {}, but no metadata found in Redis.", key);
             if (localWrapper != null) { // Local session exists, but no metadata! Recreate.
                 log.warn("Re-creating missing DB metadata for active local session on key {} during access time update.", key);
                 DbSessionMetadata newMeta = new DbSessionMetadata(key, localWrapper.getCreatedAt(), newLastAccessedTime,
                                                                  null, this.applicationInstanceId, localWrapper.getDbType().name(),
                                                                  extractHostFromConnection(localWrapper.getConnection()),
                                                                  extractDbNameFromConnection(localWrapper.getConnection()),
                                                                  extractUserFromConnection(localWrapper.getConnection()));
                // We don't know the JWT easily here to put in metadata. Token map is JWT -> Key.
                // Metadata's activeJwtToken is updated by init/keepalive.
                dbSessionMetadataRedisTemplate.opsForValue().set(
                    dbSessionMetadataRedisKey(key), newMeta,
                    dbSessionConfigProperties.getTimeoutMs(), TimeUnit.MILLISECONDS);
            }
        }
    }

    // --- JDBC URL Construction & Info Extraction ---
    private String constructJdbcUrl(DbSessionCredentials credentials) {
        String baseUrl;
        switch (credentials.getDbType()) {
            case POSTGRESQL: baseUrl = "jdbc:postgresql://" + credentials.getHost() + ":" + credentials.getPort() + "/" + credentials.getDatabaseName(); break;
            case MYSQL: baseUrl = "jdbc:mysql://" + credentials.getHost() + ":" + credentials.getPort() + "/" + credentials.getDatabaseName(); break;
            case MARIADB: baseUrl = "jdbc:mariadb://" + credentials.getHost() + ":" + credentials.getPort() + "/" + credentials.getDatabaseName(); break;
            case SQLSERVER: baseUrl = "jdbc:sqlserver://" + credentials.getHost() + ":" + credentials.getPort() + ";databaseName=" + credentials.getDatabaseName(); break;
            case ORACLE: baseUrl = "jdbc:oracle:thin:@//" + credentials.getHost() + ":" + credentials.getPort() + "/" + credentials.getDatabaseName(); break;
            default: throw new IllegalArgumentException("Unsupported DbType for JDBC URL: " + credentials.getDbType());
        }
        StringBuilder jdbcUrl = new StringBuilder(baseUrl);
        Map<String, String> props = credentials.getAdditionalProperties() != null ? credentials.getAdditionalProperties() : new ConcurrentHashMap<>();
        if (credentials.isSslEnabled()) { // Basic SSL params, might need more per driver
            if (credentials.getDbType() == DbType.POSTGRESQL) props.putIfAbsent("sslmode", "require");
            else if (credentials.getDbType() == DbType.MYSQL || credentials.getDbType() == DbType.MARIADB) props.putIfAbsent("useSSL", "true");
            else if (credentials.getDbType() == DbType.SQLSERVER) props.putIfAbsent("encrypt", "true");
        }
        if (!props.isEmpty()) {
            boolean firstParam = !jdbcUrl.toString().contains("?");
            for (Map.Entry<String, String> entry : props.entrySet()) {
                jdbcUrl.append(firstParam ? "?" : "&");
                firstParam = false;
                try { // Basic URL encoding for values
                    jdbcUrl.append(entry.getKey()).append("=").append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
                } catch (java.io.UnsupportedEncodingException e) { /* Should not happen with UTF-8 */ }
            }
        }
        return jdbcUrl.toString();
    }
    private String extractHostFromConnection(Connection conn) {
        try { if (conn == null || conn.isClosed()) return null; String url = conn.getMetaData().getURL(); if (url == null) return null; java.net.URI uri = new java.net.URI(url.substring(5)); return uri.getHost(); }
        catch (Exception e) { log.trace("Could not extract host from connection metadata", e); return null; }
    }
    private String extractDbNameFromConnection(Connection conn) {
        try { if (conn == null || conn.isClosed()) return null; String dbName = conn.getCatalog(); if (dbName != null) return dbName; String url = conn.getMetaData().getURL(); if (url == null) return null; String[] parts = url.split("/"); if (parts.length > 0) { String lastPart = parts[parts.length-1]; return lastPart.split("\\?")[0].split(";")[0]; } return null; }
        catch (Exception e) { log.trace("Could not extract dbName from connection metadata", e); return null; }
    }
    private String extractUserFromConnection(Connection conn) {
        try { if (conn == null || conn.isClosed()) return null; return conn.getMetaData().getUserName().split("@")[0]; }
        catch (Exception e) { log.trace("Could not extract user from connection metadata", e); return null; }
    }
}
