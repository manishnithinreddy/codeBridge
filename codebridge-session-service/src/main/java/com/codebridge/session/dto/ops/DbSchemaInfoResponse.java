package com.codebridge.session.dto.ops;

import java.io.Serializable;

public class DbSchemaInfoResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String databaseProductName;
    private String databaseProductVersion;
    private String driverName;
    private String driverVersion;
    // Add more fields from DatabaseMetaData as needed:
    // private String userName;
    // private String url;
    // private boolean readOnly;
    // private int defaultTransactionIsolation;

    public DbSchemaInfoResponse() {
    }

    public DbSchemaInfoResponse(String databaseProductName, String databaseProductVersion, String driverName, String driverVersion) {
        this.databaseProductName = databaseProductName;
        this.databaseProductVersion = databaseProductVersion;
        this.driverName = driverName;
        this.driverVersion = driverVersion;
    }

    // Getters and Setters
    public String getDatabaseProductName() {
        return databaseProductName;
    }

    public void setDatabaseProductName(String databaseProductName) {
        this.databaseProductName = databaseProductName;
    }

    public String getDatabaseProductVersion() {
        return databaseProductVersion;
    }

    public void setDatabaseProductVersion(String databaseProductVersion) {
        this.databaseProductVersion = databaseProductVersion;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public void setDriverVersion(String driverVersion) {
        this.driverVersion = driverVersion;
    }
}
