package com.codebridge.server.dto.client;

// This DTO is nested within DbSessionServiceApiInitRequestDto
// It represents the credentials part of a DB connection string or parameters.
public class DbSessionCredentials {
    private String dbType; // e.g., "POSTGRESQL", "MYSQL", "SQLSERVER", "ORACLE"
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password; // Actual password, not encrypted by ServerService before sending to SessionService
    // private Map<String, String> additionalProperties; // For SSL, timeouts, etc.

    // Constructors, Getters, Setters
    public DbSessionCredentials() {}

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
