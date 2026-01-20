package com.codebridge.featureflag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Feature Flag Service.
 * This service manages feature flags for dynamic service implementation switching.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class FeatureFlagApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeatureFlagApplication.class, args);
    }
}

