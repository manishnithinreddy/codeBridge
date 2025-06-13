package com.codebridge.monitoring.performance.model;

/**
 * Enum representing different types of performance metrics.
 */
public enum MetricType {
    /**
     * A counter that only increases (e.g., request count, error count).
     */
    COUNTER,
    
    /**
     * A gauge that represents a current value (e.g., memory usage, CPU usage).
     */
    GAUGE,
    
    /**
     * A timer that measures duration (e.g., request duration, method execution time).
     */
    TIMER,
    
    /**
     * A histogram that tracks the distribution of values (e.g., response size distribution).
     */
    HISTOGRAM,
    
    /**
     * A summary that provides percentile statistics (e.g., p50, p90, p99 of response times).
     */
    SUMMARY
}

