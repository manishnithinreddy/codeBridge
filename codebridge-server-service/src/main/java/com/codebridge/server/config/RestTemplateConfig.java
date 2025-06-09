package com.codebridge.server.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    // Standard RestTemplate for inter-service communication
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            // Example: Set connection and read timeouts
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }

    // If a specialized RestTemplate is also needed for specific external calls:
    /*
    @Bean("externalRestTemplate")
    public RestTemplate externalRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
    */
}

