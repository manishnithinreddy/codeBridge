package com.codebridge.performance.collector;

import com.codebridge.performance.model.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collector for API-related metrics.
 * This interceptor captures request counts, response times, and error rates
 * for API endpoints.
 */
@Component
@Slf4j
public class ApiMetricsCollector implements HandlerInterceptor {

    private static final String REQUEST_START_TIME = "requestStartTime";
    private static final String SERVICE_NAME = "api-service";
    
    private final PerformanceMetricsCollector metricsCollector;
    private final ConcurrentHashMap<String, Long> endpointRequestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> endpointErrorCounts = new ConcurrentHashMap<>();
    
    @Value("${services.api.metrics-enabled:true}")
    private boolean apiMetricsEnabled;
    
    @Value("#{'${services.api.endpoints-to-monitor:}'.split(',')}")
    private String[] endpointsToMonitor;

    @Autowired
    public ApiMetricsCollector(PerformanceMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!apiMetricsEnabled) {
            return true;
        }
        
        // Store the start time
        request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());
        
        // Increment request counter
        String endpoint = getEndpointPath(request);
        if (shouldMonitorEndpoint(endpoint)) {
            endpointRequestCounts.compute(endpoint, (k, v) -> (v == null) ? 1 : v + 1);
            
            // Record request count metric
            Map<String, String> tags = new HashMap<>();
            tags.put("endpoint", endpoint);
            tags.put("method", request.getMethod());
            metricsCollector.incrementCounter(SERVICE_NAME, "request.count", tags);
        }
        
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // Not used
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!apiMetricsEnabled) {
            return;
        }
        
        Long startTime = (Long) request.getAttribute(REQUEST_START_TIME);
        if (startTime == null) {
            return;
        }
        
        String endpoint = getEndpointPath(request);
        if (!shouldMonitorEndpoint(endpoint)) {
            return;
        }
        
        // Calculate request duration
        long duration = System.currentTimeMillis() - startTime;
        
        // Create tags
        Map<String, String> tags = new HashMap<>();
        tags.put("endpoint", endpoint);
        tags.put("method", request.getMethod());
        tags.put("status", String.valueOf(response.getStatus()));
        
        // Record response time
        metricsCollector.recordTimer(SERVICE_NAME, "response.time", duration, tags);
        
        // Record status code
        metricsCollector.incrementCounter(SERVICE_NAME, "status." + response.getStatus(), tags);
        
        // Record errors
        if (response.getStatus() >= 400 || ex != null) {
            endpointErrorCounts.compute(endpoint, (k, v) -> (v == null) ? 1 : v + 1);
            
            tags.put("error_type", ex != null ? ex.getClass().getSimpleName() : "HTTP" + response.getStatus());
            metricsCollector.incrementCounter(SERVICE_NAME, "error.count", tags);
        }
        
        // Calculate and record error rate
        long requestCount = endpointRequestCounts.getOrDefault(endpoint, 1L);
        long errorCount = endpointErrorCounts.getOrDefault(endpoint, 0L);
        double errorRate = (double) errorCount / requestCount;
        
        Map<String, String> errorRateTags = new HashMap<>();
        errorRateTags.put("endpoint", endpoint);
        metricsCollector.recordGauge(SERVICE_NAME, "error.rate", errorRate, errorRateTags);
    }
    
    /**
     * Record API test execution metrics.
     *
     * @param testId the test ID
     * @param testName the test name
     * @param duration the execution duration in milliseconds
     * @param success whether the test was successful
     */
    public void recordTestExecution(String testId, String testName, long duration, boolean success) {
        if (!apiMetricsEnabled) {
            return;
        }
        
        Map<String, String> tags = new HashMap<>();
        tags.put("test_id", testId);
        tags.put("test_name", testName);
        tags.put("success", String.valueOf(success));
        
        metricsCollector.recordTimer(SERVICE_NAME, "test.execution.time", duration, tags);
        metricsCollector.incrementCounter(SERVICE_NAME, "test.execution.count", tags);
        
        if (!success) {
            metricsCollector.incrementCounter(SERVICE_NAME, "test.execution.failure", tags);
        }
    }
    
    /**
     * Record API validation metrics.
     *
     * @param testId the test ID
     * @param testName the test name
     * @param validationType the validation type
     * @param success whether the validation was successful
     */
    public void recordValidation(String testId, String testName, String validationType, boolean success) {
        if (!apiMetricsEnabled) {
            return;
        }
        
        Map<String, String> tags = new HashMap<>();
        tags.put("test_id", testId);
        tags.put("test_name", testName);
        tags.put("validation_type", validationType);
        tags.put("success", String.valueOf(success));
        
        metricsCollector.incrementCounter(SERVICE_NAME, "validation.count", tags);
        
        if (!success) {
            metricsCollector.incrementCounter(SERVICE_NAME, "validation.failure", tags);
        }
    }
    
    /**
     * Get the endpoint path from the request.
     *
     * @param request the HTTP request
     * @return the endpoint path
     */
    private String getEndpointPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        if (contextPath != null && !contextPath.isEmpty()) {
            uri = uri.substring(contextPath.length());
        }
        
        // Remove query string
        int queryStringIndex = uri.indexOf('?');
        if (queryStringIndex > 0) {
            uri = uri.substring(0, queryStringIndex);
        }
        
        return uri;
    }
    
    /**
     * Check if an endpoint should be monitored.
     *
     * @param endpoint the endpoint path
     * @return true if the endpoint should be monitored, false otherwise
     */
    private boolean shouldMonitorEndpoint(String endpoint) {
        // If no specific endpoints are configured, monitor all
        if (endpointsToMonitor == null || endpointsToMonitor.length == 0 || 
            (endpointsToMonitor.length == 1 && endpointsToMonitor[0].isEmpty())) {
            return true;
        }
        
        // Check if the endpoint matches any of the configured endpoints
        for (String monitoredEndpoint : endpointsToMonitor) {
            if (monitoredEndpoint != null && !monitoredEndpoint.isEmpty() && 
                endpoint.startsWith(monitoredEndpoint.trim())) {
                return true;
            }
        }
        
        return false;
    }
}

