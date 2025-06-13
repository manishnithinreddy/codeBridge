package com.codebridge.aidb.db.service.connector;

import com.codebridge.aidb.db.dto.QueryResult;
import com.codebridge.aidb.db.model.DatabaseConnection;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DatabaseConnector for Time-Series databases (InfluxDB).
 */
@Component
@Slf4j
public class TimeSeriesDatabaseConnector implements DatabaseConnector {

    private InfluxDBClient influxDBClient;
    private DatabaseConnection connectionConfig;
    private String org;
    private String bucket;

    @Override
    public boolean initialize(DatabaseConnection connection) {
        this.connectionConfig = connection;
        
        try {
            // Parse URL to extract org and bucket
            // InfluxDB URL format: http://host:port?org=myorg&bucket=mybucket
            String url = connection.getUrl();
            Map<String, String> params = parseUrlParams(url);
            
            this.org = params.getOrDefault("org", "");
            this.bucket = params.getOrDefault("bucket", "");
            
            if (org.isEmpty() || bucket.isEmpty()) {
                log.error("InfluxDB org and bucket must be specified in the URL");
                return false;
            }
            
            // Create InfluxDB client
            this.influxDBClient = InfluxDBClientFactory.create(
                    url.split("\\?")[0], // Base URL without params
                    connection.getPassword().toCharArray(), // Token
                    org,
                    bucket);
            
            // Test connection
            influxDBClient.ping();
            
            log.info("Successfully initialized InfluxDB connection: {}", connection.getName());
            return true;
        } catch (Exception e) {
            log.error("Failed to initialize InfluxDB connection: {}", connection.getName(), e);
            return false;
        }
    }

    @Override
    public QueryResult executeQuery(String query, Map<String, Object> params) {
        if (influxDBClient == null) {
            return QueryResult.withError("Database connection not initialized");
        }

        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Replace parameters in the query
            String processedQuery = query;
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    processedQuery = processedQuery.replace(":" + entry.getKey(), formatFluxValue(entry.getValue()));
                }
            }
            
            // Execute the query
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(processedQuery);
            
            if (tables.isEmpty()) {
                return QueryResult.builder()
                        .columns(new ArrayList<>())
                        .rows(new ArrayList<>())
                        .rowsAffected(0)
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .warnings(warnings)
                        .build();
            }
            
            // Get columns from the first table
            Set<String> columnSet = new HashSet<>();
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    columnSet.addAll(record.getValues().keySet());
                }
            }
            List<String> columns = new ArrayList<>(columnSet);
            
            // Process records
            List<Map<String, Object>> rows = new ArrayList<>();
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> row = new HashMap<>();
                    for (String column : columns) {
                        row.put(column, record.getValueByKey(column));
                    }
                    rows.add(row);
                }
            }
            
            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("database", bucket);
            metadata.put("driver", "InfluxDB");
            metadata.put("url", connectionConfig.getUrl());
            metadata.put("org", org);
            
            return QueryResult.builder()
                    .columns(columns)
                    .rows(rows)
                    .rowsAffected(0) // InfluxDB queries don't affect rows
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .warnings(warnings)
                    .metadata(metadata)
                    .build();
        } catch (Exception e) {
            log.error("Error executing InfluxDB query: {}", query, e);
            return QueryResult.withError("InfluxDB Error: " + e.getMessage());
        }
    }

    @Override
    public int executeUpdate(String query, Map<String, Object> params) {
        if (influxDBClient == null) {
            log.error("Database connection not initialized");
            return -1;
        }

        try {
            // Replace parameters in the query
            String processedQuery = query;
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    processedQuery = processedQuery.replace(":" + entry.getKey(), formatFluxValue(entry.getValue()));
                }
            }
            
            // Execute the query
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(processedQuery);
            
            // For InfluxDB, we can't easily determine the number of affected rows
            // Return 1 if the query was successful
            return 1;
        } catch (Exception e) {
            log.error("Error executing InfluxDB update: {}", query, e);
            return -1;
        }
    }

    @Override
    public boolean testConnection() {
        if (influxDBClient == null) {
            return false;
        }

        try {
            influxDBClient.ping();
            return true;
        } catch (Exception e) {
            log.error("Error testing InfluxDB connection", e);
            return false;
        }
    }

    @Override
    public void close() {
        if (influxDBClient != null) {
            try {
                influxDBClient.close();
                influxDBClient = null;
                log.info("Closed InfluxDB connection: {}", connectionConfig.getName());
            } catch (Exception e) {
                log.error("Error closing InfluxDB connection", e);
            }
        }
    }

    @Override
    public String getDatabaseType() {
        return "TIMESERIES";
    }

    /**
     * Parse URL parameters.
     *
     * @param url the URL
     * @return the parameters
     */
    private Map<String, String> parseUrlParams(String url) {
        Map<String, String> params = new HashMap<>();
        
        if (url.contains("?")) {
            String[] parts = url.split("\\?", 2);
            if (parts.length > 1) {
                String[] paramPairs = parts[1].split("&");
                for (String pair : paramPairs) {
                    String[] keyValue = pair.split("=", 2);
                    if (keyValue.length > 1) {
                        params.put(keyValue[0], keyValue[1]);
                    }
                }
            }
        }
        
        return params;
    }

    /**
     * Format a value for use in a Flux query.
     *
     * @param value the value
     * @return the formatted value
     */
    private String formatFluxValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + ((String) value).replace("\"", "\\\"") + "\"";
        } else if (value instanceof Date) {
            return "time(" + ((Date) value).getTime() + ")";
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof List) {
            return "[" + ((List<?>) value).stream()
                    .map(this::formatFluxValue)
                    .collect(Collectors.joining(", ")) + "]";
        } else if (value instanceof Map) {
            return "{" + ((Map<?, ?>) value).entrySet().stream()
                    .map(e -> formatFluxValue(e.getKey()) + ": " + formatFluxValue(e.getValue()))
                    .collect(Collectors.joining(", ")) + "}";
        } else {
            return value.toString();
        }
    }
}

