package com.codebridge.session.controller;

import com.codebridge.session.config.ApplicationInstanceIdProvider;
import com.codebridge.session.dto.DbSessionMetadata;
import com.codebridge.session.dto.ops.DbSchemaInfoResponse;
import com.codebridge.session.exception.AccessDeniedException;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.exception.ResourceNotFoundException;
import com.codebridge.session.model.DbSessionWrapper;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.DbSessionLifecycleManager;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ops/db/{sessionToken}") // Consistent with SSH ops controller
public class DbOperationController {

    private static final Logger logger = LoggerFactory.getLogger(DbOperationController.class);
    private static final int DB_VALIDATION_TIMEOUT_SECONDS = 5;


    private final DbSessionLifecycleManager dbLifecycleManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationInstanceIdProvider instanceIdProvider;
    private final String applicationInstanceId;

    public DbOperationController(
            @Qualifier("dbSessionLifecycleManager") DbSessionLifecycleManager dbLifecycleManager,
            JwtTokenProvider jwtTokenProvider,
            ApplicationInstanceIdProvider instanceIdProvider) {
        this.dbLifecycleManager = dbLifecycleManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.instanceIdProvider = instanceIdProvider;
        this.applicationInstanceId = this.instanceIdProvider.getInstanceId();
    }

    // Helper method to get and validate the local JDBC Connection
    private Connection getValidatedLocalDbConnection(String sessionToken, String operationName) {
        if (!jwtTokenProvider.validateToken(sessionToken)) {
            logger.warn("{} attempt with invalid token: {}", operationName, sessionToken);
            throw new AccessDeniedException("Invalid or expired session token for " + operationName + ".");
        }

        Claims claims = jwtTokenProvider.getClaimsFromToken(sessionToken);
         SessionKey sessionKey = new SessionKey(
            UUID.fromString(claims.getSubject()), // platformUserId
            UUID.fromString(claims.get("resourceId", String.class)), // resourceId (dbAlias hash)
            claims.get("type", String.class) // Should start with "DB:"
        );
        
        if (!sessionKey.sessionType().startsWith("DB:")) {
             throw new AccessDeniedException("Invalid session type for " + operationName + ". Expected DB session.");
        }

        DbSessionMetadata metadata = dbLifecycleManager.getSessionMetadata(sessionKey)
            .orElseThrow(() -> {
                logger.warn("No DB session metadata found for {} operation. Key: {}, Token: {}", operationName, sessionKey, sessionToken);
                return new AccessDeniedException("Session metadata not found. Session may have expired or been released.");
            });

        if (!applicationInstanceId.equals(metadata.hostingInstanceId())) {
            logger.warn("DB Session for {} is not hosted on this instance. Key: {}, Expected Host: {}, Actual Host: {}.", 
                        operationName, sessionKey, metadata.hostingInstanceId(), applicationInstanceId);
            throw new AccessDeniedException("DB Session is not active on this service instance. Please reconnect or try another instance.");
        }
        
        if (metadata.expiresAt() < Instant.now().toEpochMilli()) {
            logger.warn("DB Session for {} has expired based on metadata. Key: {}, Token: {}", operationName, sessionKey, sessionToken);
            dbLifecycleManager.forceReleaseDbSessionByKey(sessionKey, true); // Clean up
            throw new AccessDeniedException("DB Session has expired.");
        }

        DbSessionWrapper wrapper = dbLifecycleManager.getLocalSession(sessionKey)
            .orElseThrow(() -> {
                logger.warn("Local DB session for {} not found. Key: {}. Releasing potentially stale metadata.", operationName, sessionKey);
                dbLifecycleManager.forceReleaseDbSessionByKey(sessionKey, true); // Clean up inconsistent state
                return new AccessDeniedException("DB Session not found locally. Please re-initialize the session.");
            });

        if (!wrapper.isValid(DB_VALIDATION_TIMEOUT_SECONDS)) {
             logger.warn("Local DB session for {} is invalid. Key: {}. Releasing.", operationName, sessionKey);
             dbLifecycleManager.forceReleaseDbSessionByKey(sessionKey, true); // Clean up inconsistent state
             throw new AccessDeniedException("DB Session found locally but is invalid/closed. Please re-initialize.");
        }

        dbLifecycleManager.updateSessionAccessTime(sessionKey, wrapper);
        return wrapper.getConnection();
    }

    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, String>> testConnection(@PathVariable String sessionToken) {
        Connection connection = getValidatedLocalDbConnection(sessionToken, "test-connection");
        // If getValidatedLocalDbConnection didn't throw, the connection is considered valid for this purpose.
        try {
            boolean isValid = connection.isValid(DB_VALIDATION_TIMEOUT_SECONDS);
            if (isValid) {
                return ResponseEntity.ok(Map.of("status", "Connection successful", "message", "Database connection is valid."));
            } else {
                // This case should ideally be caught by the isValid check in getValidatedLocalDbConnection
                throw new RemoteOperationException("Connection reported as invalid by JDBC driver after retrieval.");
            }
        } catch (SQLException e) {
             logger.error("Error testing DB connection for session token {}: {}", sessionToken, e.getMessage(), e);
             throw new RemoteOperationException("Error while testing database connection: " + e.getMessage(), e);
        }
    }

    @GetMapping("/get-schema-info")
    public ResponseEntity<DbSchemaInfoResponse> getSchemaInfo(@PathVariable String sessionToken) {
        Connection connection = getValidatedLocalDbConnection(sessionToken, "get-schema-info");
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            DbSchemaInfoResponse response = new DbSchemaInfoResponse(
                metaData.getDatabaseProductName(),
                metaData.getDatabaseProductVersion(),
                metaData.getDriverName(),
                metaData.getDriverVersion()
            );
            // Populate more fields if added to DbSchemaInfoResponse
            // response.setUserName(metaData.getUserName());
            // response.setUrl(metaData.getURL());
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            logger.error("Error retrieving DB schema info for session token {}: {}", sessionToken, e.getMessage(), e);
            throw new RemoteOperationException("Failed to retrieve database schema information: " + e.getMessage(), e);
        }
    }
}
