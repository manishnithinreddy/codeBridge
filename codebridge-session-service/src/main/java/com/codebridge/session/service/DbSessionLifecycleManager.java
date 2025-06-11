package com.codebridge.session.service;

import com.codebridge.session.config.ApplicationInstanceIdProvider;
import com.codebridge.session.config.DbSessionConfigProperties;
import com.codebridge.session.config.JwtConfigProperties;
import com.codebridge.session.dto.DbSessionCredentials;
import com.codebridge.session.dto.DbSessionMetadata;
import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.exception.ResourceNotFoundException;
import com.codebridge.session.model.DbSessionWrapper;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.enums.DbType;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils; // For hashing dbConnectionAlias if needed for resourceId

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Properties;
import com.codebridge.session.dto.schema.ColumnSchemaInfo; // Added
import com.codebridge.session.dto.schema.DbSchemaInfoResponse; // Added
import com.codebridge.session.dto.schema.TableSchemaInfo; // Added
import java.sql.DatabaseMetaData; // Added
import java.sql.ResultSet; // Added
import java.util.ArrayList; // Added
import java.util.List; // Added
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service("dbSessionLifecycleManager")
public class DbSessionLifecycleManager {

    private static final Logger logger = LoggerFactory.getLogger(DbSessionLifecycleManager.class);
    private static final int DB_VALIDATION_TIMEOUT_SECONDS = 5;

    private final RedisTemplate<String, SessionKey> jwtToSessionKeyRedisTemplate;
    private final RedisTemplate<String, DbSessionMetadata> dbSessionMetadataRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final DbSessionConfigProperties dbConfig;
    private final JwtConfigProperties jwtConfig; // For session token expiry
    private final ApplicationInstanceIdProvider instanceIdProvider;
    private final String applicationInstanceId;

    // Make this package-private for testing
    final ConcurrentHashMap<SessionKey, DbSessionWrapper> localActiveDbSessions = new ConcurrentHashMap<>();

    public DbSessionLifecycleManager(
            RedisTemplate<String, SessionKey> jwtToSessionKeyRedisTemplate,
            RedisTemplate<String, DbSessionMetadata> dbSessionMetadataRedisTemplate,
            JwtTokenProvider jwtTokenProvider,
            DbSessionConfigProperties dbConfig,
            JwtConfigProperties jwtConfig,
            ApplicationInstanceIdProvider instanceIdProvider) {
        this.jwtToSessionKeyRedisTemplate = jwtToSessionKeyRedisTemplate;
        this.dbSessionMetadataRedisTemplate = dbSessionMetadataRedisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.dbConfig = dbConfig;
        this.jwtConfig = jwtConfig;
        this.instanceIdProvider = instanceIdProvider;
        this.applicationInstanceId = this.instanceIdProvider.getInstanceId();
    }

    // --- Redis Key Helpers ---
    // Make this package-private for testing
    String dbTokenRedisKey(String sessionToken) {
        return "session:db:token:" + sessionToken;
    }

    // Make this package-private for testing
    String dbSessionMetadataRedisKey(SessionKey sessionKey) {
        // Ensure consistent key format, resourceId here is derived from dbConnectionAlias
        return "session:db:metadata:" + sessionKey.platformUserId() + ":" + sessionKey.resourceId() + ":" + sessionKey.sessionType();
    }

    // --- Public API Methods ---

    public SessionResponse initDbSession(UUID platformUserId, String dbConnectionAlias, DbSessionCredentials credentials) {
        // Create a consistent resourceId from dbConnectionAlias (e.g., hash it)
        // For simplicity, using UUID generated from alias string bytes.
        UUID resourceId = UUID.nameUUIDFromBytes(dbConnectionAlias.getBytes(StandardCharsets.UTF_8));
        String sessionType = "DB:" + credentials.getDbType().name();
        SessionKey sessionKey = new SessionKey(platformUserId, resourceId, sessionType);

        logger.info("Initializing DB session for key: {}, Alias: {}", sessionKey, dbConnectionAlias);

        forceReleaseDbSessionByKey(sessionKey, false); // Clean up any stale session for this exact key

        Connection jdbcConnection;
        try {
            jdbcConnection = createJdbcConnection(credentials);
            logger.info("JDBC Connection established for {}", sessionKey);
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("JDBC Connection failed for {}: {}", sessionKey, e.getMessage(), e);
            throw new RemoteOperationException("DB connection failed: " + e.getMessage(), e);
        }

        DbSessionWrapper wrapper = new DbSessionWrapper(jdbcConnection, sessionKey, credentials.getDbType());
        localActiveDbSessions.put(sessionKey, wrapper);

        String sessionToken = jwtTokenProvider.generateToken(sessionKey, platformUserId);
        long currentTime = Instant.now().toEpochMilli();
        long expiresAt = currentTime + jwtConfig.getExpirationMs(); // Use JWT general expiration

        DbSessionMetadata metadata = new DbSessionMetadata(
                sessionKey, currentTime, currentTime, expiresAt, sessionToken, applicationInstanceId,
                credentials.getDbType().name(), credentials.getHost(), credentials.getDatabaseName(), credentials.getUsername()
        );

        jwtToSessionKeyRedisTemplate.opsForValue().set(dbTokenRedisKey(sessionToken), sessionKey, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        dbSessionMetadataRedisTemplate.opsForValue().set(dbSessionMetadataRedisKey(sessionKey), metadata, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);

        logger.info("DB session initialized successfully for {}. Token: {}", sessionKey, sessionToken);
        return new SessionResponse(sessionToken, sessionType, "ACTIVE", currentTime, expiresAt);
    }

    public KeepAliveResponse keepAliveDbSession(String sessionToken) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(sessionToken);
        if (claims == null) {
            throw new RemoteOperationException("Invalid session token for keepalive.");
        }
        SessionKey sessionKey = jwtToSessionKeyRedisTemplate.opsForValue().get(dbTokenRedisKey(sessionToken));
        if (sessionKey == null) {
            throw new ResourceNotFoundException("Session not found or expired for keepalive (token mapping missing). Token: " + sessionToken);
        }
        if (!sessionKey.sessionType().startsWith("DB:")) {
            throw new RemoteOperationException("Invalid session type for DB keepalive.");
        }

        DbSessionWrapper wrapper = localActiveDbSessions.get(sessionKey);
        if (wrapper == null || !wrapper.isValid(DB_VALIDATION_TIMEOUT_SECONDS)) {
            DbSessionMetadata metadata = dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(sessionKey));
            if (metadata == null || metadata.expiresAt() < Instant.now().toEpochMilli()) {
                 forceReleaseDbSessionByKey(sessionKey, true);
                 throw new ResourceNotFoundException("DB Session expired or not found in metadata. Token: " + sessionToken);
            }
            if (wrapper != null && !wrapper.isValid(DB_VALIDATION_TIMEOUT_SECONDS)) { // Local but invalid
                logger.warn("Local DB session for key {} is invalid. Releasing.", sessionKey);
                forceReleaseDbSessionByKey(sessionKey, true); // Clean up local and Redis
                throw new ResourceNotFoundException("DB Session was found locally but is invalid. Please re-initialize.");
            }
            logger.warn("Keepalive for DB session {} not local to this instance {}. Updating metadata only.", sessionKey, applicationInstanceId);
        } else {
             wrapper.updateLastAccessedTime();
        }

        String newSessionToken = jwtTokenProvider.generateToken(sessionKey, sessionKey.platformUserId());
        long currentTime = Instant.now().toEpochMilli();
        long newExpiresAt = currentTime + jwtConfig.getExpirationMs();

        DbSessionMetadata currentMetadata = dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(sessionKey));
        String dbType = currentMetadata != null ? currentMetadata.dbType() : ""; // Preserve from old metadata if possible
        String dbHost = currentMetadata != null ? currentMetadata.dbHost() : "";
        String dbName = currentMetadata != null ? currentMetadata.dbName() : "";
        String dbUsername = currentMetadata != null ? currentMetadata.dbUsername() : "";

        DbSessionMetadata newMetadata = new DbSessionMetadata(
            sessionKey,
            (currentMetadata != null ? currentMetadata.createdAt() : currentTime),
            currentTime, newExpiresAt, newSessionToken, applicationInstanceId,
            dbType, dbHost, dbName, dbUsername
        );

        jwtToSessionKeyRedisTemplate.opsForValue().set(dbTokenRedisKey(newSessionToken), sessionKey, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        dbSessionMetadataRedisTemplate.opsForValue().set(dbSessionMetadataRedisKey(sessionKey), newMetadata, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        if (!sessionToken.equals(newSessionToken)) {
            jwtToSessionKeyRedisTemplate.delete(dbTokenRedisKey(sessionToken));
        }

        logger.info("DB session keepalive successful for {}. New token issued.", sessionKey);
        return new KeepAliveResponse(newSessionToken, "ACTIVE", newExpiresAt);
    }

    public void releaseDbSession(String sessionToken) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(sessionToken);
        if (claims == null) {
            logger.warn("Attempted to release DB session with invalid token.");
            return;
        }
        SessionKey sessionKey = jwtToSessionKeyRedisTemplate.opsForValue().get(dbTokenRedisKey(sessionToken));
        if (sessionKey == null) {
            logger.warn("No active DB session found for token to release: {}", sessionToken);
            return;
        }
        forceReleaseDbSessionByKey(sessionKey, true);
        logger.info("DB session released for key {} by token.", sessionKey);
    }

    public void forceReleaseDbSessionByKey(SessionKey key, boolean wasTokenBasedRelease) {
        logger.debug("Forcing release for DB session key: {}. Token-based: {}", key, wasTokenBasedRelease);
        DbSessionWrapper wrapper = localActiveDbSessions.remove(key);
        if (wrapper != null) {
            wrapper.closeConnection();
            logger.info("Closed local JDBC connection for key: {}", key);
        }

        String tokenToDelete = null;
        DbSessionMetadata metadata = dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(key));
        if (metadata != null) {
            tokenToDelete = metadata.sessionToken();
            dbSessionMetadataRedisTemplate.delete(dbSessionMetadataRedisKey(key));
            logger.debug("Deleted DB session metadata from Redis for key: {}", key);
        }

        if (tokenToDelete != null) { // Always try to delete token mapping if metadata existed or was token based
             jwtToSessionKeyRedisTemplate.delete(dbTokenRedisKey(tokenToDelete));
             logger.debug("Deleted DB token-to-key mapping from Redis for token: {}", tokenToDelete);
        }
    }

    @Scheduled(fixedDelayString = "${codebridge.session.db.defaultTimeoutMs:1800000}", initialDelayString = "120000")
    public void cleanupExpiredDbSessions() {
        logger.info("Running scheduled cleanup of expired DB sessions on instance {}", applicationInstanceId);
        long now = Instant.now().toEpochMilli();
        long sessionTimeoutMs = dbConfig.getDefaultTimeoutMs();

        localActiveDbSessions.forEach((key, wrapper) -> {
            if (!wrapper.isValid(DB_VALIDATION_TIMEOUT_SECONDS)) {
                logger.info("Local DB session for key {} is invalid or closed. Removing from local cache and releasing.", key);
                forceReleaseDbSessionByKey(key, false);
            } else if (now - wrapper.getLastAccessedTime() > sessionTimeoutMs) {
                logger.info("Local DB session for key {} expired ({} ms idle). Releasing.", key, now - wrapper.getLastAccessedTime());
                forceReleaseDbSessionByKey(key, false);
            }
        });
    }

    // --- Helper Methods ---
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
        if (creds.isSslEnabled()) {
            // Basic SSL, might need more properties depending on driver and server config
            props.setProperty("ssl", "true");
            props.setProperty("sslmode", "prefer"); // Or "require", "verify-full", etc.
        }
        if (creds.getAdditionalProperties() != null) {
            creds.getAdditionalProperties().forEach(props::setProperty);
        }

        // Ensure driver is loaded (optional for modern JDBC, but good practice for some environments)
        // Class.forName(getDriverClassName(creds.getDbType()));

        return DriverManager.getConnection(jdbcUrl, props);
    }

    // private String getDriverClassName(DbType dbType) throws ClassNotFoundException { ... } // Helper if Class.forName needed

    // --- Getters for internal components (e.g., for operational services) ---
    public Optional<DbSessionWrapper> getLocalSession(SessionKey key) {
        return Optional.ofNullable(localActiveDbSessions.get(key));
    }

    public Optional<DbSessionMetadata> getSessionMetadata(SessionKey key) {
        return Optional.ofNullable(dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(key)));
    }

    public void updateSessionAccessTime(SessionKey key, DbSessionWrapper wrapper) {
        if (wrapper == null || !wrapper.isValid(DB_VALIDATION_TIMEOUT_SECONDS)) return;

        wrapper.updateLastAccessedTime();

        DbSessionMetadata currentMetadata = dbSessionMetadataRedisTemplate.opsForValue().get(dbSessionMetadataRedisKey(key));
        if (currentMetadata != null) {
            DbSessionMetadata updatedMetadata = new DbSessionMetadata(
                currentMetadata.sessionKey(), currentMetadata.createdAt(), Instant.now().toEpochMilli(),
                currentMetadata.expiresAt(), currentMetadata.sessionToken(), this.applicationInstanceId,
                currentMetadata.dbType(), currentMetadata.dbHost(), currentMetadata.dbName(), currentMetadata.dbUsername()
            );
            dbSessionMetadataRedisTemplate.opsForValue().set(dbSessionMetadataRedisKey(key), updatedMetadata, jwtConfig.getExpirationMs(), TimeUnit.MILLISECONDS);
        } else {
            logger.warn("No metadata found in Redis to update access time for DB session key: {}", key);
        }
    }

    public DbSchemaInfoResponse getDetailedSchemaInfo(Connection connection) {
        DbSchemaInfoResponse response = new DbSchemaInfoResponse();
        try {
            DatabaseMetaData metaData = connection.getMetaData();

            response.setDatabaseProductName(metaData.getDatabaseProductName());
            response.setDatabaseProductVersion(metaData.getDatabaseProductVersion());
            response.setDriverName(metaData.getDriverName());
            response.setDriverVersion(metaData.getDriverVersion());

            String catalog = connection.getCatalog();
            String schema = connection.getSchema(); // May return null for some DBs, or need specific schema pattern

            logger.debug("Fetching tables for catalog: '{}', schema: '{}'", catalog, schema);

            // Get tables and views
            try (ResultSet tablesRs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE", "VIEW"})) {
                while (tablesRs.next()) {
                    String tableName = tablesRs.getString("TABLE_NAME");
                    String tableType = tablesRs.getString("TABLE_TYPE");
                    String remarks = tablesRs.getString("REMARKS");

                    TableSchemaInfo tableInfo = new TableSchemaInfo(tableName, tableType, remarks);
                    logger.debug("Fetching columns for table: '{}' (Type: {})", tableName, tableType);

                    // Get columns for each table
                    try (ResultSet columnsRs = metaData.getColumns(catalog, schema, tableName, "%")) {
                        while (columnsRs.next()) {
                            String columnName = columnsRs.getString("COLUMN_NAME");
                            String columnTypeName = columnsRs.getString("TYPE_NAME");
                            int nullableInt = columnsRs.getInt("NULLABLE");
                            boolean isNullable = nullableInt == DatabaseMetaData.columnNullable;
                            Integer columnSize = columnsRs.getObject("COLUMN_SIZE", Integer.class);
                            Integer decimalDigits = columnsRs.getObject("DECIMAL_DIGITS", Integer.class);

                            tableInfo.addColumn(new ColumnSchemaInfo(columnName, columnTypeName, isNullable, columnSize, decimalDigits));
                        }
                    }
                    response.addTable(tableInfo);
                }
            }
            return response;
        } catch (SQLException e) {
            logger.error("SQLException while fetching detailed schema info: {}", e.getMessage(), e);
            throw new RemoteOperationException("Failed to retrieve detailed database schema information: " + e.getMessage(), e);
        }
    }
    
    // Method to get schema information for a specific schema
    public DbSchemaInfoResponse getSchemaInfo(DbSessionWrapper wrapper, String schemaName, int limit, int offset) {
        if (wrapper == null || !wrapper.isValid(DB_VALIDATION_TIMEOUT_SECONDS)) {
            throw new ResourceNotFoundException("Database session is not valid or has expired");
        }
        
        return getDetailedSchemaInfo(wrapper.getConnection());
    }
    
    // Method to execute a SQL query
    public Object executeQuery(DbSessionWrapper wrapper, String sql) {
        if (wrapper == null || !wrapper.isValid(DB_VALIDATION_TIMEOUT_SECONDS)) {
            throw new ResourceNotFoundException("Database session is not valid or has expired");
        }
        
        // This is a placeholder implementation
        // In a real implementation, this would execute the SQL query and return the results
        return new Object(); // Placeholder
    }
}

