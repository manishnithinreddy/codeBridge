package com.codebridge.aidb.db.service.connector;

import com.codebridge.aidb.db.dto.QueryResult;
import com.codebridge.aidb.db.model.DatabaseConnection;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.types.Type;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of DatabaseConnector for Graph databases (Neo4j).
 */
@Component
@Slf4j
public class GraphDatabaseConnector implements DatabaseConnector {

    private Driver driver;
    private DatabaseConnection connectionConfig;

    @Override
    public boolean initialize(DatabaseConnection connection) {
        this.connectionConfig = connection;
        
        try {
            // Create Neo4j configuration
            Config config = Config.builder()
                    .withConnectionTimeout(30, TimeUnit.SECONDS)
                    .withMaxConnectionLifetime(1, TimeUnit.HOURS)
                    .withMaxConnectionPoolSize(10)
                    .withConnectionAcquisitionTimeout(30, TimeUnit.SECONDS)
                    .build();
            
            // Create Neo4j driver
            this.driver = GraphDatabase.driver(
                    connection.getUrl(),
                    AuthTokens.basic(connection.getUsername(), connection.getPassword()),
                    config);
            
            // Test connection
            try (Session session = driver.session()) {
                session.run("RETURN 1").consume();
            }
            
            log.info("Successfully initialized Neo4j connection: {}", connection.getName());
            return true;
        } catch (Exception e) {
            log.error("Failed to initialize Neo4j connection: {}", connection.getName(), e);
            return false;
        }
    }

    @Override
    public QueryResult executeQuery(String query, Map<String, Object> params) {
        if (driver == null) {
            return QueryResult.withError("Database connection not initialized");
        }

        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        
        try (Session session = driver.session()) {
            // Convert Java parameters to Neo4j parameters
            Map<String, Object> neo4jParams = new HashMap<>();
            if (params != null) {
                params.forEach((key, value) -> neo4jParams.put(key, convertToNeo4jValue(value)));
            }
            
            // Execute the query
            Result result = session.run(query, neo4jParams);
            
            // Process the result
            List<String> columns = result.keys();
            List<Map<String, Object>> rows = new ArrayList<>();
            
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> row = new HashMap<>();
                
                for (String key : columns) {
                    Value value = record.get(key);
                    row.put(key, convertFromNeo4jValue(value));
                }
                
                rows.add(row);
            }
            
            // Get summary
            ResultSummary summary = result.consume();
            
            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("database", summary.database().name());
            metadata.put("driver", "Neo4j");
            metadata.put("url", connectionConfig.getUrl());
            metadata.put("query_type", summary.queryType().name());
            metadata.put("plan", summary.hasPlan() ? summary.plan().toString() : null);
            
            return QueryResult.builder()
                    .columns(columns)
                    .rows(rows)
                    .rowsAffected(summary.counters().containsUpdates() ? 
                            summary.counters().nodesCreated() + 
                            summary.counters().nodesDeleted() + 
                            summary.counters().relationshipsCreated() + 
                            summary.counters().relationshipsDeleted() : 0)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .warnings(warnings)
                    .metadata(metadata)
                    .build();
        } catch (Exception e) {
            log.error("Error executing Neo4j query: {}", query, e);
            return QueryResult.withError("Neo4j Error: " + e.getMessage());
        }
    }

    @Override
    public int executeUpdate(String query, Map<String, Object> params) {
        if (driver == null) {
            log.error("Database connection not initialized");
            return -1;
        }

        try (Session session = driver.session()) {
            // Convert Java parameters to Neo4j parameters
            Map<String, Object> neo4jParams = new HashMap<>();
            if (params != null) {
                params.forEach((key, value) -> neo4jParams.put(key, convertToNeo4jValue(value)));
            }
            
            // Execute the query
            Result result = session.run(query, neo4jParams);
            ResultSummary summary = result.consume();
            
            // Return the number of affected nodes and relationships
            return summary.counters().nodesCreated() + 
                   summary.counters().nodesDeleted() + 
                   summary.counters().relationshipsCreated() + 
                   summary.counters().relationshipsDeleted();
        } catch (Exception e) {
            log.error("Error executing Neo4j update: {}", query, e);
            return -1;
        }
    }

    @Override
    public boolean testConnection() {
        if (driver == null) {
            return false;
        }

        try (Session session = driver.session()) {
            session.run("RETURN 1").consume();
            return true;
        } catch (Exception e) {
            log.error("Error testing Neo4j connection", e);
            return false;
        }
    }

    @Override
    public void close() {
        if (driver != null) {
            try {
                driver.close();
                driver = null;
                log.info("Closed Neo4j connection: {}", connectionConfig.getName());
            } catch (Exception e) {
                log.error("Error closing Neo4j connection", e);
            }
        }
    }

    @Override
    public String getDatabaseType() {
        return "GRAPH";
    }

    /**
     * Convert a Java value to a Neo4j value.
     *
     * @param value the Java value
     * @return the Neo4j value
     */
    private Object convertToNeo4jValue(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Map) {
            Map<String, Object> map = new HashMap<>();
            ((Map<?, ?>) value).forEach((k, v) -> map.put(k.toString(), convertToNeo4jValue(v)));
            return map;
        } else if (value instanceof List) {
            List<Object> list = new ArrayList<>();
            ((List<?>) value).forEach(v -> list.add(convertToNeo4jValue(v)));
            return list;
        } else {
            return value;
        }
    }

    /**
     * Convert a Neo4j value to a Java value.
     *
     * @param value the Neo4j value
     * @return the Java value
     */
    private Object convertFromNeo4jValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        
        Type type = value.type();
        
        switch (type.name()) {
            case "NODE":
                return convertNode(value.asNode());
            case "RELATIONSHIP":
                return convertRelationship(value.asRelationship());
            case "PATH":
                return convertPath(value.asPath());
            case "LIST":
                return value.asList(this::convertFromNeo4jValue);
            case "MAP":
                return value.asMap(this::convertFromNeo4jValue);
            default:
                return value.asObject();
        }
    }

    /**
     * Convert a Neo4j Node to a Java Map.
     *
     * @param node the Neo4j Node
     * @return the Java Map
     */
    private Map<String, Object> convertNode(Node node) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", node.id());
        result.put("labels", node.labels());
        
        Map<String, Object> properties = new HashMap<>();
        node.asMap().forEach((key, value) -> properties.put(key, value));
        result.put("properties", properties);
        
        return result;
    }

    /**
     * Convert a Neo4j Relationship to a Java Map.
     *
     * @param relationship the Neo4j Relationship
     * @return the Java Map
     */
    private Map<String, Object> convertRelationship(Relationship relationship) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", relationship.id());
        result.put("type", relationship.type());
        result.put("startNodeId", relationship.startNodeId());
        result.put("endNodeId", relationship.endNodeId());
        
        Map<String, Object> properties = new HashMap<>();
        relationship.asMap().forEach((key, value) -> properties.put(key, value));
        result.put("properties", properties);
        
        return result;
    }

    /**
     * Convert a Neo4j Path to a Java Map.
     *
     * @param path the Neo4j Path
     * @return the Java Map
     */
    private Map<String, Object> convertPath(Path path) {
        Map<String, Object> result = new HashMap<>();
        
        List<Map<String, Object>> nodes = new ArrayList<>();
        path.nodes().forEach(node -> nodes.add(convertNode(node)));
        result.put("nodes", nodes);
        
        List<Map<String, Object>> relationships = new ArrayList<>();
        path.relationships().forEach(rel -> relationships.add(convertRelationship(rel)));
        result.put("relationships", relationships);
        
        return result;
    }
}

