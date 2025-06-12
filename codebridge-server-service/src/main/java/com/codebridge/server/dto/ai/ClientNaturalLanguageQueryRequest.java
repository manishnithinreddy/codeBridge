package com.codebridge.server.dto.ai;

import jakarta.validation.constraints.NotBlank; // Though this DTO is for internal construction, validation is good practice
import java.io.Serializable;

// This DTO is what ServerService sends to AIDbAgentService
public class ClientNaturalLanguageQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String dbSessionToken;

    @NotBlank
    private String naturalLanguageQuery;

    private String dbConnectionAliasOrId; // Optional context

    public ClientNaturalLanguageQueryRequest() {}

    public ClientNaturalLanguageQueryRequest(String dbSessionToken, String naturalLanguageQuery, String dbConnectionAliasOrId) {
        this.dbSessionToken = dbSessionToken;
        this.naturalLanguageQuery = naturalLanguageQuery;
        this.dbConnectionAliasOrId = dbConnectionAliasOrId;
    }

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
