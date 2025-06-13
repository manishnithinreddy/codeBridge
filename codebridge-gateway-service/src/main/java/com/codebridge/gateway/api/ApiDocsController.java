package com.codebridge.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for aggregating and serving API documentation.
 * Collects OpenAPI documentation from all services and provides a unified view.
 */
@RestController
@RequestMapping("/api-docs")
public class ApiDocsController {

    private final WebClient.Builder webClientBuilder;
    private final DiscoveryClient discoveryClient;

    @Autowired
    public ApiDocsController(WebClient.Builder webClientBuilder, DiscoveryClient discoveryClient) {
        this.webClientBuilder = webClientBuilder;
        this.discoveryClient = discoveryClient;
    }

    /**
     * Gets a list of all available API documentation endpoints.
     *
     * @return The list of API documentation endpoints
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getApiDocs() {
        Map<String, Object> response = new HashMap<>();
        
        // Get all service IDs from Eureka
        List<String> services = discoveryClient.getServices();
        
        // Filter out non-CodeBridge services and the gateway itself
        List<String> codebridgeServices = services.stream()
                .filter(service -> service.startsWith("codebridge-") && !service.equals("codebridge-api-gateway"))
                .collect(Collectors.toList());
        
        // Add gateway documentation
        Map<String, String> apiDocs = new HashMap<>();
        apiDocs.put("API Gateway", "/v3/api-docs");
        
        // Add service documentation URLs
        codebridgeServices.forEach(service -> {
            String serviceName = service.replace("codebridge-", "").replace("-service", "");
            String path = "/api/" + serviceName + "s/v3/api-docs";
            apiDocs.put(serviceName, path);
        });
        
        response.put("services", apiDocs);
        response.put("swagger-ui", "/swagger-ui.html");
        
        return Mono.just(response);
    }

    /**
     * Gets the API documentation for a specific service.
     *
     * @param serviceId The service ID
     * @return The API documentation
     */
    private Mono<Map<String, Object>> getServiceApiDocs(String serviceId) {
        return webClientBuilder.build()
                .get()
                .uri("http://" + serviceId + "/v3/api-docs")
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Failed to retrieve API docs for " + serviceId);
                    error.put("message", e.getMessage());
                    return Mono.just(error);
                });
    }
}

