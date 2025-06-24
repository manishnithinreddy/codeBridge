package com.codebridge.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Configuration class for the API Gateway.
 * Configures routing, rate limiting, and CORS settings.
 */
@Configuration
public class GatewayConfig {

    @Value("${codebridge.gateway.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${codebridge.gateway.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${codebridge.gateway.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${codebridge.gateway.cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${codebridge.gateway.cors.max-age}")
    private long maxAge;

    /**
     * Creates a key resolver for rate limiting based on the user's identity.
     * Falls back to IP address if user is not authenticated.
     *
     * @return The key resolver
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get user ID from JWT token
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }
            
            // Fall back to IP address
            String ipAddress = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just(ipAddress);
        };
    }

    /**
     * Creates a CORS filter for handling cross-origin requests.
     *
     * @return The CORS filter
     */
    @Bean
    public WebFilter corsFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            if (CorsUtils.isCorsRequest(request)) {
                ServerHttpResponse response = exchange.getResponse();
                HttpHeaders headers = response.getHeaders();
                
                headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigins);
                headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods);
                headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
                headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, String.valueOf(maxAge));
                
                if (allowCredentials) {
                    headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                }
                
                if (request.getMethod() == HttpMethod.OPTIONS) {
                    response.setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                }
            }
            
            return chain.filter(exchange);
        };
    }
}

