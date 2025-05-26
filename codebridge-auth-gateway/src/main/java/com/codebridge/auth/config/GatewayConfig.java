package com.codebridge.auth.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

/**
 * Configuration for API Gateway routing.
 * Routes requests to appropriate microservices based on path.
 */
@Configuration
public class GatewayConfig {

    /**
     * Configures routes for the API Gateway.
     *
     * @param builder the RouteLocatorBuilder
     * @return the configured RouteLocator
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Teams Service Routes
            .route("teams-service", r -> r
                .path("/api/teams/**")
                .filters(f -> f
                    .rewritePath("/api/teams/(?<segment>.*)", "/api/${segment}")
                    .addRequestHeader("X-Gateway-Source", "auth-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("lb://TEAMS-SERVICE"))
                
            // Audit Service Routes
            .route("audit-service", r -> r
                .path("/api/audit/**")
                .filters(f -> f
                    .rewritePath("/api/audit/(?<segment>.*)", "/api/${segment}")
                    .addRequestHeader("X-Gateway-Source", "auth-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("lb://AUDIT-SERVICE"))
                
            // GitLab Service Routes
            .route("gitlab-service", r -> r
                .path("/api/gitlab/**")
                .filters(f -> f
                    .rewritePath("/api/gitlab/(?<segment>.*)", "/api/${segment}")
                    .addRequestHeader("X-Gateway-Source", "auth-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("lb://GITLAB-SERVICE"))
                
            // Docker Service Routes
            .route("docker-service", r -> r
                .path("/api/docker/**")
                .filters(f -> f
                    .rewritePath("/api/docker/(?<segment>.*)", "/api/${segment}")
                    .addRequestHeader("X-Gateway-Source", "auth-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("lb://DOCKER-SERVICE"))
                
            // Server Service Routes
            .route("server-service", r -> r
                .path("/api/server/**")
                .filters(f -> f
                    .rewritePath("/api/server/(?<segment>.*)", "/api/${segment}")
                    .addRequestHeader("X-Gateway-Source", "auth-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("lb://SERVER-SERVICE"))
                
            // API Test Service Routes
            .route("api-test-service", r -> r
                .path("/api/test/**")
                .filters(f -> f
                    .rewritePath("/api/test/(?<segment>.*)", "/api/${segment}")
                    .addRequestHeader("X-Gateway-Source", "auth-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("lb://API-TEST-SERVICE"))
                
            // Authentication Routes (handled by this service)
            .route("auth-service", r -> r
                .path("/api/auth/**")
                .filters(f -> f
                    .rewritePath("/api/auth/(?<segment>.*)", "/api/${segment}")
                    .addRequestHeader("X-Gateway-Source", "auth-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())))
                .uri("lb://AUTH-GATEWAY"))
                
            .build();
    }
    
    /**
     * Creates a Redis rate limiter for request rate limiting.
     *
     * @return the configured RedisRateLimiter
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }
    
    /**
     * Creates a key resolver for rate limiting based on the user principal.
     *
     * @return the configured KeyResolver
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            if (exchange.getRequest().getHeaders().containsKey("X-User-ID")) {
                return Mono.just(exchange.getRequest().getHeaders().getFirst("X-User-ID"));
            }
            return Mono.just("anonymous");
        };
    }
}
