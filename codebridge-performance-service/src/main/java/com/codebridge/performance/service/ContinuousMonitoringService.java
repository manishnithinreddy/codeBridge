package com.codebridge.performance.service;

import com.codebridge.performance.model.PerformanceMetric;
import com.codebridge.performance.model.PerformanceSla;
import com.codebridge.performance.model.PerformanceSlaViolation;
import com.codebridge.performance.repository.PerformanceMetricRepository;
import com.codebridge.performance.repository.PerformanceSlaRepository;
import com.codebridge.performance.repository.PerformanceSlaViolationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for continuous performance monitoring.
 */
@Service
@Slf4j
public class ContinuousMonitoringService {

    private final PerformanceMetricRepository metricRepository;
    private final PerformanceSlaRepository slaRepository;
    private final PerformanceSlaViolationRepository slaViolationRepository;
    private final TimeSeriesService timeSeriesService;
    private final AlertingService alertingService;
    
    @Value("${performance.monitoring.enabled:true}")
    private boolean monitoringEnabled;
    
    @Value("${performance.monitoring.sla-check-interval:300000}")
    private long slaCheckInterval;
    
    @Value("${performance.monitoring.degradation-threshold:20}")
    private double degradationThreshold;

    @Autowired
    public ContinuousMonitoringService(
            PerformanceMetricRepository metricRepository,
            PerformanceSlaRepository slaRepository,
            PerformanceSlaViolationRepository slaViolationRepository,
            TimeSeriesService timeSeriesService,
            AlertingService alertingService) {
        this.metricRepository = metricRepository;
        this.slaRepository = slaRepository;
        this.slaViolationRepository = slaViolationRepository;
        this.timeSeriesService = timeSeriesService;
        this.alertingService = alertingService;
    }

    /**
     * Scheduled task to check SLA compliance.
     */
    @Scheduled(fixedDelayString = "${performance.monitoring.sla-check-interval:300000}")
    public void checkSlaCompliance() {
        if (!monitoringEnabled) {
            return;
        }
        
        log.debug("Checking SLA compliance");
        
        List<PerformanceSla> activeSlas = slaRepository.findByEnabled(true);
        
        for (PerformanceSla sla : activeSlas) {
            try {
                checkSla(sla);
            } catch (Exception e) {
                log.error("Error checking SLA {}: {}", sla.getId(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Check a specific SLA.
     *
     * @param sla the SLA to check
     */
    private void checkSla(PerformanceSla sla) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusMillis(sla.getEvaluationPeriod());
        
        // Get the current value for the metric
        double currentValue = timeSeriesService.queryAggregatedMetric(
                sla.getServiceName(), sla.getMetricName(), startTime, endTime, sla.getAggregation());
        
        // Check if the SLA is violated
        boolean slaViolated = isSlaViolated(sla, currentValue);
        
        if (slaViolated) {
            // Create SLA violation
            createSlaViolation(sla, currentValue);
        }
    }
    
    /**
     * Check if an SLA is violated.
     *
     * @param sla the SLA
     * @param currentValue the current metric value
     * @return true if the SLA is violated, false otherwise
     */
    private boolean isSlaViolated(PerformanceSla sla, double currentValue) {
        switch (sla.getOperator()) {
            case GREATER_THAN:
                return currentValue > sla.getThreshold();
            case GREATER_THAN_OR_EQUAL:
                return currentValue >= sla.getThreshold();
            case LESS_THAN:
                return currentValue < sla.getThreshold();
            case LESS_THAN_OR_EQUAL:
                return currentValue <= sla.getThreshold();
            case EQUAL:
                return Math.abs(currentValue - sla.getThreshold()) < 0.0001;
            case NOT_EQUAL:
                return Math.abs(currentValue - sla.getThreshold()) >= 0.0001;
            default:
                return false;
        }
    }
    
    /**
     * Create an SLA violation.
     *
     * @param sla the SLA
     * @param currentValue the current metric value
     * @return the created SLA violation
     */
    private PerformanceSlaViolation createSlaViolation(PerformanceSla sla, double currentValue) {
        PerformanceSlaViolation violation = new PerformanceSlaViolation();
        violation.setSla(sla);
        violation.setCurrentValue(currentValue);
        violation.setThreshold(sla.getThreshold());
        violation.setViolationTime(Instant.now());
        violation.setMessage(generateSlaViolationMessage(sla, currentValue));
        
        PerformanceSlaViolation savedViolation = slaViolationRepository.save(violation);
        
        log.warn("SLA violation: {}", savedViolation.getMessage());
        
        return savedViolation;
    }
    
    /**
     * Generate an SLA violation message.
     *
     * @param sla the SLA
     * @param currentValue the current metric value
     * @return the violation message
     */
    private String generateSlaViolationMessage(PerformanceSla sla, double currentValue) {
        return String.format("SLA Violation: %s - %s.%s %s %.2f (threshold: %s %.2f)",
                sla.getName(),
                sla.getServiceName(),
                sla.getMetricName(),
                getOperatorSymbol(sla.getOperator()),
                currentValue,
                getOperatorSymbol(sla.getOperator()),
                sla.getThreshold());
    }
    
    /**
     * Get the symbol for an operator.
     *
     * @param operator the operator
     * @return the operator symbol
     */
    private String getOperatorSymbol(PerformanceSla.Operator operator) {
        switch (operator) {
            case GREATER_THAN:
                return ">";
            case GREATER_THAN_OR_EQUAL:
                return ">=";
            case LESS_THAN:
                return "<";
            case LESS_THAN_OR_EQUAL:
                return "<=";
            case EQUAL:
                return "=";
            case NOT_EQUAL:
                return "!=";
            default:
                return "";
        }
    }
    
    /**
     * Scheduled task to detect performance degradation.
     */
    @Scheduled(cron = "0 */30 * * * *") // Every 30 minutes
    public void detectPerformanceDegradation() {
        if (!monitoringEnabled) {
            return;
        }
        
        log.debug("Detecting performance degradation");
        
        // Get distinct service and metric name combinations
        List<Object[]> serviceMetricPairs = metricRepository.findDistinctServiceAndMetricNames();
        
        for (Object[] pair : serviceMetricPairs) {
            String serviceName = (String) pair[0];
            String metricName = (String) pair[1];
            
            try {
                detectDegradation(serviceName, metricName);
            } catch (Exception e) {
                log.error("Error detecting degradation for {}.{}: {}", 
                        serviceName, metricName, e.getMessage(), e);
            }
        }
    }
    
    /**
     * Detect performance degradation for a specific metric.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     */
    private void detectDegradation(String serviceName, String metricName) {
        Instant now = Instant.now();
        
        // Current period: last hour
        Instant currentStart = now.minusSeconds(3600);
        Instant currentEnd = now;
        
        // Baseline period: same hour yesterday
        Instant baselineStart = currentStart.minusSeconds(86400);
        Instant baselineEnd = currentEnd.minusSeconds(86400);
        
        // Get current and baseline values
        double currentValue = timeSeriesService.queryAggregatedMetric(
                serviceName, metricName, currentStart, currentEnd, "mean");
        
        double baselineValue = timeSeriesService.queryAggregatedMetric(
                serviceName, metricName, baselineStart, baselineEnd, "mean");
        
        // Skip if baseline value is too small
        if (Math.abs(baselineValue) < 0.0001) {
            return;
        }
        
        // Calculate percentage change
        double percentageChange = ((currentValue - baselineValue) / baselineValue) * 100;
        
        // Check for significant degradation
        if (isSignificantDegradation(metricName, percentageChange)) {
            // Create alert for degradation
            createDegradationAlert(serviceName, metricName, currentValue, baselineValue, percentageChange);
        }
    }
    
    /**
     * Check if a percentage change represents significant degradation.
     *
     * @param metricName the metric name
     * @param percentageChange the percentage change
     * @return true if the change is significant degradation, false otherwise
     */
    private boolean isSignificantDegradation(String metricName, double percentageChange) {
        // For metrics where higher is worse (response time, error rate, etc.)
        if (isHigherWorse(metricName)) {
            return percentageChange > degradationThreshold;
        }
        
        // For metrics where lower is worse (throughput, success rate, etc.)
        return percentageChange < -degradationThreshold;
    }
    
    /**
     * Check if higher values are worse for a metric.
     *
     * @param metricName the metric name
     * @return true if higher values are worse, false otherwise
     */
    private boolean isHigherWorse(String metricName) {
        return metricName.contains("response.time") ||
               metricName.contains("error.rate") ||
               metricName.contains("latency") ||
               metricName.contains("cpu.") ||
               metricName.contains("memory.") ||
               metricName.contains("disk.") ||
               metricName.contains("failure");
    }
    
    /**
     * Create an alert for performance degradation.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param currentValue the current value
     * @param baselineValue the baseline value
     * @param percentageChange the percentage change
     */
    private void createDegradationAlert(String serviceName, String metricName, 
                                       double currentValue, double baselineValue, double percentageChange) {
        String alertName = "Performance Degradation: " + serviceName + "." + metricName;
        String description = String.format(
                "Performance degradation detected for %s.%s. Current: %.2f, Baseline: %.2f, Change: %.2f%%",
                serviceName, metricName, currentValue, baselineValue, percentageChange);
        
        // Create alert rule
        alertingService.createDegradationAlert(serviceName, metricName, alertName, description, 
                Math.abs(percentageChange) > degradationThreshold * 2);
        
        log.warn(description);
    }
    
    /**
     * Create a new SLA.
     *
     * @param name the SLA name
     * @param description the SLA description
     * @param serviceName the service name
     * @param metricName the metric name
     * @param operator the operator
     * @param threshold the threshold
     * @param evaluationPeriod the evaluation period in milliseconds
     * @param aggregation the aggregation function
     * @return the created SLA
     */
    public PerformanceSla createSla(
            String name, String description, String serviceName, String metricName,
            PerformanceSla.Operator operator, double threshold, long evaluationPeriod, String aggregation) {
        
        PerformanceSla sla = new PerformanceSla();
        sla.setName(name);
        sla.setDescription(description);
        sla.setServiceName(serviceName);
        sla.setMetricName(metricName);
        sla.setOperator(operator);
        sla.setThreshold(threshold);
        sla.setEvaluationPeriod(evaluationPeriod);
        sla.setAggregation(aggregation);
        sla.setEnabled(true);
        sla.setCreatedAt(Instant.now());
        
        return slaRepository.save(sla);
    }
    
    /**
     * Get all SLAs.
     *
     * @return the list of SLAs
     */
    public List<PerformanceSla> getAllSlas() {
        return slaRepository.findAll();
    }
    
    /**
     * Get an SLA by ID.
     *
     * @param id the SLA ID
     * @return the SLA
     */
    public PerformanceSla getSlaById(UUID id) {
        return slaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SLA not found: " + id));
    }
    
    /**
     * Update an SLA.
     *
     * @param id the SLA ID
     * @param sla the updated SLA
     * @return the updated SLA
     */
    public PerformanceSla updateSla(UUID id, PerformanceSla sla) {
        PerformanceSla existingSla = getSlaById(id);
        
        existingSla.setName(sla.getName());
        existingSla.setDescription(sla.getDescription());
        existingSla.setServiceName(sla.getServiceName());
        existingSla.setMetricName(sla.getMetricName());
        existingSla.setOperator(sla.getOperator());
        existingSla.setThreshold(sla.getThreshold());
        existingSla.setEvaluationPeriod(sla.getEvaluationPeriod());
        existingSla.setAggregation(sla.getAggregation());
        existingSla.setEnabled(sla.isEnabled());
        
        return slaRepository.save(existingSla);
    }
    
    /**
     * Delete an SLA.
     *
     * @param id the SLA ID
     */
    public void deleteSla(UUID id) {
        slaRepository.deleteById(id);
    }
    
    /**
     * Get SLA violations.
     *
     * @param slaId the SLA ID (optional)
     * @param startTime the start time (optional)
     * @param endTime the end time (optional)
     * @return the list of SLA violations
     */
    public List<PerformanceSlaViolation> getSlaViolations(UUID slaId, Instant startTime, Instant endTime) {
        if (slaId != null) {
            PerformanceSla sla = getSlaById(slaId);
            
            if (startTime != null && endTime != null) {
                return slaViolationRepository.findBySlaAndViolationTimeBetween(sla, startTime, endTime);
            } else {
                return slaViolationRepository.findBySla(sla);
            }
        } else if (startTime != null && endTime != null) {
            return slaViolationRepository.findByViolationTimeBetween(startTime, endTime);
        } else {
            return slaViolationRepository.findAll();
        }
    }
    
    /**
     * Compare performance between two time periods.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param period1Start the start time for period 1
     * @param period1End the end time for period 1
     * @param period2Start the start time for period 2
     * @param period2End the end time for period 2
     * @return the comparison result
     */
    public Map<String, Object> comparePerformance(
            String serviceName, String metricName,
            Instant period1Start, Instant period1End,
            Instant period2Start, Instant period2End) {
        
        if (!monitoringEnabled) {
            return Map.of("enabled", false);
        }
        
        // Get values for both periods
        double period1Value = timeSeriesService.queryAggregatedMetric(
                serviceName, metricName, period1Start, period1End, "mean");
        
        double period2Value = timeSeriesService.queryAggregatedMetric(
                serviceName, metricName, period2Start, period2End, "mean");
        
        // Calculate difference and percentage change
        double difference = period2Value - period1Value;
        double percentageChange = period1Value != 0 ? 
                (difference / period1Value) * 100 : 0;
        
        // Determine if the change is an improvement or degradation
        boolean isHigherWorse = isHigherWorse(metricName);
        boolean isImprovement = (isHigherWorse && percentageChange < 0) || 
                               (!isHigherWorse && percentageChange > 0);
        
        // Determine if the change is significant
        boolean isSignificant = Math.abs(percentageChange) > degradationThreshold;
        
        Map<String, Object> result = new HashMap<>();
        result.put("serviceName", serviceName);
        result.put("metricName", metricName);
        result.put("period1", Map.of("start", period1Start, "end", period1End, "value", period1Value));
        result.put("period2", Map.of("start", period2Start, "end", period2End, "value", period2Value));
        result.put("difference", difference);
        result.put("percentageChange", percentageChange);
        result.put("isImprovement", isImprovement);
        result.put("isSignificant", isSignificant);
        
        return result;
    }
}

