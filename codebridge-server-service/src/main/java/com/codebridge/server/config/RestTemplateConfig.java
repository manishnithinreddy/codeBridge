package com.codebridge.server.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    // Standard RestTemplate for inter-service communication, potentially load-balanced
    @Bean
    @LoadBalanced // Enables client-side load balancing with Eureka/Consul etc.
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            // Example: Set connection and read timeouts
            // .setConnectTimeout(Duration.ofSeconds(5))
            // .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }
    
    // If a non-load-balanced RestTemplate is also needed for specific external calls:
    /*
    @Bean("externalRestTemplate")
    public RestTemplate externalRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
    */
}
