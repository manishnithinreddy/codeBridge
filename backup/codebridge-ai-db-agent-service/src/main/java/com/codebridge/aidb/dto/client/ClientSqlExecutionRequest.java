package com.codebridge.aidb.dto.client;

import java.io.Serializable;
import java.util.List;

// STUB of com.codebridge.session.dto.ops.SqlExecutionRequest
public class ClientSqlExecutionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sqlQuery;
    private List<Object> parameters; // Kept for compatibility, though not used in this phase
    private boolean readOnly = true;

    public ClientSqlExecutionRequest() {}

    public ClientSqlExecutionRequest(String sqlQuery, boolean readOnly) {
        this.sqlQuery = sqlQuery;
        this.readOnly = readOnly;
    }

    // Getters and Setters
    public String getSqlQuery() { return sqlQuery; }
    public void setSqlQuery(String sqlQuery) { this.sqlQuery = sqlQuery; }
    public List<Object> getParameters() { return parameters; }
    public void setParameters(List<Object> parameters) { this.parameters = parameters; }
    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
}
