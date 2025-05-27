package com.codebridge.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the Events Service.
 * This service consolidates webhook event handling, audit logging, and event monitoring.
 * It combines the functionality of the former Webhook Service and Audit Service.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class EventsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventsServiceApplication.class, args);
    }
}

