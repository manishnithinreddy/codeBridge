package com.codebridge.aidb.db.model;

/**
 * Enum representing the different types of databases supported by the system.
 */
public enum DatabaseType {
    SQL("Relational Database"),
    NOSQL("NoSQL Database"),
    GRAPH("Graph Database"),
    TIMESERIES("Time-Series Database"),
    CLOUD("Cloud Database Service");

    private final String description;

    DatabaseType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

