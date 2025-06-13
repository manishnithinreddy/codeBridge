package com.codebridge.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter for transforming outgoing responses.
 * Adds common headers and performs response logging.
 */
@Component
public class ResponseTransformationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ResponseTransformationFilter.class);

    /**
     * Filters the outgoing response and adds common headers.
     *
     * @param exchange The server web exchange
     * @param chain The gateway filter chain
     * @return The Mono completion
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute("startTime");
            String requestId = exchange.getAttribute("requestId");
            
            if (startTime != null) {
                long executionTime = System.currentTimeMillis() - startTime;
                ServerHttpResponse response = exchange.getResponse();
                
                // Add common headers to the response
                response.getHeaders().add("X-Response-Time", String.valueOf(executionTime));
                
                if (requestId != null) {
                    response.getHeaders().add("X-Request-ID", requestId);
                }
                
                // Add API version header
                response.getHeaders().add("X-API-Version", "v1");
                
                // Add security headers
                response.getHeaders().add("X-Content-Type-Options", "nosniff");
                response.getHeaders().add("X-XSS-Protection", "1; mode=block");
                response.getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                
                // Log the outgoing response
                logger.debug("Outgoing response: {} {} - Status: {} - Execution Time: {} ms", 
                        exchange.getRequest().getMethod(), 
                        exchange.getRequest().getURI(), 
                        response.getStatusCode(),
                        executionTime);
            }
        }));
    }

    /**
     * Gets the order of this filter.
     * Lower priority (higher order value) ensures this filter runs late in the chain.
     *
     * @return The order value
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}

