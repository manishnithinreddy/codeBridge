package com.codebridge.apitest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the API testing service.
 * Handles API testing operations.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiTestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiTestServiceApplication.class, args);
    }
}

