package com.codebridge.aidb.dto.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// STUB of com.codebridge.session.dto.ops.SqlExecutionResponse
public class ClientSqlExecutionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<String> columnNames;
    private List<List<Object>> rows;
    private int rowsAffected = 0;
    private long executionTimeMs;
    private String error;
    private String warnings;

    public ClientSqlExecutionResponse() {
        this.columnNames = new ArrayList<>();
        this.rows = new ArrayList<>();
    }

    // Getters and Setters
    public List<String> getColumnNames() { return columnNames; }
    public void setColumnNames(List<String> columnNames) { this.columnNames = columnNames; }
    public List<List<Object>> getRows() { return rows; }
    public void setRows(List<List<Object>> rows) { this.rows = rows; }
    public int getRowsAffected() { return rowsAffected; }
    public void setRowsAffected(int rowsAffected) { this.rowsAffected = rowsAffected; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getWarnings() { return warnings; }
    public void setWarnings(String warnings) { this.warnings = warnings; }
}
