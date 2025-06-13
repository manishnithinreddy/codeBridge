package com.codebridge.server.scalability.autoscaling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing auto-scaling of server instances based on load metrics
 */
@Service
@Slf4j
public class AutoScalingService {

    private final boolean enabled;
    private final AtomicBoolean scalingInProgress = new AtomicBoolean(false);
    private final AtomicInteger currentInstanceCount = new AtomicInteger(2);
    private LocalDateTime lastScaleUpTime = LocalDateTime.now();
    private LocalDateTime lastScaleDownTime = LocalDateTime.now();
    
    @Value("${codebridge.scalability.auto-scaling.cpu-threshold:70}")
    private int cpuThreshold;
    
    @Value("${codebridge.scalability.auto-scaling.memory-threshold:80}")
    private int memoryThreshold;
    
    @Value("${codebridge.scalability.auto-scaling.min-instances:2}")
    private int minInstances;
    
    @Value("${codebridge.scalability.auto-scaling.max-instances:10}")
    private int maxInstances;
    
    @Value("${codebridge.scalability.auto-scaling.scale-up-cooldown-seconds:300}")
    private int scaleUpCooldownSeconds;
    
    @Value("${codebridge.scalability.auto-scaling.scale-down-cooldown-seconds:600}")
    private int scaleDownCooldownSeconds;

    public AutoScalingService(boolean enabled) {
        this.enabled = enabled;
        log.info("Auto-scaling service initialized with enabled={}", enabled);
    }

    /**
     * Scheduled task to check metrics and scale if necessary
     */
    @Scheduled(fixedDelayString = "${codebridge.scalability.auto-scaling.check-interval-seconds:60}000")
    public void checkMetricsAndScale() {
        if (!enabled || scalingInProgress.get()) {
            return;
        }
        
        try {
            // Get current metrics
            double currentCpuUsage = getCurrentCpuUsage();
            double currentMemoryUsage = getCurrentMemoryUsage();
            int currentRequestRate = getCurrentRequestRate();
            
            log.debug("Current metrics - CPU: {}%, Memory: {}%, Request Rate: {} req/s", 
                    currentCpuUsage, currentMemoryUsage, currentRequestRate);
            
            // Check if scaling is needed
            if (shouldScaleUp(currentCpuUsage, currentMemoryUsage, currentRequestRate)) {
                scaleUp();
            } else if (shouldScaleDown(currentCpuUsage, currentMemoryUsage, currentRequestRate)) {
                scaleDown();
            }
        } catch (Exception e) {
            log.error("Error during auto-scaling check", e);
        }
    }

    /**
     * Scale up by adding more instances
     */
    private void scaleUp() {
        if (scalingInProgress.compareAndSet(false, true)) {
            try {
                // Check cooldown period
                if (LocalDateTime.now().isBefore(lastScaleUpTime.plusSeconds(scaleUpCooldownSeconds))) {
                    log.debug("Scale-up cooldown period in effect, skipping");
                    return;
                }
                
                // Check if we're already at max instances
                int current = currentInstanceCount.get();
                if (current >= maxInstances) {
                    log.debug("Already at maximum instance count: {}", maxInstances);
                    return;
                }
                
                // Perform scale-up
                log.info("Scaling up from {} to {} instances", current, current + 1);
                
                // In a real implementation, this would call a cloud provider API or orchestration platform
                // For this example, we'll just increment our counter
                currentInstanceCount.incrementAndGet();
                lastScaleUpTime = LocalDateTime.now();
                
                log.info("Scale-up completed successfully");
            } finally {
                scalingInProgress.set(false);
            }
        }
    }

    /**
     * Scale down by removing instances
     */
    private void scaleDown() {
        if (scalingInProgress.compareAndSet(false, true)) {
            try {
                // Check cooldown period
                if (LocalDateTime.now().isBefore(lastScaleDownTime.plusSeconds(scaleDownCooldownSeconds))) {
                    log.debug("Scale-down cooldown period in effect, skipping");
                    return;
                }
                
                // Check if we're already at min instances
                int current = currentInstanceCount.get();
                if (current <= minInstances) {
                    log.debug("Already at minimum instance count: {}", minInstances);
                    return;
                }
                
                // Perform scale-down
                log.info("Scaling down from {} to {} instances", current, current - 1);
                
                // In a real implementation, this would call a cloud provider API or orchestration platform
                // For this example, we'll just decrement our counter
                currentInstanceCount.decrementAndGet();
                lastScaleDownTime = LocalDateTime.now();
                
                log.info("Scale-down completed successfully");
            } finally {
                scalingInProgress.set(false);
            }
        }
    }

    /**
     * Determine if we should scale up based on metrics
     */
    private boolean shouldScaleUp(double cpuUsage, double memoryUsage, int requestRate) {
        return cpuUsage > cpuThreshold || memoryUsage > memoryThreshold;
    }

    /**
     * Determine if we should scale down based on metrics
     */
    private boolean shouldScaleDown(double cpuUsage, double memoryUsage, int requestRate) {
        // Only scale down if we're well below thresholds
        return cpuUsage < (cpuThreshold * 0.7) && memoryUsage < (memoryThreshold * 0.7);
    }

    /**
     * Get current CPU usage (simulated)
     */
    private double getCurrentCpuUsage() {
        // In a real implementation, this would get actual CPU metrics
        // For this example, we'll return a simulated value
        return Math.random() * 100;
    }

    /**
     * Get current memory usage (simulated)
     */
    private double getCurrentMemoryUsage() {
        // In a real implementation, this would get actual memory metrics
        // For this example, we'll return a simulated value
        return Math.random() * 100;
    }

    /**
     * Get current request rate (simulated)
     */
    private int getCurrentRequestRate() {
        // In a real implementation, this would get actual request rate metrics
        // For this example, we'll return a simulated value
        return (int) (Math.random() * 1000);
    }

    /**
     * Get the current number of instances
     */
    public int getCurrentInstanceCount() {
        return currentInstanceCount.get();
    }

    /**
     * Check if auto-scaling is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}

