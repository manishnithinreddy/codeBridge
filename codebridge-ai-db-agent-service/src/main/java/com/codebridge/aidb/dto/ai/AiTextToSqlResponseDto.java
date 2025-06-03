package com.codebridge.aidb.dto.ai;

import java.io.Serializable;

public class AiTextToSqlResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String generatedSql;
    private String error;

    public AiTextToSqlResponseDto() {
    }

    public AiTextToSqlResponseDto(String generatedSql, String error) {
        this.generatedSql = generatedSql;
        this.error = error;
    }

    public String getGeneratedSql() {
        return generatedSql;
    }

    public void setGeneratedSql(String generatedSql) {
        this.generatedSql = generatedSql;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
