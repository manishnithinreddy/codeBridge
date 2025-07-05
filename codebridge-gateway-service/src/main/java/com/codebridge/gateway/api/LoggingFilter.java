package com.codebridge.gateway.filter;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global filter for logging requests and responses.
 * Provides detailed logging for debugging and monitoring.
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    private static final int MAX_LOG_SIZE = 10000; // Maximum size of logged body content

    /**
     * Filters the request and response for logging.
     *
     * @param exchange The server web exchange
     * @param chain The gateway filter chain
     * @return The Mono completion
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Get the original request and response
        ServerHttpRequest originalRequest = exchange.getRequest();
        ServerHttpResponse originalResponse = exchange.getResponse();
        
        // Log request headers
        logRequestHeaders(originalRequest);
        
        // Only log request body for specific content types
        if (shouldLogRequestBody(originalRequest)) {
            return logRequestBody(exchange, chain, originalRequest);
        } else {
            // Log response without logging request body
            return logResponse(exchange, chain, originalResponse);
        }
    }

    /**
     * Logs the request headers.
     *
     * @param request The server HTTP request
     */
    private void logRequestHeaders(ServerHttpRequest request) {
        logger.debug("Request: {} {}", request.getMethod(), request.getURI());
        request.getHeaders().forEach((name, values) -> {
            values.forEach(value -> logger.debug("Request Header: {}={}", name, value));
        });
    }

    /**
     * Determines if the request body should be logged based on content type.
     *
     * @param request The server HTTP request
     * @return True if the request body should be logged
     */
    private boolean shouldLogRequestBody(ServerHttpRequest request) {
        String contentType = request.getHeaders().getFirst("Content-Type");
        return contentType != null && 
               (contentType.contains("application/json") || 
                contentType.contains("application/xml") || 
                contentType.contains("text/plain"));
    }

    /**
     * Logs the request body.
     *
     * @param exchange The server web exchange
     * @param chain The gateway filter chain
     * @param originalRequest The original server HTTP request
     * @return The Mono completion
     */
    private Mono<Void> logRequestBody(ServerWebExchange exchange, GatewayFilterChain chain, ServerHttpRequest originalRequest) {
        AtomicReference<String> requestBodyRef = new AtomicReference<>();
        
        // Read and log the request body
        return DataBufferUtils.join(originalRequest.getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    
                    String bodyContent = new String(bytes, StandardCharsets.UTF_8);
                    requestBodyRef.set(bodyContent);
                    
                    // Log the request body (truncated if too large)
                    if (bodyContent.length() > MAX_LOG_SIZE) {
                        logger.debug("Request Body (truncated): {}", bodyContent.substring(0, MAX_LOG_SIZE) + "...");
                    } else {
                        logger.debug("Request Body: {}", bodyContent);
                    }
                    
                    // Create a new request with the cached body
                    ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(originalRequest) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Flux.just(exchange.getResponse().bufferFactory().wrap(bytes));
                        }
                    };
                    
                    // Log the response
                    return logResponse(exchange.mutate().request(decoratedRequest).build(), chain, exchange.getResponse());
                });
    }

    /**
     * Logs the response.
     *
     * @param exchange The server web exchange
     * @param chain The gateway filter chain
     * @param originalResponse The original server HTTP response
     * @return The Mono completion
     */
    private Mono<Void> logResponse(ServerWebExchange exchange, GatewayFilterChain chain, ServerHttpResponse originalResponse) {
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return super.writeWith(
                        Flux.from(body)
                            .doOnNext(buffer -> {
                                // Log response status and headers
                                logger.debug("Response Status: {}", getStatusCode());
                                getHeaders().forEach((name, values) -> {
                                    values.forEach(value -> logger.debug("Response Header: {}={}", name, value));
                                });
                                
                                // Only log response body for specific content types
                                String contentType = getHeaders().getFirst("Content-Type");
                                if (contentType != null && 
                                    (contentType.contains("application/json") || 
                                     contentType.contains("application/xml") || 
                                     contentType.contains("text/plain"))) {
                                    
                                    byte[] bytes = new byte[buffer.readableByteCount()];
                                    buffer.read(bytes);
                                    buffer.readPosition(0); // Reset read position to beginning
                                    
                                    String bodyContent = new String(bytes, StandardCharsets.UTF_8);
                                    
                                    // Log the response body (truncated if too large)
                                    if (bodyContent.length() > MAX_LOG_SIZE) {
                                        logger.debug("Response Body (truncated): {}", bodyContent.substring(0, MAX_LOG_SIZE) + "...");
                                    } else {
                                        logger.debug("Response Body: {}", bodyContent);
                                    }
                                }
                            })
                );
            }
        };
        
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    /**
     * Gets the order of this filter.
     * Medium priority ensures this filter runs in the middle of the chain.
     *
     * @return The order value
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 500;
    }
}
