package com.codebridge.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Configuration
public class GatewayConfig {

    @Value("${codebridge.gateway.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${codebridge.gateway.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${codebridge.gateway.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${codebridge.gateway.cors.max-age}")
    private long maxAge;

    @Value("${codebridge.gateway.rate-limiting.ip-header:X-Forwarded-For}")
    private String ipHeader;

    /**
     * Configure CORS settings for the gateway
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        if ("*".equals(allowedOrigins)) {
            corsConfig.addAllowedOriginPattern("*");
        } else {
            Arrays.stream(allowedOrigins.split(","))
                  .forEach(corsConfig::addAllowedOrigin);
        }
        
        if ("*".equals(allowedMethods)) {
            corsConfig.addAllowedMethod("*");
        } else {
            Arrays.stream(allowedMethods.split(","))
                  .map(HttpMethod::valueOf)
                  .forEach(corsConfig::addAllowedMethod);
        }
        
        if ("*".equals(allowedHeaders)) {
            corsConfig.addAllowedHeader("*");
        } else {
            Arrays.stream(allowedHeaders.split(","))
                  .forEach(corsConfig::addAllowedHeader);
        }
        
        corsConfig.setMaxAge(maxAge);
        corsConfig.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }

    /**
     * Key resolver for rate limiting based on IP address
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String ip = request.getHeaders().getFirst(ipHeader);
            
            if (ip == null || ip.isEmpty()) {
                ip = Objects.requireNonNull(request.getRemoteAddress()).getAddress().getHostAddress();
            }
            
            final String finalIp = ip;
            return Mono.just(finalIp);
        };
    }

    /**
     * Configure Redis rate limiter for global rate limiting
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * Add correlation ID to all requests for tracing
     */
    @Bean
    public WebFilter correlationIdFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();
            
            String correlationId = request.getHeaders().getFirst("X-Correlation-ID");
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = java.util.UUID.randomUUID().toString();
                request = request.mutate().header("X-Correlation-ID", correlationId).build();
                exchange = exchange.mutate().request(request).build();
            }
            
            response.getHeaders().add("X-Correlation-ID", correlationId);
            
            return chain.filter(exchange);
        };
    }

    /**
     * Add security headers to all responses
     */
    @Bean
    public WebFilter securityHeadersFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();
            
            headers.add("X-Content-Type-Options", "nosniff");
            headers.add("X-Frame-Options", "DENY");
            headers.add("X-XSS-Protection", "1; mode=block");
            headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            headers.add("Cache-Control", "no-store");
            headers.add("Pragma", "no-cache");
            
            return chain.filter(exchange);
        };
    }
}

