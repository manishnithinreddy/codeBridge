package com.codebridge.monitoring.performance.service;

import com.codebridge.monitoring.performance.model.AlertRule;
import com.codebridge.monitoring.performance.model.AlertSeverity;
import com.codebridge.monitoring.performance.model.MetricType;
import com.codebridge.monitoring.performance.model.PerformanceMetric;
import com.codebridge.monitoring.performance.repository.PerformanceMetricRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for detecting anomalies in performance metrics.
 */
@Service
@Slf4j
public class AnomalyDetectionService {

    private final PerformanceMetricRepository metricRepository;
    private final AlertingService alertingService;
    private final TimeSeriesService timeSeriesService;
    
    @Value("${performance.anomaly-detection.enabled:true}")
    private boolean anomalyDetectionEnabled;
    
    @Value("${performance.anomaly-detection.sensitivity:2.0}")
    private double sensitivity;
    
    @Value("${performance.anomaly-detection.training-period-days:7}")
    private int trainingPeriodDays;
    
    @Value("${performance.anomaly-detection.min-data-points:100}")
    private int minDataPoints;

    @Autowired
    public AnomalyDetectionService(
            PerformanceMetricRepository metricRepository,
            AlertingService alertingService,
            TimeSeriesService timeSeriesService) {
        this.metricRepository = metricRepository;
        this.alertingService = alertingService;
        this.timeSeriesService = timeSeriesService;
    }

    /**
     * Scheduled task to detect anomalies.
     */
    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    public void detectAnomalies() {
        if (!anomalyDetectionEnabled) {
            return;
        }
        
        log.debug("Running anomaly detection");
        
        // Get distinct service and metric name combinations
        List<Object[]> serviceMetricPairs = metricRepository.findDistinctServiceAndMetricNames();
        
        for (Object[] pair : serviceMetricPairs) {
            String serviceName = (String) pair[0];
            String metricName = (String) pair[1];
            
            try {
                detectAnomaliesForMetric(serviceName, metricName);
            } catch (Exception e) {
                log.error("Error detecting anomalies for {}.{}: {}", 
                        serviceName, metricName, e.getMessage(), e);
            }
        }
    }
    
    /**
     * Detect anomalies for a specific metric.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     */
    private void detectAnomaliesForMetric(String serviceName, String metricName) {
        // Get historical data for training
        Instant now = Instant.now();
        Instant trainingStart = now.minusSeconds(trainingPeriodDays * 24 * 60 * 60);
        Instant evaluationStart = now.minusMinutes(15);
        
        List<PerformanceMetric> trainingData = metricRepository.findByServiceNameAndMetricNameAndTimestampBetween(
                serviceName, metricName, trainingStart, evaluationStart, MetricType.GAUGE);
        
        if (trainingData.size() < minDataPoints) {
            log.debug("Not enough data points for anomaly detection for {}.{}: {} (min: {})",
                    serviceName, metricName, trainingData.size(), minDataPoints);
            return;
        }
        
        // Calculate statistics from training data
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (PerformanceMetric metric : trainingData) {
            stats.addValue(metric.getValue());
        }
        
        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        double upperThreshold = mean + (sensitivity * stdDev);
        double lowerThreshold = mean - (sensitivity * stdDev);
        
        // Get recent data for evaluation
        List<PerformanceMetric> recentData = metricRepository.findByServiceNameAndMetricNameAndTimestampBetween(
                serviceName, metricName, evaluationStart, now, MetricType.GAUGE);
        
        // Check for anomalies
        List<PerformanceMetric> anomalies = new ArrayList<>();
        for (PerformanceMetric metric : recentData) {
            if (metric.getValue() > upperThreshold || metric.getValue() < lowerThreshold) {
                anomalies.add(metric);
            }
        }
        
        if (!anomalies.isEmpty()) {
            handleAnomalies(serviceName, metricName, anomalies, mean, stdDev, upperThreshold, lowerThreshold);
        }
    }
    
    /**
     * Handle detected anomalies.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param anomalies the list of anomalies
     * @param mean the mean value
     * @param stdDev the standard deviation
     * @param upperThreshold the upper threshold
     * @param lowerThreshold the lower threshold
     */
    private void handleAnomalies(String serviceName, String metricName, List<PerformanceMetric> anomalies,
                                double mean, double stdDev, double upperThreshold, double lowerThreshold) {
        
        log.info("Detected {} anomalies for {}.{}", anomalies.size(), serviceName, metricName);
        
        // Calculate average anomaly value
        double avgAnomalyValue = anomalies.stream()
                .mapToDouble(PerformanceMetric::getValue)
                .average()
                .orElse(0.0);
        
        // Determine if it's a high or low anomaly
        boolean isHighAnomaly = avgAnomalyValue > upperThreshold;
        
        // Create or update alert rule
        String ruleName = "Anomaly: " + serviceName + "." + metricName;
        String description = String.format(
                "Anomaly detection for %s.%s. Normal range: %.2f to %.2f (mean: %.2f, stdDev: %.2f)",
                serviceName, metricName, lowerThreshold, upperThreshold, mean, stdDev);
        
        // Check if rule already exists
        List<AlertRule> existingRules = alertingService.getAlertRulesByServiceAndMetric(serviceName, metricName);
        
        if (existingRules.isEmpty()) {
            // Create new rule
            AlertRule.Operator operator = isHighAnomaly ? 
                    AlertRule.Operator.GREATER_THAN : AlertRule.Operator.LESS_THAN;
            double threshold = isHighAnomaly ? upperThreshold : lowerThreshold;
            
            alertingService.createAlertRule(
                    serviceName,
                    metricName,
                    operator,
                    threshold,
                    15 * 60 * 1000, // 15 minutes
                    "avg",
                    AlertSeverity.WARNING,
                    ruleName,
                    description);
        }
        
        // Log anomaly details
        for (PerformanceMetric anomaly : anomalies) {
            log.info("Anomaly: {}.{} = {} at {} (normal range: {}-{})",
                    serviceName, metricName, anomaly.getValue(), anomaly.getTimestamp(),
                    lowerThreshold, upperThreshold);
        }
    }
    
    /**
     * Detect trend in a metric over time.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param startTime the start time
     * @param endTime the end time
     * @param intervals the number of intervals to divide the time range into
     * @return the trend analysis result
     */
    public Map<String, Object> detectTrend(String serviceName, String metricName, 
                                          Instant startTime, Instant endTime, int intervals) {
        if (!anomalyDetectionEnabled) {
            return Map.of("enabled", false);
        }
        
        // Calculate interval duration
        long intervalDuration = (endTime.toEpochMilli() - startTime.toEpochMilli()) / intervals;
        
        // Get data points for each interval
        List<Double> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        for (int i = 0; i < intervals; i++) {
            Instant intervalStart = startTime.plusMillis(i * intervalDuration);
            Instant intervalEnd = startTime.plusMillis((i + 1) * intervalDuration);
            
            double avgValue = timeSeriesService.queryAggregatedMetric(
                    serviceName, metricName, intervalStart, intervalEnd, "mean");
            
            values.add(avgValue);
            labels.add(intervalStart.toString());
        }
        
        // Calculate trend using linear regression
        double[] trend = calculateLinearRegression(values);
        
        // Determine trend direction and strength
        String direction = trend[0] > 0 ? "increasing" : (trend[0] < 0 ? "decreasing" : "stable");
        double strength = Math.abs(trend[0]);
        
        // Calculate prediction for next interval
        double nextValue = trend[0] * (intervals + 1) + trend[1];
        
        Map<String, Object> result = new HashMap<>();
        result.put("serviceName", serviceName);
        result.put("metricName", metricName);
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("intervals", intervals);
        result.put("values", values);
        result.put("labels", labels);
        result.put("slope", trend[0]);
        result.put("intercept", trend[1]);
        result.put("direction", direction);
        result.put("strength", strength);
        result.put("prediction", nextValue);
        
        return result;
    }
    
    /**
     * Calculate linear regression for a list of values.
     *
     * @param values the list of values
     * @return array containing [slope, intercept]
     */
    private double[] calculateLinearRegression(List<Double> values) {
        int n = values.size();
        
        if (n < 2) {
            return new double[] {0, 0};
        }
        
        // Create x values (0, 1, 2, ...)
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i;
        }
        
        // Calculate means
        double meanX = 0;
        double meanY = 0;
        
        for (int i = 0; i < n; i++) {
            meanX += x[i];
            meanY += values.get(i);
        }
        
        meanX /= n;
        meanY /= n;
        
        // Calculate slope and intercept
        double numerator = 0;
        double denominator = 0;
        
        for (int i = 0; i < n; i++) {
            numerator += (x[i] - meanX) * (values.get(i) - meanY);
            denominator += Math.pow(x[i] - meanX, 2);
        }
        
        double slope = denominator != 0 ? numerator / denominator : 0;
        double intercept = meanY - (slope * meanX);
        
        return new double[] {slope, intercept};
    }
    
    /**
     * Compare current metric value to baseline.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param currentStart the start time for current period
     * @param currentEnd the end time for current period
     * @param baselineStart the start time for baseline period
     * @param baselineEnd the end time for baseline period
     * @return the comparison result
     */
    public Map<String, Object> compareToBaseline(String serviceName, String metricName,
                                               Instant currentStart, Instant currentEnd,
                                               Instant baselineStart, Instant baselineEnd) {
        if (!anomalyDetectionEnabled) {
            return Map.of("enabled", false);
        }
        
        // Get current and baseline values
        double currentValue = timeSeriesService.queryAggregatedMetric(
                serviceName, metricName, currentStart, currentEnd, "mean");
        
        double baselineValue = timeSeriesService.queryAggregatedMetric(
                serviceName, metricName, baselineStart, baselineEnd, "mean");
        
        // Calculate difference and percentage change
        double difference = currentValue - baselineValue;
        double percentageChange = baselineValue != 0 ? 
                (difference / baselineValue) * 100 : 0;
        
        // Determine if the change is significant
        boolean isSignificant = Math.abs(percentageChange) > 10; // 10% threshold
        
        Map<String, Object> result = new HashMap<>();
        result.put("serviceName", serviceName);
        result.put("metricName", metricName);
        result.put("currentPeriod", Map.of("start", currentStart, "end", currentEnd));
        result.put("baselinePeriod", Map.of("start", baselineStart, "end", baselineEnd));
        result.put("currentValue", currentValue);
        result.put("baselineValue", baselineValue);
        result.put("difference", difference);
        result.put("percentageChange", percentageChange);
        result.put("isSignificant", isSignificant);
        
        return result;
    }
}

