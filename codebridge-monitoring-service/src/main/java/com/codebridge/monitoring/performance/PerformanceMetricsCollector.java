package com.codebridge.monitoring.performance.collector;

import com.codebridge.monitoring.performance.model.MetricType;
import com.codebridge.monitoring.performance.model.PerformanceMetric;
import com.codebridge.monitoring.performance.repository.PerformanceMetricRepository;
import com.codebridge.monitoring.performance.service.TimeSeriesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Core component for collecting, aggregating, and storing performance metrics.
 * This collector manages metrics from various sources and persists them to both
 * a relational database and a time-series database for efficient querying.
 */
@Component
@Slf4j
public class PerformanceMetricsCollector {

    private final PerformanceMetricRepository metricRepository;
    private final TimeSeriesService timeSeriesService;
    private final ConcurrentMap<String, List<PerformanceMetric>> metricBuffer;
    
    @Value("${performance.metrics.collection.enabled:true}")
    private boolean metricsCollectionEnabled;
    
    @Value("${performance.metrics.collection.buffer-size:1000}")
    private int bufferSize;

    @Autowired
    public PerformanceMetricsCollector(
            PerformanceMetricRepository metricRepository,
            TimeSeriesService timeSeriesService) {
        this.metricRepository = metricRepository;
        this.timeSeriesService = timeSeriesService;
        this.metricBuffer = new ConcurrentHashMap<>();
    }

    /**
     * Record a new metric value.
     *
     * @param serviceName the name of the service reporting the metric
     * @param metricName the name of the metric
     * @param metricType the type of metric (COUNTER, GAUGE, TIMER, etc.)
     * @param value the metric value
     * @param tags additional tags/dimensions for the metric
     */
    public void recordMetric(String serviceName, String metricName, MetricType metricType, 
                             double value, Map<String, String> tags) {
        if (!metricsCollectionEnabled) {
            return;
        }
        
        String metricKey = buildMetricKey(serviceName, metricName);
        PerformanceMetric metric = new PerformanceMetric();
        metric.setServiceName(serviceName);
        metric.setMetricName(metricName);
        metric.setMetricType(metricType);
        metric.setValue(value);
        metric.setTimestamp(Instant.now());
        metric.setTags(tags != null ? tags : new HashMap<>());
        
        // Add to buffer
        metricBuffer.computeIfAbsent(metricKey, k -> new ArrayList<>()).add(metric);
        
        // If buffer reaches threshold, flush immediately
        if (metricBuffer.get(metricKey).size() >= bufferSize) {
            flushMetrics(metricKey);
        }
    }
    
    /**
     * Record a timer metric (duration in milliseconds).
     *
     * @param serviceName the name of the service reporting the metric
     * @param metricName the name of the metric
     * @param durationMs the duration in milliseconds
     * @param tags additional tags/dimensions for the metric
     */
    public void recordTimer(String serviceName, String metricName, long durationMs, Map<String, String> tags) {
        recordMetric(serviceName, metricName, MetricType.TIMER, durationMs, tags);
    }
    
    /**
     * Record a counter metric (increment by 1).
     *
     * @param serviceName the name of the service reporting the metric
     * @param metricName the name of the metric
     * @param tags additional tags/dimensions for the metric
     */
    public void incrementCounter(String serviceName, String metricName, Map<String, String> tags) {
        recordMetric(serviceName, metricName, MetricType.COUNTER, 1.0, tags);
    }
    
    /**
     * Record a gauge metric (current value).
     *
     * @param serviceName the name of the service reporting the metric
     * @param metricName the name of the metric
     * @param value the current value
     * @param tags additional tags/dimensions for the metric
     */
    public void recordGauge(String serviceName, String metricName, double value, Map<String, String> tags) {
        recordMetric(serviceName, metricName, MetricType.GAUGE, value, tags);
    }

    /**
     * Scheduled task to flush all metrics to storage.
     */
    @Scheduled(fixedDelayString = "${performance.metrics.collection.interval:15000}")
    public void flushAllMetrics() {
        if (!metricsCollectionEnabled) {
            return;
        }
        
        log.debug("Flushing all metrics to storage");
        List<String> metricKeys = new ArrayList<>(metricBuffer.keySet());
        for (String metricKey : metricKeys) {
            flushMetrics(metricKey);
        }
    }
    
    /**
     * Flush metrics for a specific key to storage.
     *
     * @param metricKey the metric key
     */
    private synchronized void flushMetrics(String metricKey) {
        List<PerformanceMetric> metrics = metricBuffer.get(metricKey);
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        
        try {
            // Create a copy of the metrics to flush
            List<PerformanceMetric> metricsToFlush = new ArrayList<>(metrics);
            
            // Clear the buffer
            metrics.clear();
            
            // Store in relational database
            metricRepository.saveAll(metricsToFlush);
            
            // Store in time-series database
            timeSeriesService.storeMetrics(metricsToFlush);
            
            log.debug("Flushed {} metrics for key {}", metricsToFlush.size(), metricKey);
        } catch (Exception e) {
            log.error("Error flushing metrics for key {}: {}", metricKey, e.getMessage(), e);
        }
    }
    
    /**
     * Aggregate metrics by a specific time window.
     *
     * @param serviceName the name of the service
     * @param metricName the name of the metric
     * @param startTime the start time
     * @param endTime the end time
     * @param aggregation the aggregation function (avg, min, max, sum, count)
     * @return the aggregated metric value
     */
    public double aggregateMetrics(String serviceName, String metricName, 
                                  Instant startTime, Instant endTime, String aggregation) {
        return timeSeriesService.queryAggregatedMetric(serviceName, metricName, startTime, endTime, aggregation);
    }
    
    /**
     * Build a unique key for a metric.
     *
     * @param serviceName the name of the service
     * @param metricName the name of the metric
     * @return the metric key
     */
    private String buildMetricKey(String serviceName, String metricName) {
        return serviceName + ":" + metricName;
    }
}

