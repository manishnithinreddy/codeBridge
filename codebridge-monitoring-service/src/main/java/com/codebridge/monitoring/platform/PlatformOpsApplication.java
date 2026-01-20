package com.codebridge.monitoring.platform.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the CodeBridge Platform Operations Service.
 * This service handles admin operations, system monitoring, webhooks, and audit logging.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class PlatformOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformOpsApplication.class, args);
    }
}

