package com.codebridge.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for the Identity Service.
 * This service consolidates authentication, authorization, session management, and security features.
 * It combines the functionality of the former Auth Gateway and Security Service.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}

