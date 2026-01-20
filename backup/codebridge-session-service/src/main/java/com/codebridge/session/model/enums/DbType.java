package com.codebridge.session.model.enums;

import java.io.Serializable;

public enum DbType implements Serializable {
    POSTGRESQL,
    MYSQL,
    SQLSERVER,
    ORACLE,
    MARIADB,
    MONGODB, // Example for NoSQL, though JDBC interaction is different
    OTHER;

    // Helper method to potentially get a default port if not specified
    public int getDefaultPort() {
        return switch (this) {
            case POSTGRESQL -> 5432;
            case MYSQL -> 3306;
            case MARIADB -> 3306; // Often same as MySQL
            case SQLSERVER -> 1433;
            case ORACLE -> 1521;
            case MONGODB -> 27017;
            default -> 0; // Or throw exception for OTHER / unsupported
        };
    }
}
