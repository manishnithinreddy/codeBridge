package com.codebridge.monitoring.scalability.autoscaling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing auto-scaling of services.
 * Monitors metrics and triggers scaling operations based on configured thresholds.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoScalingService {

    private final MetricsCollector metricsCollector;
    private final ScalingOperator scalingOperator;
    
    @Value("${codebridge.scalability.auto-scaling.enabled}")
    private boolean autoScalingEnabled;
    
    @Value("${codebridge.scalability.auto-scaling.cpu-threshold}")
    private int cpuThreshold;
    
    @Value("${codebridge.scalability.auto-scaling.memory-threshold}")
    private int memoryThreshold;
    
    @Value("${codebridge.scalability.auto-scaling.min-instances}")
    private int minInstances;
    
    @Value("${codebridge.scalability.auto-scaling.max-instances}")
    private int maxInstances;
    
    @Value("${codebridge.scalability.auto-scaling.scale-up-cooldown-seconds}")
    private int scaleUpCooldownSeconds;
    
    @Value("${codebridge.scalability.auto-scaling.scale-down-cooldown-seconds}")
    private int scaleDownCooldownSeconds;
    
    private final Map<String, Instant> lastScaleUpMap = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastScaleDownMap = new ConcurrentHashMap<>();

    /**
     * Periodically checks metrics and triggers scaling operations if needed.
     */
    @Scheduled(fixedDelayString = "60000") // Every minute
    public void checkMetricsAndScale() {
        if (!autoScalingEnabled) {
            return;
        }
        
        metricsCollector.getServiceMetrics().forEach((serviceId, metrics) -> {
            int currentInstances = scalingOperator.getCurrentInstanceCount(serviceId);
            
            // Check if scaling up is needed
            if (shouldScaleUp(metrics, currentInstances)) {
                if (canScaleUp(serviceId)) {
                    int targetInstances = Math.min(currentInstances + 1, maxInstances);
                    log.info("Scaling up service {} from {} to {} instances", serviceId, currentInstances, targetInstances);
                    
                    boolean success = scalingOperator.scaleService(serviceId, targetInstances);
                    
                    if (success) {
                        lastScaleUpMap.put(serviceId, Instant.now());
                    }
                }
            }
            // Check if scaling down is needed
            else if (shouldScaleDown(metrics, currentInstances)) {
                if (canScaleDown(serviceId)) {
                    int targetInstances = Math.max(currentInstances - 1, minInstances);
                    log.info("Scaling down service {} from {} to {} instances", serviceId, currentInstances, targetInstances);
                    
                    boolean success = scalingOperator.scaleService(serviceId, targetInstances);
                    
                    if (success) {
                        lastScaleDownMap.put(serviceId, Instant.now());
                    }
                }
            }
        });
    }
    
    private boolean shouldScaleUp(ServiceMetrics metrics, int currentInstances) {
        return currentInstances < maxInstances && 
               (metrics.getCpuUtilization() > cpuThreshold || 
                metrics.getMemoryUtilization() > memoryThreshold);
    }
    
    private boolean shouldScaleDown(ServiceMetrics metrics, int currentInstances) {
        return currentInstances > minInstances && 
               metrics.getCpuUtilization() < cpuThreshold * 0.7 && 
               metrics.getMemoryUtilization() < memoryThreshold * 0.7;
    }
    
    private boolean canScaleUp(String serviceId) {
        Instant lastScaleUp = lastScaleUpMap.get(serviceId);
        
        if (lastScaleUp == null) {
            return true;
        }
        
        Instant cooldownEnd = lastScaleUp.plusSeconds(scaleUpCooldownSeconds);
        return Instant.now().isAfter(cooldownEnd);
    }
    
    private boolean canScaleDown(String serviceId) {
        Instant lastScaleDown = lastScaleDownMap.get(serviceId);
        
        if (lastScaleDown == null) {
            return true;
        }
        
        Instant cooldownEnd = lastScaleDown.plusSeconds(scaleDownCooldownSeconds);
        return Instant.now().isAfter(cooldownEnd);
    }
}

