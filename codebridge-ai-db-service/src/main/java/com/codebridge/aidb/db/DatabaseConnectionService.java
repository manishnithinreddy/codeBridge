package com.codebridge.aidb.db.service;

import com.codebridge.aidb.db.dto.QueryResult;
import com.codebridge.aidb.db.exception.DatabaseConnectionException;
import com.codebridge.aidb.db.model.DatabaseConnection;
import com.codebridge.aidb.db.model.DatabaseType;
import com.codebridge.aidb.db.repository.DatabaseConnectionRepository;
import com.codebridge.aidb.db.service.connector.DatabaseConnector;
import com.codebridge.aidb.db.service.connector.DatabaseConnectorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing database connections.
 */
@Service
@Slf4j
public class DatabaseConnectionService {

    private final DatabaseConnectionRepository connectionRepository;
    private final DatabaseConnectorFactory connectorFactory;
    
    // Cache of active database connectors
    private final Map<UUID, DatabaseConnector> activeConnectors = new ConcurrentHashMap<>();

    @Autowired
    public DatabaseConnectionService(
            DatabaseConnectionRepository connectionRepository,
            DatabaseConnectorFactory connectorFactory) {
        this.connectionRepository = connectionRepository;
        this.connectorFactory = connectorFactory;
    }

    /**
     * Get all database connections.
     *
     * @return list of database connections
     */
    public List<DatabaseConnection> getAllConnections() {
        return connectionRepository.findAll();
    }

    /**
     * Get all database connections of a specific type.
     *
     * @param type the database type
     * @return list of database connections
     */
    public List<DatabaseConnection> getConnectionsByType(DatabaseType type) {
        return connectionRepository.findByType(type);
    }

    /**
     * Get a database connection by ID.
     *
     * @param id the connection ID
     * @return the database connection
     * @throws DatabaseConnectionException if the connection is not found
     */
    public DatabaseConnection getConnectionById(UUID id) {
        return connectionRepository.findById(id)
                .orElseThrow(() -> new DatabaseConnectionException("Database connection not found with ID: " + id));
    }

    /**
     * Create a new database connection.
     *
     * @param connection the database connection to create
     * @return the created database connection
     */
    public DatabaseConnection createConnection(DatabaseConnection connection) {
        // Test the connection before saving
        DatabaseConnector connector = connectorFactory.createConnector(connection);
        boolean initialized = connector.initialize(connection);
        
        if (!initialized) {
            throw new DatabaseConnectionException("Failed to initialize database connection: " + connection.getName());
        }
        
        // Test the connection
        boolean valid = connector.testConnection();
        if (!valid) {
            throw new DatabaseConnectionException("Failed to test database connection: " + connection.getName());
        }
        
        // Close the test connection
        connector.close();
        
        // Save the connection
        return connectionRepository.save(connection);
    }

    /**
     * Update an existing database connection.
     *
     * @param id the connection ID
     * @param connection the updated database connection
     * @return the updated database connection
     * @throws DatabaseConnectionException if the connection is not found
     */
    @CacheEvict(value = "databaseConnectors", key = "#id")
    public DatabaseConnection updateConnection(UUID id, DatabaseConnection connection) {
        DatabaseConnection existingConnection = getConnectionById(id);
        
        // Close any active connector for this connection
        closeConnection(id);
        
        // Update the connection
        existingConnection.setName(connection.getName());
        existingConnection.setDescription(connection.getDescription());
        existingConnection.setType(connection.getType());
        existingConnection.setDriver(connection.getDriver());
        existingConnection.setUrl(connection.getUrl());
        existingConnection.setUsername(connection.getUsername());
        existingConnection.setPassword(connection.getPassword());
        existingConnection.setProperties(connection.getProperties());
        existingConnection.setEnabled(connection.isEnabled());
        
        // Test the updated connection
        DatabaseConnector connector = connectorFactory.createConnector(existingConnection);
        boolean initialized = connector.initialize(existingConnection);
        
        if (!initialized) {
            throw new DatabaseConnectionException("Failed to initialize updated database connection: " + existingConnection.getName());
        }
        
        // Test the connection
        boolean valid = connector.testConnection();
        if (!valid) {
            throw new DatabaseConnectionException("Failed to test updated database connection: " + existingConnection.getName());
        }
        
        // Close the test connection
        connector.close();
        
        // Save the updated connection
        return connectionRepository.save(existingConnection);
    }

    /**
     * Delete a database connection.
     *
     * @param id the connection ID
     * @throws DatabaseConnectionException if the connection is not found
     */
    @CacheEvict(value = "databaseConnectors", key = "#id")
    public void deleteConnection(UUID id) {
        DatabaseConnection connection = getConnectionById(id);
        
        // Close any active connector for this connection
        closeConnection(id);
        
        // Delete the connection
        connectionRepository.delete(connection);
    }

    /**
     * Test a database connection.
     *
     * @param id the connection ID
     * @return true if the connection is valid, false otherwise
     * @throws DatabaseConnectionException if the connection is not found
     */
    public boolean testConnection(UUID id) {
        DatabaseConnection connection = getConnectionById(id);
        
        // Create a connector for testing
        DatabaseConnector connector = connectorFactory.createConnector(connection);
        boolean initialized = connector.initialize(connection);
        
        if (!initialized) {
            return false;
        }
        
        // Test the connection
        boolean valid = connector.testConnection();
        
        // Close the test connection
        connector.close();
        
        return valid;
    }

    /**
     * Execute a query on a database connection.
     *
     * @param id the connection ID
     * @param query the query to execute
     * @param params the parameters for the query
     * @return the query result
     * @throws DatabaseConnectionException if the connection is not found or the query fails
     */
    public QueryResult executeQuery(UUID id, String query, Map<String, Object> params) {
        // Get or create a connector for the connection
        DatabaseConnector connector = getConnector(id);
        
        // Execute the query
        return connector.executeQuery(query, params);
    }

    /**
     * Execute an update on a database connection.
     *
     * @param id the connection ID
     * @param query the update query to execute
     * @param params the parameters for the query
     * @return the number of rows affected
     * @throws DatabaseConnectionException if the connection is not found or the update fails
     */
    public int executeUpdate(UUID id, String query, Map<String, Object> params) {
        // Get or create a connector for the connection
        DatabaseConnector connector = getConnector(id);
        
        // Execute the update
        return connector.executeUpdate(query, params);
    }

    /**
     * Close a database connection.
     *
     * @param id the connection ID
     */
    @CacheEvict(value = "databaseConnectors", key = "#id")
    public void closeConnection(UUID id) {
        DatabaseConnector connector = activeConnectors.remove(id);
        if (connector != null) {
            connector.close();
            log.info("Closed database connection: {}", id);
        }
    }

    /**
     * Close all active database connections.
     */
    @CacheEvict(value = "databaseConnectors", allEntries = true)
    public void closeAllConnections() {
        for (Map.Entry<UUID, DatabaseConnector> entry : activeConnectors.entrySet()) {
            entry.getValue().close();
            log.info("Closed database connection: {}", entry.getKey());
        }
        activeConnectors.clear();
    }

    /**
     * Get or create a connector for a database connection.
     *
     * @param id the connection ID
     * @return the database connector
     * @throws DatabaseConnectionException if the connection is not found or the connector cannot be created
     */
    @Cacheable(value = "databaseConnectors", key = "#id")
    public DatabaseConnector getConnector(UUID id) {
        // Check if we already have an active connector
        DatabaseConnector connector = activeConnectors.get(id);
        if (connector != null && connector.testConnection()) {
            return connector;
        }
        
        // Get the connection
        DatabaseConnection connection = getConnectionById(id);
        
        // Create a new connector
        connector = connectorFactory.createConnector(connection);
        boolean initialized = connector.initialize(connection);
        
        if (!initialized) {
            throw new DatabaseConnectionException("Failed to initialize database connection: " + connection.getName());
        }
        
        // Test the connection
        boolean valid = connector.testConnection();
        if (!valid) {
            throw new DatabaseConnectionException("Failed to test database connection: " + connection.getName());
        }
        
        // Cache the connector
        activeConnectors.put(id, connector);
        
        return connector;
    }
}

