package com.codebridge.gateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Value("${codebridge.gateway.rate-limiting.default-limit:100}")
    private int defaultLimit;
    
    @Value("${codebridge.gateway.rate-limiting.default-duration:60}")
    private int defaultDuration;
    
    @Value("${codebridge.gateway.rate-limiting.ip-header:X-Forwarded-For}")
    private String ipHeader;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Skip rate limiting for certain paths
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/actuator") || path.startsWith("/api-docs") || path.startsWith("/swagger-ui")) {
            return chain.filter(exchange);
        }
        
        // Get client IP
        String clientIp = exchange.getRequest().getHeaders().getFirst(ipHeader);
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        
        // Get or create rate limiter bucket for this IP
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);
        
        // Try to consume a token
        if (bucket.tryConsume(1)) {
            // Request allowed, add rate limit headers
            exchange.getResponse().getHeaders().add("X-Rate-Limit-Remaining", 
                    String.valueOf(bucket.getAvailableTokens()));
            return chain.filter(exchange);
        } else {
            // Rate limit exceeded
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-Rate-Limit-Retry-After-Seconds", 
                    String.valueOf(defaultDuration));
            return exchange.getResponse().setComplete();
        }
    }

    private Bucket createBucket(String key) {
        Bandwidth limit = Bandwidth.classic(defaultLimit, 
                Refill.greedy(defaultLimit, Duration.ofSeconds(defaultDuration)));
        return Bucket4j.builder().addLimit(limit).build();
    }

    @Override
    public int getOrder() {
        // Execute this filter before the main gateway filter
        return -1;
    }
}

