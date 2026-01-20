package com.codebridge.aidb.agent;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public class NaturalLanguageQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Database session token cannot be blank")
    private String dbSessionToken;

    @NotBlank(message = "Natural language query cannot be blank")
    private String naturalLanguageQuery;

    private String dbConnectionAliasOrId; // Optional, for context

    // Getters and Setters
    public String getDbSessionToken() {
        return dbSessionToken;
    }

    public void setDbSessionToken(String dbSessionToken) {
        this.dbSessionToken = dbSessionToken;
    }

    public String getNaturalLanguageQuery() {
        return naturalLanguageQuery;
    }

    public void setNaturalLanguageQuery(String naturalLanguageQuery) {
        this.naturalLanguageQuery = naturalLanguageQuery;
    }

    public String getDbConnectionAliasOrId() {
        return dbConnectionAliasOrId;
    }

    public void setDbConnectionAliasOrId(String dbConnectionAliasOrId) {
        this.dbConnectionAliasOrId = dbConnectionAliasOrId;
    }
}
