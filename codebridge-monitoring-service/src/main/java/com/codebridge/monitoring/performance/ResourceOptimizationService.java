package com.codebridge.monitoring.performance.service;

import com.codebridge.monitoring.performance.model.MetricType;
import com.codebridge.monitoring.performance.model.OptimizationRecommendation;
import com.codebridge.monitoring.performance.model.OptimizationRecommendationType;
import com.codebridge.monitoring.performance.model.PerformanceMetric;
import com.codebridge.monitoring.performance.repository.OptimizationRecommendationRepository;
import com.codebridge.monitoring.performance.repository.PerformanceMetricRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for resource optimization and performance bottleneck identification.
 */
@Service
@Slf4j
public class ResourceOptimizationService {

    private final PerformanceMetricRepository metricRepository;
    private final OptimizationRecommendationRepository recommendationRepository;
    private final TimeSeriesService timeSeriesService;
    
    @Value("${performance.optimization.enabled:true}")
    private boolean optimizationEnabled;
    
    @Value("${performance.optimization.analysis-period-hours:24}")
    private int analysisPeriodHours;
    
    @Value("${performance.optimization.cpu-threshold:80}")
    private double cpuThreshold;
    
    @Value("${performance.optimization.memory-threshold:80}")
    private double memoryThreshold;
    
    @Value("${performance.optimization.response-time-threshold:500}")
    private double responseTimeThreshold;
    
    @Value("${performance.optimization.error-rate-threshold:5}")
    private double errorRateThreshold;

    @Autowired
    public ResourceOptimizationService(
            PerformanceMetricRepository metricRepository,
            OptimizationRecommendationRepository recommendationRepository,
            TimeSeriesService timeSeriesService) {
        this.metricRepository = metricRepository;
        this.recommendationRepository = recommendationRepository;
        this.timeSeriesService = timeSeriesService;
    }

    /**
     * Scheduled task to analyze resource usage and generate optimization recommendations.
     */
    @Scheduled(cron = "0 0 */6 * * *") // Every 6 hours
    public void analyzeResourceUsage() {
        if (!optimizationEnabled) {
            return;
        }
        
        log.info("Analyzing resource usage for optimization recommendations");
        
        try {
            // Analyze CPU usage
            analyzeCpuUsage();
            
            // Analyze memory usage
            analyzeMemoryUsage();
            
            // Analyze response times
            analyzeResponseTimes();
            
            // Analyze error rates
            analyzeErrorRates();
            
            // Analyze database queries
            analyzeDatabaseQueries();
            
            // Analyze caching opportunities
            analyzeCachingOpportunities();
        } catch (Exception e) {
            log.error("Error analyzing resource usage: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Analyze CPU usage.
     */
    private void analyzeCpuUsage() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(analysisPeriodHours * 3600);
        
        // Get CPU usage metrics
        double avgCpuUsage = timeSeriesService.queryAggregatedMetric(
                "resource-metrics", "cpu.system.load", startTime, endTime, "mean");
        
        double maxCpuUsage = timeSeriesService.queryAggregatedMetric(
                "resource-metrics", "cpu.system.load", startTime, endTime, "max");
        
        // Check if CPU usage exceeds threshold
        if (avgCpuUsage > cpuThreshold || maxCpuUsage > cpuThreshold + 10) {
            // Create recommendation for CPU optimization
            createRecommendation(
                    OptimizationRecommendationType.CPU_SCALING,
                    "High CPU usage detected",
                    String.format("Average CPU usage: %.2f%%, Max CPU usage: %.2f%%", avgCpuUsage, maxCpuUsage),
                    Map.of(
                            "avgCpuUsage", avgCpuUsage,
                            "maxCpuUsage", maxCpuUsage,
                            "threshold", cpuThreshold
                    ),
                    avgCpuUsage > 90 || maxCpuUsage > 95 ? 
                            OptimizationRecommendation.Priority.HIGH : 
                            OptimizationRecommendation.Priority.MEDIUM
            );
        }
    }
    
    /**
     * Analyze memory usage.
     */
    private void analyzeMemoryUsage() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(analysisPeriodHours * 3600);
        
        // Get memory usage metrics
        double avgHeapUsage = timeSeriesService.queryAggregatedMetric(
                "resource-metrics", "memory.heap.utilization", startTime, endTime, "mean");
        
        double maxHeapUsage = timeSeriesService.queryAggregatedMetric(
                "resource-metrics", "memory.heap.utilization", startTime, endTime, "max");
        
        // Check if memory usage exceeds threshold
        if (avgHeapUsage > memoryThreshold || maxHeapUsage > memoryThreshold + 10) {
            // Create recommendation for memory optimization
            createRecommendation(
                    OptimizationRecommendationType.MEMORY_OPTIMIZATION,
                    "High memory usage detected",
                    String.format("Average heap usage: %.2f%%, Max heap usage: %.2f%%", avgHeapUsage, maxHeapUsage),
                    Map.of(
                            "avgHeapUsage", avgHeapUsage,
                            "maxHeapUsage", maxHeapUsage,
                            "threshold", memoryThreshold
                    ),
                    avgHeapUsage > 90 || maxHeapUsage > 95 ? 
                            OptimizationRecommendation.Priority.HIGH : 
                            OptimizationRecommendation.Priority.MEDIUM
            );
        }
    }
    
    /**
     * Analyze response times.
     */
    private void analyzeResponseTimes() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(analysisPeriodHours * 3600);
        
        // Get API response time metrics
        List<Object[]> slowEndpoints = metricRepository.findSlowEndpoints(
                "api-service", "response.time", responseTimeThreshold, startTime, endTime);
        
        for (Object[] endpoint : slowEndpoints) {
            String endpointPath = (String) endpoint[0];
            Double avgResponseTime = (Double) endpoint[1];
            Long requestCount = (Long) endpoint[2];
            
            // Create recommendation for slow endpoint optimization
            createRecommendation(
                    OptimizationRecommendationType.API_OPTIMIZATION,
                    "Slow API endpoint detected: " + endpointPath,
                    String.format("Average response time: %.2f ms, Request count: %d", 
                            avgResponseTime, requestCount),
                    Map.of(
                            "endpoint", endpointPath,
                            "avgResponseTime", avgResponseTime,
                            "requestCount", requestCount,
                            "threshold", responseTimeThreshold
                    ),
                    avgResponseTime > responseTimeThreshold * 2 ? 
                            OptimizationRecommendation.Priority.HIGH : 
                            OptimizationRecommendation.Priority.MEDIUM
            );
        }
    }
    
    /**
     * Analyze error rates.
     */
    private void analyzeErrorRates() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(analysisPeriodHours * 3600);
        
        // Get API error rate metrics
        List<Object[]> highErrorEndpoints = metricRepository.findHighErrorRateEndpoints(
                "api-service", "error.rate", errorRateThreshold, startTime, endTime);
        
        for (Object[] endpoint : highErrorEndpoints) {
            String endpointPath = (String) endpoint[0];
            Double errorRate = (Double) endpoint[1];
            Long requestCount = (Long) endpoint[2];
            
            // Create recommendation for high error rate optimization
            createRecommendation(
                    OptimizationRecommendationType.ERROR_HANDLING,
                    "High error rate detected: " + endpointPath,
                    String.format("Error rate: %.2f%%, Request count: %d", 
                            errorRate, requestCount),
                    Map.of(
                            "endpoint", endpointPath,
                            "errorRate", errorRate,
                            "requestCount", requestCount,
                            "threshold", errorRateThreshold
                    ),
                    errorRate > errorRateThreshold * 2 ? 
                            OptimizationRecommendation.Priority.HIGH : 
                            OptimizationRecommendation.Priority.MEDIUM
            );
        }
    }
    
    /**
     * Analyze database queries.
     */
    private void analyzeDatabaseQueries() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(analysisPeriodHours * 3600);
        
        // Get slow query metrics
        List<PerformanceMetric> slowQueries = metricRepository.findByServiceNameAndMetricNameAndMetricTypeAndTimestampBetween(
                "database-service", "query.slow", MetricType.COUNTER, startTime, endTime);
        
        Map<String, Integer> slowQueryCounts = new HashMap<>();
        Map<String, String> slowQueryTypes = new HashMap<>();
        
        for (PerformanceMetric metric : slowQueries) {
            String databaseType = metric.getTags().get("database_type");
            String queryType = metric.getTags().get("query_type");
            String key = databaseType + ":" + queryType;
            
            slowQueryCounts.put(key, slowQueryCounts.getOrDefault(key, 0) + 1);
            slowQueryTypes.put(key, queryType);
        }
        
        // Create recommendations for slow queries
        for (Map.Entry<String, Integer> entry : slowQueryCounts.entrySet()) {
            String key = entry.getKey();
            int count = entry.getValue();
            String[] parts = key.split(":");
            String databaseType = parts[0];
            String queryType = slowQueryTypes.get(key);
            
            if (count > 10) { // More than 10 slow queries of the same type
                createRecommendation(
                        OptimizationRecommendationType.DATABASE_OPTIMIZATION,
                        "Slow database queries detected: " + databaseType + " " + queryType,
                        String.format("Detected %d slow %s queries on %s", 
                                count, queryType, databaseType),
                        Map.of(
                                "databaseType", databaseType,
                                "queryType", queryType,
                                "count", count
                        ),
                        count > 50 ? 
                                OptimizationRecommendation.Priority.HIGH : 
                                OptimizationRecommendation.Priority.MEDIUM
                );
            }
        }
    }
    
    /**
     * Analyze caching opportunities.
     */
    private void analyzeCachingOpportunities() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(analysisPeriodHours * 3600);
        
        // Get API request count metrics
        List<Object[]> frequentEndpoints = metricRepository.findFrequentEndpoints(
                "api-service", "request.count", 100, startTime, endTime);
        
        for (Object[] endpoint : frequentEndpoints) {
            String endpointPath = (String) endpoint[0];
            Long requestCount = (Long) endpoint[1];
            
            // Check if the endpoint is cacheable (GET requests that don't contain dynamic path parameters)
            if (isCacheable(endpointPath)) {
                // Create recommendation for caching
                createRecommendation(
                        OptimizationRecommendationType.CACHING,
                        "Caching opportunity detected: " + endpointPath,
                        String.format("Frequent endpoint with %d requests", requestCount),
                        Map.of(
                                "endpoint", endpointPath,
                                "requestCount", requestCount
                        ),
                        requestCount > 1000 ? 
                                OptimizationRecommendation.Priority.HIGH : 
                                OptimizationRecommendation.Priority.MEDIUM
                );
            }
        }
    }
    
    /**
     * Check if an endpoint is cacheable.
     *
     * @param endpoint the endpoint path
     * @return true if the endpoint is cacheable, false otherwise
     */
    private boolean isCacheable(String endpoint) {
        // Simple heuristic: GET endpoints that don't contain dynamic path parameters
        // In a real implementation, this would be more sophisticated
        return endpoint.startsWith("/api/") && 
               !endpoint.contains("{") && 
               !endpoint.contains("}") &&
               (endpoint.contains("/get") || 
                endpoint.contains("/list") || 
                endpoint.contains("/search") || 
                endpoint.contains("/find"));
    }
    
    /**
     * Create an optimization recommendation.
     *
     * @param type the recommendation type
     * @param title the recommendation title
     * @param description the recommendation description
     * @param metadata the recommendation metadata
     * @param priority the recommendation priority
     * @return the created recommendation
     */
    private OptimizationRecommendation createRecommendation(
            OptimizationRecommendationType type, String title, String description, 
            Map<String, Object> metadata, OptimizationRecommendation.Priority priority) {
        
        // Check if a similar recommendation already exists
        List<OptimizationRecommendation> existingRecommendations = 
                recommendationRepository.findByTypeAndTitleAndStatus(
                        type, title, OptimizationRecommendation.Status.OPEN);
        
        if (!existingRecommendations.isEmpty()) {
            // Update existing recommendation
            OptimizationRecommendation existing = existingRecommendations.get(0);
            existing.setDescription(description);
            existing.setMetadata(metadata);
            existing.setPriority(priority);
            existing.setLastUpdatedAt(Instant.now());
            
            log.info("Updated existing optimization recommendation: {}", title);
            
            return recommendationRepository.save(existing);
        } else {
            // Create new recommendation
            OptimizationRecommendation recommendation = new OptimizationRecommendation();
            recommendation.setType(type);
            recommendation.setTitle(title);
            recommendation.setDescription(description);
            recommendation.setMetadata(metadata);
            recommendation.setPriority(priority);
            recommendation.setStatus(OptimizationRecommendation.Status.OPEN);
            recommendation.setCreatedAt(Instant.now());
            recommendation.setLastUpdatedAt(Instant.now());
            
            log.info("Created new optimization recommendation: {}", title);
            
            return recommendationRepository.save(recommendation);
        }
    }
    
    /**
     * Get all optimization recommendations.
     *
     * @return the list of recommendations
     */
    public List<OptimizationRecommendation> getAllRecommendations() {
        return recommendationRepository.findAll();
    }
    
    /**
     * Get optimization recommendations by status.
     *
     * @param status the recommendation status
     * @return the list of recommendations
     */
    public List<OptimizationRecommendation> getRecommendationsByStatus(OptimizationRecommendation.Status status) {
        return recommendationRepository.findByStatus(status);
    }
    
    /**
     * Get optimization recommendations by type.
     *
     * @param type the recommendation type
     * @return the list of recommendations
     */
    public List<OptimizationRecommendation> getRecommendationsByType(OptimizationRecommendationType type) {
        return recommendationRepository.findByType(type);
    }
    
    /**
     * Get optimization recommendations by priority.
     *
     * @param priority the recommendation priority
     * @return the list of recommendations
     */
    public List<OptimizationRecommendation> getRecommendationsByPriority(OptimizationRecommendation.Priority priority) {
        return recommendationRepository.findByPriority(priority);
    }
    
    /**
     * Get an optimization recommendation by ID.
     *
     * @param id the recommendation ID
     * @return the recommendation
     */
    public OptimizationRecommendation getRecommendationById(UUID id) {
        return recommendationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found: " + id));
    }
    
    /**
     * Update the status of an optimization recommendation.
     *
     * @param id the recommendation ID
     * @param status the new status
     * @param comment the status change comment
     * @return the updated recommendation
     */
    public OptimizationRecommendation updateRecommendationStatus(
            UUID id, OptimizationRecommendation.Status status, String comment) {
        
        OptimizationRecommendation recommendation = getRecommendationById(id);
        recommendation.setStatus(status);
        recommendation.setStatusComment(comment);
        recommendation.setLastUpdatedAt(Instant.now());
        
        if (status == OptimizationRecommendation.Status.IMPLEMENTED) {
            recommendation.setImplementedAt(Instant.now());
        }
        
        return recommendationRepository.save(recommendation);
    }
    
    /**
     * Identify performance bottlenecks.
     *
     * @return the list of bottlenecks
     */
    public List<Map<String, Object>> identifyBottlenecks() {
        if (!optimizationEnabled) {
            return Collections.emptyList();
        }
        
        List<Map<String, Object>> bottlenecks = new ArrayList<>();
        
        try {
            // Identify CPU bottlenecks
            identifyCpuBottlenecks(bottlenecks);
            
            // Identify memory bottlenecks
            identifyMemoryBottlenecks(bottlenecks);
            
            // Identify network bottlenecks
            identifyNetworkBottlenecks(bottlenecks);
            
            // Identify database bottlenecks
            identifyDatabaseBottlenecks(bottlenecks);
            
            // Identify API bottlenecks
            identifyApiBottlenecks(bottlenecks);
        } catch (Exception e) {
            log.error("Error identifying bottlenecks: {}", e.getMessage(), e);
        }
        
        return bottlenecks;
    }
    
    /**
     * Identify CPU bottlenecks.
     *
     * @param bottlenecks the list of bottlenecks
     */
    private void identifyCpuBottlenecks(List<Map<String, Object>> bottlenecks) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(analysisPeriodHours * 3600);
        
        double avgCpuUsage = timeSeriesService.queryAggregatedMetric(
                "resource-metrics", "cpu.system.load", startTime, endTime, "mean");
        
        double maxCpuUsage = timeSeriesService.queryAggregatedMetric(
                "resource-metrics", "cpu.system.load", startTime, endTime, "max");
        
        if (avgCpuUsage > cpuThreshold || maxCpuUsage > cpuThreshold + 10) {
            Map<String, Object> bottleneck = new HashMap<>();
            bottleneck.put("type", "CPU");
            bottleneck.put("avgUsage", avgCpuUsage);
            bottleneck.put("maxUsage", maxCpuUsage);
            bottleneck.put("threshold", cpuThreshold);
            bottleneck.put("severity", avgCpuUsage > 90 ? "HIGH" : "MEDIUM");
            bottleneck.put("description", String.format(
                    "CPU usage is high (avg: %.2f%%, max: %.2f%%)", avgCpuUsage, maxCpuUsage));
            
            bottlenecks.add(bottleneck);
        }
    }
    
    /**
     * Identify memory bottlenecks.
     *
     * @param bottlenecks the list of bottlenecks
     */
    private void identifyMemoryBottlenecks(List<Map<String, Object>> bottlenecks) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(analysisPeriodHours * 3600);
        
        double avgHeapUsage = timeSeriesService.queryAggregatedMetric(
                "resource-metrics", "memory.heap.utilization", startTime, endTime, "mean");
        
        double maxHeapUsage = timeSeriesService.queryAggregatedMetric(
                "resource-metrics", "memory.heap.utilization", startTime, endTime, "max");
        
        if (avgHeapUsage > memoryThreshold || maxHeapUsage > memoryThreshold + 10) {
            Map<String, Object> bottleneck = new HashMap<>();
            bottleneck.put("type", "Memory");
            bottleneck.put("avgUsage", avgHeapUsage);
            bottleneck.put("maxUsage", maxHeapUsage);
            bottleneck.put("threshold", memoryThreshold);
            bottleneck.put("severity", avgHeapUsage > 90 ? "HIGH" : "MEDIUM");
            bottleneck.put("description", String.format(
                    "Memory usage is high (avg: %.2f%%, max: %.2f%%)", avgHeapUsage, maxHeapUsage));
            
            bottlenecks.add(bottleneck);
        }
    }
    
    /**
     * Identify network bottlenecks.
     *
     * @param bottlenecks the list of bottlenecks
     */
    private void identifyNetworkBottlenecks(List<Map<String, Object>> bottlenecks) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(analysisPeriodHours * 3600);
        
        // Check for high network latency
        double avgNetworkLatency = timeSeriesService.queryAggregatedMetric(
                "client-metrics", "network.latency", startTime, endTime, "mean");
        
        if (avgNetworkLatency > 100) { // 100ms threshold
            Map<String, Object> bottleneck = new HashMap<>();
            bottleneck.put("type", "Network");
            bottleneck.put("avgLatency", avgNetworkLatency);
            bottleneck.put("threshold", 100);
            bottleneck.put("severity", avgNetworkLatency > 200 ? "HIGH" : "MEDIUM");
            bottleneck.put("description", String.format(
                    "Network latency is high (avg: %.2f ms)", avgNetworkLatency));
            
            bottlenecks.add(bottleneck);
        }
    }
    
    /**
     * Identify database bottlenecks.
     *
     * @param bottlenecks the list of bottlenecks
     */
    private void identifyDatabaseBottlenecks(List<Map<String, Object>> bottlenecks) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(analysisPeriodHours * 3600);
        
        // Check for slow database queries
        List<Object[]> slowQueryTypes = metricRepository.findSlowQueryTypes(
                "database-service", "query.slow", startTime, endTime);
        
        for (Object[] queryType : slowQueryTypes) {
            String dbType = (String) queryType[0];
            String type = (String) queryType[1];
            Long count = (Long) queryType[2];
            
            if (count > 10) {
                Map<String, Object> bottleneck = new HashMap<>();
                bottleneck.put("type", "Database");
                bottleneck.put("databaseType", dbType);
                bottleneck.put("queryType", type);
                bottleneck.put("count", count);
                bottleneck.put("severity", count > 50 ? "HIGH" : "MEDIUM");
                bottleneck.put("description", String.format(
                        "Slow %s queries on %s database (%d occurrences)", type, dbType, count));
                
                bottlenecks.add(bottleneck);
            }
        }
    }
    
    /**
     * Identify API bottlenecks.
     *
     * @param bottlenecks the list of bottlenecks
     */
    private void identifyApiBottlenecks(List<Map<String, Object>> bottlenecks) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(analysisPeriodHours * 3600);
        
        // Check for slow API endpoints
        List<Object[]> slowEndpoints = metricRepository.findSlowEndpoints(
                "api-service", "response.time", responseTimeThreshold, startTime, endTime);
        
        for (Object[] endpoint : slowEndpoints) {
            String endpointPath = (String) endpoint[0];
            Double avgResponseTime = (Double) endpoint[1];
            Long requestCount = (Long) endpoint[2];
            
            Map<String, Object> bottleneck = new HashMap<>();
            bottleneck.put("type", "API");
            bottleneck.put("endpoint", endpointPath);
            bottleneck.put("avgResponseTime", avgResponseTime);
            bottleneck.put("requestCount", requestCount);
            bottleneck.put("threshold", responseTimeThreshold);
            bottleneck.put("severity", avgResponseTime > responseTimeThreshold * 2 ? "HIGH" : "MEDIUM");
            bottleneck.put("description", String.format(
                    "Slow API endpoint: %s (avg: %.2f ms, count: %d)", 
                    endpointPath, avgResponseTime, requestCount));
            
            bottlenecks.add(bottleneck);
        }
    }
}
