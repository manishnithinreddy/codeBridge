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

/**
 * Global filter for transforming incoming requests.
 * Adds common headers and performs request validation.
 */
@Component
public class RequestTransformationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestTransformationFilter.class);

    /**
     * Filters the incoming request and adds common headers.
     *
     * @param exchange The server web exchange
     * @param chain The gateway filter chain
     * @return The Mono completion
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        
        // Add common headers to the request
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-Request-ID", requestId)
                .header("X-Request-Time", String.valueOf(startTime))
                .build();
        
        // Log the incoming request
        logger.debug("Incoming request: {} {} from {}", 
                request.getMethod(), 
                request.getURI(), 
                request.getRemoteAddress());
        
        // Store attributes in the exchange for use by other filters
        exchange.getAttributes().put("requestId", requestId);
        exchange.getAttributes().put("startTime", startTime);
        
        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * Gets the order of this filter.
     * Higher priority (lower order value) ensures this filter runs early in the chain.
     *
     * @return The order value
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}

