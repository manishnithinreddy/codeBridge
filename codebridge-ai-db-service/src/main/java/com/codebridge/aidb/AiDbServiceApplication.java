package com.codebridge.aidb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the CodeBridge AI-Enhanced Database Service.
 * This service combines functionality from:
 * - DB Service
 * - AI DB Agent Service
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AiDbServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDbServiceApplication.class, args);
    }
}

