package com.codebridge.monitoring.performance.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * DTO for client-side metrics.
 */
@Data
public class ClientMetricsDto {

    @NotBlank
    private String page;
    
    private String browser;
    private String browserVersion;
    private String os;
    private String deviceType;
    private String connectionType;
    
    // Page load metrics
    private Long pageLoadTime;
    private Long domContentLoadedTime;
    private Long firstContentfulPaint;
    private Long largestContentfulPaint;
    private Long firstInputDelay;
    private Double cumulativeLayoutShift;
    
    // Resource metrics
    private Long resourceLoadTime;
    private Integer resourceCount;
    
    // Network metrics
    private Long networkRequestTime;
    private Long networkResponseTime;
    private Long networkLatency;
    
    // User experience metrics
    private Long timeToInteractive;
    private Long userTiming;
    
    // Error metrics
    private Integer jsErrorCount;
    private Integer apiErrorCount;
    
    // Custom metrics
    private Map<String, Double> customMetrics;
}

