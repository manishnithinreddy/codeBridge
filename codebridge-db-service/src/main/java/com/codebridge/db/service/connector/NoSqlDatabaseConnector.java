package com.codebridge.db.service.connector;

import com.codebridge.db.dto.QueryResult;
import com.codebridge.db.model.DatabaseConnection;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of DatabaseConnector for NoSQL databases (MongoDB).
 */
@Component
@Slf4j
public class NoSqlDatabaseConnector implements DatabaseConnector {

    private MongoClient mongoClient;
    private DatabaseConnection connectionConfig;
    private String databaseName;

    @Override
    public boolean initialize(DatabaseConnection connection) {
        this.connectionConfig = connection;
        
        try {
            // Extract database name from URL
            // MongoDB URL format: mongodb://host:port/database
            String url = connection.getUrl();
            this.databaseName = url.substring(url.lastIndexOf('/') + 1);
            
            // Create connection string with credentials
            ConnectionString connectionString = new ConnectionString(
                    url.replace(databaseName, "") + 
                    databaseName + 
                    "?authSource=admin");
            
            // Configure MongoDB client settings
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .credential(com.mongodb.MongoCredential.createCredential(
                            connection.getUsername(),
                            "admin",
                            connection.getPassword().toCharArray()))
                    .build();
            
            // Create MongoDB client
            this.mongoClient = MongoClients.create(settings);
            
            // Test connection by listing collections
            mongoClient.getDatabase(databaseName).listCollectionNames().first();
            
            log.info("Successfully initialized MongoDB connection: {}", connection.getName());
            return true;
        } catch (Exception e) {
            log.error("Failed to initialize MongoDB connection: {}", connection.getName(), e);
            return false;
        }
    }

    @Override
    public QueryResult executeQuery(String query, Map<String, Object> params) {
        if (mongoClient == null) {
            return QueryResult.withError("Database connection not initialized");
        }

        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Parse the query as a MongoDB command
            Document queryDocument = Document.parse(query);
            
            // Replace parameters in the query
            if (params != null && !params.isEmpty()) {
                replaceParameters(queryDocument, params);
            }
            
            // Execute the query
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            Document result;
            
            // Check if this is a find operation
            if (queryDocument.containsKey("find")) {
                String collectionName = queryDocument.getString("find");
                MongoCollection<Document> collection = database.getCollection(collectionName);
                
                // Extract filter, projection, sort, limit, skip
                Document filter = queryDocument.get("filter", new Document());
                Document projection = queryDocument.get("projection", new Document());
                Document sort = queryDocument.get("sort", new Document());
                int limit = queryDocument.getInteger("limit", 0);
                int skip = queryDocument.getInteger("skip", 0);
                
                // Execute find operation
                FindIterable<Document> findResult = collection.find(filter)
                        .projection(projection)
                        .sort(sort);
                
                if (skip > 0) {
                    findResult = findResult.skip(skip);
                }
                
                if (limit > 0) {
                    findResult = findResult.limit(limit);
                }
                
                // Convert result to list
                List<Document> documents = new ArrayList<>();
                findResult.into(documents);
                
                return convertDocumentsToQueryResult(documents, startTime, warnings);
            } else {
                // Execute command directly
                result = database.runCommand(queryDocument);
                
                // Convert result to QueryResult
                return convertCommandResultToQueryResult(result, startTime, warnings);
            }
        } catch (Exception e) {
            log.error("Error executing MongoDB query: {}", query, e);
            return QueryResult.withError("MongoDB Error: " + e.getMessage());
        }
    }

    @Override
    public int executeUpdate(String query, Map<String, Object> params) {
        if (mongoClient == null) {
            log.error("Database connection not initialized");
            return -1;
        }

        try {
            // Parse the query as a MongoDB command
            Document queryDocument = Document.parse(query);
            
            // Replace parameters in the query
            if (params != null && !params.isEmpty()) {
                replaceParameters(queryDocument, params);
            }
            
            // Execute the command
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            Document result = database.runCommand(queryDocument);
            
            // Check if the command was successful
            if (result.getBoolean("ok", false)) {
                // For insert, update, delete operations, return the number of affected documents
                if (result.containsKey("n")) {
                    return result.getInteger("n");
                }
                return 1; // Command was successful but no count available
            } else {
                return 0; // Command failed
            }
        } catch (Exception e) {
            log.error("Error executing MongoDB update: {}", query, e);
            return -1;
        }
    }

    @Override
    public boolean testConnection() {
        if (mongoClient == null) {
            return false;
        }

        try {
            // Test connection by listing collections
            mongoClient.getDatabase(databaseName).listCollectionNames().first();
            return true;
        } catch (Exception e) {
            log.error("Error testing MongoDB connection", e);
            return false;
        }
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                mongoClient = null;
                log.info("Closed MongoDB connection: {}", connectionConfig.getName());
            } catch (Exception e) {
                log.error("Error closing MongoDB connection", e);
            }
        }
    }

    @Override
    public String getDatabaseType() {
        return "NOSQL";
    }

    /**
     * Replace parameters in a MongoDB document.
     *
     * @param document the document
     * @param params the parameters
     */
    private void replaceParameters(Document document, Map<String, Object> params) {
        for (String key : document.keySet()) {
            Object value = document.get(key);
            
            if (value instanceof Document) {
                // Recursively process nested documents
                replaceParameters((Document) value, params);
            } else if (value instanceof List) {
                // Process list elements
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Document) {
                        replaceParameters((Document) item, params);
                    } else if (item instanceof String) {
                        String strItem = (String) item;
                        if (strItem.startsWith(":") && params.containsKey(strItem.substring(1))) {
                            ((List<Object>) list).set(i, params.get(strItem.substring(1)));
                        }
                    }
                }
            } else if (value instanceof String) {
                String strValue = (String) value;
                if (strValue.startsWith(":") && params.containsKey(strValue.substring(1))) {
                    document.put(key, params.get(strValue.substring(1)));
                }
            }
        }
    }

    /**
     * Convert a list of MongoDB documents to a QueryResult.
     *
     * @param documents the documents
     * @param startTime the query start time
     * @param warnings the list of warnings
     * @return the query result
     */
    private QueryResult convertDocumentsToQueryResult(List<Document> documents, long startTime, List<String> warnings) {
        if (documents.isEmpty()) {
            return QueryResult.builder()
                    .columns(new ArrayList<>())
                    .rows(new ArrayList<>())
                    .rowsAffected(0)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .warnings(warnings)
                    .build();
        }
        
        // Get all unique keys from all documents
        Set<String> allKeys = new HashSet<>();
        for (Document doc : documents) {
            allKeys.addAll(doc.keySet());
        }
        
        List<String> columns = new ArrayList<>(allKeys);
        List<Map<String, Object>> rows = new ArrayList<>();
        
        // Convert each document to a row
        for (Document doc : documents) {
            Map<String, Object> row = new HashMap<>();
            for (String key : columns) {
                row.put(key, doc.get(key));
            }
            rows.add(row);
        }
        
        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("database", databaseName);
        metadata.put("driver", "MongoDB");
        metadata.put("url", connectionConfig.getUrl());
        
        return QueryResult.builder()
                .columns(columns)
                .rows(rows)
                .rowsAffected(0)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .warnings(warnings)
                .metadata(metadata)
                .build();
    }

    /**
     * Convert a MongoDB command result to a QueryResult.
     *
     * @param result the command result
     * @param startTime the query start time
     * @param warnings the list of warnings
     * @return the query result
     */
    private QueryResult convertCommandResultToQueryResult(Document result, long startTime, List<String> warnings) {
        // Check if the command was successful
        if (!result.getBoolean("ok", false)) {
            return QueryResult.withError("MongoDB command failed: " + result.getString("errmsg"));
        }
        
        // Convert the result to a QueryResult
        List<String> columns = new ArrayList<>(result.keySet());
        List<Map<String, Object>> rows = new ArrayList<>();
        
        Map<String, Object> row = new HashMap<>();
        for (String key : columns) {
            row.put(key, result.get(key));
        }
        rows.add(row);
        
        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("database", databaseName);
        metadata.put("driver", "MongoDB");
        metadata.put("url", connectionConfig.getUrl());
        
        return QueryResult.builder()
                .columns(columns)
                .rows(rows)
                .rowsAffected(result.getInteger("n", 0))
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .warnings(warnings)
                .metadata(metadata)
                .build();
    }
}

