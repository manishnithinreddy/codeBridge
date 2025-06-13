package com.codebridge.db.controller;

import com.codebridge.db.dto.QueryResult;
import com.codebridge.db.service.QueryBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for building and executing database queries.
 */
@RestController
@RequestMapping("/api/query-builder")
@Slf4j
public class QueryBuilderController {

    private final QueryBuilderService queryBuilderService;

    @Autowired
    public QueryBuilderController(QueryBuilderService queryBuilderService) {
        this.queryBuilderService = queryBuilderService;
    }

    /**
     * Execute a SELECT query.
     *
     * @param connectionId the database connection ID
     * @param table the table to query
     * @param columns the columns to select
     * @param whereClause the WHERE clause
     * @param orderBy the ORDER BY clause
     * @param limit the LIMIT clause
     * @param offset the OFFSET clause
     * @return the query result
     */
    @GetMapping("/select")
    public ResponseEntity<QueryResult> executeSelect(
            @RequestParam UUID connectionId,
            @RequestParam String table,
            @RequestParam(required = false) List<String> columns,
            @RequestParam(required = false) String whereClause,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        
        return ResponseEntity.ok(queryBuilderService.executeSelect(
                connectionId, table, columns, whereClause, orderBy, limit, offset));
    }

    /**
     * Execute an INSERT query.
     *
     * @param connectionId the database connection ID
     * @param table the table to insert into
     * @param values the values to insert
     * @return the number of rows affected
     */
    @PostMapping("/insert")
    public ResponseEntity<Map<String, Integer>> executeInsert(
            @RequestParam UUID connectionId,
            @RequestParam String table,
            @RequestBody Map<String, Object> values) {
        
        int rowsAffected = queryBuilderService.executeInsert(connectionId, table, values);
        return ResponseEntity.ok(Map.of("rowsAffected", rowsAffected));
    }

    /**
     * Execute an UPDATE query.
     *
     * @param connectionId the database connection ID
     * @param table the table to update
     * @param values the values to update
     * @param whereClause the WHERE clause
     * @return the number of rows affected
     */
    @PutMapping("/update")
    public ResponseEntity<Map<String, Integer>> executeUpdate(
            @RequestParam UUID connectionId,
            @RequestParam String table,
            @RequestBody Map<String, Object> values,
            @RequestParam(required = false) String whereClause) {
        
        int rowsAffected = queryBuilderService.executeUpdate(connectionId, table, values, whereClause);
        return ResponseEntity.ok(Map.of("rowsAffected", rowsAffected));
    }

    /**
     * Execute a DELETE query.
     *
     * @param connectionId the database connection ID
     * @param table the table to delete from
     * @param whereClause the WHERE clause
     * @return the number of rows affected
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Integer>> executeDelete(
            @RequestParam UUID connectionId,
            @RequestParam String table,
            @RequestParam(required = false) String whereClause) {
        
        int rowsAffected = queryBuilderService.executeDelete(connectionId, table, whereClause);
        return ResponseEntity.ok(Map.of("rowsAffected", rowsAffected));
    }
}

