package com.codebridge.session.dto; // Adapted package

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
// Assuming DbSessionCredentials will also be in com.codebridge.session.dto
// If it's defined elsewhere in this module, the import will be different.

public class DbSessionInitRequest {

    @NotBlank(message = "Database connection alias (dbConnectionAlias) cannot be blank.")
    private String dbConnectionAlias;

    @Valid
    @NotNull(message = "Database credentials must be provided.")
    private DbSessionCredentials credentials;

    // Getters and Setters
    public String getDbConnectionAlias() {
        return dbConnectionAlias;
    }

    public void setDbConnectionAlias(String dbConnectionAlias) {
        this.dbConnectionAlias = dbConnectionAlias;
    }

    public DbSessionCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(DbSessionCredentials credentials) {
        this.credentials = credentials;
    }
}
