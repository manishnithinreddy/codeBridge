package com.codebridge.server.dto.sessions;

import com.codebridge.server.model.enums.DbType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty; // For password, can be empty but not null
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min; // For port
import java.util.Map;

public class DbSessionCredentials {

    @NotNull(message = "Database type (dbType) must be specified.")
    private DbType dbType;

    @NotBlank(message = "Host cannot be blank.")
    private String host;

    @NotNull(message = "Port cannot be null.")
    @Min(value = 1, message = "Port number must be at least 1.")
    private Integer port;

    @NotBlank(message = "Username cannot be blank.")
    private String username;

    @NotEmpty(message = "Password cannot be null (it can be empty if allowed by the database).")
    private String password; // Can be empty, but not null

    @NotBlank(message = "Database name cannot be blank.")
    private String databaseName;

    private boolean sslEnabled = false;

    private Map<String, String> additionalProperties; // e.g., ?characterEncoding=UTF-8

    // Getters and Setters

    public DbType getDbType() {
        return dbType;
    }

    public void setDbType(DbType dbType) {
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

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}
