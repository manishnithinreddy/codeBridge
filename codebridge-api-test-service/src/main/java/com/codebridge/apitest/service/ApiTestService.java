package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.ApiTestRequest;
import com.codebridge.apitest.dto.ApiTestResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.ApiTest;
import com.codebridge.apitest.model.HttpMethod;
import com.codebridge.apitest.model.ProtocolType;
import com.codebridge.apitest.model.TestResult;
import com.codebridge.apitest.repository.ApiTestRepository;
import com.codebridge.apitest.repository.TestResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for API test operations.
 */
@Service
public class ApiTestService {

    private static final Logger logger = LoggerFactory.getLogger(ApiTestService.class);

    private final ApiTestRepository apiTestRepository;
    private final TestResultRepository testResultRepository;
    private final TestSnapshotService testSnapshotService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public ApiTestService(ApiTestRepository apiTestRepository, 
                         TestResultRepository testResultRepository,
                         TestSnapshotService testSnapshotService,
                         ObjectMapper objectMapper,
                         RestTemplate restTemplate) {
        this.apiTestRepository = apiTestRepository;
        this.testResultRepository = testResultRepository;
        this.testSnapshotService = testSnapshotService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    /**
     * Get all API tests for a project.
     *
     * @param projectId the project ID
     * @return list of API test responses
     */
    public List<ApiTestResponse> getAllTests(Long projectId) {
        return apiTestRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get an API test by ID.
     *
     * @param id the API test ID
     * @param projectId the project ID
     * @return the API test response
     * @throws ResourceNotFoundException if the API test is not found
     */
    public ApiTestResponse getTestById(Long id, Long projectId) {
        ApiTest test = apiTestRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id));
        
        return mapToResponse(test);
    }

    /**
     * Create a new API test.
     *
     * @param request the API test request
     * @param projectId the project ID
     * @return the created API test response
     */
    public ApiTestResponse createTest(ApiTestRequest request, Long projectId) {
        ApiTest test = new ApiTest();
        test.setName(request.getName());
        test.setDescription(request.getDescription());
        test.setProjectId(projectId);
        test.setMethod(request.getMethod());
        test.setProtocol(request.getProtocol());
        test.setEndpoint(request.getEndpoint());
        test.setRequestBody(request.getRequestBody());
        test.setCreatedAt(LocalDateTime.now());
        test.setUpdatedAt(LocalDateTime.now());
        
        try {
            if (request.getRequestHeaders() != null) {
                test.setRequestHeaders(objectMapper.writeValueAsString(request.getRequestHeaders()));
            }
            if (request.getRequestParams() != null) {
                test.setRequestParams(objectMapper.writeValueAsString(request.getRequestParams()));
            }
            if (request.getAssertions() != null) {
                test.setAssertions(objectMapper.writeValueAsString(request.getAssertions()));
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing JSON", e);
        }
        
        ApiTest savedTest = apiTestRepository.save(test);
        return mapToResponse(savedTest);
    }

    /**
     * Update an API test.
     *
     * @param id the API test ID
     * @param request the API test request
     * @param projectId the project ID
     * @return the updated API test response
     * @throws ResourceNotFoundException if the API test is not found
     */
    public ApiTestResponse updateTest(Long id, ApiTestRequest request, Long projectId) {
        ApiTest test = apiTestRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id));
        
        test.setName(request.getName());
        test.setDescription(request.getDescription());
        test.setMethod(request.getMethod());
        test.setProtocol(request.getProtocol());
        test.setEndpoint(request.getEndpoint());
        test.setRequestBody(request.getRequestBody());
        test.setUpdatedAt(LocalDateTime.now());
        
        try {
            if (request.getRequestHeaders() != null) {
                test.setRequestHeaders(objectMapper.writeValueAsString(request.getRequestHeaders()));
            }
            if (request.getRequestParams() != null) {
                test.setRequestParams(objectMapper.writeValueAsString(request.getRequestParams()));
            }
            if (request.getAssertions() != null) {
                test.setAssertions(objectMapper.writeValueAsString(request.getAssertions()));
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing JSON", e);
        }
        
        ApiTest savedTest = apiTestRepository.save(test);
        return mapToResponse(savedTest);
    }

    /**
     * Delete an API test.
     *
     * @param id the API test ID
     * @param projectId the project ID
     * @throws ResourceNotFoundException if the API test is not found
     */
    public void deleteTest(Long id, Long projectId) {
        ApiTest test = apiTestRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id));
        
        apiTestRepository.delete(test);
    }

    /**
     * Execute an API test.
     *
     * @param id the API test ID
     * @param projectId the project ID
     * @param userId the user ID
     * @param environmentId the environment ID
     * @param additionalVariables additional variables for the test
     * @return the test result response
     * @throws ResourceNotFoundException if the API test is not found
     */
    public TestResultResponse executeTest(Long id, Long projectId, Long userId, Long environmentId, Map<String, String> additionalVariables) {
        // Get the test
        ApiTest test = apiTestRepository.findByIdAndProjectId(id, projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id));
        
        // Get the environment
        String baseUrl = "http://localhost:8080"; // Default for local testing
        if (environmentId != null) {
            // In a real implementation, we would get the environment URL from the environment service
            baseUrl = "http://environment-" + environmentId + ".example.com";
        }
        
        // Build the full URL with variables
        String url = buildUrl(test, baseUrl, additionalVariables);
        
        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        try {
            if (test.getRequestHeaders() != null && !test.getRequestHeaders().isEmpty()) {
                Map<String, String> testHeaders = objectMapper.readValue(
                        test.getRequestHeaders(), new TypeReference<Map<String, String>>() {});
                testHeaders.forEach(headers::add);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing headers", e);
        }
        
        // Prepare request body
        String body = test.getRequestBody();
        if (body != null && !body.isEmpty() && additionalVariables != null) {
            // Replace variables in body
            for (Map.Entry<String, String> entry : additionalVariables.entrySet()) {
                body = body.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        
        // Create test result
        TestResult result = new TestResult();
        result.setTestId(test.getId());
        result.setEnvironmentId(environmentId);
        result.setUserId(userId);
        result.setRequestUrl(url);
        result.setRequestMethod(test.getMethod());
        result.setRequestBody(body);
        result.setCreatedAt(LocalDateTime.now());
        
        try {
            result.setRequestHeaders(objectMapper.writeValueAsString(headers.toSingleValueMap()));
        } catch (JsonProcessingException e) {
            logger.error("Error serializing headers", e);
        }
        
        // Execute the request
        long startTime = System.currentTimeMillis();
        try {
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            org.springframework.http.HttpMethod httpMethod = convertToSpringHttpMethod(test.getMethod());
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, httpMethod, requestEntity, String.class);
            
            long endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            result.setResponseStatus(response.getStatusCode().value());
            result.setResponseBody(response.getBody());
            
            try {
                result.setResponseHeaders(objectMapper.writeValueAsString(response.getHeaders()));
            } catch (JsonProcessingException e) {
                logger.error("Error serializing response headers", e);
            }
            
        } catch (HttpStatusCodeException e) {
            // Handle HTTP error responses
            long endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            result.setResponseStatus(e.getRawStatusCode());
            result.setResponseBody(e.getResponseBodyAsString());
            
            try {
                result.setResponseHeaders(objectMapper.writeValueAsString(e.getResponseHeaders()));
            } catch (JsonProcessingException ex) {
                logger.error("Error serializing error response headers", ex);
            }
        } catch (Exception e) {
            // Handle other exceptions
            long endTime = System.currentTimeMillis();
            result.setResponseTime(endTime - startTime);
            result.setResponseStatus(500);
            result.setResponseBody("Error executing request: " + e.getMessage());
            logger.error("Error executing request", e);
        }
        
        // Evaluate assertions
        boolean passed = evaluateAssertions(test, result);
        result.setPassed(passed);
        
        TestResult savedResult = testResultRepository.save(result);
        
        // Map to response
        TestResultResponse resultResponse = mapResultToResponse(savedResult);
        
        // Compare with snapshot if available
        Map<String, Object> snapshotComparison = testSnapshotService.compareWithSnapshot(test.getId(), savedResult);
        resultResponse.setSnapshotComparison(snapshotComparison);
        
        return resultResponse;
    }

    /**
     * Build the full URL for a test.
     *
     * @param test the API test
     * @param baseUrl the base URL
     * @param variables the variables
     * @return the full URL
     */
    private String buildUrl(ApiTest test, String baseUrl, Map<String, String> variables) {
        String protocol = test.getProtocol() == ProtocolType.HTTPS ? "https://" : "http://";
        if (baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            protocol = "";
        }
        
        String url = protocol + baseUrl;
        if (!url.endsWith("/") && !test.getEndpoint().startsWith("/")) {
            url += "/";
        }
        url += test.getEndpoint();
        
        // Add query parameters if any
        try {
            if (test.getRequestParams() != null && !test.getRequestParams().isEmpty()) {
                Map<String, String> params = objectMapper.readValue(
                        test.getRequestParams(), new TypeReference<Map<String, String>>() {});
                
                if (!params.isEmpty()) {
                    url += "?";
                    boolean first = true;
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        if (!first) {
                            url += "&";
                        }
                        String value = entry.getValue();
                        if (variables != null && value.startsWith("{{") && value.endsWith("}}")) {
                            String key = value.substring(2, value.length() - 2);
                            if (variables.containsKey(key)) {
                                value = variables.get(key);
                            }
                        }
                        url += entry.getKey() + "=" + value;
                        first = false;
                    }
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing query parameters", e);
        }
        
        return url;
    }

    /**
     * Convert API test method to Spring HTTP method.
     *
     * @param method the API test method
     * @return the Spring HTTP method
     */
    private org.springframework.http.HttpMethod convertToSpringHttpMethod(HttpMethod method) {
        return switch (method) {
            case GET -> org.springframework.http.HttpMethod.GET;
            case POST -> org.springframework.http.HttpMethod.POST;
            case PUT -> org.springframework.http.HttpMethod.PUT;
            case DELETE -> org.springframework.http.HttpMethod.DELETE;
            case PATCH -> org.springframework.http.HttpMethod.PATCH;
            case HEAD -> org.springframework.http.HttpMethod.HEAD;
            case OPTIONS -> org.springframework.http.HttpMethod.OPTIONS;
        };
    }

    /**
     * Evaluate assertions for a test result.
     *
     * @param test the API test
     * @param result the test result
     * @return true if all assertions pass
     */
    private boolean evaluateAssertions(ApiTest test, TestResult result) {
        if (test.getAssertions() == null || test.getAssertions().isEmpty()) {
            return true; // No assertions to evaluate
        }
        
        try {
            List<Map<String, Object>> assertions = objectMapper.readValue(
                    test.getAssertions(), new TypeReference<List<Map<String, Object>>>() {});
            
            for (Map<String, Object> assertion : assertions) {
                String type = (String) assertion.get("type");
                if (type == null) continue;
                
                switch (type) {
                    case "status":
                        Integer expectedStatus = (Integer) assertion.get("value");
                        if (expectedStatus != null && !expectedStatus.equals(result.getResponseStatus())) {
                            return false;
                        }
                        break;
                    case "contains":
                        String expectedText = (String) assertion.get("value");
                        if (expectedText != null && result.getResponseBody() != null && 
                                !result.getResponseBody().contains(expectedText)) {
                            return false;
                        }
                        break;
                    // Add more assertion types as needed
                }
            }
            
            return true;
        } catch (JsonProcessingException e) {
            logger.error("Error parsing assertions", e);
            return false;
        }
    }

    /**
     * Map an API test to a response DTO.
     *
     * @param test the API test
     * @return the API test response
     */
    private ApiTestResponse mapToResponse(ApiTest test) {
        ApiTestResponse response = new ApiTestResponse();
        response.setId(test.getId());
        response.setName(test.getName());
        response.setDescription(test.getDescription());
        response.setProjectId(test.getProjectId());
        response.setMethod(test.getMethod());
        response.setProtocol(test.getProtocol());
        response.setEndpoint(test.getEndpoint());
        response.setRequestBody(test.getRequestBody());
        response.setCreatedAt(test.getCreatedAt());
        response.setUpdatedAt(test.getUpdatedAt());
        
        try {
            if (test.getRequestHeaders() != null && !test.getRequestHeaders().isEmpty()) {
                response.setRequestHeaders(objectMapper.readValue(
                        test.getRequestHeaders(), new TypeReference<Map<String, String>>() {}));
            }
            if (test.getRequestParams() != null && !test.getRequestParams().isEmpty()) {
                response.setRequestParams(objectMapper.readValue(
                        test.getRequestParams(), new TypeReference<Map<String, String>>() {}));
            }
            if (test.getAssertions() != null && !test.getAssertions().isEmpty()) {
                response.setAssertions(objectMapper.readValue(
                        test.getAssertions(), new TypeReference<List<Map<String, Object>>>() {}));
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing JSON", e);
        }
        
        return response;
    }

    /**
     * Map a test result to a response DTO.
     *
     * @param result the test result
     * @return the test result response
     */
    private TestResultResponse mapResultToResponse(TestResult result) {
        TestResultResponse response = new TestResultResponse();
        response.setId(result.getId());
        response.setTestId(result.getTestId());
        response.setEnvironmentId(result.getEnvironmentId());
        response.setUserId(result.getUserId());
        response.setRequestUrl(result.getRequestUrl());
        response.setRequestMethod(result.getRequestMethod());
        response.setRequestBody(result.getRequestBody());
        response.setResponseBody(result.getResponseBody());
        response.setResponseStatus(result.getResponseStatus());
        response.setResponseTime(result.getResponseTime());
        response.setPassed(result.getPassed());
        response.setCreatedAt(result.getCreatedAt());
        
        try {
            if (result.getRequestHeaders() != null && !result.getRequestHeaders().isEmpty()) {
                response.setRequestHeaders(objectMapper.readValue(
                        result.getRequestHeaders(), new TypeReference<Map<String, String>>() {}));
            }
            if (result.getResponseHeaders() != null && !result.getResponseHeaders().isEmpty()) {
                response.setResponseHeaders(objectMapper.readValue(
                        result.getResponseHeaders(), new TypeReference<Map<String, String>>() {}));
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing JSON", e);
        }
        
        return response;
    }
}

