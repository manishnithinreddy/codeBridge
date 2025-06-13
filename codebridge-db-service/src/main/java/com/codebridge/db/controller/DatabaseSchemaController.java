package com.codebridge.db.controller;

import com.codebridge.db.model.DatabaseSchema;
import com.codebridge.db.service.DatabaseSchemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for managing database schemas.
 */
@RestController
@RequestMapping("/api/schemas")
@Slf4j
public class DatabaseSchemaController {

    private final DatabaseSchemaService schemaService;

    @Autowired
    public DatabaseSchemaController(DatabaseSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    /**
     * Get all schemas for a specific connection.
     *
     * @param connectionId the database connection ID
     * @return list of database schemas
     */
    @GetMapping("/connection/{connectionId}")
    public ResponseEntity<List<DatabaseSchema>> getSchemasByConnectionId(@PathVariable UUID connectionId) {
        return ResponseEntity.ok(schemaService.getSchemasByConnectionId(connectionId));
    }

    /**
     * Get a schema by ID.
     *
     * @param id the schema ID
     * @return the database schema
     */
    @GetMapping("/{id}")
    public ResponseEntity<DatabaseSchema> getSchemaById(@PathVariable UUID id) {
        return ResponseEntity.ok(schemaService.getSchemaById(id));
    }

    /**
     * Get the latest schema for a specific connection.
     *
     * @param connectionId the database connection ID
     * @return the latest database schema if found
     */
    @GetMapping("/connection/{connectionId}/latest")
    public ResponseEntity<DatabaseSchema> getLatestSchema(@PathVariable UUID connectionId) {
        Optional<DatabaseSchema> schema = schemaService.getLatestSchema(connectionId);
        return schema.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Capture the current schema for a database connection.
     *
     * @param connectionId the database connection ID
     * @return the captured database schema
     */
    @PostMapping("/connection/{connectionId}/capture")
    public ResponseEntity<DatabaseSchema> captureSchema(@PathVariable UUID connectionId) {
        return new ResponseEntity<>(schemaService.captureSchema(connectionId), HttpStatus.CREATED);
    }
}

