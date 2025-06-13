package com.codebridge.aidb.db.service;

import com.codebridge.aidb.db.dto.QueryResult;
import com.codebridge.aidb.db.exception.DatabaseConnectionException;
import com.codebridge.aidb.db.model.DatabaseConnection;
import com.codebridge.aidb.db.model.DatabaseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for building and executing database queries.
 */
@Service
@Slf4j
public class QueryBuilderService {

    private final DatabaseConnectionService connectionService;

    @Autowired
    public QueryBuilderService(DatabaseConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Build and execute a SELECT query.
     *
     * @param connectionId the database connection ID
     * @param table the table to query
     * @param columns the columns to select
     * @param whereClause the WHERE clause
     * @param orderBy the ORDER BY clause
     * @param limit the LIMIT clause
     * @param offset the OFFSET clause
     * @return the query result
     * @throws DatabaseConnectionException if the query fails
     */
    public QueryResult executeSelect(
            UUID connectionId,
            String table,
            List<String> columns,
            String whereClause,
            String orderBy,
            Integer limit,
            Integer offset) {
        
        // Get the database connection
        DatabaseConnection connection = connectionService.getConnectionById(connectionId);
        
        // Build the query based on the database type
        String query;
        Map<String, Object> params = new HashMap<>();
        
        switch (connection.getType()) {
            case SQL:
                query = buildSqlSelect(table, columns, whereClause, orderBy, limit, offset);
                break;
            case NOSQL:
                query = buildNoSqlSelect(table, columns, whereClause, orderBy, limit, offset);
                break;
            case GRAPH:
                query = buildGraphSelect(table, columns, whereClause, orderBy, limit, offset);
                break;
            case TIMESERIES:
                query = buildTimeSeriesSelect(table, columns, whereClause, orderBy, limit, offset);
                break;
            case CLOUD:
                // Determine the actual database type from the driver or URL
                if (connection.getDriver().contains("mongodb")) {
                    query = buildNoSqlSelect(table, columns, whereClause, orderBy, limit, offset);
                } else if (connection.getDriver().contains("neo4j")) {
                    query = buildGraphSelect(table, columns, whereClause, orderBy, limit, offset);
                } else if (connection.getDriver().contains("influx")) {
                    query = buildTimeSeriesSelect(table, columns, whereClause, orderBy, limit, offset);
                } else {
                    // Default to SQL for cloud databases
                    query = buildSqlSelect(table, columns, whereClause, orderBy, limit, offset);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + connection.getType());
        }
        
        // Execute the query
        return connectionService.executeQuery(connectionId, query, params);
    }

    /**
     * Build and execute an INSERT query.
     *
     * @param connectionId the database connection ID
     * @param table the table to insert into
     * @param values the values to insert
     * @return the number of rows affected
     * @throws DatabaseConnectionException if the query fails
     */
    public int executeInsert(
            UUID connectionId,
            String table,
            Map<String, Object> values) {
        
        // Get the database connection
        DatabaseConnection connection = connectionService.getConnectionById(connectionId);
        
        // Build the query based on the database type
        String query;
        Map<String, Object> params = new HashMap<>();
        
        switch (connection.getType()) {
            case SQL:
                query = buildSqlInsert(table, values, params);
                break;
            case NOSQL:
                query = buildNoSqlInsert(table, values, params);
                break;
            case GRAPH:
                query = buildGraphInsert(table, values, params);
                break;
            case TIMESERIES:
                query = buildTimeSeriesInsert(table, values, params);
                break;
            case CLOUD:
                // Determine the actual database type from the driver or URL
                if (connection.getDriver().contains("mongodb")) {
                    query = buildNoSqlInsert(table, values, params);
                } else if (connection.getDriver().contains("neo4j")) {
                    query = buildGraphInsert(table, values, params);
                } else if (connection.getDriver().contains("influx")) {
                    query = buildTimeSeriesInsert(table, values, params);
                } else {
                    // Default to SQL for cloud databases
                    query = buildSqlInsert(table, values, params);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + connection.getType());
        }
        
        // Execute the query
        return connectionService.executeUpdate(connectionId, query, params);
    }

    /**
     * Build and execute an UPDATE query.
     *
     * @param connectionId the database connection ID
     * @param table the table to update
     * @param values the values to update
     * @param whereClause the WHERE clause
     * @return the number of rows affected
     * @throws DatabaseConnectionException if the query fails
     */
    public int executeUpdate(
            UUID connectionId,
            String table,
            Map<String, Object> values,
            String whereClause) {
        
        // Get the database connection
        DatabaseConnection connection = connectionService.getConnectionById(connectionId);
        
        // Build the query based on the database type
        String query;
        Map<String, Object> params = new HashMap<>();
        
        switch (connection.getType()) {
            case SQL:
                query = buildSqlUpdate(table, values, whereClause, params);
                break;
            case NOSQL:
                query = buildNoSqlUpdate(table, values, whereClause, params);
                break;
            case GRAPH:
                query = buildGraphUpdate(table, values, whereClause, params);
                break;
            case TIMESERIES:
                query = buildTimeSeriesUpdate(table, values, whereClause, params);
                break;
            case CLOUD:
                // Determine the actual database type from the driver or URL
                if (connection.getDriver().contains("mongodb")) {
                    query = buildNoSqlUpdate(table, values, whereClause, params);
                } else if (connection.getDriver().contains("neo4j")) {
                    query = buildGraphUpdate(table, values, whereClause, params);
                } else if (connection.getDriver().contains("influx")) {
                    query = buildTimeSeriesUpdate(table, values, whereClause, params);
                } else {
                    // Default to SQL for cloud databases
                    query = buildSqlUpdate(table, values, whereClause, params);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + connection.getType());
        }
        
        // Execute the query
        return connectionService.executeUpdate(connectionId, query, params);
    }

    /**
     * Build and execute a DELETE query.
     *
     * @param connectionId the database connection ID
     * @param table the table to delete from
     * @param whereClause the WHERE clause
     * @return the number of rows affected
     * @throws DatabaseConnectionException if the query fails
     */
    public int executeDelete(
            UUID connectionId,
            String table,
            String whereClause) {
        
        // Get the database connection
        DatabaseConnection connection = connectionService.getConnectionById(connectionId);
        
        // Build the query based on the database type
        String query;
        Map<String, Object> params = new HashMap<>();
        
        switch (connection.getType()) {
            case SQL:
                query = buildSqlDelete(table, whereClause);
                break;
            case NOSQL:
                query = buildNoSqlDelete(table, whereClause, params);
                break;
            case GRAPH:
                query = buildGraphDelete(table, whereClause);
                break;
            case TIMESERIES:
                query = buildTimeSeriesDelete(table, whereClause);
                break;
            case CLOUD:
                // Determine the actual database type from the driver or URL
                if (connection.getDriver().contains("mongodb")) {
                    query = buildNoSqlDelete(table, whereClause, params);
                } else if (connection.getDriver().contains("neo4j")) {
                    query = buildGraphDelete(table, whereClause);
                } else if (connection.getDriver().contains("influx")) {
                    query = buildTimeSeriesDelete(table, whereClause);
                } else {
                    // Default to SQL for cloud databases
                    query = buildSqlDelete(table, whereClause);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + connection.getType());
        }
        
        // Execute the query
        return connectionService.executeUpdate(connectionId, query, params);
    }

    /**
     * Build a SQL SELECT query.
     *
     * @param table the table to query
     * @param columns the columns to select
     * @param whereClause the WHERE clause
     * @param orderBy the ORDER BY clause
     * @param limit the LIMIT clause
     * @param offset the OFFSET clause
     * @return the SQL query
     */
    private String buildSqlSelect(
            String table,
            List<String> columns,
            String whereClause,
            String orderBy,
            Integer limit,
            Integer offset) {
        
        StringBuilder query = new StringBuilder("SELECT ");
        
        // Add columns
        if (columns == null || columns.isEmpty()) {
            query.append("*");
        } else {
            query.append(String.join(", ", columns));
        }
        
        // Add table
        query.append(" FROM ").append(table);
        
        // Add WHERE clause
        if (whereClause != null && !whereClause.isEmpty()) {
            query.append(" WHERE ").append(whereClause);
        }
        
        // Add ORDER BY clause
        if (orderBy != null && !orderBy.isEmpty()) {
            query.append(" ORDER BY ").append(orderBy);
        }
        
        // Add LIMIT clause
        if (limit != null && limit > 0) {
            query.append(" LIMIT ").append(limit);
        }
        
        // Add OFFSET clause
        if (offset != null && offset > 0) {
            query.append(" OFFSET ").append(offset);
        }
        
        return query.toString();
    }

    /**
     * Build a NoSQL (MongoDB) SELECT query.
     *
     * @param collection the collection to query
     * @param fields the fields to select
     * @param whereClause the WHERE clause
     * @param orderBy the ORDER BY clause
     * @param limit the LIMIT clause
     * @param skip the SKIP clause
     * @return the MongoDB query
     */
    private String buildNoSqlSelect(
            String collection,
            List<String> fields,
            String whereClause,
            String orderBy,
            Integer limit,
            Integer skip) {
        
        StringBuilder query = new StringBuilder("{ find: \"").append(collection).append("\"");
        
        // Add filter
        if (whereClause != null && !whereClause.isEmpty()) {
            query.append(", filter: ").append(whereClause);
        } else {
            query.append(", filter: {}");
        }
        
        // Add projection
        if (fields != null && !fields.isEmpty()) {
            query.append(", projection: { ");
            for (int i = 0; i < fields.size(); i++) {
                query.append("\"").append(fields.get(i)).append("\": 1");
                if (i < fields.size() - 1) {
                    query.append(", ");
                }
            }
            query.append(" }");
        }
        
        // Add sort
        if (orderBy != null && !orderBy.isEmpty()) {
            query.append(", sort: ").append(orderBy);
        }
        
        // Add limit
        if (limit != null && limit > 0) {
            query.append(", limit: ").append(limit);
        }
        
        // Add skip
        if (skip != null && skip > 0) {
            query.append(", skip: ").append(skip);
        }
        
        query.append(" }");
        
        return query.toString();
    }

    /**
     * Build a Graph (Neo4j) SELECT query.
     *
     * @param label the node label to query
     * @param properties the properties to select
     * @param whereClause the WHERE clause
     * @param orderBy the ORDER BY clause
     * @param limit the LIMIT clause
     * @param skip the SKIP clause
     * @return the Cypher query
     */
    private String buildGraphSelect(
            String label,
            List<String> properties,
            String whereClause,
            String orderBy,
            Integer limit,
            Integer skip) {
        
        StringBuilder query = new StringBuilder("MATCH (n:");
        
        // Add label
        query.append(label).append(")");
        
        // Add WHERE clause
        if (whereClause != null && !whereClause.isEmpty()) {
            query.append(" WHERE ").append(whereClause);
        }
        
        // Add RETURN clause
        query.append(" RETURN ");
        if (properties == null || properties.isEmpty()) {
            query.append("n");
        } else {
            for (int i = 0; i < properties.size(); i++) {
                query.append("n.").append(properties.get(i));
                if (i < properties.size() - 1) {
                    query.append(", ");
                }
            }
        }
        
        // Add ORDER BY clause
        if (orderBy != null && !orderBy.isEmpty()) {
            query.append(" ORDER BY ").append(orderBy);
        }
        
        // Add SKIP clause
        if (skip != null && skip > 0) {
            query.append(" SKIP ").append(skip);
        }
        
        // Add LIMIT clause
        if (limit != null && limit > 0) {
            query.append(" LIMIT ").append(limit);
        }
        
        return query.toString();
    }

    /**
     * Build a Time-Series (InfluxDB) SELECT query.
     *
     * @param measurement the measurement to query
     * @param fields the fields to select
     * @param whereClause the WHERE clause
     * @param orderBy the ORDER BY clause
     * @param limit the LIMIT clause
     * @param offset the OFFSET clause
     * @return the Flux query
     */
    private String buildTimeSeriesSelect(
            String measurement,
            List<String> fields,
            String whereClause,
            String orderBy,
            Integer limit,
            Integer offset) {
        
        StringBuilder query = new StringBuilder("from(bucket: \"${bucket}\")\n");
        
        // Add range
        query.append("  |> range(start: -30d)\n");
        
        // Add filter for measurement
        query.append("  |> filter(fn: (r) => r._measurement == \"").append(measurement).append("\")\n");
        
        // Add WHERE clause
        if (whereClause != null && !whereClause.isEmpty()) {
            query.append("  |> filter(fn: (r) => ").append(whereClause).append(")\n");
        }
        
        // Add fields filter
        if (fields != null && !fields.isEmpty()) {
            query.append("  |> filter(fn: (r) => ");
            for (int i = 0; i < fields.size(); i++) {
                query.append("r._field == \"").append(fields.get(i)).append("\"");
                if (i < fields.size() - 1) {
                    query.append(" or ");
                }
            }
            query.append(")\n");
        }
        
        // Add ORDER BY clause
        if (orderBy != null && !orderBy.isEmpty()) {
            query.append("  |> sort(columns: [").append(orderBy).append("])\n");
        } else {
            query.append("  |> sort(columns: [\"_time\"])\n");
        }
        
        // Add OFFSET clause
        if (offset != null && offset > 0) {
            query.append("  |> tail(n: ").append(offset).append(")\n");
        }
        
        // Add LIMIT clause
        if (limit != null && limit > 0) {
            query.append("  |> limit(n: ").append(limit).append(")\n");
        }
        
        return query.toString();
    }

    /**
     * Build a SQL INSERT query.
     *
     * @param table the table to insert into
     * @param values the values to insert
     * @param params the parameters for the query
     * @return the SQL query
     */
    private String buildSqlInsert(
            String table,
            Map<String, Object> values,
            Map<String, Object> params) {
        
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(table).append(" (");
        
        // Add columns
        List<String> columns = new ArrayList<>(values.keySet());
        query.append(String.join(", ", columns));
        
        // Add values
        query.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            query.append(":").append(column);
            params.put(column, values.get(column));
            
            if (i < columns.size() - 1) {
                query.append(", ");
            }
        }
        
        query.append(")");
        
        return query.toString();
    }

    /**
     * Build a NoSQL (MongoDB) INSERT query.
     *
     * @param collection the collection to insert into
     * @param values the values to insert
     * @param params the parameters for the query
     * @return the MongoDB query
     */
    private String buildNoSqlInsert(
            String collection,
            Map<String, Object> values,
            Map<String, Object> params) {
        
        StringBuilder query = new StringBuilder("{ insert: \"").append(collection).append("\", documents: [");
        
        // Add document
        query.append("{ ");
        List<String> fields = new ArrayList<>(values.keySet());
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            query.append("\"").append(field).append("\": :").append(field);
            params.put(field, values.get(field));
            
            if (i < fields.size() - 1) {
                query.append(", ");
            }
        }
        
        query.append(" }] }");
        
        return query.toString();
    }

    /**
     * Build a Graph (Neo4j) INSERT query.
     *
     * @param label the node label to insert
     * @param properties the properties to insert
     * @param params the parameters for the query
     * @return the Cypher query
     */
    private String buildGraphInsert(
            String label,
            Map<String, Object> properties,
            Map<String, Object> params) {
        
        StringBuilder query = new StringBuilder("CREATE (n:");
        
        // Add label
        query.append(label).append(" {");
        
        // Add properties
        List<String> propNames = new ArrayList<>(properties.keySet());
        for (int i = 0; i < propNames.size(); i++) {
            String propName = propNames.get(i);
            query.append(propName).append(": $").append(propName);
            params.put(propName, properties.get(propName));
            
            if (i < propNames.size() - 1) {
                query.append(", ");
            }
        }
        
        query.append("}) RETURN n");
        
        return query.toString();
    }

    /**
     * Build a Time-Series (InfluxDB) INSERT query.
     *
     * @param measurement the measurement to insert into
     * @param values the values to insert
     * @param params the parameters for the query
     * @return the Line Protocol query
     */
    private String buildTimeSeriesInsert(
            String measurement,
            Map<String, Object> values,
            Map<String, Object> params) {
        
        // For InfluxDB, we need to separate tags and fields
        Map<String, Object> tags = new HashMap<>();
        Map<String, Object> fields = new HashMap<>();
        
        // Assume _time is provided, otherwise use current time
        Object timestamp = values.getOrDefault("_time", "now()");
        
        // Separate tags and fields
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (key.equals("_time")) {
                continue;
            }
            
            if (key.startsWith("tag_")) {
                tags.put(key.substring(4), value);
            } else {
                fields.put(key, value);
            }
        }
        
        // Build the Line Protocol query
        StringBuilder query = new StringBuilder("from(bucket: \"${bucket}\")\n");
        query.append("  |> to(bucket: \"${bucket}\", org: \"${org}\")\n");
        query.append("  |> write(data: [\n");
        query.append("    {measurement: \"").append(measurement).append("\", ");
        
        // Add tags
        if (!tags.isEmpty()) {
            query.append("tags: {");
            List<String> tagNames = new ArrayList<>(tags.keySet());
            for (int i = 0; i < tagNames.size(); i++) {
                String tagName = tagNames.get(i);
                query.append(tagName).append(": :tag_").append(tagName);
                params.put("tag_" + tagName, tags.get(tagName));
                
                if (i < tagNames.size() - 1) {
                    query.append(", ");
                }
            }
            query.append("}, ");
        }
        
        // Add fields
        query.append("fields: {");
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            query.append(fieldName).append(": :").append(fieldName);
            params.put(fieldName, fields.get(fieldName));
            
            if (i < fieldNames.size() - 1) {
                query.append(", ");
            }
        }
        query.append("}, ");
        
        // Add timestamp
        query.append("time: ").append(timestamp);
        
        query.append("}\n  ])\n");
        
        return query.toString();
    }

    /**
     * Build a SQL UPDATE query.
     *
     * @param table the table to update
     * @param values the values to update
     * @param whereClause the WHERE clause
     * @param params the parameters for the query
     * @return the SQL query
     */
    private String buildSqlUpdate(
            String table,
            Map<String, Object> values,
            String whereClause,
            Map<String, Object> params) {
        
        StringBuilder query = new StringBuilder("UPDATE ");
        query.append(table).append(" SET ");
        
        // Add columns and values
        List<String> columns = new ArrayList<>(values.keySet());
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            query.append(column).append(" = :").append(column);
            params.put(column, values.get(column));
            
            if (i < columns.size() - 1) {
                query.append(", ");
            }
        }
        
        // Add WHERE clause
        if (whereClause != null && !whereClause.isEmpty()) {
            query.append(" WHERE ").append(whereClause);
        }
        
        return query.toString();
    }

    /**
     * Build a NoSQL (MongoDB) UPDATE query.
     *
     * @param collection the collection to update
     * @param values the values to update
     * @param whereClause the WHERE clause
     * @param params the parameters for the query
     * @return the MongoDB query
     */
    private String buildNoSqlUpdate(
            String collection,
            Map<String, Object> values,
            String whereClause,
            Map<String, Object> params) {
        
        StringBuilder query = new StringBuilder("{ update: \"").append(collection).append("\", ");
        
        // Add filter
        if (whereClause != null && !whereClause.isEmpty()) {
            query.append("filter: ").append(whereClause).append(", ");
        } else {
            query.append("filter: {}, ");
        }
        
        // Add update
        query.append("update: { $set: { ");
        List<String> fields = new ArrayList<>(values.keySet());
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            query.append("\"").append(field).append("\": :").append(field);
            params.put(field, values.get(field));
            
            if (i < fields.size() - 1) {
                query.append(", ");
            }
        }
        
        query.append(" } }, multi: true }");
        
        return query.toString();
    }

    /**
     * Build a Graph (Neo4j) UPDATE query.
     *
     * @param label the node label to update
     * @param properties the properties to update
     * @param whereClause the WHERE clause
     * @param params the parameters for the query
     * @return the Cypher query
     */
    private String buildGraphUpdate(
            String label,
            Map<String, Object> properties,
            String whereClause,
            Map<String, Object> params) {
        
        StringBuilder query = new StringBuilder("MATCH (n:");
        
        // Add label
        query.append(label).append(")");
        
        // Add WHERE clause
        if (whereClause != null && !whereClause.isEmpty()) {
            query.append(" WHERE ").append(whereClause);
        }
        
        // Add SET clause
        query.append(" SET ");
        List<String> propNames = new ArrayList<>(properties.keySet());
        for (int i = 0; i < propNames.size(); i++) {
            String propName = propNames.get(i);
            query.append("n.").append(propName).append(" = $").append(propName);
            params.put(propName, properties.get(propName));
            
            if (i < propNames.size() - 1) {
                query.append(", ");
            }
        }
        
        query.append(" RETURN n");
        
        return query.toString();
    }

    /**
     * Build a Time-Series (InfluxDB) UPDATE query.
     *
     * @param measurement the measurement to update
     * @param values the values to update
     * @param whereClause the WHERE clause
     * @param params the parameters for the query
     * @return the Flux query
     */
    private String buildTimeSeriesUpdate(
            String measurement,
            Map<String, Object> values,
            String whereClause,
            Map<String, Object> params) {
        
        // InfluxDB doesn't support direct updates, so we need to delete and insert
        
        // First, build the delete query
        StringBuilder deleteQuery = new StringBuilder("from(bucket: \"${bucket}\")\n");
        deleteQuery.append("  |> range(start: -30d)\n");
        deleteQuery.append("  |> filter(fn: (r) => r._measurement == \"").append(measurement).append("\")\n");
        
        // Add WHERE clause
        if (whereClause != null && !whereClause.isEmpty()) {
            deleteQuery.append("  |> filter(fn: (r) => ").append(whereClause).append(")\n");
        }
        
        deleteQuery.append("  |> drop()\n");
        
        // Then, build the insert query
        String insertQuery = buildTimeSeriesInsert(measurement, values, params);
        
        // Combine the queries
        return deleteQuery.toString() + "\n" + insertQuery;
    }

    /**
     * Build a SQL DELETE query.
     *
     * @param table the table to delete from
     * @param whereClause the WHERE clause
     * @return the SQL query
     */
    private String buildSqlDelete(
            String table,
            String whereClause) {
        
        StringBuilder query = new StringBuilder("DELETE FROM ");
        query.append(table);
        
        // Add WHERE clause
        if (whereClause != null && !whereClause.isEmpty()) {
            query.append(" WHERE ").append(whereClause);
        }
        
        return query.toString();
    }

    /**
     * Build a NoSQL (MongoDB) DELETE query.
     *
     * @param collection the collection to delete from
     * @param whereClause the WHERE clause
     * @param params the parameters for the query
     * @return the MongoDB query
     */
    private String buildNoSqlDelete(
            String collection,
            String whereClause,
            Map<String, Object> params) {
        
        StringBuilder query = new StringBuilder("{ delete: \"").append(collection).append("\", ");
        
        // Add filter
        if (whereClause != null && !whereClause.isEmpty()) {
            query.append("filter: ").append(whereClause).append(", ");
        } else {
            query.append("filter: {}, ");
        }
        
        query.append("limit: 0 }"); // 0 means no limit
        
        return query.toString();
    }

    /**
     * Build a Graph (Neo4j) DELETE query.
     *
     * @param label the node label to delete
     * @param whereClause the WHERE clause
     * @return the Cypher query
     */
    private String buildGraphDelete(
            String label,
            String whereClause) {
        
        StringBuilder query = new StringBuilder("MATCH (n:");
        
        // Add label
        query.append(label).append(")");
        
        // Add WHERE clause
        if (whereClause != null && !whereClause.isEmpty()) {
            query.append(" WHERE ").append(whereClause);
        }
        
        query.append(" DELETE n");
        
        return query.toString();
    }

    /**
     * Build a Time-Series (InfluxDB) DELETE query.
     *
     * @param measurement the measurement to delete from
     * @param whereClause the WHERE clause
     * @return the Flux query
     */
    private String buildTimeSeriesDelete(
            String measurement,
            String whereClause) {
        
        StringBuilder query = new StringBuilder("from(bucket: \"${bucket}\")\n");
        query.append("  |> range(start: -30d)\n");
        query.append("  |> filter(fn: (r) => r._measurement == \"").append(measurement).append("\")\n");
        
        // Add WHERE clause
        if (whereClause != null && !whereClause.isEmpty()) {
            query.append("  |> filter(fn: (r) => ").append(whereClause).append(")\n");
        }
        
        query.append("  |> drop()\n");
        
        return query.toString();
    }
}

