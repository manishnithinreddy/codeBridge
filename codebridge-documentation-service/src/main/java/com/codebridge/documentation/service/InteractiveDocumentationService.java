package com.codebridge.documentation.service;

import com.codebridge.documentation.model.*;
import com.codebridge.documentation.repository.ApiDocumentationRepository;
import com.codebridge.documentation.repository.ApiExampleRepository;
import com.codebridge.documentation.repository.SearchIndexRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for interactive API documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InteractiveDocumentationService {

    private final ApiDocumentationRepository documentationRepository;
    private final ApiExampleRepository exampleRepository;
    private final SearchIndexRepository searchIndexRepository;
    private final OpenApiService openApiService;

    @Value("${documentation.interactive.enabled:true}")
    private boolean interactiveEnabled;

    @Value("${documentation.interactive.examples-enabled:true}")
    private boolean examplesEnabled;

    @Value("${documentation.interactive.search-enabled:true}")
    private boolean searchEnabled;

    /**
     * Scheduled task to build search index.
     */
    @Scheduled(cron = "0 0 */12 * * *") // Every 12 hours
    @Transactional
    public void buildSearchIndex() {
        if (!searchEnabled) {
            log.info("Search indexing is disabled. Skipping search index build.");
            return;
        }

        log.info("Building search index for all documentation");
        List<ApiDocumentation> allDocs = documentationRepository.findAll();

        // Clear existing index
        searchIndexRepository.deleteAll();

        for (ApiDocumentation doc : allDocs) {
            try {
                indexDocumentation(doc);
            } catch (Exception e) {
                log.error("Error indexing documentation {}: {}", doc.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Index an API documentation for search.
     *
     * @param documentation the API documentation to index
     */
    @Transactional
    public void indexDocumentation(ApiDocumentation documentation) {
        log.info("Indexing documentation for service: {} version: {}", 
                documentation.getService().getName(), documentation.getVersion().getName());

        try {
            OpenAPI openAPI = openApiService.parseOpenApiSpec(documentation.getOpenApiSpec());
            if (openAPI == null || openAPI.getPaths() == null) {
                log.warn("Invalid OpenAPI specification. Cannot index documentation.");
                return;
            }
            
            // Index service info
            indexServiceInfo(documentation, openAPI);
            
            // Index paths and operations
            for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();
                
                // Process each HTTP method
                indexOperation(documentation, path, "GET", pathItem.getGet());
                indexOperation(documentation, path, "POST", pathItem.getPost());
                indexOperation(documentation, path, "PUT", pathItem.getPut());
                indexOperation(documentation, path, "DELETE", pathItem.getDelete());
                indexOperation(documentation, path, "PATCH", pathItem.getPatch());
            }
        } catch (Exception e) {
            log.error("Error indexing documentation: {}", e.getMessage(), e);
        }
    }

    /**
     * Index service information.
     *
     * @param documentation the API documentation
     * @param openAPI the OpenAPI specification
     */
    private void indexServiceInfo(ApiDocumentation documentation, OpenAPI openAPI) {
        if (openAPI.getInfo() == null) {
            return;
        }
        
        SearchIndex index = new SearchIndex();
        index.setDocumentation(documentation);
        index.setType(SearchIndexType.SERVICE);
        index.setTitle(openAPI.getInfo().getTitle());
        index.setDescription(openAPI.getInfo().getDescription());
        index.setContent(openAPI.getInfo().getTitle() + " " + openAPI.getInfo().getDescription());
        index.setPath("/");
        index.setMethod(null);
        index.setCreatedAt(Instant.now());
        
        searchIndexRepository.save(index);
    }

    /**
     * Index an operation for search.
     *
     * @param documentation the API documentation
     * @param path the API path
     * @param method the HTTP method
     * @param operation the operation
     */
    private void indexOperation(ApiDocumentation documentation, String path, String method, Operation operation) {
        if (operation == null) {
            return;
        }
        
        SearchIndex index = new SearchIndex();
        index.setDocumentation(documentation);
        index.setType(SearchIndexType.ENDPOINT);
        index.setTitle(operation.getSummary());
        index.setDescription(operation.getDescription());
        
        // Build content from all relevant operation fields
        StringBuilder content = new StringBuilder();
        content.append(operation.getSummary()).append(" ");
        content.append(operation.getDescription()).append(" ");
        content.append(path).append(" ");
        content.append(method).append(" ");
        
        // Add parameter information
        if (operation.getParameters() != null) {
            for (Parameter parameter : operation.getParameters()) {
                content.append(parameter.getName()).append(" ");
                content.append(parameter.getDescription()).append(" ");
            }
        }
        
        // Add response information
        if (operation.getResponses() != null) {
            for (Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {
                content.append(responseEntry.getKey()).append(" ");
                content.append(responseEntry.getValue().getDescription()).append(" ");
            }
        }
        
        index.setContent(content.toString());
        index.setPath(path);
        index.setMethod(method);
        index.setCreatedAt(Instant.now());
        
        searchIndexRepository.save(index);
    }

    /**
     * Search the API documentation.
     *
     * @param query the search query
     * @return the list of search results
     */
    public List<SearchResult> search(String query) {
        if (!searchEnabled || query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // Split query into terms
        String[] terms = query.toLowerCase().split("\\s+");
        
        // Find matching index entries
        List<SearchIndex> matches = searchIndexRepository.findByContentContainingIgnoreCase(query);
        
        // Add entries that match individual terms
        for (String term : terms) {
            if (term.length() >= 3) { // Only search for terms with at least 3 characters
                List<SearchIndex> termMatches = searchIndexRepository.findByContentContainingIgnoreCase(term);
                for (SearchIndex match : termMatches) {
                    if (!matches.contains(match)) {
                        matches.add(match);
                    }
                }
            }
        }
        
        // Convert to search results
        return matches.stream()
                .map(this::convertToSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * Convert a search index to a search result.
     *
     * @param index the search index
     * @return the search result
     */
    private SearchResult convertToSearchResult(SearchIndex index) {
        SearchResult result = new SearchResult();
        result.setId(index.getId());
        result.setType(index.getType());
        result.setTitle(index.getTitle());
        result.setDescription(index.getDescription());
        result.setPath(index.getPath());
        result.setMethod(index.getMethod());
        result.setServiceName(index.getDocumentation().getService().getName());
        result.setVersionName(index.getDocumentation().getVersion().getName());
        return result;
    }

    /**
     * Generate request and response examples for an API documentation.
     *
     * @param documentation the API documentation
     * @return the list of generated examples
     */
    @Transactional
    public List<ApiExample> generateExamples(ApiDocumentation documentation) {
        if (!examplesEnabled) {
            log.info("Examples generation is disabled. Skipping examples generation.");
            return Collections.emptyList();
        }

        log.info("Generating examples for service: {} version: {}", 
                documentation.getService().getName(), documentation.getVersion().getName());

        List<ApiExample> examples = new ArrayList<>();
        
        try {
            OpenAPI openAPI = openApiService.parseOpenApiSpec(documentation.getOpenApiSpec());
            if (openAPI == null || openAPI.getPaths() == null) {
                log.warn("Invalid OpenAPI specification. Cannot generate examples.");
                return Collections.emptyList();
            }
            
            // Generate examples for each endpoint
            for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();
                
                // Process each HTTP method
                processExampleForOperation(documentation, examples, path, "GET", pathItem.getGet());
                processExampleForOperation(documentation, examples, path, "POST", pathItem.getPost());
                processExampleForOperation(documentation, examples, path, "PUT", pathItem.getPut());
                processExampleForOperation(documentation, examples, path, "DELETE", pathItem.getDelete());
                processExampleForOperation(documentation, examples, path, "PATCH", pathItem.getPatch());
            }
            
            return examples;
        } catch (Exception e) {
            log.error("Error generating examples: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Process an operation to generate examples.
     *
     * @param documentation the API documentation
     * @param examples the list of examples
     * @param path the API path
     * @param method the HTTP method
     * @param operation the operation
     */
    private void processExampleForOperation(ApiDocumentation documentation, List<ApiExample> examples, 
                                          String path, String method, Operation operation) {
        if (operation == null) {
            return;
        }
        
        String operationId = operation.getOperationId();
        if (operationId == null) {
            operationId = method.toLowerCase() + path.replaceAll("[^a-zA-Z0-9]", "");
        }
        
        try {
            // Generate request example
            String requestExample = generateRequestExample(operation);
            
            // Generate response example
            String responseExample = generateResponseExample(operation);
            
            if ((requestExample != null && !requestExample.isEmpty()) || 
                (responseExample != null && !responseExample.isEmpty())) {
                
                ApiExample example = createOrUpdateExample(documentation, operationId, path, method, 
                        requestExample, responseExample);
                examples.add(example);
            }
        } catch (Exception e) {
            log.error("Error generating example for {}.{}: {}", path, method, e.getMessage(), e);
        }
    }

    /**
     * Generate a request example for an operation.
     *
     * @param operation the operation
     * @return the generated request example
     */
    private String generateRequestExample(Operation operation) {
        if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
            return null;
        }
        
        Content content = operation.getRequestBody().getContent();
        
        // Try to get JSON example
        MediaType jsonMediaType = content.get("application/json");
        if (jsonMediaType != null) {
            if (jsonMediaType.getExample() != null) {
                return jsonMediaType.getExample().toString();
            } else if (jsonMediaType.getSchema() != null) {
                return generateExampleFromSchema(jsonMediaType.getSchema());
            }
        }
        
        // Try to get XML example
        MediaType xmlMediaType = content.get("application/xml");
        if (xmlMediaType != null) {
            if (xmlMediaType.getExample() != null) {
                return xmlMediaType.getExample().toString();
            } else if (xmlMediaType.getSchema() != null) {
                return generateExampleFromSchema(xmlMediaType.getSchema());
            }
        }
        
        return null;
    }

    /**
     * Generate a response example for an operation.
     *
     * @param operation the operation
     * @return the generated response example
     */
    private String generateResponseExample(Operation operation) {
        if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
            return null;
        }
        
        // Try to get 200 OK response
        ApiResponse okResponse = operation.getResponses().get("200");
        if (okResponse == null) {
            // Try to get any 2xx response
            for (String key : operation.getResponses().keySet()) {
                if (key.startsWith("2")) {
                    okResponse = operation.getResponses().get(key);
                    break;
                }
            }
        }
        
        if (okResponse == null || okResponse.getContent() == null) {
            return null;
        }
        
        Content content = okResponse.getContent();
        
        // Try to get JSON example
        MediaType jsonMediaType = content.get("application/json");
        if (jsonMediaType != null) {
            if (jsonMediaType.getExample() != null) {
                return jsonMediaType.getExample().toString();
            } else if (jsonMediaType.getSchema() != null) {
                return generateExampleFromSchema(jsonMediaType.getSchema());
            }
        }
        
        // Try to get XML example
        MediaType xmlMediaType = content.get("application/xml");
        if (xmlMediaType != null) {
            if (xmlMediaType.getExample() != null) {
                return xmlMediaType.getExample().toString();
            } else if (xmlMediaType.getSchema() != null) {
                return generateExampleFromSchema(xmlMediaType.getSchema());
            }
        }
        
        return null;
    }

    /**
     * Generate an example from a schema.
     *
     * @param schema the schema
     * @return the generated example
     */
    private String generateExampleFromSchema(Schema<?> schema) {
        // In a real implementation, this would generate an example based on the schema
        // For simplicity, we're just returning a placeholder
        if (schema.getType() == null) {
            return "{}";
        }
        
        switch (schema.getType()) {
            case "object":
                return "{\n  \"property1\": \"value1\",\n  \"property2\": \"value2\"\n}";
            case "array":
                return "[\n  {\n    \"property\": \"value\"\n  }\n]";
            case "string":
                return "\"example\"";
            case "integer":
            case "number":
                return "123";
            case "boolean":
                return "true";
            default:
                return "{}";
        }
    }

    /**
     * Create or update an API example.
     *
     * @param documentation the API documentation
     * @param operationId the operation ID
     * @param path the API path
     * @param method the HTTP method
     * @param requestExample the request example
     * @param responseExample the response example
     * @return the created or updated API example
     */
    private ApiExample createOrUpdateExample(ApiDocumentation documentation, String operationId, 
                                           String path, String method, String requestExample, String responseExample) {
        Optional<ApiExample> existingExample = exampleRepository.findByDocumentationAndOperationId(
                documentation, operationId);
        
        if (existingExample.isPresent()) {
            ApiExample example = existingExample.get();
            example.setRequestExample(requestExample);
            example.setResponseExample(responseExample);
            example.setUpdatedAt(Instant.now());
            return exampleRepository.save(example);
        } else {
            ApiExample example = new ApiExample();
            example.setDocumentation(documentation);
            example.setOperationId(operationId);
            example.setPath(path);
            example.setMethod(method);
            example.setRequestExample(requestExample);
            example.setResponseExample(responseExample);
            example.setCreatedAt(Instant.now());
            example.setUpdatedAt(Instant.now());
            return exampleRepository.save(example);
        }
    }

    /**
     * Get examples for an API documentation.
     *
     * @param documentation the API documentation
     * @return the list of API examples
     */
    public List<ApiExample> getExamples(ApiDocumentation documentation) {
        return exampleRepository.findByDocumentation(documentation);
    }

    /**
     * Get an example by ID.
     *
     * @param id the example ID
     * @return the API example
     */
    public ApiExample getExampleById(UUID id) {
        return exampleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Example not found with ID: " + id));
    }

    /**
     * Update an API example.
     *
     * @param id the example ID
     * @param requestExample the new request example
     * @param responseExample the new response example
     * @return the updated API example
     */
    @Transactional
    public ApiExample updateExample(UUID id, String requestExample, String responseExample) {
        ApiExample example = getExampleById(id);
        example.setRequestExample(requestExample);
        example.setResponseExample(responseExample);
        example.setUpdatedAt(Instant.now());
        return exampleRepository.save(example);
    }

    /**
     * Delete an API example.
     *
     * @param id the example ID
     */
    @Transactional
    public void deleteExample(UUID id) {
        exampleRepository.deleteById(id);
    }
}

