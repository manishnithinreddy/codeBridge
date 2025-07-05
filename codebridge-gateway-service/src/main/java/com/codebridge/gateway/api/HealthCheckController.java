package com.codebridge.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for health check endpoints.
 * Provides information about the gateway and connected services.
 */
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    private final WebClient.Builder webClientBuilder;
    private final DiscoveryClient discoveryClient;

    @Autowired
    public HealthCheckController(WebClient.Builder webClientBuilder, DiscoveryClient discoveryClient) {
        this.webClientBuilder = webClientBuilder;
        this.discoveryClient = discoveryClient;
    }

    /**
     * Gets the health status of the API Gateway.
     *
     * @return The health status
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "codebridge-api-gateway");
        response.put("timestamp", LocalDateTime.now().toString());
        
        // Add JVM metrics
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        jvm.put("freeMemory", Runtime.getRuntime().freeMemory());
        jvm.put("totalMemory", Runtime.getRuntime().totalMemory());
        jvm.put("maxMemory", Runtime.getRuntime().maxMemory());
        
        response.put("jvm", jvm);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets the health status of all services.
     *
     * @return The health status of all services
     */
    @GetMapping(path = "/services", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getServicesHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("gateway", getGatewayHealth());
        
        // Get all service IDs from Eureka
        List<String> services = discoveryClient.getServices();
        
        // Filter out non-CodeBridge services
        List<String> codebridgeServices = services.stream()
                .filter(service -> service.startsWith("codebridge-"))
                .collect(Collectors.toList());
        
        // Create a map of service health checks
        Map<String, Mono<Map<String, Object>>> serviceHealthChecks = new HashMap<>();
        
        codebridgeServices.forEach(service -> {
            serviceHealthChecks.put(service, getServiceHealth(service));
        });
        
        // Combine all health checks into a single response
        return Mono.zip(
                serviceHealthChecks.values().stream().collect(Collectors.toList()),
                results -> {
                    Map<String, Object> servicesHealth = new HashMap<>();
                    
                    for (int i = 0; i < results.length; i++) {
                        String serviceName = codebridgeServices.get(i);
                        servicesHealth.put(serviceName, results[i]);
                    }
                    
                    response.put("services", servicesHealth);
                    response.put("timestamp", LocalDateTime.now().toString());
                    
                    return response;
                }
        ).defaultIfEmpty(response);
    }

    /**
     * Gets the health status of the API Gateway.
     *
     * @return The health status
     */
    private Map<String, Object> getGatewayHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("name", "codebridge-api-gateway");
        health.put("timestamp", LocalDateTime.now().toString());
        
        return health;
    }

    /**
     * Gets the health status of a specific service.
     *
     * @param serviceId The service ID
     * @return The health status
     */
    private Mono<Map<String, Object>> getServiceHealth(String serviceId) {
        return webClientBuilder.build()
                .get()
                .uri("http://" + serviceId + "/actuator/health")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("status", "DOWN");
                    error.put("name", serviceId);
                    error.put("error", e.getMessage());
                    error.put("timestamp", LocalDateTime.now().toString());
                    return Mono.just(error);
                });
    }
}
