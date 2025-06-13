package com.codebridge.server.dto.ai;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public class PluginNaturalLanguageQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Database connection alias cannot be blank")
    private String dbConnectionAlias;

    @NotBlank(message = "Natural language query cannot be blank")
    private String naturalLanguageQuery;

    @NotBlank(message = "Active dbSessionToken is required") // Made mandatory as per worker decision
    private String dbSessionToken;

    // Getters and Setters
    public String getDbConnectionAlias() {
        return dbConnectionAlias;
    }

    public void setDbConnectionAlias(String dbConnectionAlias) {
        this.dbConnectionAlias = dbConnectionAlias;
    }

    public String getNaturalLanguageQuery() {
        return naturalLanguageQuery;
    }

    public void setNaturalLanguageQuery(String naturalLanguageQuery) {
        this.naturalLanguageQuery = naturalLanguageQuery;
    }

    public String getDbSessionToken() {
        return dbSessionToken;
    }

    public void setDbSessionToken(String dbSessionToken) {
        this.dbSessionToken = dbSessionToken;
    }
}
