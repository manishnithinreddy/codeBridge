package com.codebridge.core.filter;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filter that adds a correlation ID to each request.
 * This ID is used for tracing requests across multiple services.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = generateCorrelationId();
        }
        
        final String finalCorrelationId = correlationId;
        
        // Add the correlation ID to the response headers
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        
        // Add the correlation ID to the MDC for logging
        return Mono.fromRunnable(() -> MDC.put(CORRELATION_ID_MDC_KEY, finalCorrelationId))
                .then(chain.filter(exchange))
                .doFinally(signalType -> MDC.remove(CORRELATION_ID_MDC_KEY));
    }

    /**
     * Generates a new correlation ID.
     *
     * @return the generated correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}

