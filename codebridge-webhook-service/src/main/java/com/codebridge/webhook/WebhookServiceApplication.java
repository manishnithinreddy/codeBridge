package com.codebridge.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the Webhook microservice.
 * Handles webhook events from external services like GitLab, GitHub, Docker, etc.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class WebhookServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookServiceApplication.class, args);
    }
}

