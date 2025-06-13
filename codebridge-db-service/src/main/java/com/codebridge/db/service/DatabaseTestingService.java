package com.codebridge.db.service;

import com.codebridge.db.dto.QueryResult;
import com.codebridge.db.exception.DatabaseConnectionException;
import com.codebridge.db.model.DatabaseConnection;
import com.codebridge.db.model.DatabaseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for testing database connections and performance.
 */
@Service
@Slf4j
public class DatabaseTestingService {

    private final DatabaseConnectionService connectionService;
    private final ExecutorService executorService;

    @Autowired
    public DatabaseTestingService(DatabaseConnectionService connectionService) {
        this.connectionService = connectionService;
        this.executorService = Executors.newFixedThreadPool(10);
    }

    /**
     * Test a database connection.
     *
     * @param connectionId the database connection ID
     * @return the test result
     */
    public Map<String, Object> testConnection(UUID connectionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get the database connection
            DatabaseConnection connection = connectionService.getConnectionById(connectionId);
            
            // Test the connection
            boolean success = connectionService.testConnection(connectionId);
            
            result.put("success", success);
            result.put("connectionName", connection.getName());
            result.put("databaseType", connection.getType());
            
            if (success) {
                result.put("message", "Connection successful");
            } else {
                result.put("message", "Connection failed");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error testing connection: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Test database performance.
     *
     * @param connectionId the database connection ID
     * @param query the query to execute
     * @param iterations the number of iterations
     * @param concurrentUsers the number of concurrent users
     * @return the performance test result
     */
    public Map<String, Object> testPerformance(
            UUID connectionId,
            String query,
            int iterations,
            int concurrentUsers) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get the database connection
            DatabaseConnection connection = connectionService.getConnectionById(connectionId);
            
            // Validate parameters
            if (iterations <= 0) {
                iterations = 1;
            }
            
            if (concurrentUsers <= 0) {
                concurrentUsers = 1;
            }
            
            // Limit the number of concurrent users
            concurrentUsers = Math.min(concurrentUsers, 10);
            
            // Execute the query once to warm up
            QueryResult warmupResult = connectionService.executeQuery(connectionId, query, null);
            
            if (warmupResult.getError() != null) {
                result.put("success", false);
                result.put("message", "Error executing query: " + warmupResult.getError());
                return result;
            }
            
            // Execute the query multiple times
            long startTime = System.currentTimeMillis();
            List<Long> executionTimes = Collections.synchronizedList(new ArrayList<>());
            
            // Create tasks for concurrent execution
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (int i = 0; i < concurrentUsers; i++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < iterations / concurrentUsers; j++) {
                        try {
                            long queryStartTime = System.currentTimeMillis();
                            QueryResult queryResult = connectionService.executeQuery(connectionId, query, null);
                            long queryEndTime = System.currentTimeMillis();
                            
                            if (queryResult.getError() == null) {
                                executionTimes.add(queryEndTime - queryStartTime);
                            }
                        } catch (Exception e) {
                            log.error("Error executing query", e);
                        }
                    }
                }, executorService);
                
                futures.add(future);
            }
            
            // Wait for all tasks to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.MINUTES);
            
            long endTime = System.currentTimeMillis();
            
            // Calculate statistics
            long totalTime = endTime - startTime;
            int successfulQueries = executionTimes.size();
            double queriesPerSecond = (double) successfulQueries / (totalTime / 1000.0);
            
            double avgExecutionTime = executionTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0);
            
            long minExecutionTime = executionTimes.stream()
                    .mapToLong(Long::longValue)
                    .min()
                    .orElse(0);
            
            long maxExecutionTime = executionTimes.stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0);
            
            // Calculate percentiles
            List<Long> sortedTimes = new ArrayList<>(executionTimes);
            Collections.sort(sortedTimes);
            
            long p50 = calculatePercentile(sortedTimes, 50);
            long p90 = calculatePercentile(sortedTimes, 90);
            long p95 = calculatePercentile(sortedTimes, 95);
            long p99 = calculatePercentile(sortedTimes, 99);
            
            // Build the result
            result.put("success", true);
            result.put("connectionName", connection.getName());
            result.put("databaseType", connection.getType());
            result.put("query", query);
            result.put("iterations", iterations);
            result.put("concurrentUsers", concurrentUsers);
            result.put("totalTime", totalTime);
            result.put("successfulQueries", successfulQueries);
            result.put("queriesPerSecond", queriesPerSecond);
            result.put("avgExecutionTime", avgExecutionTime);
            result.put("minExecutionTime", minExecutionTime);
            result.put("maxExecutionTime", maxExecutionTime);
            result.put("p50", p50);
            result.put("p90", p90);
            result.put("p95", p95);
            result.put("p99", p99);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error testing performance: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Test database data validation.
     *
     * @param connectionId the database connection ID
     * @param table the table to validate
     * @param column the column to validate
     * @param validationRules the validation rules
     * @return the validation test result
     */
    public Map<String, Object> testDataValidation(
            UUID connectionId,
            String table,
            String column,
            Map<String, Object> validationRules) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get the database connection
            DatabaseConnection connection = connectionService.getConnectionById(connectionId);
            
            // Build the validation query based on the database type
            String query;
            
            switch (connection.getType()) {
                case SQL:
                    query = buildSqlValidationQuery(table, column, validationRules);
                    break;
                case NOSQL:
                    query = buildNoSqlValidationQuery(table, column, validationRules);
                    break;
                case GRAPH:
                    query = buildGraphValidationQuery(table, column, validationRules);
                    break;
                case TIMESERIES:
                    query = buildTimeSeriesValidationQuery(table, column, validationRules);
                    break;
                case CLOUD:
                    // Determine the actual database type from the driver or URL
                    if (connection.getDriver().contains("mongodb")) {
                        query = buildNoSqlValidationQuery(table, column, validationRules);
                    } else if (connection.getDriver().contains("neo4j")) {
                        query = buildGraphValidationQuery(table, column, validationRules);
                    } else if (connection.getDriver().contains("influx")) {
                        query = buildTimeSeriesValidationQuery(table, column, validationRules);
                    } else {
                        // Default to SQL for cloud databases
                        query = buildSqlValidationQuery(table, column, validationRules);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported database type: " + connection.getType());
            }
            
            // Execute the validation query
            QueryResult validationResult = connectionService.executeQuery(connectionId, query, null);
            
            if (validationResult.getError() != null) {
                result.put("success", false);
                result.put("message", "Error executing validation query: " + validationResult.getError());
                return result;
            }
            
            // Process the validation result
            List<Map<String, Object>> invalidRows = validationResult.getRows();
            
            result.put("success", true);
            result.put("connectionName", connection.getName());
            result.put("databaseType", connection.getType());
            result.put("table", table);
            result.put("column", column);
            result.put("validationRules", validationRules);
            result.put("invalidRowCount", invalidRows.size());
            result.put("invalidRows", invalidRows);
            
            if (invalidRows.isEmpty()) {
                result.put("message", "All data is valid");
            } else {
                result.put("message", "Found " + invalidRows.size() + " invalid rows");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error testing data validation: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Calculate a percentile value.
     *
     * @param sortedValues the sorted values
     * @param percentile the percentile to calculate
     * @return the percentile value
     */
    private long calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }

    /**
     * Build a SQL validation query.
     *
     * @param table the table to validate
     * @param column the column to validate
     * @param validationRules the validation rules
     * @return the SQL query
     */
    private String buildSqlValidationQuery(
            String table,
            String column,
            Map<String, Object> validationRules) {
        
        StringBuilder query = new StringBuilder("SELECT * FROM ");
        query.append(table).append(" WHERE ");
        
        List<String> conditions = new ArrayList<>();
        
        // Process validation rules
        for (Map.Entry<String, Object> entry : validationRules.entrySet()) {
            String rule = entry.getKey();
            Object value = entry.getValue();
            
            switch (rule) {
                case "notNull":
                    if ((boolean) value) {
                        conditions.add(column + " IS NULL");
                    }
                    break;
                case "notEmpty":
                    if ((boolean) value) {
                        conditions.add("(" + column + " IS NULL OR " + column + " = '')");
                    }
                    break;
                case "minLength":
                    conditions.add("LENGTH(" + column + ") < " + value);
                    break;
                case "maxLength":
                    conditions.add("LENGTH(" + column + ") > " + value);
                    break;
                case "minValue":
                    conditions.add(column + " < " + value);
                    break;
                case "maxValue":
                    conditions.add(column + " > " + value);
                    break;
                case "pattern":
                    conditions.add(column + " NOT REGEXP '" + value + "'");
                    break;
                case "unique":
                    if ((boolean) value) {
                        // This requires a subquery to find duplicates
                        return "SELECT * FROM " + table + " WHERE " + column + " IN (" +
                               "SELECT " + column + " FROM " + table + " GROUP BY " + column + " HAVING COUNT(*) > 1)";
                    }
                    break;
                case "inList":
                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        if (!list.isEmpty()) {
                            StringBuilder inClause = new StringBuilder();
                            inClause.append(column).append(" NOT IN (");
                            for (int i = 0; i < list.size(); i++) {
                                if (list.get(i) instanceof String) {
                                    inClause.append("'").append(list.get(i)).append("'");
                                } else {
                                    inClause.append(list.get(i));
                                }
                                
                                if (i < list.size() - 1) {
                                    inClause.append(", ");
                                }
                            }
                            inClause.append(")");
                            conditions.add(inClause.toString());
                        }
                    }
                    break;
                case "custom":
                    if (value instanceof String) {
                        conditions.add((String) value);
                    }
                    break;
            }
        }
        
        if (conditions.isEmpty()) {
            // If no conditions, return a query that returns no rows
            return "SELECT * FROM " + table + " WHERE 1 = 0";
        }
        
        query.append(String.join(" OR ", conditions));
        
        return query.toString();
    }

    /**
     * Build a NoSQL (MongoDB) validation query.
     *
     * @param collection the collection to validate
     * @param field the field to validate
     * @param validationRules the validation rules
     * @return the MongoDB query
     */
    private String buildNoSqlValidationQuery(
            String collection,
            String field,
            Map<String, Object> validationRules) {
        
        StringBuilder query = new StringBuilder("{ find: \"").append(collection).append("\", filter: { $or: [");
        
        List<String> conditions = new ArrayList<>();
        
        // Process validation rules
        for (Map.Entry<String, Object> entry : validationRules.entrySet()) {
            String rule = entry.getKey();
            Object value = entry.getValue();
            
            switch (rule) {
                case "notNull":
                    if ((boolean) value) {
                        conditions.add("{ \"" + field + "\": { $exists: false } }");
                    }
                    break;
                case "notEmpty":
                    if ((boolean) value) {
                        conditions.add("{ $or: [{ \"" + field + "\": { $exists: false } }, { \"" + field + "\": \"\" }] }");
                    }
                    break;
                case "minLength":
                    conditions.add("{ \"" + field + "\": { $exists: true, $type: \"string\", $expr: { $lt: [{ $strLenCP: \"$" + field + "\" }, " + value + "] } } }");
                    break;
                case "maxLength":
                    conditions.add("{ \"" + field + "\": { $exists: true, $type: \"string\", $expr: { $gt: [{ $strLenCP: \"$" + field + "\" }, " + value + "] } } }");
                    break;
                case "minValue":
                    conditions.add("{ \"" + field + "\": { $exists: true, $lt: " + value + " } }");
                    break;
                case "maxValue":
                    conditions.add("{ \"" + field + "\": { $exists: true, $gt: " + value + " } }");
                    break;
                case "pattern":
                    conditions.add("{ \"" + field + "\": { $exists: true, $not: { $regex: \"" + value + "\" } } }");
                    break;
                case "inList":
                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        if (!list.isEmpty()) {
                            conditions.add("{ \"" + field + "\": { $exists: true, $nin: " + list + " } }");
                        }
                    }
                    break;
            }
        }
        
        if (conditions.isEmpty()) {
            // If no conditions, return a query that returns no documents
            return "{ find: \"" + collection + "\", filter: { $where: \"false\" } }";
        }
        
        query.append(String.join(", ", conditions));
        query.append("] } }");
        
        return query.toString();
    }

    /**
     * Build a Graph (Neo4j) validation query.
     *
     * @param label the node label to validate
     * @param property the property to validate
     * @param validationRules the validation rules
     * @return the Cypher query
     */
    private String buildGraphValidationQuery(
            String label,
            String property,
            Map<String, Object> validationRules) {
        
        StringBuilder query = new StringBuilder("MATCH (n:");
        query.append(label).append(") WHERE ");
        
        List<String> conditions = new ArrayList<>();
        
        // Process validation rules
        for (Map.Entry<String, Object> entry : validationRules.entrySet()) {
            String rule = entry.getKey();
            Object value = entry.getValue();
            
            switch (rule) {
                case "notNull":
                    if ((boolean) value) {
                        conditions.add("n." + property + " IS NULL");
                    }
                    break;
                case "notEmpty":
                    if ((boolean) value) {
                        conditions.add("(n." + property + " IS NULL OR n." + property + " = '')");
                    }
                    break;
                case "minLength":
                    conditions.add("size(n." + property + ") < " + value);
                    break;
                case "maxLength":
                    conditions.add("size(n." + property + ") > " + value);
                    break;
                case "minValue":
                    conditions.add("n." + property + " < " + value);
                    break;
                case "maxValue":
                    conditions.add("n." + property + " > " + value);
                    break;
                case "pattern":
                    conditions.add("NOT n." + property + " =~ '" + value + "'");
                    break;
                case "unique":
                    if ((boolean) value) {
                        // This requires a different query structure
                        return "MATCH (n:" + label + ") WITH n." + property + " AS prop, COUNT(*) AS count " +
                               "WHERE count > 1 MATCH (m:" + label + ") WHERE m." + property + " = prop RETURN m";
                    }
                    break;
                case "inList":
                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        if (!list.isEmpty()) {
                            conditions.add("NOT n." + property + " IN " + list);
                        }
                    }
                    break;
            }
        }
        
        if (conditions.isEmpty()) {
            // If no conditions, return a query that returns no nodes
            return "MATCH (n:" + label + ") WHERE false RETURN n";
        }
        
        query.append(String.join(" OR ", conditions));
        query.append(" RETURN n");
        
        return query.toString();
    }

    /**
     * Build a Time-Series (InfluxDB) validation query.
     *
     * @param measurement the measurement to validate
     * @param field the field to validate
     * @param validationRules the validation rules
     * @return the Flux query
     */
    private String buildTimeSeriesValidationQuery(
            String measurement,
            String field,
            Map<String, Object> validationRules) {
        
        StringBuilder query = new StringBuilder("from(bucket: \"${bucket}\")\n");
        query.append("  |> range(start: -30d)\n");
        query.append("  |> filter(fn: (r) => r._measurement == \"").append(measurement).append("\")\n");
        query.append("  |> filter(fn: (r) => r._field == \"").append(field).append("\")\n");
        query.append("  |> filter(fn: (r) => ");
        
        List<String> conditions = new ArrayList<>();
        
        // Process validation rules
        for (Map.Entry<String, Object> entry : validationRules.entrySet()) {
            String rule = entry.getKey();
            Object value = entry.getValue();
            
            switch (rule) {
                case "notNull":
                    if ((boolean) value) {
                        conditions.add("r._value == null");
                    }
                    break;
                case "minValue":
                    conditions.add("r._value < " + value);
                    break;
                case "maxValue":
                    conditions.add("r._value > " + value);
                    break;
                case "inList":
                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        if (!list.isEmpty()) {
                            StringBuilder inClause = new StringBuilder();
                            inClause.append("not (");
                            for (int i = 0; i < list.size(); i++) {
                                inClause.append("r._value == ");
                                if (list.get(i) instanceof String) {
                                    inClause.append("\"").append(list.get(i)).append("\"");
                                } else {
                                    inClause.append(list.get(i));
                                }
                                
                                if (i < list.size() - 1) {
                                    inClause.append(" or ");
                                }
                            }
                            inClause.append(")");
                            conditions.add(inClause.toString());
                        }
                    }
                    break;
            }
        }
        
        if (conditions.isEmpty()) {
            // If no conditions, return a query that returns no points
            query.append("false");
        } else {
            query.append(String.join(" or ", conditions));
        }
        
        query.append(")\n");
        
        return query.toString();
    }
}

