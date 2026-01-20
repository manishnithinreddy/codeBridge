package com.codebridge.aidb.db.controller;

import com.codebridge.aidb.db.service.DatabaseTestingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for testing database connections and performance.
 */
@RestController
@RequestMapping("/api/testing")
@Slf4j
public class DatabaseTestingController {

    private final DatabaseTestingService testingService;

    @Autowired
    public DatabaseTestingController(DatabaseTestingService testingService) {
        this.testingService = testingService;
    }

    /**
     * Test a database connection.
     *
     * @param connectionId the database connection ID
     * @return the test result
     */
    @GetMapping("/connection/{connectionId}")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable UUID connectionId) {
        return ResponseEntity.ok(testingService.testConnection(connectionId));
    }

    /**
     * Test database performance.
     *
     * @param connectionId the database connection ID
     * @param query the query to execute
     * @param iterations the number of iterations
     * @param concurrentUsers the number of concurrent users
     * @return the performance test result
     */
    @PostMapping("/performance/{connectionId}")
    public ResponseEntity<Map<String, Object>> testPerformance(
            @PathVariable UUID connectionId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int iterations,
            @RequestParam(defaultValue = "1") int concurrentUsers) {
        
        return ResponseEntity.ok(testingService.testPerformance(connectionId, query, iterations, concurrentUsers));
    }

    /**
     * Test database data validation.
     *
     * @param connectionId the database connection ID
     * @param table the table to validate
     * @param column the column to validate
     * @param validationRules the validation rules
     * @return the validation test result
     */
    @PostMapping("/validation/{connectionId}")
    public ResponseEntity<Map<String, Object>> testDataValidation(
            @PathVariable UUID connectionId,
            @RequestParam String table,
            @RequestParam String column,
            @RequestBody Map<String, Object> validationRules) {
        
        return ResponseEntity.ok(testingService.testDataValidation(connectionId, table, column, validationRules));
    }
}

