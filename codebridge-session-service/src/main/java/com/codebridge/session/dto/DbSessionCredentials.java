package com.codebridge.session.dto;

import com.codebridge.session.model.enums.DbType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.util.Map;

public class DbSessionCredentials implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Database type cannot be null")
    private DbType dbType;

    @NotBlank(message = "Host cannot be blank")
    private String host;

    @NotNull(message = "Port cannot be null")
    @Positive(message = "Port must be a positive number")
    private Integer port;

    @NotBlank(message = "Username cannot be blank")
    private String username;

    // Password can be blank for some auth methods or if not required by DB
    private String password; 

    @NotBlank(message = "Database name cannot be blank")
    private String databaseName;

    private boolean sslEnabled = false; // Default to false

    private Map<String, String> additionalProperties; // For driver-specific params like SSL cert paths etc.

    // Constructors
    public DbSessionCredentials() {}

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
