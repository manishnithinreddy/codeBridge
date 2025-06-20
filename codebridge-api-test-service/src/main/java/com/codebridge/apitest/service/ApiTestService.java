package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.ApiTestRequest;
import com.codebridge.apitest.dto.ApiTestResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.ApiTest;
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
import org.springframework.http.HttpMethod;
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
    private final EnvironmentService environmentService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TestSnapshotService testSnapshotService;

    @Autowired
    public ApiTestService(
            ApiTestRepository apiTestRepository,
            TestResultRepository testResultRepository,
            EnvironmentService environmentService,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            TestSnapshotService testSnapshotService) {
        this.apiTestRepository = apiTestRepository;
        this.testResultRepository = testResultRepository;
        this.environmentService = environmentService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.testSnapshotService = testSnapshotService;
    }

    /**
     * Get all API tests for a project.
     *
     * @param projectId the project ID
     * @return list of API test responses
     */
    public List<ApiTestResponse> getApiTests(Long projectId) {
        List<ApiTest> tests = apiTestRepository.findByProjectId(projectId);
        return tests.stream()
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
    public ApiTestResponse getApiTest(Long id, Long projectId) {
        ApiTest test = apiTestRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("API test not found"));
        return mapToResponse(test);
    }

    /**
     * Create a new API test.
     *
     * @param request the API test request
     * @param projectId the project ID
     * @return the created API test response
     */
    public ApiTestResponse createApiTest(ApiTestRequest request, Long projectId) {
        ApiTest test = new ApiTest();
        test.setName(request.getName());
        test.setDescription(request.getDescription());
        test.setProjectId(projectId);
        test.setMethod(request.getMethod());
        test.setProtocol(request.getProtocol());
        test.setEndpoint(request.getEndpoint());
        test.setRequestBody(request.getRequestBody());
        
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
            throw new RuntimeException("Error processing JSON", e);
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
    public ApiTestResponse updateApiTest(Long id, ApiTestRequest request, Long projectId) {
        ApiTest test = apiTestRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("API test not found"));
        
        test.setName(request.getName());
        test.setDescription(request.getDescription());
        test.setMethod(request.getMethod());
        test.setProtocol(request.getProtocol());
        test.setEndpoint(request.getEndpoint());
        test.setRequestBody(request.getRequestBody());
        
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
            throw new RuntimeException("Error processing JSON", e);
        }
        
        ApiTest updatedTest = apiTestRepository.save(test);
        return mapToResponse(updatedTest);
    }

    /**
     * Delete an API test.
     *
     * @param id the API test ID
     * @param projectId the project ID
     * @throws ResourceNotFoundException if the API test is not found
     */
    public void deleteApiTest(Long id, Long projectId) {
        ApiTest test = apiTestRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("API test not found"));
        apiTestRepository.delete(test);
    }

    /**
     * Execute an API test.
     *
     * @param id the API test ID
     * @param projectId the project ID
     * @param userId the user ID
     * @param environmentId the environment ID
     * @param additionalVariables additional variables to use
     * @return the test result response
     * @throws ResourceNotFoundException if the API test is not found
     */
    public TestResultResponse executeTest(Long id, Long projectId, Long userId, Long environmentId, Map<String, String> additionalVariables) {
        ApiTest test;
        if (projectId != null) {
            test = apiTestRepository.findByIdAndProjectId(id, projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("API test not found"));
        } else {
            test = apiTestRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("API test not found"));
            projectId = test.getProjectId();
        }
        
        // Get environment
        var environment = environmentId != null
                ? environmentService.getEnvironment(environmentId, projectId)
                : environmentService.getDefaultEnvironment(projectId);
        
        // Prepare variables
        Map<String, String> variables = new HashMap<>();
        if (environment.getVariables() != null) {
            variables.putAll(environment.getVariables());
        }
        if (additionalVariables != null) {
            variables.putAll(additionalVariables);
        }
        
        // Prepare request
        String url = buildUrl(test, environment.getBaseUrl(), variables);
        HttpMethod method = HttpMethod.valueOf(test.getMethod().name());
        HttpHeaders headers = buildHeaders(test, environment.getHeaders(), variables);
        String body = replaceVariables(test.getRequestBody(), variables);
        
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        
        // Execute request
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response;
        String responseBody = null;
        int statusCode = 0;
        Map<String, String> responseHeaders = new HashMap<>();
        
        try {
            response = restTemplate.exchange(url, method, requestEntity, String.class);
            responseBody = response.getBody();
            statusCode = response.getStatusCode().value();
            response.getHeaders().forEach((key, values) -> 
                    responseHeaders.put(key, String.join(", ", values)));
        } catch (HttpStatusCodeException e) {
            responseBody = e.getResponseBodyAsString();
            statusCode = e.getStatusCode().value();
            e.getResponseHeaders().forEach((key, values) -> 
                    responseHeaders.put(key, String.join(", ", values)));
        } catch (Exception e) {
            logger.error("Error executing test", e);
            responseBody = "Error: " + e.getMessage();
            statusCode = 500;
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Save test result
        TestResult result = new TestResult();
        result.setTestId(test.getId());
        result.setEnvironmentId(environment.getId());
        result.setUserId(userId);
        result.setRequestUrl(url);
        result.setRequestMethod(test.getMethod());
        result.setRequestBody(body);
        
        try {
            result.setRequestHeaders(objectMapper.writeValueAsString(headers.toSingleValueMap()));
            result.setResponseHeaders(objectMapper.writeValueAsString(responseHeaders));
        } catch (JsonProcessingException e) {
            logger.error("Error serializing headers", e);
        }
        
        result.setResponseBody(responseBody);
        result.setResponseStatus(statusCode);
        result.setResponseTime(duration);
        
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
        String endpoint = replaceVariables(test.getEndpoint(), variables);
        
        // If baseUrl already has protocol, don't add it again
        if (baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            protocol = "";
        }
        
        // Remove trailing slash from baseUrl and leading slash from endpoint
        if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        } else if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        
        String url = protocol + baseUrl + endpoint;
        
        // Add query parameters
        try {
            if (test.getRequestParams() != null && !test.getRequestParams().isEmpty()) {
                Map<String, String> params = objectMapper.readValue(
                        test.getRequestParams(), new TypeReference<Map<String, String>>() {});
                
                if (!params.isEmpty()) {
                    StringBuilder queryString = new StringBuilder();
                    queryString.append(url.contains("?") ? "&" : "?");
                    
                    boolean first = true;
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        if (!first) {
                            queryString.append("&");
                        }
                        String value = replaceVariables(entry.getValue(), variables);
                        queryString.append(entry.getKey()).append("=").append(value);
                        first = false;
                    }
                    
                    url += queryString.toString();
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing request parameters", e);
        }
        
        return url;
    }

    /**
     * Build headers for a test.
     *
     * @param test the API test
     * @param environmentHeaders the environment headers
     * @param variables the variables
     * @return the HTTP headers
     */
    private HttpHeaders buildHeaders(ApiTest test, Map<String, String> environmentHeaders, Map<String, String> variables) {
        HttpHeaders headers = new HttpHeaders();
        
        // Add environment headers
        if (environmentHeaders != null) {
            environmentHeaders.forEach((key, value) -> 
                    headers.add(key, replaceVariables(value, variables)));
        }
        
        // Add test-specific headers
        try {
            if (test.getRequestHeaders() != null && !test.getRequestHeaders().isEmpty()) {
                Map<String, String> testHeaders = objectMapper.readValue(
                        test.getRequestHeaders(), new TypeReference<Map<String, String>>() {});
                
                testHeaders.forEach((key, value) -> 
                        headers.add(key, replaceVariables(value, variables)));
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing request headers", e);
        }
        
        return headers;
    }

    /**
     * Replace variables in a string.
     *
     * @param input the input string
     * @param variables the variables
     * @return the string with variables replaced
     */
    private String replaceVariables(String input, Map<String, String> variables) {
        if (input == null || variables == null || variables.isEmpty()) {
            return input;
        }
        
        String result = input;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue());
        }
        
        return result;
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
            // No assertions, consider it passed
            return true;
        }
        
        try {
            List<Map<String, Object>> assertions = objectMapper.readValue(
                    test.getAssertions(), new TypeReference<List<Map<String, Object>>>() {});
            
            if (assertions.isEmpty()) {
                return true;
            }
            
            for (Map<String, Object> assertion : assertions) {
                String type = (String) assertion.get("type");
                
                if ("status".equals(type)) {
                    int expectedStatus = ((Number) assertion.get("value")).intValue();
                    if (result.getResponseStatus() != expectedStatus) {
                        return false;
                    }
                } else if ("responseTime".equals(type)) {
                    long maxTime = ((Number) assertion.get("value")).longValue();
                    if (result.getResponseTime() > maxTime) {
                        return false;
                    }
                } else if ("contains".equals(type)) {
                    String value = (String) assertion.get("value");
                    if (result.getResponseBody() == null || !result.getResponseBody().contains(value)) {
                        return false;
                    }
                } else if ("jsonPath".equals(type)) {
                    // JSON path assertions would require a JSON path library
                    // This is a placeholder for that functionality
                    logger.warn("JSON path assertions not implemented yet");
                }
            }
            
            return true;
        } catch (JsonProcessingException e) {
            logger.error("Error parsing assertions", e);
            return false;
        }
    }

    /**
     * Map an API test entity to a response DTO.
     *
     * @param test the API test entity
     * @return the API test response DTO
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
     * Map a test result entity to a response DTO.
     *
     * @param result the test result entity
     * @return the test result response DTO
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

