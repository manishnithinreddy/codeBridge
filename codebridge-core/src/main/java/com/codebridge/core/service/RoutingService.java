package com.codebridge.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Service for routing requests to microservices with circuit breaker protection.
 */
@Service
@Slf4j
public class RoutingService {

    private final WebClient.Builder webClientBuilder;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;
    private final AuditService auditService;

    public RoutingService(
            WebClient.Builder webClientBuilder,
            ReactiveCircuitBreakerFactory circuitBreakerFactory,
            AuditService auditService) {
        this.webClientBuilder = webClientBuilder;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.auditService = auditService;
    }

    /**
     * Routes a request to a microservice with circuit breaker protection.
     *
     * @param serviceName The name of the service to route to
     * @param path The path to route to
     * @param method The HTTP method to use
     * @param headers The headers to include
     * @param body The request body (optional)
     * @param <T> The type of the response body
     * @param <R> The type of the request body
     * @return A Mono containing the response
     */
    public <T, R> Mono<T> routeRequest(
            String serviceName,
            String path,
            String method,
            Map<String, String> headers,
            R body,
            Class<T> responseType) {
        
        String auditId = UUID.randomUUID().toString();
        
        // Log the request
        auditService.logRequest(auditId, serviceName, path, method, headers.get("X-User-Id"));
        
        // Create the WebClient request
        WebClient client = webClientBuilder.build();
        WebClient.RequestBodySpec requestSpec = client.method(org.springframework.http.HttpMethod.valueOf(method))
                .uri("lb://" + serviceName + path);
        
        // Add headers
        headers.forEach(requestSpec::header);
        requestSpec.header("X-Audit-Id", auditId);
        
        // Add body if present
        WebClient.RequestHeadersSpec<?> headersSpec;
        if (body != null) {
            headersSpec = requestSpec.bodyValue(body);
        } else {
            headersSpec = requestSpec;
        }
        
        // Execute the request with circuit breaker
        return headersSpec.retrieve()
                .bodyToMono(responseType)
                .transform(mono -> circuitBreakerFactory.create(serviceName)
                        .run(mono, throwable -> {
                            log.error("Circuit breaker triggered for service: {}", serviceName, throwable);
                            auditService.logError(auditId, serviceName, throwable.getMessage());
                            return Mono.error(new RuntimeException("Service unavailable: " + serviceName));
                        }))
                .doOnSuccess(response -> auditService.logResponse(auditId, serviceName, "SUCCESS"))
                .doOnError(error -> auditService.logError(auditId, serviceName, error.getMessage()));
    }
}

