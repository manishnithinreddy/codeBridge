package com.codebridge.aidb.db.service;

import com.codebridge.aidb.db.exception.DatabaseConnectionException;
import com.codebridge.aidb.db.model.*;
import com.codebridge.aidb.db.repository.DatabaseSchemaRepository;
import com.codebridge.aidb.db.service.connector.DatabaseConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for managing database schemas.
 */
@Service
@Slf4j
public class DatabaseSchemaService {

    private final DatabaseSchemaRepository schemaRepository;
    private final DatabaseConnectionService connectionService;

    @Autowired
    public DatabaseSchemaService(
            DatabaseSchemaRepository schemaRepository,
            DatabaseConnectionService connectionService) {
        this.schemaRepository = schemaRepository;
        this.connectionService = connectionService;
    }

    /**
     * Get all schemas for a specific connection.
     *
     * @param connectionId the database connection ID
     * @return list of database schemas
     */
    public List<DatabaseSchema> getSchemasByConnectionId(UUID connectionId) {
        return schemaRepository.findByConnectionId(connectionId);
    }

    /**
     * Get a schema by ID.
     *
     * @param id the schema ID
     * @return the database schema
     * @throws IllegalArgumentException if the schema is not found
     */
    public DatabaseSchema getSchemaById(UUID id) {
        return schemaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found with ID: " + id));
    }

    /**
     * Get the latest schema for a specific connection.
     *
     * @param connectionId the database connection ID
     * @return the latest database schema if found
     * @throws IllegalArgumentException if the connection is not found
     */
    public Optional<DatabaseSchema> getLatestSchema(UUID connectionId) {
        DatabaseConnection connection = connectionService.getConnectionById(connectionId);
        return schemaRepository.findTopByConnectionOrderByCapturedAtDesc(connection);
    }

    /**
     * Capture the current schema for a database connection.
     *
     * @param connectionId the database connection ID
     * @return the captured database schema
     * @throws DatabaseConnectionException if the connection is not found or the schema capture fails
     */
    @Transactional
    public DatabaseSchema captureSchema(UUID connectionId) {
        DatabaseConnection connection = connectionService.getConnectionById(connectionId);
        
        // Generate a version based on the current timestamp
        String version = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // Create a new schema
        DatabaseSchema schema = new DatabaseSchema();
        schema.setName(connection.getName() + " Schema");
        schema.setDescription("Schema captured from " + connection.getName());
        schema.setConnection(connection);
        schema.setVersion(version);
        
        // Capture the schema based on the database type
        switch (connection.getType()) {
            case SQL:
                captureSqlSchema(schema, connectionId);
                break;
            case NOSQL:
                captureNoSqlSchema(schema, connectionId);
                break;
            case GRAPH:
                captureGraphSchema(schema, connectionId);
                break;
            case TIMESERIES:
                captureTimeSeriesSchema(schema, connectionId);
                break;
            case CLOUD:
                // Determine the actual database type from the driver or URL
                if (connection.getDriver().contains("mongodb")) {
                    captureNoSqlSchema(schema, connectionId);
                } else if (connection.getDriver().contains("neo4j")) {
                    captureGraphSchema(schema, connectionId);
                } else if (connection.getDriver().contains("influx")) {
                    captureTimeSeriesSchema(schema, connectionId);
                } else {
                    // Default to SQL for cloud databases
                    captureSqlSchema(schema, connectionId);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + connection.getType());
        }
        
        // Save the schema
        return schemaRepository.save(schema);
    }

    /**
     * Capture the schema for a SQL database.
     *
     * @param schema the database schema to populate
     * @param connectionId the database connection ID
     * @throws DatabaseConnectionException if the schema capture fails
     */
    private void captureSqlSchema(DatabaseSchema schema, UUID connectionId) {
        try {
            // Get a connector for the connection
            DatabaseConnector connector = connectionService.getConnector(connectionId);
            
            // Execute a query to get the database metadata
            Map<String, Object> params = new HashMap<>();
            
            // For SQL databases, we need to use JDBC metadata directly
            if (connector instanceof com.codebridge.db.service.connector.SqlDatabaseConnector) {
                // Get the JDBC connection from the connector
                Connection connection = ((com.codebridge.db.service.connector.SqlDatabaseConnector) connector).getConnection();
                
                // Get the database metadata
                DatabaseMetaData metaData = connection.getMetaData();
                
                // Get the catalog and schema names
                String catalog = connection.getCatalog();
                String schemaName = connection.getSchema();
                
                // Get the tables
                try (ResultSet tables = metaData.getTables(catalog, schemaName, null, new String[]{"TABLE", "VIEW"})) {
                    while (tables.next()) {
                        String tableName = tables.getString("TABLE_NAME");
                        String tableType = tables.getString("TABLE_TYPE");
                        
                        // Create a new table
                        DatabaseTable table = new DatabaseTable();
                        table.setName(tableName);
                        table.setTableType(tableType);
                        table.setSchema(schema);
                        
                        // Get the columns
                        try (ResultSet columns = metaData.getColumns(catalog, schemaName, tableName, null)) {
                            while (columns.next()) {
                                String columnName = columns.getString("COLUMN_NAME");
                                String dataType = columns.getString("TYPE_NAME");
                                int position = columns.getInt("ORDINAL_POSITION");
                                boolean nullable = columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                                String defaultValue = columns.getString("COLUMN_DEF");
                                int maxLength = columns.getInt("COLUMN_SIZE");
                                int precision = columns.getInt("DECIMAL_DIGITS");
                                
                                // Create a new column
                                DatabaseColumn column = new DatabaseColumn();
                                column.setName(columnName);
                                column.setDataType(dataType);
                                column.setPosition(position);
                                column.setNullable(nullable);
                                column.setDefaultValue(defaultValue);
                                column.setMaxLength(maxLength);
                                column.setPrecision(precision);
                                column.setTable(table);
                                
                                // Add the column to the table
                                table.getColumns().add(column);
                            }
                        }
                        
                        // Get the primary keys
                        try (ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schemaName, tableName)) {
                            while (primaryKeys.next()) {
                                String columnName = primaryKeys.getString("COLUMN_NAME");
                                
                                // Find the column
                                for (DatabaseColumn column : table.getColumns()) {
                                    if (column.getName().equals(columnName)) {
                                        column.setPrimaryKey(true);
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // Get the foreign keys
                        try (ResultSet foreignKeys = metaData.getImportedKeys(catalog, schemaName, tableName)) {
                            while (foreignKeys.next()) {
                                String columnName = foreignKeys.getString("FKCOLUMN_NAME");
                                String referencedTable = foreignKeys.getString("PKTABLE_NAME");
                                String referencedColumn = foreignKeys.getString("PKCOLUMN_NAME");
                                
                                // Find the column
                                for (DatabaseColumn column : table.getColumns()) {
                                    if (column.getName().equals(columnName)) {
                                        column.setForeignKey(true);
                                        column.setReferencedTable(referencedTable);
                                        column.setReferencedColumn(referencedColumn);
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // Get the indexes
                        try (ResultSet indexes = metaData.getIndexInfo(catalog, schemaName, tableName, false, false)) {
                            Map<String, DatabaseIndex> indexMap = new HashMap<>();
                            
                            while (indexes.next()) {
                                String indexName = indexes.getString("INDEX_NAME");
                                if (indexName == null) {
                                    continue; // Skip null index names
                                }
                                
                                boolean unique = !indexes.getBoolean("NON_UNIQUE");
                                String columnName = indexes.getString("COLUMN_NAME");
                                String type = indexes.getString("TYPE");
                                
                                // Get or create the index
                                DatabaseIndex index = indexMap.get(indexName);
                                if (index == null) {
                                    index = new DatabaseIndex();
                                    index.setName(indexName);
                                    index.setUnique(unique);
                                    index.setType(type == null ? null : String.valueOf(type));
                                    index.setTable(table);
                                    index.setColumns(columnName);
                                    indexMap.put(indexName, index);
                                } else {
                                    // Append the column name to the existing columns
                                    index.setColumns(index.getColumns() + "," + columnName);
                                }
                            }
                            
                            // Add the indexes to the table
                            table.getIndexes().addAll(indexMap.values());
                        }
                        
                        // Add the table to the schema
                        schema.getTables().add(table);
                    }
                }
                
                // Set the raw schema
                schema.setRawSchema("SQL schema captured from JDBC metadata");
            }
        } catch (Exception e) {
            log.error("Error capturing SQL schema", e);
            throw new DatabaseConnectionException("Failed to capture SQL schema: " + e.getMessage(), e);
        }
    }

    /**
     * Capture the schema for a NoSQL database.
     *
     * @param schema the database schema to populate
     * @param connectionId the database connection ID
     * @throws DatabaseConnectionException if the schema capture fails
     */
    private void captureNoSqlSchema(DatabaseSchema schema, UUID connectionId) {
        try {
            // Get a connector for the connection
            DatabaseConnector connector = connectionService.getConnector(connectionId);
            
            // For MongoDB, we can query the collections and their stats
            Map<String, Object> params = new HashMap<>();
            
            // Get the collections
            String query = "{ listCollections: 1 }";
            com.codebridge.db.dto.QueryResult result = connector.executeQuery(query, params);
            
            if (result.getError() != null) {
                throw new DatabaseConnectionException("Failed to list collections: " + result.getError());
            }
            
            // Process the collections
            List<Map<String, Object>> collections = new ArrayList<>();
            for (Map<String, Object> row : result.getRows()) {
                if (row.containsKey("cursor") && row.get("cursor") instanceof Map) {
                    Map<String, Object> cursor = (Map<String, Object>) row.get("cursor");
                    if (cursor.containsKey("firstBatch") && cursor.get("firstBatch") instanceof List) {
                        collections = (List<Map<String, Object>>) cursor.get("firstBatch");
                    }
                }
            }
            
            // Create tables for each collection
            for (Map<String, Object> collection : collections) {
                String collectionName = (String) collection.get("name");
                String collectionType = (String) collection.get("type");
                
                // Create a new table
                DatabaseTable table = new DatabaseTable();
                table.setName(collectionName);
                table.setTableType(collectionType);
                table.setSchema(schema);
                
                // Get the collection stats
                query = "{ collStats: \"" + collectionName + "\" }";
                result = connector.executeQuery(query, params);
                
                if (result.getError() == null && !result.getRows().isEmpty()) {
                    Map<String, Object> stats = result.getRows().get(0);
                    
                    // Set the row count and size
                    if (stats.containsKey("count")) {
                        table.setRowCount(((Number) stats.get("count")).longValue());
                    }
                    
                    if (stats.containsKey("size")) {
                        table.setSizeBytes(((Number) stats.get("size")).longValue());
                    }
                }
                
                // Get a sample document to infer the schema
                query = "{ find: \"" + collectionName + "\", limit: 1 }";
                result = connector.executeQuery(query, params);
                
                if (result.getError() == null && !result.getRows().isEmpty()) {
                    // Process the sample document
                    Map<String, Object> sample = result.getRows().get(0);
                    
                    // Create columns for each field
                    int position = 1;
                    for (Map.Entry<String, Object> entry : sample.entrySet()) {
                        String fieldName = entry.getKey();
                        Object fieldValue = entry.getValue();
                        
                        // Skip internal MongoDB fields
                        if (fieldName.startsWith("_") && !fieldName.equals("_id")) {
                            continue;
                        }
                        
                        // Create a new column
                        DatabaseColumn column = new DatabaseColumn();
                        column.setName(fieldName);
                        column.setDataType(getNoSqlDataType(fieldValue));
                        column.setPosition(position++);
                        column.setNullable(true);
                        column.setTable(table);
                        
                        // Set primary key for _id field
                        if (fieldName.equals("_id")) {
                            column.setPrimaryKey(true);
                            column.setNullable(false);
                        }
                        
                        // Add the column to the table
                        table.getColumns().add(column);
                    }
                }
                
                // Get the indexes
                query = "{ listIndexes: \"" + collectionName + "\" }";
                result = connector.executeQuery(query, params);
                
                if (result.getError() == null && !result.getRows().isEmpty()) {
                    // Process the indexes
                    List<Map<String, Object>> indexes = new ArrayList<>();
                    for (Map<String, Object> row : result.getRows()) {
                        if (row.containsKey("cursor") && row.get("cursor") instanceof Map) {
                            Map<String, Object> cursor = (Map<String, Object>) row.get("cursor");
                            if (cursor.containsKey("firstBatch") && cursor.get("firstBatch") instanceof List) {
                                indexes = (List<Map<String, Object>>) cursor.get("firstBatch");
                            }
                        }
                    }
                    
                    // Create indexes
                    for (Map<String, Object> indexInfo : indexes) {
                        String indexName = (String) indexInfo.get("name");
                        boolean unique = indexInfo.containsKey("unique") && (boolean) indexInfo.get("unique");
                        
                        // Get the key fields
                        Map<String, Object> key = (Map<String, Object>) indexInfo.get("key");
                        List<String> keyFields = new ArrayList<>();
                        
                        for (Map.Entry<String, Object> entry : key.entrySet()) {
                            keyFields.add(entry.getKey());
                        }
                        
                        // Create a new index
                        DatabaseIndex index = new DatabaseIndex();
                        index.setName(indexName);
                        index.setUnique(unique);
                        index.setColumns(String.join(",", keyFields));
                        index.setTable(table);
                        
                        // Add the index to the table
                        table.getIndexes().add(index);
                    }
                }
                
                // Add the table to the schema
                schema.getTables().add(table);
            }
            
            // Set the raw schema
            schema.setRawSchema("NoSQL schema captured from MongoDB metadata");
        } catch (Exception e) {
            log.error("Error capturing NoSQL schema", e);
            throw new DatabaseConnectionException("Failed to capture NoSQL schema: " + e.getMessage(), e);
        }
    }

    /**
     * Capture the schema for a Graph database.
     *
     * @param schema the database schema to populate
     * @param connectionId the database connection ID
     * @throws DatabaseConnectionException if the schema capture fails
     */
    private void captureGraphSchema(DatabaseSchema schema, UUID connectionId) {
        try {
            // Get a connector for the connection
            DatabaseConnector connector = connectionService.getConnector(connectionId);
            
            // For Neo4j, we can query the node labels and relationship types
            Map<String, Object> params = new HashMap<>();
            
            // Get the node labels
            String query = "CALL db.labels()";
            com.codebridge.db.dto.QueryResult result = connector.executeQuery(query, params);
            
            if (result.getError() != null) {
                throw new DatabaseConnectionException("Failed to get node labels: " + result.getError());
            }
            
            // Process the node labels
            List<String> nodeLabels = new ArrayList<>();
            for (Map<String, Object> row : result.getRows()) {
                nodeLabels.add((String) row.get("label"));
            }
            
            // Create tables for each node label
            for (String label : nodeLabels) {
                // Create a new table for the node label
                DatabaseTable table = new DatabaseTable();
                table.setName(label);
                table.setTableType("NODE");
                table.setSchema(schema);
                
                // Get a sample node to infer the schema
                query = "MATCH (n:" + label + ") RETURN n LIMIT 1";
                result = connector.executeQuery(query, params);
                
                if (result.getError() == null && !result.getRows().isEmpty()) {
                    // Process the sample node
                    Map<String, Object> sample = result.getRows().get(0);
                    
                    if (sample.containsKey("n") && sample.get("n") instanceof Map) {
                        Map<String, Object> node = (Map<String, Object>) sample.get("n");
                        
                        // Get the node properties
                        if (node.containsKey("properties") && node.get("properties") instanceof Map) {
                            Map<String, Object> properties = (Map<String, Object>) node.get("properties");
                            
                            // Create columns for each property
                            int position = 1;
                            
                            // Add id column
                            DatabaseColumn idColumn = new DatabaseColumn();
                            idColumn.setName("id");
                            idColumn.setDataType("Long");
                            idColumn.setPosition(position++);
                            idColumn.setNullable(false);
                            idColumn.setPrimaryKey(true);
                            idColumn.setTable(table);
                            table.getColumns().add(idColumn);
                            
                            // Add property columns
                            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                                String propertyName = entry.getKey();
                                Object propertyValue = entry.getValue();
                                
                                // Create a new column
                                DatabaseColumn column = new DatabaseColumn();
                                column.setName(propertyName);
                                column.setDataType(getGraphDataType(propertyValue));
                                column.setPosition(position++);
                                column.setNullable(true);
                                column.setTable(table);
                                
                                // Add the column to the table
                                table.getColumns().add(column);
                            }
                        }
                    }
                }
                
                // Add the table to the schema
                schema.getTables().add(table);
            }
            
            // Get the relationship types
            query = "CALL db.relationshipTypes()";
            result = connector.executeQuery(query, params);
            
            if (result.getError() != null) {
                throw new DatabaseConnectionException("Failed to get relationship types: " + result.getError());
            }
            
            // Process the relationship types
            List<String> relationshipTypes = new ArrayList<>();
            for (Map<String, Object> row : result.getRows()) {
                relationshipTypes.add((String) row.get("relationshipType"));
            }
            
            // Create tables for each relationship type
            for (String type : relationshipTypes) {
                // Create a new table for the relationship type
                DatabaseTable table = new DatabaseTable();
                table.setName(type);
                table.setTableType("RELATIONSHIP");
                table.setSchema(schema);
                
                // Add standard relationship columns
                int position = 1;
                
                // Add id column
                DatabaseColumn idColumn = new DatabaseColumn();
                idColumn.setName("id");
                idColumn.setDataType("Long");
                idColumn.setPosition(position++);
                idColumn.setNullable(false);
                idColumn.setPrimaryKey(true);
                idColumn.setTable(table);
                table.getColumns().add(idColumn);
                
                // Add start node id column
                DatabaseColumn startColumn = new DatabaseColumn();
                startColumn.setName("startNodeId");
                startColumn.setDataType("Long");
                startColumn.setPosition(position++);
                startColumn.setNullable(false);
                startColumn.setForeignKey(true);
                startColumn.setTable(table);
                table.getColumns().add(startColumn);
                
                // Add end node id column
                DatabaseColumn endColumn = new DatabaseColumn();
                endColumn.setName("endNodeId");
                endColumn.setDataType("Long");
                endColumn.setPosition(position++);
                endColumn.setNullable(false);
                endColumn.setForeignKey(true);
                endColumn.setTable(table);
                table.getColumns().add(endColumn);
                
                // Get a sample relationship to infer the schema
                query = "MATCH ()-[r:" + type + "]->() RETURN r LIMIT 1";
                result = connector.executeQuery(query, params);
                
                if (result.getError() == null && !result.getRows().isEmpty()) {
                    // Process the sample relationship
                    Map<String, Object> sample = result.getRows().get(0);
                    
                    if (sample.containsKey("r") && sample.get("r") instanceof Map) {
                        Map<String, Object> relationship = (Map<String, Object>) sample.get("r");
                        
                        // Get the relationship properties
                        if (relationship.containsKey("properties") && relationship.get("properties") instanceof Map) {
                            Map<String, Object> properties = (Map<String, Object>) relationship.get("properties");
                            
                            // Create columns for each property
                            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                                String propertyName = entry.getKey();
                                Object propertyValue = entry.getValue();
                                
                                // Create a new column
                                DatabaseColumn column = new DatabaseColumn();
                                column.setName(propertyName);
                                column.setDataType(getGraphDataType(propertyValue));
                                column.setPosition(position++);
                                column.setNullable(true);
                                column.setTable(table);
                                
                                // Add the column to the table
                                table.getColumns().add(column);
                            }
                        }
                    }
                }
                
                // Add the table to the schema
                schema.getTables().add(table);
            }
            
            // Set the raw schema
            schema.setRawSchema("Graph schema captured from Neo4j metadata");
        } catch (Exception e) {
            log.error("Error capturing Graph schema", e);
            throw new DatabaseConnectionException("Failed to capture Graph schema: " + e.getMessage(), e);
        }
    }

    /**
     * Capture the schema for a Time-Series database.
     *
     * @param schema the database schema to populate
     * @param connectionId the database connection ID
     * @throws DatabaseConnectionException if the schema capture fails
     */
    private void captureTimeSeriesSchema(DatabaseSchema schema, UUID connectionId) {
        try {
            // Get a connector for the connection
            DatabaseConnector connector = connectionService.getConnector(connectionId);
            
            // For InfluxDB, we can query the measurements and field keys
            Map<String, Object> params = new HashMap<>();
            
            // Get the measurements
            String query = "import \"influxdata/influxdb/schema\"\n" +
                           "schema.measurements(bucket: \"" + schema.getConnection().getProperties().get("bucket") + "\")";
            com.codebridge.db.dto.QueryResult result = connector.executeQuery(query, params);
            
            if (result.getError() != null) {
                throw new DatabaseConnectionException("Failed to get measurements: " + result.getError());
            }
            
            // Process the measurements
            List<String> measurements = new ArrayList<>();
            for (Map<String, Object> row : result.getRows()) {
                measurements.add((String) row.get("_value"));
            }
            
            // Create tables for each measurement
            for (String measurement : measurements) {
                // Create a new table for the measurement
                DatabaseTable table = new DatabaseTable();
                table.setName(measurement);
                table.setTableType("MEASUREMENT");
                table.setSchema(schema);
                
                // Add standard time-series columns
                int position = 1;
                
                // Add time column
                DatabaseColumn timeColumn = new DatabaseColumn();
                timeColumn.setName("_time");
                timeColumn.setDataType("TIMESTAMP");
                timeColumn.setPosition(position++);
                timeColumn.setNullable(false);
                timeColumn.setPrimaryKey(true);
                timeColumn.setTable(table);
                table.getColumns().add(timeColumn);
                
                // Get the field keys
                query = "import \"influxdata/influxdb/schema\"\n" +
                        "schema.fieldKeys(bucket: \"" + schema.getConnection().getProperties().get("bucket") + "\", measurement: \"" + measurement + "\")";
                result = connector.executeQuery(query, params);
                
                if (result.getError() == null) {
                    // Process the field keys
                    for (Map<String, Object> row : result.getRows()) {
                        String fieldName = (String) row.get("_value");
                        
                        // Create a new column
                        DatabaseColumn column = new DatabaseColumn();
                        column.setName(fieldName);
                        column.setDataType("FIELD");
                        column.setPosition(position++);
                        column.setNullable(true);
                        column.setTable(table);
                        
                        // Add the column to the table
                        table.getColumns().add(column);
                    }
                }
                
                // Get the tag keys
                query = "import \"influxdata/influxdb/schema\"\n" +
                        "schema.tagKeys(bucket: \"" + schema.getConnection().getProperties().get("bucket") + "\", measurement: \"" + measurement + "\")";
                result = connector.executeQuery(query, params);
                
                if (result.getError() == null) {
                    // Process the tag keys
                    for (Map<String, Object> row : result.getRows()) {
                        String tagName = (String) row.get("_value");
                        
                        // Create a new column
                        DatabaseColumn column = new DatabaseColumn();
                        column.setName(tagName);
                        column.setDataType("TAG");
                        column.setPosition(position++);
                        column.setNullable(true);
                        column.setTable(table);
                        
                        // Add the column to the table
                        table.getColumns().add(column);
                    }
                }
                
                // Add the table to the schema
                schema.getTables().add(table);
            }
            
            // Set the raw schema
            schema.setRawSchema("Time-Series schema captured from InfluxDB metadata");
        } catch (Exception e) {
            log.error("Error capturing Time-Series schema", e);
            throw new DatabaseConnectionException("Failed to capture Time-Series schema: " + e.getMessage(), e);
        }
    }

    /**
     * Get the data type for a NoSQL value.
     *
     * @param value the value
     * @return the data type
     */
    private String getNoSqlDataType(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            return "STRING";
        } else if (value instanceof Integer) {
            return "INTEGER";
        } else if (value instanceof Long) {
            return "LONG";
        } else if (value instanceof Double) {
            return "DOUBLE";
        } else if (value instanceof Boolean) {
            return "BOOLEAN";
        } else if (value instanceof Date) {
            return "DATE";
        } else if (value instanceof List) {
            return "ARRAY";
        } else if (value instanceof Map) {
            return "OBJECT";
        } else {
            return value.getClass().getSimpleName();
        }
    }

    /**
     * Get the data type for a Graph value.
     *
     * @param value the value
     * @return the data type
     */
    private String getGraphDataType(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            return "String";
        } else if (value instanceof Integer) {
            return "Integer";
        } else if (value instanceof Long) {
            return "Long";
        } else if (value instanceof Double) {
            return "Double";
        } else if (value instanceof Boolean) {
            return "Boolean";
        } else if (value instanceof List) {
            return "List";
        } else if (value instanceof Map) {
            return "Map";
        } else {
            return value.getClass().getSimpleName();
        }
    }
}

