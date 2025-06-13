package com.codebridge.aidb.db;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the CodeBridge Database Service.
 * This service provides enhanced database integration capabilities including:
 * - Support for multiple database types (SQL, NoSQL, Graph, Time-series)
 * - Schema management and migration tools
 * - Query building and management
 * - Database testing and validation
 * - Performance monitoring
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class DbServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbServiceApplication.class, args);
    }
}

