package com.codebridge.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Main application class for the CodeBridge Service Discovery.
 * This service provides service registration and discovery for all CodeBridge microservices.
 */
@SpringBootApplication
@EnableEurekaServer
public class ServiceDiscoveryApplication {

    /**
     * Main method to start the Service Discovery application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ServiceDiscoveryApplication.class, args);
    }
}

