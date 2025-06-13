package com.codebridge.db.service.connector;

import com.codebridge.db.model.DatabaseConnection;
import com.codebridge.db.dto.QueryResult;

import java.util.Map;

/**
 * Interface for database connectors.
 * Provides a unified interface for connecting to and interacting with different types of databases.
 */
public interface DatabaseConnector {

    /**
     * Initialize the database connection.
     *
     * @param connection the database connection configuration
     * @return true if connection was successful, false otherwise
     */
    boolean initialize(DatabaseConnection connection);

    /**
     * Execute a query on the database.
     *
     * @param query the query to execute
     * @param params the parameters for the query
     * @return the query result
     */
    QueryResult executeQuery(String query, Map<String, Object> params);

    /**
     * Execute an update on the database.
     *
     * @param query the update query to execute
     * @param params the parameters for the query
     * @return the number of rows affected
     */
    int executeUpdate(String query, Map<String, Object> params);

    /**
     * Test the database connection.
     *
     * @return true if connection is valid, false otherwise
     */
    boolean testConnection();

    /**
     * Close the database connection.
     */
    void close();

    /**
     * Get the database type supported by this connector.
     *
     * @return the database type
     */
    String getDatabaseType();
}

