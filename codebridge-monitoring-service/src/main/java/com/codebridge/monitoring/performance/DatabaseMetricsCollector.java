package com.codebridge.monitoring.performance.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collector for database-related metrics.
 * This collector captures query execution times, connection pool statistics,
 * and database operation success/failure rates.
 */
@Component
@Slf4j
public class DatabaseMetricsCollector {

    private static final String SERVICE_NAME = "database-service";
    
    private final PerformanceMetricsCollector metricsCollector;
    private final ConcurrentHashMap<String, AtomicLong> queryTypeCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> queryTypeFailureCounts = new ConcurrentHashMap<>();
    
    @Value("${services.database.metrics-enabled:true}")
    private boolean databaseMetricsEnabled;
    
    @Value("${services.database.slow-query-threshold:1000}")
    private long slowQueryThreshold;

    @Autowired
    public DatabaseMetricsCollector(PerformanceMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    /**
     * Record database query execution metrics.
     *
     * @param databaseType the database type (e.g., MySQL, PostgreSQL, MongoDB)
     * @param queryType the query type (e.g., SELECT, INSERT, UPDATE, DELETE)
     * @param durationMs the query execution duration in milliseconds
     * @param success whether the query was successful
     */
    public void recordQueryExecution(String databaseType, String queryType, long durationMs, boolean success) {
        if (!databaseMetricsEnabled) {
            return;
        }
        
        Map<String, String> tags = new HashMap<>();
        tags.put("database_type", databaseType);
        tags.put("query_type", queryType);
        tags.put("success", String.valueOf(success));
        
        // Record query execution time
        metricsCollector.recordTimer(SERVICE_NAME, "query.execution.time", durationMs, tags);
        
        // Increment query counter
        metricsCollector.incrementCounter(SERVICE_NAME, "query.execution.count", tags);
        
        // Track query type metrics
        String queryTypeKey = databaseType + ":" + queryType;
        queryTypeCounts.computeIfAbsent(queryTypeKey, k -> new AtomicLong(0)).incrementAndGet();
        
        if (!success) {
            // Record query failure
            metricsCollector.incrementCounter(SERVICE_NAME, "query.execution.failure", tags);
            
            // Track query type failure
            queryTypeFailureCounts.computeIfAbsent(queryTypeKey, k -> new AtomicLong(0)).incrementAndGet();
        }
        
        // Record slow query if applicable
        if (durationMs >= slowQueryThreshold) {
            metricsCollector.incrementCounter(SERVICE_NAME, "query.slow", tags);
        }
        
        // Calculate and record failure rate
        long queryCount = queryTypeCounts.get(queryTypeKey).get();
        long failureCount = queryTypeFailureCounts.getOrDefault(queryTypeKey, new AtomicLong(0)).get();
        double failureRate = (double) failureCount / queryCount;
        
        Map<String, String> failureRateTags = new HashMap<>();
        failureRateTags.put("database_type", databaseType);
        failureRateTags.put("query_type", queryType);
        metricsCollector.recordGauge(SERVICE_NAME, "query.failure.rate", failureRate, failureRateTags);
    }

    /**
     * Record database connection pool metrics.
     *
     * @param databaseType the database type
     * @param poolName the connection pool name
     * @param activeConnections the number of active connections
     * @param idleConnections the number of idle connections
     * @param maxConnections the maximum number of connections
     * @param waitingThreads the number of threads waiting for a connection
     */
    public void recordConnectionPoolStats(String databaseType, String poolName, 
                                         int activeConnections, int idleConnections, 
                                         int maxConnections, int waitingThreads) {
        if (!databaseMetricsEnabled) {
            return;
        }
        
        Map<String, String> tags = new HashMap<>();
        tags.put("database_type", databaseType);
        tags.put("pool_name", poolName);
        
        // Record connection pool metrics
        metricsCollector.recordGauge(SERVICE_NAME, "connection.pool.active", activeConnections, tags);
        metricsCollector.recordGauge(SERVICE_NAME, "connection.pool.idle", idleConnections, tags);
        metricsCollector.recordGauge(SERVICE_NAME, "connection.pool.max", maxConnections, tags);
        metricsCollector.recordGauge(SERVICE_NAME, "connection.pool.waiting", waitingThreads, tags);
        
        // Calculate and record utilization percentage
        double utilizationPercentage = maxConnections > 0 ? 
                (double) activeConnections / maxConnections * 100 : 0;
        metricsCollector.recordGauge(SERVICE_NAME, "connection.pool.utilization", utilizationPercentage, tags);
    }

    /**
     * Record database transaction metrics.
     *
     * @param databaseType the database type
     * @param transactionName the transaction name or identifier
     * @param durationMs the transaction duration in milliseconds
     * @param success whether the transaction was successful
     */
    public void recordTransaction(String databaseType, String transactionName, long durationMs, boolean success) {
        if (!databaseMetricsEnabled) {
            return;
        }
        
        Map<String, String> tags = new HashMap<>();
        tags.put("database_type", databaseType);
        tags.put("transaction_name", transactionName);
        tags.put("success", String.valueOf(success));
        
        // Record transaction execution time
        metricsCollector.recordTimer(SERVICE_NAME, "transaction.time", durationMs, tags);
        
        // Increment transaction counter
        metricsCollector.incrementCounter(SERVICE_NAME, "transaction.count", tags);
        
        if (!success) {
            // Record transaction failure
            metricsCollector.incrementCounter(SERVICE_NAME, "transaction.failure", tags);
        }
    }

    /**
     * Record database schema operation metrics.
     *
     * @param databaseType the database type
     * @param operationType the operation type (e.g., CREATE_TABLE, ALTER_TABLE, CREATE_INDEX)
     * @param durationMs the operation duration in milliseconds
     * @param success whether the operation was successful
     */
    public void recordSchemaOperation(String databaseType, String operationType, long durationMs, boolean success) {
        if (!databaseMetricsEnabled) {
            return;
        }
        
        Map<String, String> tags = new HashMap<>();
        tags.put("database_type", databaseType);
        tags.put("operation_type", operationType);
        tags.put("success", String.valueOf(success));
        
        // Record schema operation execution time
        metricsCollector.recordTimer(SERVICE_NAME, "schema.operation.time", durationMs, tags);
        
        // Increment schema operation counter
        metricsCollector.incrementCounter(SERVICE_NAME, "schema.operation.count", tags);
        
        if (!success) {
            // Record schema operation failure
            metricsCollector.incrementCounter(SERVICE_NAME, "schema.operation.failure", tags);
        }
    }
}

