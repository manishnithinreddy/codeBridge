package com.codebridge.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the CodeBridge Monitoring Service.
 * This consolidated service combines the functionality of the previous
 * Monitoring Service and Performance Service.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
    "com.codebridge.monitoring",
    "com.codebridge.monitoring.performance",
    "com.codebridge.monitoring.platform",
    "com.codebridge.monitoring.scalability"
})
public class MonitoringServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoringServiceApplication.class, args);
    }
}

