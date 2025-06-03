package com.codebridge.session.dto.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DbSchemaInfoResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String databaseProductName;
    private String databaseProductVersion;
    private String driverName;
    private String driverVersion;
    private List<TableSchemaInfo> tables;

    public DbSchemaInfoResponse() {
        this.tables = new ArrayList<>();
    }

    public DbSchemaInfoResponse(String databaseProductName, String databaseProductVersion, String driverName, String driverVersion) {
        this.databaseProductName = databaseProductName;
        this.databaseProductVersion = databaseProductVersion;
        this.driverName = driverName;
        this.driverVersion = driverVersion;
        this.tables = new ArrayList<>();
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

    public List<TableSchemaInfo> getTables() {
        return tables;
    }

    public void setTables(List<TableSchemaInfo> tables) {
        this.tables = tables;
    }

    public void addTable(TableSchemaInfo table) {
        this.tables.add(table);
    }
}
