package com.codebridge.session.controller;

import com.codebridge.session.dto.DbSchemaInfoResponse; // New DTO to be created
import com.codebridge.session.dto.DbSessionMetadata;
import com.codebridge.session.exception.AccessDeniedException;
import com.codebridge.session.exception.RemoteOperationException; // Can be reused or make a DbOperationException
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.sessions.DbSessionWrapper;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.ApplicationInstanceIdProvider;
import com.codebridge.session.service.DbSessionLifecycleManager; // Changed from Ssh to Db
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Optional;

@RestController
@RequestMapping("/ops/db/{sessionToken}") // Base path for DB operations
public class DbOperationController {

    private static final Logger log = LoggerFactory.getLogger(DbOperationController.class);

    private final DbSessionLifecycleManager dbSessionLifecycleManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationInstanceIdProvider instanceIdProvider;

    public DbOperationController(
            @Qualifier("dbSessionManager") DbSessionLifecycleManager dbSessionLifecycleManager,
            JwtTokenProvider jwtTokenProvider,
            ApplicationInstanceIdProvider instanceIdProvider) {
        this.dbSessionLifecycleManager = dbSessionLifecycleManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.instanceIdProvider = instanceIdProvider;
    }

    private Connection getValidatedLocalDbConnection(String sessionToken, String operationName) throws AccessDeniedException, RemoteOperationException {
        log.debug("Validating DB session token for {} operation: {}", operationName, sessionToken);
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        if (keyOpt.isEmpty() || !keyOpt.get().resourceType().startsWith("DB:")) {
            throw new AccessDeniedException("Invalid, expired, or non-DB session token for " + operationName + ".");
        }
        SessionKey sessionKey = keyOpt.get();
        log.debug("DB Session key {} extracted from token for {}", sessionKey, operationName);

        Optional<DbSessionMetadata> metadataOpt = dbSessionLifecycleManager.getSessionMetadata(sessionKey);
        if (metadataOpt.isEmpty()) {
            dbSessionLifecycleManager.releaseDbSession(sessionToken); // Clean up token as metadata is gone
            throw new AccessDeniedException("DB Session metadata not found or expired for " + operationName + ". Please re-initialize.");
        }
        DbSessionMetadata metadata = metadataOpt.get();

        if (!instanceIdProvider.getInstanceId().equals(metadata.getApplicationInstanceId())) {
            log.warn("DB Session for key {} (token: {}) is hosted on instance {} but operation {} requested on instance {}.",
                     sessionKey, sessionToken, metadata.getApplicationInstanceId(), operationName, instanceIdProvider.getInstanceId());
            throw new AccessDeniedException("DB Session not active on this service instance. Please retry or re-initialize.");
        }

        DbSessionWrapper wrapper = dbSessionLifecycleManager.getLocalSession(sessionKey)
            .orElseThrow(() -> {
                log.warn("DB Session for key {} (token: {}) metadata indicates local hosting but not found in local map on instance {}. Cleaning up.",
                         sessionKey, sessionToken, instanceIdProvider.getInstanceId());
                dbSessionLifecycleManager.releaseDbSession(sessionToken); // Use token based release to ensure full cleanup
                return new AccessDeniedException("DB Session not found locally or disconnected. Please re-initialize.");
            });

        try {
            if (!wrapper.isValid(2)) { // Check connection validity with a short timeout
                log.warn("DB Session for key {} (token: {}) found locally but connection is invalid. Cleaning up.", sessionKey, sessionToken);
                dbSessionLifecycleManager.releaseDbSession(sessionToken);
                throw new AccessDeniedException("DB Session connection is invalid. Please re-initialize.");
            }
        } catch (Exception e) { // Catch any exception from isValid (e.g. SQLException)
            log.error("Error validating DB connection for session key {} (token: {}). Cleaning up.", sessionKey, sessionToken, e);
            dbSessionLifecycleManager.releaseDbSession(sessionToken);
            throw new RemoteOperationException("Error validating DB session connection: " + e.getMessage(), e);
        }

        dbSessionLifecycleManager.updateSessionAccessTime(sessionKey); // Update access times
        log.info("DB Session token validated for {} operation. Session key: {}. Hosting instance: {}", operationName, sessionKey, metadata.getApplicationInstanceId());
        return wrapper.getConnection();
    }

    @PostMapping("/test-connection")
    public ResponseEntity<String> testConnection(@PathVariable String sessionToken) {
        try {
            getValidatedLocalDbConnection(sessionToken, "test-connection");
            // If getValidatedLocalDbConnection completes without throwing, the connection is considered valid
            return ResponseEntity.ok("DB Connection test successful.");
        } catch (AccessDeniedException | RemoteOperationException e) {
            // These are already logged by getValidatedLocalDbConnection or its callees
            // Re-throw to be caught by GlobalExceptionHandler or let Spring handle it
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during DB connection test for token {}: {}", sessionToken, e.getMessage(), e);
            throw new RemoteOperationException("Unexpected error during DB connection test: " + e.getMessage(), e);
        }
    }

    @GetMapping("/get-schema-info")
    public ResponseEntity<DbSchemaInfoResponse> getSchemaInfo(@PathVariable String sessionToken) {
        Connection connection = null;
        SessionKey sessionKey = null; // For logging context in case of error
        try {
            // Extract sessionKey for logging even if connection fails later
            Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
            if (keyOpt.isPresent()) sessionKey = keyOpt.get();
            else throw new AccessDeniedException("Invalid token for get-schema-info.");

            connection = getValidatedLocalDbConnection(sessionToken, "get-schema-info");
            DatabaseMetaData dbmd = connection.getMetaData();

            DbSchemaInfoResponse response = new DbSchemaInfoResponse(
                dbmd.getDatabaseProductName(),
                dbmd.getDatabaseProductVersion(),
                dbmd.getDriverName(),
                dbmd.getDriverVersion(),
                dbmd.getUserName(),
                dbmd.getURL()
            );
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException | RemoteOperationException e) {
            throw e;
        } catch (SQLException e) {
            log.error("SQLException while fetching DB metadata for session key {}: {}", sessionKey, e.getMessage(), e);
            // If SQL error occurs, connection might be compromised
            if (sessionKey != null) { // If we have the key, try to release the session fully
                 log.warn("Releasing potentially compromised DB session (key: {}) due to SQLException.", sessionKey);
                 dbSessionLifecycleManager.releaseDbSession(sessionToken); // Use token to ensure full cleanup
            }
            throw new RemoteOperationException("Failed to retrieve database metadata: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during get-schema-info for token {}: {}", sessionToken, e.getMessage(), e);
            throw new RemoteOperationException("Unexpected error retrieving DB schema info: " + e.getMessage(), e);
        }
        // Note: Connection itself is not closed here; it's managed by DbSessionLifecycleManager's cleanup.
    }
}
