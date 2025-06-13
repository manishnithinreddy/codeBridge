package com.codebridge.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Get or generate correlation ID
        String correlationId = request.getHeaders().getFirst("X-Correlation-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            request = request.mutate().header("X-Correlation-ID", correlationId).build();
            exchange = exchange.mutate().request(request).build();
        }
        
        // Log request details
        logger.info("[{}] Request: {} {} from {}", 
                correlationId,
                request.getMethod(), 
                request.getURI(),
                request.getRemoteAddress());
        
        // Log request headers if debug is enabled
        if (logger.isDebugEnabled()) {
            request.getHeaders().forEach((name, values) -> {
                values.forEach(value -> logger.debug("[{}] Header: {}={}", correlationId, name, value));
            });
        }
        
        // Record start time
        long startTime = System.currentTimeMillis();
        
        // Continue the filter chain and log response details
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("[{}] Response: {} completed in {} ms", 
                            correlationId,
                            exchange.getResponse().getStatusCode(),
                            duration);
                }));
    }

    @Override
    public int getOrder() {
        // Execute this filter before the rate limiting filter
        return -2;
    }
}

