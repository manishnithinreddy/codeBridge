package com.codebridge.session.controller;

import com.codebridge.session.config.ApplicationInstanceIdProvider;
import com.codebridge.session.config.ApplicationInstanceIdProvider;
import com.codebridge.session.dto.DbSessionMetadata;
import com.codebridge.session.dto.schema.DbSchemaInfoResponse; // Updated DTO path
import com.codebridge.session.dto.sql.SqlExecutionRequest; // Added
import com.codebridge.session.dto.sql.SqlExecutionResponse; // Added
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
import java.sql.*; // Added for Statement, ResultSet, ResultSetMetaData
import java.time.Instant;
import java.util.ArrayList; // Added
import java.util.List; // Added
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
        // The service method now handles SQLException and throws RemoteOperationException
        DbSchemaInfoResponse schemaInfo = dbLifecycleManager.getDetailedSchemaInfo(connection);
        return ResponseEntity.ok(schemaInfo);
    }

    private boolean containsDmlKeywords(String sql) {
        if (sql == null) return false;
        String upperSql = sql.toUpperCase();
        // Basic keyword check, not foolproof for complex SQL or comments.
        return upperSql.contains("INSERT ") || upperSql.contains("UPDATE ") || upperSql.contains("DELETE ") ||
               upperSql.contains("DROP ") || upperSql.contains("CREATE ") || upperSql.contains("ALTER ") ||
               upperSql.contains("TRUNCATE ") || upperSql.contains("MERGE ");
    }

    @PostMapping("/execute-sql")
    public ResponseEntity<SqlExecutionResponse> executeSql(
            @PathVariable String sessionToken,
            @RequestBody @jakarta.validation.Valid SqlExecutionRequest request) { // Added @Valid

        long startTime = System.currentTimeMillis();
        Connection connection = getValidatedLocalDbConnection(sessionToken, "execute-sql");
        SqlExecutionResponse responseDto = new SqlExecutionResponse();

        // TODO: Implement robust SQL validation/sanitization here before execution!
        // This is a critical security measure.
        if (request.isReadOnly() && containsDmlKeywords(request.getSqlQuery())) {
            logger.warn("DML keyword detected in read-only query for session token {}: {}", sessionToken, request.getSqlQuery());
            // For now, throwing an exception. Could also set error in responseDto.
            throw new IllegalArgumentException("Only SELECT queries are allowed in read-only mode. Detected potential DML/DDL keywords.");
        }

        try (Statement stmt = connection.createStatement()) {
            // TODO: Implement parameter binding for PreparedStatement if request.getParameters() is not empty.
            // For now, direct execution (vulnerable to SQL injection if not carefully controlled upstream).
            boolean isResultSet = stmt.execute(request.getSqlQuery());

            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    int columnCount = rsmd.getColumnCount();
                    List<String> columnNames = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columnNames.add(rsmd.getColumnLabel(i)); // Use getColumnLabel for aliases
                    }
                    responseDto.setColumnNames(columnNames);

                    List<List<Object>> rows = new ArrayList<>();
                    while (rs.next()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(rs.getObject(i)); // getObject tries to map to appropriate Java type
                        }
                        rows.add(row);
                    }
                    responseDto.setRows(rows);
                    responseDto.setRowsAffected(0); // Typically 0 for SELECT unless driver reports otherwise
                }
            } else {
                responseDto.setRowsAffected(stmt.getUpdateCount());
            }

            // Check for warnings
            SQLWarning warning = stmt.getWarnings();
            if (warning != null) {
                StringBuilder warningMessages = new StringBuilder();
                while (warning != null) {
                    warningMessages.append(warning.getMessage()).append("; ");
                    warning = warning.getNextWarning();
                }
                responseDto.setWarnings(warningMessages.toString());
                logger.warn("SQL Warnings for session token {}: {}", sessionToken, warningMessages.toString());
            }
            // responseDto.setError(null); // Explicitly null if successful
        } catch (SQLException e) {
            logger.error("SQLException during SQL execution for session token {}: {}", sessionToken, e.getMessage(), e);
            responseDto.setError("SQL Error: " + e.getErrorCode() + " - " + e.getMessage());
            // For some errors, could return different HTTP status, e.g. BAD_REQUEST if query is malformed.
            // For now, returning 200 OK with error in DTO.
        } catch (Exception e) {
            logger.error("Unexpected error during SQL execution for session token {}: {}", sessionToken, e.getMessage(), e);
            responseDto.setError("Unexpected error: " + e.getMessage());
        } finally {
            responseDto.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        }
        return ResponseEntity.ok(responseDto);
    }
}
