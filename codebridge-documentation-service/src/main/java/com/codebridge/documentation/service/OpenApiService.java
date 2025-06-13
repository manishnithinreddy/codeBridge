package com.codebridge.documentation.service;

import com.codebridge.documentation.model.ServiceDefinition;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * Service for working with OpenAPI specifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenApiService {

    private final RestTemplate restTemplate;

    /**
     * Fetch OpenAPI specification from a service.
     *
     * @param service the service definition
     * @return the OpenAPI specification as a string
     */
    public String fetchOpenApiSpec(ServiceDefinition service) {
        String url = buildOpenApiUrl(service);
        log.info("Fetching OpenAPI spec from URL: {}", url);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String openApiSpec = response.getBody();
                if (validateOpenApiSpec(openApiSpec)) {
                    return openApiSpec;
                } else {
                    log.error("Invalid OpenAPI specification received from service: {}", service.getName());
                    return null;
                }
            } else {
                log.error("Failed to fetch OpenAPI spec from service: {}. Status code: {}", 
                        service.getName(), response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            log.error("Error fetching OpenAPI spec from service {}: {}", service.getName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Build the URL for fetching OpenAPI specification.
     *
     * @param service the service definition
     * @return the OpenAPI URL
     */
    private String buildOpenApiUrl(ServiceDefinition service) {
        String baseUrl = service.getUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String contextPath = service.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
            baseUrl += contextPath;
        }

        return baseUrl + "/v3/api-docs";
    }

    /**
     * Validate an OpenAPI specification.
     *
     * @param openApiSpec the OpenAPI specification as a string
     * @return true if the specification is valid, false otherwise
     */
    public boolean validateOpenApiSpec(String openApiSpec) {
        try {
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            options.setResolveFully(true);

            SwaggerParseResult result = new OpenAPIParser().readContents(openApiSpec, Collections.emptyList(), options);
            
            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                log.warn("OpenAPI validation warnings: {}", result.getMessages());
            }
            
            return result.getOpenAPI() != null;
        } catch (Exception e) {
            log.error("Error validating OpenAPI spec: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Parse an OpenAPI specification.
     *
     * @param openApiSpec the OpenAPI specification as a string
     * @return the parsed OpenAPI object
     */
    public OpenAPI parseOpenApiSpec(String openApiSpec) {
        try {
            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            options.setResolveFully(true);

            SwaggerParseResult result = new OpenAPIParser().readContents(openApiSpec, Collections.emptyList(), options);
            
            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                log.warn("OpenAPI parsing warnings: {}", result.getMessages());
            }
            
            return result.getOpenAPI();
        } catch (Exception e) {
            log.error("Error parsing OpenAPI spec: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Merge multiple OpenAPI specifications.
     *
     * @param specs the list of OpenAPI specifications
     * @return the merged OpenAPI specification
     */
    public String mergeOpenApiSpecs(String... specs) {
        // In a real implementation, this would merge multiple OpenAPI specs
        // For simplicity, we're just returning the first spec
        if (specs != null && specs.length > 0) {
            return specs[0];
        }
        return null;
    }
}

