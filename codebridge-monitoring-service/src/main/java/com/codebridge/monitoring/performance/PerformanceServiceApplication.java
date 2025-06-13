package com.codebridge.monitoring.performance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the CodeBridge Performance Monitoring Service.
 * This service provides comprehensive performance monitoring, alerting, and optimization
 * capabilities for the CodeBridge platform.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class PerformanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerformanceServiceApplication.class, args);
    }
}

