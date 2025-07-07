package com.codebridge.monitoring.performance.service;

import com.codebridge.monitoring.performance.model.AlertRule;
import com.codebridge.monitoring.performance.model.AlertSeverity;
import com.codebridge.monitoring.performance.model.AlertStatus;
import com.codebridge.monitoring.performance.model.PerformanceAlert;
import com.codebridge.monitoring.performance.repository.AlertRuleRepository;
import com.codebridge.monitoring.performance.repository.PerformanceAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing performance alerts.
 */
@Service
@Slf4j
public class AlertingService {

    private final AlertRuleRepository alertRuleRepository;
    private final PerformanceAlertRepository alertRepository;
    private final TimeSeriesService timeSeriesService;
    private final RestTemplate restTemplate;
    
    @Value("${performance.alerting.enabled:true}")
    private boolean alertingEnabled;
    
    @Value("${performance.alerting.notification.email.enabled:false}")
    private boolean emailNotificationEnabled;
    
    @Value("${performance.alerting.notification.email.recipients:}")
    private String emailRecipients;
    
    @Value("${performance.alerting.notification.webhook.enabled:false}")
    private boolean webhookNotificationEnabled;
    
    @Value("${performance.alerting.notification.webhook.url:}")
    private String webhookUrl;

    @Autowired
    public AlertingService(
            AlertRuleRepository alertRuleRepository,
            PerformanceAlertRepository alertRepository,
            TimeSeriesService timeSeriesService) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertRepository = alertRepository;
        this.timeSeriesService = timeSeriesService;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Scheduled task to check alert rules.
     */
    @Scheduled(fixedDelayString = "${performance.alerting.check-interval:60000}")
    public void checkAlertRules() {
        if (!alertingEnabled) {
            return;
        }
        
        log.debug("Checking alert rules");
        List<AlertRule> activeRules = alertRuleRepository.findByEnabled(true);
        
        for (AlertRule rule : activeRules) {
            try {
                checkAlertRule(rule);
            } catch (Exception e) {
                log.error("Error checking alert rule {}: {}", rule.getId(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Check a specific alert rule.
     *
     * @param rule the alert rule
     */
    private void checkAlertRule(AlertRule rule) {
        // Get the current value for the metric
        double currentValue = getCurrentMetricValue(rule);
        
        // Check if the threshold is violated
        boolean thresholdViolated = isThresholdViolated(rule, currentValue);
        
        // Get the most recent alert for this rule
        PerformanceAlert recentAlert = alertRepository.findTopByAlertRuleOrderByCreatedAtDesc(rule);
        
        if (thresholdViolated) {
            // If there's no recent alert or it's resolved, create a new one
            if (recentAlert == null || recentAlert.getStatus() == AlertStatus.RESOLVED) {
                createAlert(rule, currentValue);
            } else if (recentAlert.getStatus() == AlertStatus.ACTIVE) {
                // Update the existing alert
                updateAlert(recentAlert, currentValue);
            }
        } else if (recentAlert != null && recentAlert.getStatus() == AlertStatus.ACTIVE) {
            // Resolve the alert
            resolveAlert(recentAlert);
        }
    }
    
    /**
     * Get the current value for a metric.
     *
     * @param rule the alert rule
     * @return the current metric value
     */
    private double getCurrentMetricValue(AlertRule rule) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusMillis(rule.getEvaluationPeriod());
        
        return timeSeriesService.queryAggregatedMetric(
                rule.getServiceName(), 
                rule.getMetricName(), 
                startTime, 
                endTime, 
                rule.getAggregation());
    }
    
    /**
     * Check if a threshold is violated.
     *
     * @param rule the alert rule
     * @param currentValue the current metric value
     * @return true if the threshold is violated, false otherwise
     */
    private boolean isThresholdViolated(AlertRule rule, double currentValue) {
        switch (rule.getOperator()) {
            case GREATER_THAN:
                return currentValue > rule.getThreshold();
            case GREATER_THAN_OR_EQUAL:
                return currentValue >= rule.getThreshold();
            case LESS_THAN:
                return currentValue < rule.getThreshold();
            case LESS_THAN_OR_EQUAL:
                return currentValue <= rule.getThreshold();
            case EQUAL:
                return Math.abs(currentValue - rule.getThreshold()) < 0.0001;
            case NOT_EQUAL:
                return Math.abs(currentValue - rule.getThreshold()) >= 0.0001;
            default:
                return false;
        }
    }
    
    /**
     * Create a new alert.
     *
     * @param rule the alert rule
     * @param currentValue the current metric value
     * @return the created alert
     */
    private PerformanceAlert createAlert(AlertRule rule, double currentValue) {
        PerformanceAlert alert = new PerformanceAlert();
        alert.setAlertRule(rule);
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setCurrentValue(currentValue);
        alert.setThreshold(rule.getThreshold());
        alert.setMessage(generateAlertMessage(rule, currentValue));
        alert.setTriggeredAt(Instant.now());
        
        PerformanceAlert savedAlert = alertRepository.save(alert);
        
        // Send notifications
        sendAlertNotifications(savedAlert);
        
        log.info("Created new alert: {}", savedAlert.getMessage());
        
        return savedAlert;
    }
    
    /**
     * Update an existing alert.
     *
     * @param alert the alert to update
     * @param currentValue the current metric value
     * @return the updated alert
     */
    private PerformanceAlert updateAlert(PerformanceAlert alert, double currentValue) {
        alert.setCurrentValue(currentValue);
        alert.setLastCheckedAt(Instant.now());
        
        return alertRepository.save(alert);
    }
    
    /**
     * Resolve an alert.
     *
     * @param alert the alert to resolve
     * @return the resolved alert
     */
    private PerformanceAlert resolveAlert(PerformanceAlert alert) {
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now());
        
        PerformanceAlert resolvedAlert = alertRepository.save(alert);
        
        // Send resolution notifications
        sendAlertResolutionNotifications(resolvedAlert);
        
        log.info("Resolved alert: {}", resolvedAlert.getMessage());
        
        return resolvedAlert;
    }
    
    /**
     * Generate an alert message.
     *
     * @param rule the alert rule
     * @param currentValue the current metric value
     * @return the alert message
     */
    private String generateAlertMessage(AlertRule rule, double currentValue) {
        return String.format("Alert: %s - %s %s %.2f (threshold: %s %.2f)",
                rule.getName(),
                rule.getMetricName(),
                getOperatorSymbol(rule.getOperator()),
                currentValue,
                getOperatorSymbol(rule.getOperator()),
                rule.getThreshold());
    }
    
    /**
     * Get the symbol for an operator.
     *
     * @param operator the operator
     * @return the operator symbol
     */
    private String getOperatorSymbol(AlertRule.Operator operator) {
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
     * Send alert notifications.
     *
     * @param alert the alert
     */
    private void sendAlertNotifications(PerformanceAlert alert) {
        if (emailNotificationEnabled) {
            sendEmailNotification(alert);
        }
        
        if (webhookNotificationEnabled) {
            sendWebhookNotification(alert);
        }
    }
    
    /**
     * Send alert resolution notifications.
     *
     * @param alert the alert
     */
    private void sendAlertResolutionNotifications(PerformanceAlert alert) {
        if (emailNotificationEnabled) {
            sendEmailResolutionNotification(alert);
        }
        
        if (webhookNotificationEnabled) {
            sendWebhookResolutionNotification(alert);
        }
    }
    
    /**
     * Send an email notification.
     *
     * @param alert the alert
     */
    private void sendEmailNotification(PerformanceAlert alert) {
        // In a real implementation, this would use JavaMailSender or an email service
        log.info("Sending email notification for alert {} to {}", alert.getId(), emailRecipients);
    }
    
    /**
     * Send an email resolution notification.
     *
     * @param alert the alert
     */
    private void sendEmailResolutionNotification(PerformanceAlert alert) {
        // In a real implementation, this would use JavaMailSender or an email service
        log.info("Sending email resolution notification for alert {} to {}", alert.getId(), emailRecipients);
    }
    
    /**
     * Send a webhook notification.
     *
     * @param alert the alert
     */
    private void sendWebhookNotification(PerformanceAlert alert) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", alert.getId());
            payload.put("name", alert.getAlertRule().getName());
            payload.put("message", alert.getMessage());
            payload.put("severity", alert.getAlertRule().getSeverity());
            payload.put("currentValue", alert.getCurrentValue());
            payload.put("threshold", alert.getThreshold());
            payload.put("triggeredAt", alert.getTriggeredAt());
            payload.put("status", alert.getStatus());
            
            restTemplate.postForEntity(webhookUrl, payload, String.class);
            
            log.info("Sent webhook notification for alert {}", alert.getId());
        } catch (Exception e) {
            log.error("Error sending webhook notification for alert {}: {}", alert.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Send a webhook resolution notification.
     *
     * @param alert the alert
     */
    private void sendWebhookResolutionNotification(PerformanceAlert alert) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", alert.getId());
            payload.put("name", alert.getAlertRule().getName());
            payload.put("message", "Resolved: " + alert.getMessage());
            payload.put("severity", alert.getAlertRule().getSeverity());
            payload.put("currentValue", alert.getCurrentValue());
            payload.put("threshold", alert.getThreshold());
            payload.put("triggeredAt", alert.getTriggeredAt());
            payload.put("resolvedAt", alert.getResolvedAt());
            payload.put("status", alert.getStatus());
            
            restTemplate.postForEntity(webhookUrl, payload, String.class);
            
            log.info("Sent webhook resolution notification for alert {}", alert.getId());
        } catch (Exception e) {
            log.error("Error sending webhook resolution notification for alert {}: {}", 
                    alert.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Create a new alert rule.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param operator the operator
     * @param threshold the threshold
     * @param evaluationPeriod the evaluation period in milliseconds
     * @param aggregation the aggregation function
     * @param severity the alert severity
     * @param name the alert name
     * @param description the alert description
     * @return the created alert rule
     */
    public AlertRule createAlertRule(
            String serviceName, String metricName, AlertRule.Operator operator,
            double threshold, long evaluationPeriod, String aggregation,
            AlertSeverity severity, String name, String description) {
        
        AlertRule rule = new AlertRule();
        rule.setServiceName(serviceName);
        rule.setMetricName(metricName);
        rule.setOperator(operator);
        rule.setThreshold(threshold);
        rule.setEvaluationPeriod(evaluationPeriod);
        rule.setAggregation(aggregation);
        rule.setSeverity(severity);
        rule.setName(name);
        rule.setDescription(description);
        rule.setEnabled(true);
        
        return alertRuleRepository.save(rule);
    }
    
    /**
     * Get all alert rules.
     *
     * @return the list of alert rules
     */
    public List<AlertRule> getAllAlertRules() {
        return alertRuleRepository.findAll();
    }
    
    /**
     * Get an alert rule by ID.
     *
     * @param id the alert rule ID
     * @return the alert rule
     */
    public AlertRule getAlertRuleById(UUID id) {
        return alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found: " + id));
    }
    
    /**
     * Update an alert rule.
     *
     * @param id the alert rule ID
     * @param rule the updated alert rule
     * @return the updated alert rule
     */
    public AlertRule updateAlertRule(UUID id, AlertRule rule) {
        AlertRule existingRule = getAlertRuleById(id);
        
        existingRule.setServiceName(rule.getServiceName());
        existingRule.setMetricName(rule.getMetricName());
        existingRule.setOperator(rule.getOperator());
        existingRule.setThreshold(rule.getThreshold());
        existingRule.setEvaluationPeriod(rule.getEvaluationPeriod());
        existingRule.setAggregation(rule.getAggregation());
        existingRule.setSeverity(rule.getSeverity());
        existingRule.setName(rule.getName());
        existingRule.setDescription(rule.getDescription());
        existingRule.setEnabled(rule.isEnabled());
        
        return alertRuleRepository.save(existingRule);
    }
    
    /**
     * Delete an alert rule.
     *
     * @param id the alert rule ID
     */
    public void deleteAlertRule(UUID id) {
        alertRuleRepository.deleteById(id);
    }
    
    /**
     * Get all alerts.
     *
     * @return the list of alerts
     */
    public List<PerformanceAlert> getAllAlerts() {
        return alertRepository.findAll();
    }
    
    /**
     * Get active alerts.
     *
     * @return the list of active alerts
     */
    public List<PerformanceAlert> getActiveAlerts() {
        return alertRepository.findByStatus(AlertStatus.ACTIVE);
    }
    
    /**
     * Get alerts by status.
     *
     * @param status the alert status
     * @return the list of alerts
     */
    public List<PerformanceAlert> getAlertsByStatus(AlertStatus status) {
        return alertRepository.findByStatus(status);
    }
    
    /**
     * Get an alert by ID.
     *
     * @param id the alert ID
     * @return the alert
     */
    public PerformanceAlert getAlertById(UUID id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
    }
    
    /**
     * Acknowledge an alert.
     *
     * @param id the alert ID
     * @param acknowledgedBy the user who acknowledged the alert
     * @return the acknowledged alert
     */
    public PerformanceAlert acknowledgeAlert(UUID id, String acknowledgedBy) {
        PerformanceAlert alert = getAlertById(id);
        
        if (alert.getStatus() == AlertStatus.ACTIVE) {
            alert.setStatus(AlertStatus.ACKNOWLEDGED);
            alert.setAcknowledgedBy(acknowledgedBy);
            alert.setAcknowledgedAt(Instant.now());
            
            return alertRepository.save(alert);
        }
        
        return alert;
    }

    /**
     * Get alert rules by service and metric name.
     *
     * @param serviceName Service name
     * @param metricName Metric name
     * @return List of alert rules
     */
    public List<AlertRule> getAlertRulesByServiceAndMetric(String serviceName, String metricName) {
        log.info("Getting alert rules for service: {} and metric: {}", serviceName, metricName);
        // Implementation for getting alert rules by service and metric
        return alertRuleRepository.findByServiceNameAndMetricName(serviceName, metricName);
    }

    /**
     * Create a degradation alert.
     *
     * @param serviceName Service name
     * @param metricName Metric name
     * @param alertName Alert name
     * @param description Alert description
     * @param isCritical Whether the alert is critical
     */
    public void createDegradationAlert(String serviceName, String metricName, String alertName, 
                                     String description, boolean isCritical) {
        log.info("Creating degradation alert for service: {} and metric: {}", serviceName, metricName);
        
        // First, create or find an alert rule for this service and metric
        AlertRule alertRule = new AlertRule();
        alertRule.setId(UUID.randomUUID());
        alertRule.setServiceName(serviceName);
        alertRule.setMetricName(metricName);
        alertRule.setName(alertName);
        alertRule.setDescription(description);
        alertRule.setSeverity(isCritical ? AlertSeverity.CRITICAL : AlertSeverity.WARNING);
        alertRule.setEnabled(true);
        alertRule.setOperator(AlertRule.Operator.GREATER_THAN);
        alertRule.setThreshold(0.0); // Default threshold
        
        alertRule = alertRuleRepository.save(alertRule);
        
        // Create performance alert
        PerformanceAlert alert = new PerformanceAlert();
        alert.setAlertRule(alertRule);
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setMessage(description);
        alert.setCurrentValue(0.0); // Default value
        alert.setThreshold(0.0); // Default threshold
        alert.setTriggeredAt(Instant.now());
        
        alertRepository.save(alert);
        
        // Send notifications if enabled
        if (webhookNotificationEnabled && webhookUrl != null && !webhookUrl.isEmpty()) {
            sendWebhookNotification(alert);
        }
        
        if (emailNotificationEnabled && emailRecipients != null && !emailRecipients.isEmpty()) {
            sendEmailNotification(alert);
        }
    }
}
