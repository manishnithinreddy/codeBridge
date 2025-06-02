package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.ApiTestRequest;
import com.codebridge.apitest.dto.ApiTestResponse;
import com.codebridge.apitest.dto.EnvironmentResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.exception.TestExecutionException;
import com.codebridge.apitest.model.ApiTest;
import com.codebridge.apitest.model.HttpMethod;
import com.codebridge.apitest.model.ProtocolType;
import com.codebridge.apitest.model.TestResult;
import com.codebridge.apitest.model.TestStatus;
import com.codebridge.apitest.repository.ApiTestRepository;
import com.codebridge.apitest.repository.TestResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for API test operations.
 */
@Service
public class ApiTestService {

    private final ApiTestRepository apiTestRepository;
    private final TestResultRepository testResultRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final EnvironmentService environmentService;

    @Autowired
    public ApiTestService(
            ApiTestRepository apiTestRepository,
            TestResultRepository testResultRepository,
            ObjectMapper objectMapper,
            RestTemplate restTemplate,
            EnvironmentService environmentService) {
        this.apiTestRepository = apiTestRepository;
        this.testResultRepository = testResultRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.environmentService = environmentService;
    }

    /**
     * Creates a new API test.
     *
     * @param request the API test request
     * @param userId the user ID
     * @return the created API test
     */
    @Transactional
    public ApiTestResponse createTest(ApiTestRequest request, UUID userId) {
        ApiTest apiTest = new ApiTest();
        apiTest.setId(UUID.randomUUID());
        apiTest.setName(request.getName());
        apiTest.setDescription(request.getDescription());
        apiTest.setUserId(userId);
        apiTest.setUrl(request.getUrl());
        apiTest.setMethod(HttpMethod.valueOf(request.getMethod()));
        
        if (request.getProtocolType() != null) {
            apiTest.setProtocolType(ProtocolType.valueOf(request.getProtocolType()));
        } else {
            apiTest.setProtocolType(ProtocolType.HTTP); // Default to HTTP
        }
        
        apiTest.setEnvironmentId(request.getEnvironmentId());
        
        try {
            if (request.getHeaders() != null) {
                apiTest.setHeaders(objectMapper.writeValueAsString(request.getHeaders()));
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
        apiTest.setExpectedResponseBody(request.getExpectedResponseBody());
        apiTest.setPreRequestScript(request.getPreRequestScript());
        apiTest.setPostRequestScript(request.getPostRequestScript());
        apiTest.setValidationScript(request.getValidationScript());
        apiTest.setTimeoutMs(request.getTimeoutMs());
        apiTest.setActive(true);
        
        ApiTest savedTest = apiTestRepository.save(apiTest);
        return mapToApiTestResponse(savedTest);
    }

    /**
     * Gets all API tests for a user.
     *
     * @param userId the user ID
     * @return the list of API tests
     */
    @Transactional(readOnly = true)
    public List<ApiTestResponse> getAllTests(UUID userId) {
        List<ApiTest> tests = apiTestRepository.findByUserId(userId);
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
    @Transactional(readOnly = true)
    public ApiTestResponse getTestById(UUID id, UUID userId) {
        ApiTest test = apiTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id));
        return mapToApiTestResponse(test);
    }

    /**
     * Updates an API test.
     *
     * @param id the API test ID
     * @param request the API test request
     * @param userId the user ID
     * @return the updated API test
     */
    @Transactional
    public ApiTestResponse updateTest(UUID id, ApiTestRequest request, UUID userId) {
        ApiTest test = apiTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id));
        
        test.setName(request.getName());
        test.setDescription(request.getDescription());
        test.setUrl(request.getUrl());
        test.setMethod(HttpMethod.valueOf(request.getMethod()));
        
        if (request.getProtocolType() != null) {
            test.setProtocolType(ProtocolType.valueOf(request.getProtocolType()));
        }
        
        test.setEnvironmentId(request.getEnvironmentId());
        
        try {
            if (request.getHeaders() != null) {
                test.setHeaders(objectMapper.writeValueAsString(request.getHeaders()));
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
        test.setExpectedResponseBody(request.getExpectedResponseBody());
        test.setPreRequestScript(request.getPreRequestScript());
        test.setPostRequestScript(request.getPostRequestScript());
        test.setValidationScript(request.getValidationScript());
        test.setTimeoutMs(request.getTimeoutMs());
        
        ApiTest updatedTest = apiTestRepository.save(test);
        return mapToApiTestResponse(updatedTest);
    }

    /**
     * Deletes an API test.
     *
     * @param id the API test ID
     * @param userId the user ID
     */
    @Transactional
    public void deleteTest(UUID id, UUID userId) {
        ApiTest test = apiTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id));
        
        apiTestRepository.delete(test);
    }

    /**
     * Executes an API test.
     *
     * @param id the API test ID
     * @param userId the user ID
     * @return the test result
     */
    @Transactional
    public TestResultResponse executeTest(UUID id, UUID userId) {
        ApiTest test = apiTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id));

        TestResult result = new TestResult();
        result.setId(UUID.randomUUID());
        result.setTestId(test.getId());

        long startTime = System.currentTimeMillis();

        try {
            // Process environment variables if an environment is specified
            Map<String, String> environmentVariables = null;
            if (test.getEnvironmentId() != null) {
                EnvironmentResponse environment = environmentService.getEnvironmentById(test.getEnvironmentId(), userId);
                environmentVariables = environment.getVariables();
            }
            
            // Execute pre-request script (future implementation)
            if (test.getPreRequestScript() != null && !test.getPreRequestScript().isEmpty()) {
                // Execute pre-request script
            }

            // Execute the test based on protocol type
            if (test.getProtocolType() == null || test.getProtocolType() == ProtocolType.HTTP) {
                executeHttpTest(test, result, environmentVariables);
            } else if (test.getProtocolType() == ProtocolType.GRAPHQL) {
                executeGraphQLTest(test, result, environmentVariables);
            } else if (test.getProtocolType() == ProtocolType.GRPC) {
                executeGrpcTest(test, result, environmentVariables);
            } else if (test.getProtocolType() == ProtocolType.WEBSOCKET) {
                executeWebSocketTest(test, result, environmentVariables);
            }

            // Execute post-request script (future implementation)
            if (test.getPostRequestScript() != null && !test.getPostRequestScript().isEmpty()) {
                // Execute post-request script
            }

        } catch (Exception e) {
            result.setStatus(TestStatus.ERROR);
            result.setErrorMessage(e.getMessage());
        } finally {
            long endTime = System.currentTimeMillis();
            result.setExecutionTimeMs(endTime - startTime);
            testResultRepository.save(result);
        }
        
        return mapToTestResultResponse(result);
    }

    /**
     * Gets all test results for an API test.
     *
     * @param id the API test ID
     * @param userId the user ID
     * @return the list of test results
     */
    @Transactional(readOnly = true)
    public List<TestResultResponse> getTestResults(UUID id, UUID userId) {
        // Verify the test exists and belongs to the user
        apiTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id));
        
        List<TestResult> results = testResultRepository.findByTestIdOrderByCreatedAtDesc(id);
        return results.stream()
                .map(this::mapToTestResultResponse)
                .collect(Collectors.toList());
    }

    /**
     * Execute an HTTP API test.
     *
     * @param test the API test
     * @param result the test result
     * @param environmentVariables the environment variables
     * @throws Exception if an error occurs
     */
    private void executeHttpTest(ApiTest test, TestResult result, Map<String, String> environmentVariables) throws Exception {
        // Process URL with environment variables
        String processedUrl = processEnvironmentVariables(test.getUrl(), environmentVariables);

        // Create HTTP request with processed URL
        HttpUriRequest request = createHttpRequest(test, processedUrl, environmentVariables);

        // Execute request
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                result.setResponseStatusCode(statusCode);
                
                // Process response headers
                Header[] headers = response.getAllHeaders();
                Map<String, String> headerMap = new HashMap<>();
                for (Header header : headers) {
                    headerMap.put(header.getName(), header.getValue());
                }
                result.setResponseHeaders(objectMapper.writeValueAsString(headerMap));
                
                // Process response body
                HttpEntity entity = response.getEntity();
                String responseBody = entity != null ? EntityUtils.toString(entity) : "";
                result.setResponseBody(responseBody);

                // Validate response
                boolean isValid = validateResponse(test, statusCode, responseBody);
                result.setStatus(isValid ? TestStatus.SUCCESS : TestStatus.FAILURE);
            }
        }
    }

    /**
     * Execute a GraphQL API test.
     *
     * @param test the API test
     * @param result the test result
     * @param environmentVariables the environment variables
     * @throws Exception if an error occurs
     */
    private void executeGraphQLTest(ApiTest test, TestResult result, Map<String, String> environmentVariables) throws Exception {
        // Process URL with environment variables
        String processedUrl = processEnvironmentVariables(test.getUrl(), environmentVariables);
        
        // Process GraphQL query and variables with environment variables
        String processedQuery = processEnvironmentVariables(test.getGraphqlQuery(), environmentVariables);
        String processedVariables = processEnvironmentVariables(test.getGraphqlVariables(), environmentVariables);

        // Create GraphQL request
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", processedQuery);

        if (processedVariables != null && !processedVariables.isEmpty()) {
            try {
                Map<String, Object> variables = objectMapper.readValue(processedVariables, new TypeReference<Map<String, Object>>() {});
                requestBody.put("variables", variables);
            } catch (JsonProcessingException e) {
                throw new TestExecutionException("Invalid GraphQL variables JSON: " + e.getMessage());
            }
        }

        // Create HTTP entity
        StringEntity entity = new StringEntity(objectMapper.writeValueAsString(requestBody), ContentType.APPLICATION_JSON);

        // Create HTTP request
        HttpPost httpPost = new HttpPost(processedUrl);
        httpPost.setEntity(entity);

        // Add headers
        if (test.getHeaders() != null) {
            Map<String, String> headers = objectMapper.readValue(test.getHeaders(), new TypeReference<Map<String, String>>() {});
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String processedValue = processEnvironmentVariables(header.getValue(), environmentVariables);
                httpPost.addHeader(header.getKey(), processedValue);
            }
        }

        // Execute request
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                result.setResponseStatusCode(statusCode);
                
                // Process response headers
                Header[] headers = response.getAllHeaders();
                Map<String, String> headerMap = new HashMap<>();
                for (Header header : headers) {
                    headerMap.put(header.getName(), header.getValue());
                }
                result.setResponseHeaders(objectMapper.writeValueAsString(headerMap));
                
                // Process response body
                HttpEntity responseEntity = response.getEntity();
                String responseBody = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
                result.setResponseBody(responseBody);

                // Validate response
                boolean isValid = validateResponse(test, statusCode, responseBody);
                result.setStatus(isValid ? TestStatus.SUCCESS : TestStatus.FAILURE);
            }
        }
    }

    /**
     * Execute a gRPC API test.
     * Note: This is a placeholder implementation. Actual gRPC implementation would require additional dependencies.
     *
     * @param test the API test
     * @param result the test result
     * @param environmentVariables the environment variables
     * @throws Exception if an error occurs
     */
    private void executeGrpcTest(ApiTest test, TestResult result, Map<String, String> environmentVariables) throws Exception {
        // This is a placeholder for gRPC implementation
        result.setStatus(TestStatus.ERROR);
        result.setErrorMessage("gRPC testing is not yet implemented");
    }

    /**
     * Execute a WebSocket API test.
     * Note: This is a placeholder implementation. Actual WebSocket implementation would require additional dependencies.
     *
     * @param test the API test
     * @param result the test result
     * @param environmentVariables the environment variables
     * @throws Exception if an error occurs
     */
    private void executeWebSocketTest(ApiTest test, TestResult result, Map<String, String> environmentVariables) throws Exception {
        // This is a placeholder for WebSocket implementation
        result.setStatus(TestStatus.ERROR);
        result.setErrorMessage("WebSocket testing is not yet implemented");
    }

    /**
     * Process environment variables in a string.
     *
     * @param input the input string
     * @param environmentVariables the environment variables
     * @return the processed string
     */
    private String processEnvironmentVariables(String input, Map<String, String> environmentVariables) {
        if (input == null || environmentVariables == null || environmentVariables.isEmpty()) {
            return input;
        }
        
        String result = input;
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        
        return result;
    }

    /**
     * Create an HTTP request for an API test.
     *
     * @param test the API test
     * @param processedUrl the processed URL
     * @param environmentVariables the environment variables
     * @return the HTTP request
     * @throws IOException if an error occurs
     */
    private HttpUriRequest createHttpRequest(ApiTest test, String processedUrl, Map<String, String> environmentVariables) throws IOException {
        HttpUriRequest request;

        switch (test.getMethod()) {
            case GET:
                request = new HttpGet(processedUrl);
                break;
            case POST:
                HttpPost post = new HttpPost(processedUrl);
                if (test.getRequestBody() != null) {
                    String processedBody = processEnvironmentVariables(test.getRequestBody(), environmentVariables);
                    post.setEntity(new StringEntity(processedBody));
                }
                request = post;
                break;
            case PUT:
                HttpPut put = new HttpPut(processedUrl);
                if (test.getRequestBody() != null) {
                    String processedBody = processEnvironmentVariables(test.getRequestBody(), environmentVariables);
                    put.setEntity(new StringEntity(processedBody));
                }
                request = put;
                break;
            case DELETE:
                request = new HttpDelete(processedUrl);
                break;
            case PATCH:
                HttpPatch patch = new HttpPatch(processedUrl);
                if (test.getRequestBody() != null) {
                    String processedBody = processEnvironmentVariables(test.getRequestBody(), environmentVariables);
                    patch.setEntity(new StringEntity(processedBody));
                }
                request = patch;
                break;
            case HEAD:
                request = new HttpHead(processedUrl);
                break;
            case OPTIONS:
                request = new HttpOptions(processedUrl);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + test.getMethod());
        }

        // Add headers
        if (test.getHeaders() != null) {
            Map<String, String> headers = objectMapper.readValue(test.getHeaders(), new TypeReference<Map<String, String>>() {});
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String processedValue = processEnvironmentVariables(header.getValue(), environmentVariables);
                request.addHeader(header.getKey(), processedValue);
            }
        }

        // Set timeout
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(test.getTimeoutMs())
                .setSocketTimeout(test.getTimeoutMs())
                .build();

        if (request instanceof HttpRequestBase) {
            ((HttpRequestBase) request).setConfig(requestConfig);
        }
        
        return request;
    }

    /**
     * Validates the response of an API test.
     *
     * @param test the API test
     * @param statusCode the response status code
     * @param responseBody the response body
     * @return true if the response is valid, false otherwise
     */
    private boolean validateResponse(ApiTest test, int statusCode, String responseBody) {
        // Validate status code
        if (test.getExpectedStatusCode() != null && statusCode != test.getExpectedStatusCode()) {
            return false;
        }
        
        // Validate response body
        if (test.getExpectedResponseBody() != null && !test.getExpectedResponseBody().equals(responseBody)) {
            return false;
        }

        // TODO: Implement validation script execution

        return true;
    }

    /**
     * Maps an ApiTest entity to an ApiTestResponse DTO.
     *
     * @param apiTest the API test entity
     * @return the API test response DTO
     */
    public ApiTestResponse mapToApiTestResponse(ApiTest apiTest) {
        ApiTestResponse response = new ApiTestResponse();
        response.setId(apiTest.getId());
        response.setName(apiTest.getName());
        response.setDescription(apiTest.getDescription());
        response.setUrl(apiTest.getUrl());
        response.setMethod(apiTest.getMethod().name());
        
        if (apiTest.getProtocolType() != null) {
            response.setProtocolType(apiTest.getProtocolType().name());
        }
        
        response.setEnvironmentId(apiTest.getEnvironmentId());
        
        if (apiTest.getEnvironmentId() != null) {
            try {
                EnvironmentResponse environment = environmentService.getEnvironmentById(apiTest.getEnvironmentId(), apiTest.getUserId());
                response.setEnvironment(environment);
            } catch (ResourceNotFoundException e) {
                // Environment not found, continue without it
            }
        }
        
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
        response.setExpectedResponseBody(apiTest.getExpectedResponseBody());
        response.setPreRequestScript(apiTest.getPreRequestScript());
        response.setPostRequestScript(apiTest.getPostRequestScript());
        response.setValidationScript(apiTest.getValidationScript());
        response.setTimeoutMs(apiTest.getTimeoutMs());
        response.setActive(apiTest.isActive());
        response.setCreatedAt(apiTest.getCreatedAt());
        response.setUpdatedAt(apiTest.getUpdatedAt());
        
        return response;
    }

    /**
     * Maps a TestResult entity to a TestResultResponse DTO.
     *
     * @param testResult the test result entity
     * @return the test result response DTO
     */
    private TestResultResponse mapToTestResultResponse(TestResult testResult) {
        TestResultResponse response = new TestResultResponse();
        response.setId(testResult.getId());
        response.setTestId(testResult.getTestId());
        response.setStatus(testResult.getStatus().name());
        response.setResponseStatusCode(testResult.getResponseStatusCode());
        response.setErrorMessage(testResult.getErrorMessage());
        response.setExecutionTimeMs(testResult.getExecutionTimeMs());
        response.setCreatedAt(testResult.getCreatedAt());
        
        if (testResult.getResponseHeaders() != null) {
            try {
                response.setResponseHeaders(objectMapper.readValue(testResult.getResponseHeaders(), new TypeReference<Map<String, String>>() {}));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing response headers JSON", e);
            }
        }
        
        response.setResponseBody(testResult.getResponseBody());
        
        return response;
    }
}
