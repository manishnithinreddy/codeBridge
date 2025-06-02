package com.codebridge.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling; // Added

/**
 * Main application class for the Server Management service.
 * Handles server infrastructure management and SSH key management.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling // Added
public class ServerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerServiceApplication.class, args);
    }
}

