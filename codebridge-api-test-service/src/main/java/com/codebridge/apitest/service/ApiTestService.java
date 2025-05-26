package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.ApiTestRequest;
import com.codebridge.apitest.dto.ApiTestResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.exception.TestExecutionException;
import com.codebridge.apitest.model.ApiTest;
import com.codebridge.apitest.model.HttpMethod;
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

    @Autowired
    public ApiTestService(
            ApiTestRepository apiTestRepository,
            TestResultRepository testResultRepository,
            ObjectMapper objectMapper) {
        this.apiTestRepository = apiTestRepository;
        this.testResultRepository = testResultRepository;
        this.objectMapper = objectMapper;
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
        
        if (request.getHeaders() != null) {
            try {
                apiTest.setHeaders(objectMapper.writeValueAsString(request.getHeaders()));
            } catch (JsonProcessingException e) {
                throw new TestExecutionException("Failed to serialize headers", e);
            }
        }
        
        apiTest.setRequestBody(request.getRequestBody());
        apiTest.setExpectedStatusCode(request.getExpectedStatusCode());
        apiTest.setExpectedResponseBody(request.getExpectedResponseBody());
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
        
        if (request.getHeaders() != null) {
            try {
                test.setHeaders(objectMapper.writeValueAsString(request.getHeaders()));
            } catch (JsonProcessingException e) {
                throw new TestExecutionException("Failed to serialize headers", e);
            }
        }
        
        test.setRequestBody(request.getRequestBody());
        test.setExpectedStatusCode(request.getExpectedStatusCode());
        test.setExpectedResponseBody(request.getExpectedResponseBody());
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
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpUriRequest request = createHttpRequest(test);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                long endTime = System.currentTimeMillis();
                result.setExecutionTimeMs(endTime - startTime);
                
                int statusCode = response.getCode();
                result.setResponseStatusCode(statusCode);
                
                Map<String, String> responseHeaders = new HashMap<>();
                for (Header header : response.getHeaders()) {
                    responseHeaders.put(header.getName(), header.getValue());
                }
                result.setResponseHeaders(objectMapper.writeValueAsString(responseHeaders));
                
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String responseBody = new BufferedReader(new InputStreamReader(entity.getContent()))
                            .lines().collect(Collectors.joining("\n"));
                    result.setResponseBody(responseBody);
                }
                
                // Validate response
                boolean isSuccess = validateResponse(test, statusCode, result.getResponseBody());
                result.setStatus(isSuccess ? TestStatus.SUCCESS : TestStatus.FAILURE);
            }
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            result.setExecutionTimeMs(endTime - startTime);
            result.setStatus(TestStatus.ERROR);
            result.setErrorMessage(e.getMessage());
        }
        
        TestResult savedResult = testResultRepository.save(result);
        return mapToTestResultResponse(savedResult);
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
     * Creates an HTTP request for an API test.
     *
     * @param test the API test
     * @return the HTTP request
     * @throws IOException if an I/O error occurs
     */
    private HttpUriRequest createHttpRequest(ApiTest test) throws IOException {
        HttpUriRequest request;
        
        switch (test.getMethod()) {
            case GET:
                request = new HttpGet(test.getUrl());
                break;
            case POST:
                HttpPost postRequest = new HttpPost(test.getUrl());
                if (test.getRequestBody() != null) {
                    postRequest.setEntity(new StringEntity(test.getRequestBody(), ContentType.APPLICATION_JSON));
                }
                request = postRequest;
                break;
            case PUT:
                HttpPut putRequest = new HttpPut(test.getUrl());
                if (test.getRequestBody() != null) {
                    putRequest.setEntity(new StringEntity(test.getRequestBody(), ContentType.APPLICATION_JSON));
                }
                request = putRequest;
                break;
            case DELETE:
                request = new HttpDelete(test.getUrl());
                break;
            case PATCH:
                HttpPatch patchRequest = new HttpPatch(test.getUrl());
                if (test.getRequestBody() != null) {
                    patchRequest.setEntity(new StringEntity(test.getRequestBody(), ContentType.APPLICATION_JSON));
                }
                request = patchRequest;
                break;
            case HEAD:
                request = new HttpHead(test.getUrl());
                break;
            case OPTIONS:
                request = new HttpOptions(test.getUrl());
                break;
            default:
                throw new TestExecutionException("Unsupported HTTP method: " + test.getMethod());
        }
        
        // Add headers
        if (test.getHeaders() != null) {
            Map<String, String> headers = objectMapper.readValue(test.getHeaders(), new TypeReference<Map<String, String>>() {});
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
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
    private ApiTestResponse mapToApiTestResponse(ApiTest apiTest) {
        ApiTestResponse response = new ApiTestResponse();
        response.setId(apiTest.getId());
        response.setName(apiTest.getName());
        response.setDescription(apiTest.getDescription());
        response.setUrl(apiTest.getUrl());
        response.setMethod(apiTest.getMethod().name());
        
        if (apiTest.getHeaders() != null) {
            try {
                response.setHeaders(objectMapper.readValue(apiTest.getHeaders(), new TypeReference<Map<String, String>>() {}));
            } catch (JsonProcessingException e) {
                throw new TestExecutionException("Failed to deserialize headers", e);
            }
        }
        
        response.setRequestBody(apiTest.getRequestBody());
        response.setExpectedStatusCode(apiTest.getExpectedStatusCode());
        response.setExpectedResponseBody(apiTest.getExpectedResponseBody());
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
                throw new TestExecutionException("Failed to deserialize response headers", e);
            }
        }
        
        response.setResponseBody(testResult.getResponseBody());
        
        return response;
    }
}

