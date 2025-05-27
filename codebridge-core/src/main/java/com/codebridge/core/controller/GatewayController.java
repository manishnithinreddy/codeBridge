package com.codebridge.core.controller;

import com.codebridge.core.service.RoutingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling requests that need custom processing before routing.
 * Most requests will be handled directly by Spring Cloud Gateway routes.
 */
@RestController
@Slf4j
public class GatewayController {

    private final RoutingService routingService;

    public GatewayController(RoutingService routingService) {
        this.routingService = routingService;
    }

    /**
     * Example of a custom routing endpoint that needs special processing.
     * This demonstrates how to manually route a request when the standard
     * Spring Cloud Gateway routes are not sufficient.
     */
    @RequestMapping("/api/custom/**")
    public Mono<Object> handleCustomRequest(ServerHttpRequest request, @RequestBody(required = false) Object body) {
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        
        // Extract headers
        Map<String, String> headers = new HashMap<>();
        HttpHeaders httpHeaders = request.getHeaders();
        httpHeaders.forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0));
            }
        });
        
        // Determine which service to route to based on the path
        String serviceName;
        String servicePath;
        
        if (path.startsWith("/api/custom/git")) {
            serviceName = "git-service";
            servicePath = path.replace("/api/custom/git", "/api/git");
        } else if (path.startsWith("/api/custom/docker")) {
            serviceName = "docker-service";
            servicePath = path.replace("/api/custom/docker", "/api/docker");
        } else if (path.startsWith("/api/custom/server")) {
            serviceName = "server-service";
            servicePath = path.replace("/api/custom/server", "/api/server");
        } else if (path.startsWith("/api/custom/testing")) {
            serviceName = "api-testing-service";
            servicePath = path.replace("/api/custom/testing", "/api/testing");
        } else {
            return Mono.error(new IllegalArgumentException("Unknown service path: " + path));
        }
        
        // Add custom processing here if needed
        // For example, you could add additional headers, transform the request, etc.
        
        // Route the request
        return routingService.routeRequest(
                serviceName,
                servicePath,
                method,
                headers,
                body,
                Object.class
        );
    }
}

