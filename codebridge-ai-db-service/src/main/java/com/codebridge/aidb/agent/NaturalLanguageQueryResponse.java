package com.codebridge.aidb.agent;

import com.codebridge.aidb.agent.client.ClientSqlExecutionResponse; // Stub DTO
import java.io.Serializable;

public class NaturalLanguageQueryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String generatedSql; // Nullable
    private ClientSqlExecutionResponse sqlExecutionResult; // Stub DTO
    private String aiError; // Nullable, if AI itself failed
    private String processingError; // Nullable, for errors within AIDbAgentService

    public NaturalLanguageQueryResponse() {}

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
