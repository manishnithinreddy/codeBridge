package com.codebridge.teams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for the Teams microservice.
 * Handles team management, team membership, and team-related operations.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
public class TeamsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamsServiceApplication.class, args);
    }
}

