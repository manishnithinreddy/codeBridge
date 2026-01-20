package com.codebridge.monitoring.scalability.autoscaling;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Component for collecting metrics from service instances.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsCollector {

    private static final String METRICS_PATH = "/actuator/metrics";
    
    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * Gets metrics for all services.
     *
     * @return a map of service IDs to service metrics
     */
    public Map<String, ServiceMetrics> getServiceMetrics() {
        Map<String, ServiceMetrics> result = new HashMap<>();
        
        discoveryClient.getServices().forEach(serviceId -> {
            try {
                ServiceMetrics metrics = collectMetricsForService(serviceId);
                result.put(serviceId, metrics);
            } catch (Exception e) {
                log.error("Failed to collect metrics for service {}: {}", serviceId, e.getMessage());
            }
        });
        
        return result;
    }
    
    private ServiceMetrics collectMetricsForService(String serviceId) {
        double cpuUtilization = getAverageCpuUtilization(serviceId);
        double memoryUtilization = getAverageMemoryUtilization(serviceId);
        double requestRate = getRequestRate(serviceId);
        double errorRate = getErrorRate(serviceId);
        double responseTime = getAverageResponseTime(serviceId);
        
        return ServiceMetrics.builder()
                .serviceId(serviceId)
                .cpuUtilization(cpuUtilization)
                .memoryUtilization(memoryUtilization)
                .requestRate(requestRate)
                .errorRate(errorRate)
                .responseTime(responseTime)
                .build();
    }
    
    private double getAverageCpuUtilization(String serviceId) {
        // In a real implementation, this would collect CPU metrics from all instances
        // For simplicity, we're returning a random value between 0 and 100
        return Math.random() * 100;
    }
    
    private double getAverageMemoryUtilization(String serviceId) {
        // In a real implementation, this would collect memory metrics from all instances
        // For simplicity, we're returning a random value between 0 and 100
        return Math.random() * 100;
    }
    
    private double getRequestRate(String serviceId) {
        // In a real implementation, this would collect request rate metrics from all instances
        // For simplicity, we're returning a random value between 0 and 1000
        return Math.random() * 1000;
    }
    
    private double getErrorRate(String serviceId) {
        // In a real implementation, this would collect error rate metrics from all instances
        // For simplicity, we're returning a random value between 0 and 10
        return Math.random() * 10;
    }
    
    private double getAverageResponseTime(String serviceId) {
        // In a real implementation, this would collect response time metrics from all instances
        // For simplicity, we're returning a random value between 0 and 500
        return Math.random() * 500;
    }
}

