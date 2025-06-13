package com.codebridge.db.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object representing the result of a database query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {

    /**
     * List of column names in the result.
     */
    private List<String> columns;

    /**
     * List of rows, each row is a map of column name to value.
     */
    private List<Map<String, Object>> rows;

    /**
     * Number of rows affected by the query (for update operations).
     */
    private int rowsAffected;

    /**
     * Execution time of the query in milliseconds.
     */
    private long executionTimeMs;

    /**
     * Error message if the query failed.
     */
    private String error;

    /**
     * Warning messages from the query execution.
     */
    private List<String> warnings;

    /**
     * Metadata about the query execution.
     */
    private Map<String, Object> metadata;

    /**
     * Create a new QueryResult with an error.
     *
     * @param error the error message
     * @return a new QueryResult with the error
     */
    public static QueryResult withError(String error) {
        return QueryResult.builder()
                .error(error)
                .columns(new ArrayList<>())
                .rows(new ArrayList<>())
                .rowsAffected(0)
                .executionTimeMs(0)
                .warnings(new ArrayList<>())
                .build();
    }
}

