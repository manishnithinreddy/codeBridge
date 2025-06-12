package com.codebridge.db.service.connector;

import com.codebridge.db.dto.QueryResult;
import com.codebridge.db.model.DatabaseConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of DatabaseConnector for SQL databases.
 */
@Component
@Slf4j
public class SqlDatabaseConnector implements DatabaseConnector {

    private Connection connection;
    private DatabaseConnection connectionConfig;

    @Override
    public boolean initialize(DatabaseConnection connection) {
        this.connectionConfig = connection;
        try {
            // Register the JDBC driver
            Class.forName(connection.getDriver());
            
            // Create properties with additional connection parameters
            Properties props = new Properties();
            props.setProperty("user", connection.getUsername());
            props.setProperty("password", connection.getPassword());
            
            // Add any additional properties from the connection config
            if (connection.getProperties() != null) {
                connection.getProperties().forEach(props::setProperty);
            }
            
            // Establish the connection
            this.connection = DriverManager.getConnection(connection.getUrl(), props);
            
            log.info("Successfully initialized SQL database connection: {}", connection.getName());
            return true;
        } catch (ClassNotFoundException e) {
            log.error("Database driver not found: {}", connection.getDriver(), e);
            return false;
        } catch (SQLException e) {
            log.error("Failed to initialize SQL database connection: {}", connection.getName(), e);
            return false;
        }
    }

    @Override
    public QueryResult executeQuery(String query, Map<String, Object> params) {
        if (connection == null) {
            return QueryResult.withError("Database connection not initialized");
        }

        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        
        try (PreparedStatement stmt = prepareStatement(query, params)) {
            boolean isResultSet = stmt.execute();
            
            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    return processResultSet(rs, startTime, warnings);
                }
            } else {
                int rowsAffected = stmt.getUpdateCount();
                return QueryResult.builder()
                        .columns(new ArrayList<>())
                        .rows(new ArrayList<>())
                        .rowsAffected(rowsAffected)
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .warnings(warnings)
                        .build();
            }
        } catch (SQLException e) {
            log.error("Error executing SQL query: {}", query, e);
            return QueryResult.withError("SQL Error: " + e.getMessage());
        }
    }

    @Override
    public int executeUpdate(String query, Map<String, Object> params) {
        if (connection == null) {
            log.error("Database connection not initialized");
            return -1;
        }

        try (PreparedStatement stmt = prepareStatement(query, params)) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error executing SQL update: {}", query, e);
            return -1;
        }
    }

    @Override
    public boolean testConnection() {
        if (connection == null) {
            return false;
        }

        try {
            return connection.isValid(5); // 5 seconds timeout
        } catch (SQLException e) {
            log.error("Error testing SQL connection", e);
            return false;
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                log.info("Closed SQL database connection: {}", connectionConfig.getName());
            } catch (SQLException e) {
                log.error("Error closing SQL connection", e);
            }
        }
    }

    @Override
    public String getDatabaseType() {
        return "SQL";
    }

    /**
     * Prepare a SQL statement with parameters.
     *
     * @param query the SQL query
     * @param params the parameters for the query
     * @return the prepared statement
     * @throws SQLException if an error occurs
     */
    private PreparedStatement prepareStatement(String query, Map<String, Object> params) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(query);
        
        if (params != null && !params.isEmpty()) {
            // Named parameters are not natively supported in JDBC, so we need to convert them
            // This is a simplified implementation that assumes parameters are in the format :paramName
            Map<String, List<Integer>> paramMap = new HashMap<>();
            
            // Find all parameter positions
            String[] parts = query.split(":");
            int paramIndex = 1;
            
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                String paramName = part.split("\\W")[0]; // Get the parameter name
                
                paramMap.computeIfAbsent(paramName, k -> new ArrayList<>()).add(paramIndex++);
            }
            
            // Set parameter values
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                List<Integer> positions = paramMap.get(entry.getKey());
                if (positions != null) {
                    for (int position : positions) {
                        setParameter(stmt, position, entry.getValue());
                    }
                }
            }
        }
        
        return stmt;
    }

    /**
     * Set a parameter in a prepared statement.
     *
     * @param stmt the prepared statement
     * @param position the parameter position
     * @param value the parameter value
     * @throws SQLException if an error occurs
     */
    private void setParameter(PreparedStatement stmt, int position, Object value) throws SQLException {
        if (value == null) {
            stmt.setNull(position, Types.NULL);
        } else if (value instanceof String) {
            stmt.setString(position, (String) value);
        } else if (value instanceof Integer) {
            stmt.setInt(position, (Integer) value);
        } else if (value instanceof Long) {
            stmt.setLong(position, (Long) value);
        } else if (value instanceof Double) {
            stmt.setDouble(position, (Double) value);
        } else if (value instanceof Boolean) {
            stmt.setBoolean(position, (Boolean) value);
        } else if (value instanceof Date) {
            stmt.setDate(position, (Date) value);
        } else if (value instanceof Timestamp) {
            stmt.setTimestamp(position, (Timestamp) value);
        } else {
            stmt.setObject(position, value);
        }
    }

    /**
     * Process a SQL result set into a QueryResult.
     *
     * @param rs the result set
     * @param startTime the query start time
     * @param warnings the list of warnings
     * @return the query result
     * @throws SQLException if an error occurs
     */
    private QueryResult processResultSet(ResultSet rs, long startTime, List<String> warnings) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Get column names
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }
        
        // Get rows
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        
        // Check for warnings
        SQLWarning sqlWarning = rs.getWarnings();
        while (sqlWarning != null) {
            warnings.add(sqlWarning.getMessage());
            sqlWarning = sqlWarning.getNextWarning();
        }
        
        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("database", connection.getMetaData().getDatabaseProductName());
        metadata.put("driver", connection.getMetaData().getDriverName());
        metadata.put("url", connectionConfig.getUrl());
        
        return QueryResult.builder()
                .columns(columns)
                .rows(rows)
                .rowsAffected(0) // SELECT queries don't affect rows
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .warnings(warnings)
                .metadata(metadata)
                .build();
    }
}

