package com.codebridge.identity.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the CodeBridge Identity Platform.
 * This service handles user authentication, authorization, and organization management.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class IdentityPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityPlatformApplication.class, args);
    }
}

