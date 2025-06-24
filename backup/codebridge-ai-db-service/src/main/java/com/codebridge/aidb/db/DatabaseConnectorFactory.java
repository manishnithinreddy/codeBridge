package com.codebridge.aidb.db;

import com.codebridge.aidb.db.model.DatabaseConnection;
import com.codebridge.aidb.db.model.DatabaseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for creating database connectors based on database type.
 */
@Component
@Slf4j
public class DatabaseConnectorFactory {

    private final SqlDatabaseConnector sqlDatabaseConnector;
    private final NoSqlDatabaseConnector noSqlDatabaseConnector;
    private final GraphDatabaseConnector graphDatabaseConnector;
    private final TimeSeriesDatabaseConnector timeSeriesDatabaseConnector;

    @Autowired
    public DatabaseConnectorFactory(
            SqlDatabaseConnector sqlDatabaseConnector,
            NoSqlDatabaseConnector noSqlDatabaseConnector,
            GraphDatabaseConnector graphDatabaseConnector,
            TimeSeriesDatabaseConnector timeSeriesDatabaseConnector) {
        this.sqlDatabaseConnector = sqlDatabaseConnector;
        this.noSqlDatabaseConnector = noSqlDatabaseConnector;
        this.graphDatabaseConnector = graphDatabaseConnector;
        this.timeSeriesDatabaseConnector = timeSeriesDatabaseConnector;
    }

    /**
     * Create a database connector for the given database connection.
     *
     * @param connection the database connection
     * @return the database connector
     * @throws IllegalArgumentException if the database type is not supported
     */
    public DatabaseConnector createConnector(DatabaseConnection connection) {
        DatabaseType type = connection.getType();
        DatabaseConnector connector;

        switch (type) {
            case SQL:
                connector = sqlDatabaseConnector;
                break;
            case NOSQL:
                connector = noSqlDatabaseConnector;
                break;
            case GRAPH:
                connector = graphDatabaseConnector;
                break;
            case TIMESERIES:
                connector = timeSeriesDatabaseConnector;
                break;
            case CLOUD:
                // Determine the actual database type from the driver or URL
                if (connection.getDriver().contains("mongodb")) {
                    connector = noSqlDatabaseConnector;
                } else if (connection.getDriver().contains("neo4j")) {
                    connector = graphDatabaseConnector;
                } else if (connection.getDriver().contains("influx")) {
                    connector = timeSeriesDatabaseConnector;
                } else {
                    // Default to SQL for cloud databases
                    connector = sqlDatabaseConnector;
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }

        log.info("Created connector of type {} for database connection: {}", connector.getDatabaseType(), connection.getName());
        return connector;
    }
}
