package com.codebridge.db.controller;

import com.codebridge.db.dto.QueryResult;
import com.codebridge.db.model.DatabaseConnection;
import com.codebridge.db.model.DatabaseType;
import com.codebridge.db.service.DatabaseConnectionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing database connections.
 */
@RestController
@RequestMapping("/api/connections")
@Slf4j
public class DatabaseConnectionController {

    private final DatabaseConnectionService connectionService;

    @Autowired
    public DatabaseConnectionController(DatabaseConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Get all database connections.
     *
     * @return list of database connections
     */
    @GetMapping
    public ResponseEntity<List<DatabaseConnection>> getAllConnections() {
        return ResponseEntity.ok(connectionService.getAllConnections());
    }

    /**
     * Get all database connections of a specific type.
     *
     * @param type the database type
     * @return list of database connections
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<DatabaseConnection>> getConnectionsByType(@PathVariable DatabaseType type) {
        return ResponseEntity.ok(connectionService.getConnectionsByType(type));
    }

    /**
     * Get a database connection by ID.
     *
     * @param id the connection ID
     * @return the database connection
     */
    @GetMapping("/{id}")
    public ResponseEntity<DatabaseConnection> getConnectionById(@PathVariable UUID id) {
        return ResponseEntity.ok(connectionService.getConnectionById(id));
    }

    /**
     * Create a new database connection.
     *
     * @param connection the database connection to create
     * @return the created database connection
     */
    @PostMapping
    public ResponseEntity<DatabaseConnection> createConnection(@Valid @RequestBody DatabaseConnection connection) {
        return new ResponseEntity<>(connectionService.createConnection(connection), HttpStatus.CREATED);
    }

    /**
     * Update an existing database connection.
     *
     * @param id the connection ID
     * @param connection the updated database connection
     * @return the updated database connection
     */
    @PutMapping("/{id}")
    public ResponseEntity<DatabaseConnection> updateConnection(
            @PathVariable UUID id,
            @Valid @RequestBody DatabaseConnection connection) {
        return ResponseEntity.ok(connectionService.updateConnection(id, connection));
    }

    /**
     * Delete a database connection.
     *
     * @param id the connection ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable UUID id) {
        connectionService.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Test a database connection.
     *
     * @param id the connection ID
     * @return the test result
     */
    @GetMapping("/{id}/test")
    public ResponseEntity<Map<String, Boolean>> testConnection(@PathVariable UUID id) {
        boolean result = connectionService.testConnection(id);
        return ResponseEntity.ok(Map.of("success", result));
    }

    /**
     * Execute a query on a database connection.
     *
     * @param id the connection ID
     * @param query the query to execute
     * @param params the parameters for the query
     * @return the query result
     */
    @PostMapping("/{id}/query")
    public ResponseEntity<QueryResult> executeQuery(
            @PathVariable UUID id,
            @RequestParam String query,
            @RequestBody(required = false) Map<String, Object> params) {
        return ResponseEntity.ok(connectionService.executeQuery(id, query, params));
    }

    /**
     * Execute an update on a database connection.
     *
     * @param id the connection ID
     * @param query the update query to execute
     * @param params the parameters for the query
     * @return the number of rows affected
     */
    @PostMapping("/{id}/update")
    public ResponseEntity<Map<String, Integer>> executeUpdate(
            @PathVariable UUID id,
            @RequestParam String query,
            @RequestBody(required = false) Map<String, Object> params) {
        int rowsAffected = connectionService.executeUpdate(id, query, params);
        return ResponseEntity.ok(Map.of("rowsAffected", rowsAffected));
    }

    /**
     * Close a database connection.
     *
     * @param id the connection ID
     * @return no content
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<Void> closeConnection(@PathVariable UUID id) {
        connectionService.closeConnection(id);
        return ResponseEntity.noContent().build();
    }
}

