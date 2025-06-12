package com.codebridge.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Global filter for handling API versioning.
 * Supports version specification via URL path, header, or query parameter.
 */
@Component
public class ApiVersioningFilter implements GlobalFilter, Ordered {

    private static final Pattern VERSION_PATTERN = Pattern.compile("/v(\\d+)/");
    private static final String DEFAULT_VERSION = "1";

    /**
     * Filters the request to handle API versioning.
     *
     * @param exchange The server web exchange
     * @param chain The gateway filter chain
     * @return The Mono completion
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String version = DEFAULT_VERSION;
        
        // Check for version in URL path
        Matcher matcher = VERSION_PATTERN.matcher(path);
        if (matcher.find()) {
            version = matcher.group(1);
            
            // Rewrite the path to remove the version segment
            String newPath = path.replaceFirst("/v" + version + "/", "/");
            request = request.mutate().path(newPath).build();
        } else {
            // Check for version in header
            String headerVersion = request.getHeaders().getFirst("X-API-Version");
            if (headerVersion != null && !headerVersion.isEmpty()) {
                version = headerVersion.replaceAll("[^0-9]", ""); // Extract numeric version
            } else {
                // Check for version in query parameter
                String queryVersion = request.getQueryParams().getFirst("version");
                if (queryVersion != null && !queryVersion.isEmpty()) {
                    version = queryVersion.replaceAll("[^0-9]", ""); // Extract numeric version
                }
            }
        }
        
        // Add the version as a request header for downstream services
        request = request.mutate().header("X-API-Version", "v" + version).build();
        
        // Store the version in the exchange attributes
        exchange.getAttributes().put("apiVersion", version);
        
        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * Gets the order of this filter.
     * High priority ensures this filter runs early in the chain.
     *
     * @return The order value
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 200;
    }
}

