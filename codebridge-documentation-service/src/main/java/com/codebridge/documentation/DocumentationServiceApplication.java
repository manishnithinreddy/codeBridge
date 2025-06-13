package com.codebridge.documentation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Documentation Service.
 * This service provides API documentation generation, versioning, and publishing capabilities.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class DocumentationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentationServiceApplication.class, args);
    }
}

