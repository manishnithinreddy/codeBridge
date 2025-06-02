package com.codebridge.server.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced // Enables client-side load balancing (e.g., with Eureka and Ribbon/Spring Cloud LoadBalancer)
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
