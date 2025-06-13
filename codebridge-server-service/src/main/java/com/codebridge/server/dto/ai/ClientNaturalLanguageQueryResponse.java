package com.codebridge.server.dto.ai;

import java.io.Serializable;

// This DTO is what ServerService expects from AIDbAgentService
public class ClientNaturalLanguageQueryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String generatedSql;
    private ClientSqlExecutionResponse sqlExecutionResult; // Using the stub defined above
    private String aiError;
    private String processingError;

    public ClientNaturalLanguageQueryResponse() {}

    // Getters and Setters
    public String getGeneratedSql() {
        return generatedSql;
    }

    public void setGeneratedSql(String generatedSql) {
        this.generatedSql = generatedSql;
    }

    public ClientSqlExecutionResponse getSqlExecutionResult() {
        return sqlExecutionResult;
    }

    public void setSqlExecutionResult(ClientSqlExecutionResponse sqlExecutionResult) {
        this.sqlExecutionResult = sqlExecutionResult;
    }

    public String getAiError() {
        return aiError;
    }

    public void setAiError(String aiError) {
        this.aiError = aiError;
    }

    public String getProcessingError() {
        return processingError;
    }

    public void setProcessingError(String processingError) {
        this.processingError = processingError;
    }
}
