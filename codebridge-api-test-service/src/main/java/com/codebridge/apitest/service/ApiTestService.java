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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for API tests.
 */
@Service
public class ApiTestService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiTestService.class);
    
    @Autowired
    private ApiTestRepository apiTestRepository;
    
    @Autowired
    private TestResultRepository testResultRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * Creates a new API test.
     *
     * @param request the API test request
     * @param projectId the project ID
     * @param userId the user ID
     * @return the created API test
     */
    public ApiTestResponse createApiTest(ApiTestRequest request, Long projectId, Long userId) {
        // Validate project exists
        // projectRepository.findById(projectId)
        //     .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));
        
        // Create API test
        ApiTest apiTest = new ApiTest();
        // ID will be auto-generated
        apiTest.setName(request.getName());
        apiTest.setProjectId(projectId);
        apiTest.setProtocolType(request.getProtocolType());
        apiTest.setUrl(request.getUrl());
        apiTest.setMethod(request.getMethod());
        
        try {
            if (request.getHeaders() != null) {
                apiTest.setHeaders(objectMapper.readValue(request.getHeaders(), new TypeReference<Map<String, String>>() {}));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing headers JSON", e);
        }
        
        apiTest.setRequestBody(request.getRequestBody());
        apiTest.setGraphqlQuery(request.getGraphqlQuery());
        apiTest.setGraphqlVariables(request.getGraphqlVariables());
        apiTest.setGrpcRequest(request.getGrpcRequest());
        apiTest.setGrpcServiceDefinition(request.getGrpcServiceDefinition());
        apiTest.setExpectedStatusCode(request.getExpectedStatusCode());
        // apiTest.setExpectedResponseBody(request.getExpectedResponseBody());
        // apiTest.setPreRequestScript(request.getPreRequestScript());
        // apiTest.setPostRequestScript(request.getPostRequestScript());
        // apiTest.setValidationScript(request.getValidationScript());
        apiTest.setTimeoutMs(request.getTimeoutMs());
        apiTest.setActive(true);
        
        ApiTest savedTest = apiTestRepository.save(apiTest);
        return mapToApiTestResponse(savedTest);
    }
    
    /**
     * Updates an API test.
     *
     * @param id the API test ID
     * @param request the API test request
     * @param userId the user ID
     * @return the updated API test
     */
    public ApiTestResponse updateApiTest(Long id, ApiTestRequest request, Long userId) {
        ApiTest test = apiTestRepository.findByIdAndProjectId(id, request.getProjectId())
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id.toString()));
        
        // Update fields
        test.setName(request.getName());
        test.setProtocolType(request.getProtocolType());
        test.setUrl(request.getUrl());
        test.setMethod(request.getMethod());
        
        try {
            if (request.getHeaders() != null) {
                test.setHeaders(objectMapper.readValue(request.getHeaders(), new TypeReference<Map<String, String>>() {}));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing headers JSON", e);
        }
        
        test.setRequestBody(request.getRequestBody());
        test.setGraphqlQuery(request.getGraphqlQuery());
        test.setGraphqlVariables(request.getGraphqlVariables());
        test.setGrpcRequest(request.getGrpcRequest());
        test.setGrpcServiceDefinition(request.getGrpcServiceDefinition());
        test.setExpectedStatusCode(request.getExpectedStatusCode());
        // test.setExpectedResponseBody(request.getExpectedResponseBody());
        // test.setPreRequestScript(request.getPreRequestScript());
        // test.setPostRequestScript(request.getPostRequestScript());
        // test.setValidationScript(request.getValidationScript());
        test.setTimeoutMs(request.getTimeoutMs());
        
        ApiTest updatedTest = apiTestRepository.save(test);
        return mapToApiTestResponse(updatedTest);
    }
    
    /**
     * Gets all API tests for a project.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @return the list of API tests
     */
    public List<ApiTestResponse> getApiTests(Long projectId, Long userId) {
        // Validate project exists and user has access
        // projectRepository.findById(projectId)
        //     .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId.toString()));
        
        List<ApiTest> tests = apiTestRepository.findByProjectId(projectId);
        return tests.stream()
            .map(this::mapToApiTestResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets an API test by ID.
     *
     * @param id the API test ID
     * @param userId the user ID
     * @return the API test
     */
    public ApiTestResponse getApiTest(Long id, Long userId) {
        ApiTest test = apiTestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id.toString()));
        
        // Validate user has access to the project
        // projectRepository.findById(test.getProjectId())
        //     .orElseThrow(() -> new ResourceNotFoundException("Project", "id", test.getProjectId().toString()));
        
        return mapToApiTestResponse(test);
    }
    
    /**
     * Executes a test.
     *
     * @param id the test ID
     * @param userId the user ID
     * @return the test result
     */
    public TestResultResponse executeTest(Long id, Long userId) {
        long startTime = System.currentTimeMillis();
        
        ApiTest test = apiTestRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id.toString()));
        
        TestResult testResult = new TestResult();
        testResult.setId(UUID.randomUUID());
        testResult.setTestId(test.getId());
        testResult.setUserId(userId);
        testResult.setStartTime(LocalDateTime.now());
        
        try {
            // Execute test based on protocol type
            switch (test.getProtocolType()) {
                case HTTP:
                    executeHttpTest(test, testResult);
                    break;
                case WEBSOCKET:
                    executeWebSocketTest(test, testResult);
                    break;
                case GRAPHQL:
                    executeGraphQLTest(test, testResult);
                    break;
                case GRPC:
                    executeGrpcTest(test, testResult);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported protocol type: " + test.getProtocolType());
            }
            
            // Run validation script if provided
            if (test.getValidationScript() != null && !test.getValidationScript().isEmpty()) {
                boolean validationPassed = runValidationScript(test, testResult);
                testResult.setValidationPassed(validationPassed);
            } else {
                // If no validation script, check expected status code
                if (test.getExpectedStatusCode() != null) {
                    boolean statusCodeMatches = testResult.getStatusCode() != null && 
                                               testResult.getStatusCode().equals(test.getExpectedStatusCode());
                    testResult.setValidationPassed(statusCodeMatches);
                } else {
                    // No validation criteria, assume passed
                    testResult.setValidationPassed(true);
                }
            }
        } catch (Exception e) {
            logger.error("Error executing test", e);
            testResult.setError(e.getMessage());
            testResult.setValidationPassed(false);
        } finally {
            testResult.setEndTime(LocalDateTime.now());
            testResult.setDurationMs(System.currentTimeMillis() - startTime);
            testResultRepository.save(testResult);
        }
        
        return mapToTestResultResponse(testResult);
    }
    
    /**
     * Executes an HTTP test.
     *
     * @param apiTest the API test
     * @param testResult the test result
     */
    private void executeHttpTest(ApiTest apiTest, TestResult testResult) {
        try {
            // Run pre-request script if provided
            if (apiTest.getPreRequestScript() != null && !apiTest.getPreRequestScript().isEmpty()) {
                runPreRequestScript(apiTest, testResult);
            }
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            if (apiTest.getHeaders() != null) {
                try {
                    Map<String, String> headersMap = objectMapper.readValue(apiTest.getHeaders(), new TypeReference<Map<String, String>>() {});
                    headersMap.forEach(headers::add);
                } catch (JsonProcessingException e) {
                    logger.error("Error parsing headers", e);
                    testResult.setError("Error parsing headers: " + e.getMessage());
                    return;
                }
            }
            
            // Prepare request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(apiTest.getRequestBody(), headers);
            
            // Execute request
            ResponseEntity<String> responseEntity;
            try {
                HttpMethod method = HttpMethod.valueOf(apiTest.getMethod());
                responseEntity = restTemplate.exchange(apiTest.getUrl(), method, requestEntity, String.class);
                
                // Record response
                testResult.setStatusCode(responseEntity.getStatusCode().value());
                testResult.setResponseBody(responseEntity.getBody());
                
                try {
                    testResult.setResponseHeaders(objectMapper.writeValueAsString(responseEntity.getHeaders().toSingleValueMap()));
                } catch (JsonProcessingException e) {
                    logger.error("Error processing response headers JSON", e);
                }
                
                // Run post-request script if provided
                if (apiTest.getPostRequestScript() != null && !apiTest.getPostRequestScript().isEmpty()) {
                    runPostRequestScript(apiTest, testResult);
                }
            } catch (HttpStatusCodeException e) {
                // Capture error response
                testResult.setStatusCode(e.getRawStatusCode());
                testResult.setResponseBody(e.getResponseBodyAsString());
                
                try {
                    testResult.setResponseHeaders(objectMapper.writeValueAsString(e.getResponseHeaders().toSingleValueMap()));
                } catch (JsonProcessingException ex) {
                    logger.error("Error processing response headers JSON", ex);
                }
            }
        } catch (Exception e) {
            logger.error("Error executing HTTP test", e);
            testResult.setError("Error executing HTTP test: " + e.getMessage());
        }
    }
    
    /**
     * Executes a WebSocket test.
     *
     * @param apiTest the API test
     * @param testResult the test result
     */
    private void executeWebSocketTest(ApiTest apiTest, TestResult testResult) {
        // WebSocket implementation would go here
        testResult.setError("WebSocket testing not implemented yet");
    }
    
    /**
     * Executes a GraphQL test.
     *
     * @param apiTest the API test
     * @param testResult the test result
     */
    private void executeGraphQLTest(ApiTest apiTest, TestResult testResult) {
        // GraphQL implementation would go here
        testResult.setError("GraphQL testing not implemented yet");
    }
    
    /**
     * Executes a gRPC test.
     *
     * @param apiTest the API test
     * @param testResult the test result
     */
    private void executeGrpcTest(ApiTest apiTest, TestResult testResult) {
        // gRPC implementation would go here
        testResult.setError("gRPC testing not implemented yet");
    }
    
    /**
     * Runs the pre-request script.
     *
     * @param apiTest the API test
     * @param testResult the test result
     */
    private void runPreRequestScript(ApiTest apiTest, TestResult testResult) {
        // Script execution would go here
    }
    
    /**
     * Runs the post-request script.
     *
     * @param apiTest the API test
     * @param testResult the test result
     */
    private void runPostRequestScript(ApiTest apiTest, TestResult testResult) {
        // Script execution would go here
    }
    
    /**
     * Runs the validation script.
     *
     * @param apiTest the API test
     * @param testResult the test result
     * @return true if validation passed, false otherwise
     */
    private boolean runValidationScript(ApiTest apiTest, TestResult testResult) {
        // Script execution would go here
        return true;
    }
    
    /**
     * Deletes an API test.
     *
     * @param id the API test ID
     * @param userId the user ID
     */
    public void deleteApiTest(Long id, Long userId) {
        ApiTest test = apiTestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id.toString()));
        
        // Validate user has access to the project
        // projectRepository.findById(test.getProjectId())
        //     .orElseThrow(() -> new ResourceNotFoundException("Project", "id", test.getProjectId().toString()));
        
        apiTestRepository.delete(test);
    }
    
    /**
     * Gets test results for an API test.
     *
     * @param testId the API test ID
     * @param userId the user ID
     * @return the list of test results
     */
    public List<TestResultResponse> getTestResults(Long testId, Long userId) {
        ApiTest test = apiTestRepository.findById(testId)
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", testId.toString()));
        
        // Validate user has access to the project
        // projectRepository.findById(test.getProjectId())
        //     .orElseThrow(() -> new ResourceNotFoundException("Project", "id", test.getProjectId().toString()));
        
        List<TestResult> results = testResultRepository.findByTestIdOrderByStartTimeDesc(testId);
        return results.stream()
            .map(this::mapToTestResultResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets a test result by ID.
     *
     * @param id the test result ID
     * @param userId the user ID
     * @return the test result
     */
    public TestResultResponse getTestResult(UUID id, Long userId) {
        TestResult result = testResultRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TestResult", "id", id.toString()));
        
        // Validate user has access to the test
        ApiTest test = apiTestRepository.findById(result.getTestId())
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", result.getTestId().toString()));
        
        // Validate user has access to the project
        // projectRepository.findById(test.getProjectId())
        //     .orElseThrow(() -> new ResourceNotFoundException("Project", "id", test.getProjectId().toString()));
        
        return mapToTestResultResponse(result);
    }
    
    /**
     * Maps an API test to an API test response.
     *
     * @param apiTest the API test
     * @return the API test response
     */
    private ApiTestResponse mapToApiTestResponse(ApiTest apiTest) {
        ApiTestResponse response = new ApiTestResponse();
        response.setId(apiTest.getId());
        response.setName(apiTest.getName());
        response.setProjectId(apiTest.getProjectId());
        response.setProtocolType(apiTest.getProtocolType());
        response.setUrl(apiTest.getUrl());
        response.setMethod(apiTest.getMethod());
        
        if (apiTest.getHeaders() != null) {
            try {
                response.setHeaders(objectMapper.readValue(apiTest.getHeaders(), new TypeReference<Map<String, String>>() {}));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing headers JSON", e);
            }
        }
        
        response.setRequestBody(apiTest.getRequestBody());
        response.setGraphqlQuery(apiTest.getGraphqlQuery());
        response.setGraphqlVariables(apiTest.getGraphqlVariables());
        response.setGrpcRequest(apiTest.getGrpcRequest());
        response.setGrpcServiceDefinition(apiTest.getGrpcServiceDefinition());
        response.setExpectedStatusCode(apiTest.getExpectedStatusCode());
        // These fields don't exist in ApiTest entity
        // response.setExpectedResponseBody(apiTest.getExpectedResponseBody());
        // response.setPreRequestScript(apiTest.getPreRequestScript());
        // response.setPostRequestScript(apiTest.getPostRequestScript());
        // response.setValidationScript(apiTest.getValidationScript());
        response.setTimeoutMs(apiTest.getTimeoutMs());
        response.setActive(apiTest.isActive());
        response.setCreatedAt(apiTest.getCreatedAt());
        response.setUpdatedAt(apiTest.getUpdatedAt());
        
        return response;
    }
    
    /**
     * Maps a test result to a test result response.
     *
     * @param testResult the test result
     * @return the test result response
     */
    private TestResultResponse mapToTestResultResponse(TestResult testResult) {
        TestResultResponse response = new TestResultResponse();
        response.setId(testResult.getId());
        response.setTestId(testResult.getTestId());
        response.setStatusCode(testResult.getStatusCode());
        response.setResponseBody(testResult.getResponseBody());
        
        if (testResult.getResponseHeaders() != null) {
            try {
                response.setResponseHeaders(objectMapper.readValue(testResult.getResponseHeaders(), new TypeReference<Map<String, String>>() {}));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing response headers JSON", e);
            }
        }
        
        response.setError(testResult.getError());
        response.setValidationPassed(testResult.isValidationPassed());
        response.setDurationMs(testResult.getDurationMs());
        response.setStartTime(testResult.getStartTime());
        response.setEndTime(testResult.getEndTime());
        
        return response;
    }
}

