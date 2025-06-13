package com.codebridge.monitoring.scalability.autoscaling;

import lombok.Builder;
import lombok.Data;

/**
 * Represents metrics for a service.
 */
@Data
@Builder
public class ServiceMetrics {
    
    /**
     * The service ID.
     */
    private String serviceId;
    
    /**
     * The CPU utilization (percentage).
     */
    private double cpuUtilization;
    
    /**
     * The memory utilization (percentage).
     */
    private double memoryUtilization;
    
    /**
     * The request rate (requests per second).
     */
    private double requestRate;
    
    /**
     * The error rate (percentage).
     */
    private double errorRate;
    
    /**
     * The average response time (milliseconds).
     */
    private double responseTime;
}

