package com.codebridge.session.dto.sql;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

public class SqlExecutionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "SQL query cannot be blank")
    private String sqlQuery;

    private List<Object> parameters; // For future PreparedStatement use

    private boolean readOnly = true; // Default to true for safety

    // Getters and Setters
    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    public void setParameters(List<Object> parameters) {
        this.parameters = parameters;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
}
