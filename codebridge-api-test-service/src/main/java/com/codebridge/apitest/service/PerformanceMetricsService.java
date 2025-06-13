package com.codebridge.apitest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and reporting performance metrics.
 */
@Service
public class PerformanceMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetricsService.class);
    
    // Metrics for API test execution
    private final AtomicInteger totalTestsExecuted = new AtomicInteger(0);
    private final AtomicInteger successfulTests = new AtomicInteger(0);
    private final AtomicInteger failedTests = new AtomicInteger(0);
    private final AtomicLong totalExecutionTimeMs = new AtomicLong(0);
    private final AtomicLong maxExecutionTimeMs = new AtomicLong(0);
    
    // Metrics for HTTP client operations
    private final ConcurrentHashMap<String, AtomicInteger> requestsByMethod = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> executionTimeByMethod = new ConcurrentHashMap<>();
    
    // Metrics for caching
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    
    /**
     * Records metrics for an API test execution.
     * 
     * @param executionTimeMs The execution time in milliseconds
     * @param successful Whether the test was successful
     */
    public void recordTestExecution(long executionTimeMs, boolean successful) {
        totalTestsExecuted.incrementAndGet();
        totalExecutionTimeMs.addAndGet(executionTimeMs);
        
        // Update max execution time if this test took longer
        long currentMax = maxExecutionTimeMs.get();
        while (executionTimeMs > currentMax) {
            if (maxExecutionTimeMs.compareAndSet(currentMax, executionTimeMs)) {
                break;
            }
            currentMax = maxExecutionTimeMs.get();
        }
        
        if (successful) {
            successfulTests.incrementAndGet();
        } else {
            failedTests.incrementAndGet();
        }
        
        // Log metrics periodically
        if (totalTestsExecuted.get() % 100 == 0) {
            logCurrentMetrics();
        }
    }
    
    /**
     * Records metrics for an HTTP request.
     * 
     * @param method The HTTP method
     * @param executionTimeMs The execution time in milliseconds
     */
    public void recordHttpRequest(String method, long executionTimeMs) {
        requestsByMethod.computeIfAbsent(method, k -> new AtomicInteger(0)).incrementAndGet();
        executionTimeByMethod.computeIfAbsent(method, k -> new AtomicLong(0)).addAndGet(executionTimeMs);
    }
    
    /**
     * Records a cache hit.
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }
    
    /**
     * Records a cache miss.
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }
    
    /**
     * Gets the current performance metrics.
     * 
     * @return A map of metrics
     */
    public Map<String, Object> getCurrentMetrics() {
        int total = totalTestsExecuted.get();
        long totalTime = totalExecutionTimeMs.get();
        
        return Map.of(
                "totalTestsExecuted", total,
                "successfulTests", successfulTests.get(),
                "failedTests", failedTests.get(),
                "successRate", total > 0 ? (double) successfulTests.get() / total * 100 : 0,
                "averageExecutionTimeMs", total > 0 ? (double) totalTime / total : 0,
                "maxExecutionTimeMs", maxExecutionTimeMs.get(),
                "requestsByMethod", requestsByMethod,
                "cacheHits", cacheHits.get(),
                "cacheMisses", cacheMisses.get(),
                "cacheHitRate", (cacheHits.get() + cacheMisses.get() > 0) ? 
                        (double) cacheHits.get() / (cacheHits.get() + cacheMisses.get()) * 100 : 0
        );
    }
    
    /**
     * Resets all metrics.
     */
    public void resetMetrics() {
        totalTestsExecuted.set(0);
        successfulTests.set(0);
        failedTests.set(0);
        totalExecutionTimeMs.set(0);
        maxExecutionTimeMs.set(0);
        requestsByMethod.clear();
        executionTimeByMethod.clear();
        cacheHits.set(0);
        cacheMisses.set(0);
        
        logger.info("Performance metrics have been reset");
    }
    
    /**
     * Logs the current metrics.
     */
    private void logCurrentMetrics() {
        Map<String, Object> metrics = getCurrentMetrics();
        logger.info("Performance Metrics - Tests: {}, Success Rate: {}%, Avg Time: {}ms, Cache Hit Rate: {}%",
                metrics.get("totalTestsExecuted"),
                String.format("%.2f", metrics.get("successRate")),
                String.format("%.2f", metrics.get("averageExecutionTimeMs")),
                String.format("%.2f", metrics.get("cacheHitRate")));
    }
}

