package com.codebridge.server.model.enums;

/**
 * Enum for supported database types.
 */
public enum DbType {
    POSTGRESQL,
    MYSQL,
    ORACLE,
    SQLSERVER,
    MARIADB, // Added MariaDB as it's common
    MONGODB, // Example for NoSQL, though JDBC model might not directly apply
    OTHER
}
