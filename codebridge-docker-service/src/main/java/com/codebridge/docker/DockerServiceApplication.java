package com.codebridge.docker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the Docker Management service.
 * Handles Docker container lifecycle management.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class DockerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DockerServiceApplication.class, args);
    }
}

