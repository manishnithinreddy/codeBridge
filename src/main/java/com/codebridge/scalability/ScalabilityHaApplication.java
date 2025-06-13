package com.codebridge.scalability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Scalability and High Availability service.
 * This service provides components and utilities for implementing scalability
 * and high availability across the CodeBridge platform.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableEurekaClient
@EnableScheduling
public class ScalabilityHaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScalabilityHaApplication.class, args);
    }
}

