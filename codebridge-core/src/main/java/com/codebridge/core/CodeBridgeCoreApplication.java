package com.codebridge.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
public class CodeBridgeCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeBridgeCoreApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Git Service Routes
                .route("git-service", r -> r.path("/api/git/**")
                        .filters(f -> f
                                .rewritePath("/api/git/(?<segment>.*)", "/api/git/${segment}")
                                .addRequestHeader("X-Audit-Id", "#{T(java.util.UUID).randomUUID().toString()}")
                        )
                        .uri("lb://git-service"))
                
                // Docker Service Routes
                .route("docker-service", r -> r.path("/api/docker/**")
                        .filters(f -> f
                                .rewritePath("/api/docker/(?<segment>.*)", "/api/docker/${segment}")
                                .addRequestHeader("X-Audit-Id", "#{T(java.util.UUID).randomUUID().toString()}")
                        )
                        .uri("lb://docker-service"))
                
                // Server Access Service Routes
                .route("server-service", r -> r.path("/api/server/**")
                        .filters(f -> f
                                .rewritePath("/api/server/(?<segment>.*)", "/api/server/${segment}")
                                .addRequestHeader("X-Audit-Id", "#{T(java.util.UUID).randomUUID().toString()}")
                        )
                        .uri("lb://server-service"))
                
                // API Testing Service Routes
                .route("api-testing-service", r -> r.path("/api/testing/**")
                        .filters(f -> f
                                .rewritePath("/api/testing/(?<segment>.*)", "/api/testing/${segment}")
                                .addRequestHeader("X-Audit-Id", "#{T(java.util.UUID).randomUUID().toString()}")
                        )
                        .uri("lb://api-testing-service"))
                .build();
    }
}

