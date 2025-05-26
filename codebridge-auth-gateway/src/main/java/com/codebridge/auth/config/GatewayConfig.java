package com.codebridge.auth.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the API Gateway routes.
 */
@Configuration
public class GatewayConfig {

    /**
     * Configures the API Gateway routes.
     *
     * @param builder the RouteLocatorBuilder
     * @return the configured RouteLocator
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Teams Service
                .route("teams-service", r -> r
                        .path("/api/teams/**")
                        .uri("lb://teams-service"))
                
                // Audit Service
                .route("audit-service", r -> r
                        .path("/api/audit/**")
                        .uri("lb://audit-service"))
                
                // GitLab Integration Service
                .route("gitlab-service", r -> r
                        .path("/api/gitlab/**")
                        .uri("lb://gitlab-service"))
                
                // Docker Management Service
                .route("docker-service", r -> r
                        .path("/api/docker/**")
                        .uri("lb://docker-service"))
                
                // Server Management Service
                .route("server-service", r -> r
                        .path("/api/server/**")
                        .uri("lb://server-service"))
                
                // API Testing Service
                .route("api-test-service", r -> r
                        .path("/api/test/**")
                        .uri("lb://api-test-service"))
                
                .build();
    }
}

