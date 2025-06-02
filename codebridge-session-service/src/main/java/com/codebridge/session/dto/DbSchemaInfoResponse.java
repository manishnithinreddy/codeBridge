package com.codebridge.session.dto;

// This DTO is used to return basic database metadata
public class DbSchemaInfoResponse {
    private String databaseProductName;
    private String databaseProductVersion;
    private String driverName;
    private String driverVersion;
    private String userName;
    private String url;

    public DbSchemaInfoResponse(String databaseProductName, String databaseProductVersion,
                                String driverName, String driverVersion,
                                String userName, String url) {
        this.databaseProductName = databaseProductName;
        this.databaseProductVersion = databaseProductVersion;
        this.driverName = driverName;
        this.driverVersion = driverVersion;
        this.userName = userName;
        this.url = url;
    }

    // Getters (setters not strictly necessary if only used for response)
    public String getDatabaseProductName() {
        return databaseProductName;
    }

    public String getDatabaseProductVersion() {
        return databaseProductVersion;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public String getUserName() {
        return userName;
    }

    public String getUrl() {
        return url;
    }
}
