package com.codebridge.session.dto.sql;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class SqlExecutionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<String> columnNames;
    private List<List<Object>> rows;
    private int rowsAffected = 0; // Default for SELECT or if no update
    private long executionTimeMs;
    private String error;     // Nullable
    private String warnings;  // Nullable

    public SqlExecutionResponse() {
        this.columnNames = new ArrayList<>();
        this.rows = new ArrayList<>();
    }

    // Getters and Setters
    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public void setRows(List<List<Object>> rows) {
        this.rows = rows;
    }

    public int getRowsAffected() {
        return rowsAffected;
    }

    public void setRowsAffected(int rowsAffected) {
        this.rowsAffected = rowsAffected;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getWarnings() {
        return warnings;
    }

    public void setWarnings(String warnings) {
        this.warnings = warnings;
    }
}
