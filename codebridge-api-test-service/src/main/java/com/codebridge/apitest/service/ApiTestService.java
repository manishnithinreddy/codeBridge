package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.ApiTestRequest;
import com.codebridge.apitest.dto.ApiTestResponse;
import com.codebridge.apitest.dto.CachedResponse;
import com.codebridge.apitest.dto.EnvironmentResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.exception.TestExecutionException;
import com.codebridge.apitest.model.ApiTest;
import com.codebridge.apitest.model.HttpMethod;
import com.codebridge.apitest.model.ProjectToken;
import com.codebridge.apitest.model.ProtocolType;
import com.codebridge.apitest.model.TestResult;
import com.codebridge.apitest.model.TestStatus;
import com.codebridge.apitest.repository.ApiTestRepository;
import com.codebridge.apitest.repository.TestResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
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
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for API test operations.
 */
@Service
public class ApiTestService {

    private final ApiTestRepository apiTestRepository;
    private final TestResultRepository testResultRepository;
    private final EnvironmentService environmentService;
    private final ProjectTokenService projectTokenService;
    private final ObjectMapper objectMapper;
    private final ResponseCacheService cacheService;
    private final PerformanceMetricsService metricsService;
    private final ExecutorService executorService;

    @Autowired
    public ApiTestService(ApiTestRepository apiTestRepository,
                         TestResultRepository testResultRepository,
                         EnvironmentService environmentService,
                         ProjectTokenService projectTokenService,
                         ObjectMapper objectMapper,
                         ResponseCacheService cacheService,
                         PerformanceMetricsService metricsService) {
        this.apiTestRepository = apiTestRepository;
        this.testResultRepository = testResultRepository;
        this.environmentService = environmentService;
        this.projectTokenService = projectTokenService;
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
        this.metricsService = metricsService;
        this.executorService = Executors.newFixedThreadPool(10); // Thread pool for parallel operations
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
        long startTime = System.currentTimeMillis();
        
        ApiTest test = apiTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id));
        
        TestResult testResult = new TestResult();
        testResult.setId(UUID.randomUUID());
        testResult.setTestId(test.getId());
        testResult.setUserId(userId);
        
        try {
            // Execute pre-request script if present
            if (test.getPreRequestScript() != null && !test.getPreRequestScript().isEmpty()) {
                CompletableFuture<Void> scriptFuture = CompletableFuture.runAsync(() -> {
                    try {
                        executeScript(test.getPreRequestScript(), null, null, null);
                    } catch (Exception e) {
                        throw new TestExecutionException("Error executing pre-request script: " + e.getMessage(), e);
                    }
                }, executorService);
                
                // Wait for script execution to complete
                scriptFuture.join();
            }
            
            // Execute the API test based on protocol type
            Map<String, String> headers = new HashMap<>();
            if (test.getHeaders() != null) {
                headers = objectMapper.readValue(test.getHeaders(), new TypeReference<Map<String, String>>() {});
            }
            
            // Check if response is cached
            String cacheKey = null;
            if (test.getProtocolType() == ProtocolType.HTTP && test.getHttpMethod() == HttpMethod.GET) {
                cacheKey = cacheService.generateCacheKey(
                        test.getUrl(), 
                        test.getHttpMethod().name(), 
                        headers, 
                        test.getRequestBody());
                
                Optional<CachedResponse> cachedResponse = cacheService.getCachedResponse(cacheKey);
                if (cachedResponse.isPresent()) {
                    metricsService.recordCacheHit();
                    
                    CachedResponse response = cachedResponse.get();
                    testResult.setStatus(TestStatus.SUCCESS);
                    testResult.setResponseStatusCode(response.getStatusCode());
                    testResult.setResponseBody(response.getResponseBody());
                    
                    try {
                        testResult.setResponseHeaders(objectMapper.writeValueAsString(response.getResponseHeaders()));
                    } catch (JsonProcessingException e) {
                        logger.error("Error processing response headers JSON", e);
                    }
                    
                    testResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                    testResult = testResultRepository.save(testResult);
                    
                    metricsService.recordTestExecution(testResult.getExecutionTimeMs(), true);
                    return mapToTestResultResponse(testResult);
                }
                
                metricsService.recordCacheMiss();
            }
            
            // Execute the test based on protocol type
            int statusCode;
            String responseBody;
            Map<String, String> responseHeaders = new HashMap<>();
            
            switch (test.getProtocolType()) {
                case HTTP:
                    HttpResult httpResult = executeHttpTest(test, headers);
                    statusCode = httpResult.statusCode();
                    responseBody = httpResult.responseBody();
                    responseHeaders = httpResult.responseHeaders();
                    break;
                case WEBSOCKET:
                    // WebSocket implementation
                    throw new TestExecutionException("WebSocket tests not implemented yet");
                case GRAPHQL:
                    // GraphQL implementation
                    throw new TestExecutionException("GraphQL tests not implemented yet");
                case GRPC:
                    // gRPC implementation
                    throw new TestExecutionException("gRPC tests not implemented yet");
                default:
                    throw new TestExecutionException("Unsupported protocol type: " + test.getProtocolType());
            }
            
            // Cache the response if applicable
            if (cacheKey != null && statusCode >= 200 && statusCode < 300) {
                cacheService.cacheResponse(cacheKey, statusCode, responseBody, responseHeaders, 300); // 5 minutes TTL
            }
            
            // Execute post-request script if present
            if (test.getPostRequestScript() != null && !test.getPostRequestScript().isEmpty()) {
                CompletableFuture<Void> scriptFuture = CompletableFuture.runAsync(() -> {
                    try {
                        executeScript(test.getPostRequestScript(), statusCode, responseBody, responseHeaders);
                    } catch (Exception e) {
                        throw new TestExecutionException("Error executing post-request script: " + e.getMessage(), e);
                    }
                }, executorService);
                
                // Wait for script execution to complete
                scriptFuture.join();
            }
            
            // Validate the response
            boolean isValid = validateResponse(test, statusCode, responseBody);
            
            // Save the test result
            testResult.setStatus(isValid ? TestStatus.SUCCESS : TestStatus.FAILURE);
            testResult.setResponseStatusCode(statusCode);
            testResult.setResponseBody(responseBody);
            
            try {
                testResult.setResponseHeaders(objectMapper.writeValueAsString(responseHeaders));
            } catch (JsonProcessingException e) {
                logger.error("Error processing response headers JSON", e);
            }
            
            if (!isValid) {
                testResult.setErrorMessage("Response validation failed");
            }
        } catch (Exception e) {
            testResult.setStatus(TestStatus.ERROR);
            testResult.setErrorMessage(e.getMessage());
            logger.error("Error executing API test: {}", e.getMessage(), e);
        }
        
        testResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        testResult = testResultRepository.save(testResult);
        
        // Record metrics
        metricsService.recordTestExecution(
                testResult.getExecutionTimeMs(), 
                testResult.getStatus() == TestStatus.SUCCESS);
        
        return mapToTestResultResponse(testResult);
    }

    /**
     * Executes an HTTP API test.
     *
     * @param test the API test
     * @param headers the request headers
     * @return the HTTP result
     * @throws Exception if an error occurs
     */
    private HttpResult executeHttpTest(ApiTest test, Map<String, String> headers) throws Exception {
        long startTime = System.currentTimeMillis();
        
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(test.getTimeoutMs() != null ? test.getTimeoutMs() : 30000, TimeUnit.MILLISECONDS)
                        .setResponseTimeout(test.getTimeoutMs() != null ? test.getTimeoutMs() : 30000, TimeUnit.MILLISECONDS)
                        .build())
                .build()) {
            
            // Create HTTP request based on method
            HttpUriRequestBase request = createHttpRequest(test.getUrl(), test.getHttpMethod());
            
            // Set headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
                request.setHeader(header.getKey(), header.getValue());
            }
            
            // Set request body if present
            if (test.getRequestBody() != null && !test.getRequestBody().isEmpty() &&
                    (test.getMethod() == HttpMethod.POST || test.getMethod() == HttpMethod.PUT || 
                     test.getMethod() == HttpMethod.PATCH)) {
                StringEntity entity = new StringEntity(test.getRequestBody(), ContentType.APPLICATION_JSON);
                request.setEntity(entity);
            }
            
            // Inject project tokens if environment is set
            if (test.getEnvironmentId() != null) {
                try {
                    injectProjectTokens(request, test.getEnvironmentId());
                } catch (URISyntaxException e) {
                    throw new TestExecutionException("Error injecting project tokens: " + e.getMessage(), e);
                }
            }
            
            // Execute the request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                
                // Extract response headers
                Map<String, String> responseHeaders = new HashMap<>();
                for (Header header : response.getHeaders()) {
                    responseHeaders.put(header.getName(), header.getValue());
                }
                
                // Extract response body
                HttpEntity entity = response.getEntity();
                String responseBody = entity != null ? EntityUtils.toString(entity) : "";
                
                // Record metrics
                metricsService.recordHttpRequest(
                        test.getHttpMethod().name(), 
                        System.currentTimeMillis() - startTime);
                
                return new HttpResult(statusCode, responseBody, responseHeaders);
            }
        }
    }

    /**
     * Creates an HTTP request based on the method.
     *
     * @param url the URL
     * @param method the HTTP method
     * @return the HTTP request
     * @throws URISyntaxException if the URL is invalid
     */
    private HttpUriRequest createHttpRequest(String url, HttpMethod method) throws URISyntaxException {
        URI uri = new URI(url);
        return switch (method) {
            case GET -> new HttpGet(uri);
            case POST -> new HttpPost(uri);
            case PUT -> new HttpPut(uri);
            case DELETE -> new HttpDelete(uri);
            case PATCH -> new HttpPatch(uri);
            case HEAD -> new HttpHead(uri);
            case OPTIONS -> new HttpOptions(uri);
        };
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
        
        // Create HTTP request
        HttpUriRequest request;
        
        switch (test.getHttpMethod()) {
            case GET:
                request = new HttpGet(processedUrl);
                break;
            case POST:
                HttpPost post = new HttpPost(processedUrl);
                if (test.getBody() != null) {
                    String processedBody = processEnvironmentVariables(test.getBody(), environmentVariables);
                    post.setEntity(new StringEntity(processedBody, ContentType.APPLICATION_JSON));
                }
                request = post;
                break;
            case PUT:
                HttpPut put = new HttpPut(processedUrl);
                if (test.getBody() != null) {
                    String processedBody = processEnvironmentVariables(test.getBody(), environmentVariables);
                    put.setEntity(new StringEntity(processedBody, ContentType.APPLICATION_JSON));
                }
                request = put;
                break;
            case DELETE:
                request = new HttpDelete(processedUrl);
                break;
            case PATCH:
                HttpPatch patch = new HttpPatch(processedUrl);
                if (test.getBody() != null) {
                    String processedBody = processEnvironmentVariables(test.getBody(), environmentVariables);
                    patch.setEntity(new StringEntity(processedBody, ContentType.APPLICATION_JSON));
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
                throw new IllegalArgumentException("Unsupported HTTP method: " + test.getHttpMethod());
        }
        
        // Set timeout
        if (request instanceof HttpUriRequestBase) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(test.getTimeoutMs(), TimeUnit.MILLISECONDS)
                    .setResponseTimeout(test.getTimeoutMs(), TimeUnit.MILLISECONDS)
                    .build();
            ((HttpUriRequestBase) request).setConfig(requestConfig);
        }
        
        // Execute request
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(request)) {
            
            // Get response status code
            int statusCode = response.getCode();
            result.setResponseStatusCode(statusCode);
            
            // Get response headers
            Map<String, String> responseHeaders = new HashMap<>();
            for (Header header : response.getHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }
            result.setResponseHeaders(objectMapper.writeValueAsString(responseHeaders));
            
            // Get response body
            HttpEntity responseEntity = response.getEntity();
            String responseBody = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
            result.setResponseBody(responseBody);
            
            // Execute post-request script if present
            if (test.getPostRequestScript() != null && !test.getPostRequestScript().isEmpty()) {
                executeScript(test.getPostRequestScript(), statusCode, responseBody, responseHeaders);
            }
            
            // Validate response
            boolean isValid = validateResponse(test, statusCode, responseBody);
            result.setStatus(isValid ? TestStatus.SUCCESS : TestStatus.FAILURE);
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
        
        // Create GraphQL request body
        Map<String, Object> graphqlRequest = new HashMap<>();
        graphqlRequest.put("query", processEnvironmentVariables(test.getGraphqlQuery(), environmentVariables));
        
        if (test.getGraphqlVariables() != null && !test.getGraphqlVariables().isEmpty()) {
            try {
                Map<String, Object> variables = objectMapper.readValue(
                    processEnvironmentVariables(test.getGraphqlVariables(), environmentVariables),
                    new TypeReference<Map<String, Object>>() {}
                );
                graphqlRequest.put("variables", variables);
            } catch (JsonProcessingException e) {
                throw new TestExecutionException("Invalid GraphQL variables format: " + e.getMessage());
            }
        }
        
        // Create HTTP POST request
        HttpPost request = new HttpPost(processedUrl);
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(graphqlRequest), ContentType.APPLICATION_JSON));
        
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
                .setConnectTimeout(test.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .setResponseTimeout(test.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
        request.setConfig(requestConfig);
        
        // Execute request
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(request)) {
            
            // Get response status code
            int statusCode = response.getCode();
            result.setResponseStatusCode(statusCode);
            
            // Get response headers
            Map<String, String> responseHeaders = new HashMap<>();
            for (Header header : response.getHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }
            result.setResponseHeaders(objectMapper.writeValueAsString(responseHeaders));
            
            // Get response body
            HttpEntity responseEntity = response.getEntity();
            String responseBody = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
            result.setResponseBody(responseBody);
            
            // Execute post-request script if present
            if (test.getPostRequestScript() != null && !test.getPostRequestScript().isEmpty()) {
                executeScript(test.getPostRequestScript(), statusCode, responseBody, responseHeaders);
            }
            
            // Validate response
            boolean isValid = validateResponse(test, statusCode, responseBody);
            result.setStatus(isValid ? TestStatus.SUCCESS : TestStatus.FAILURE);
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
        String processedUrl = processEnvironmentVariables(test.getUrl(), environmentVariables);
        String processedGrpcRequest = processEnvironmentVariables(test.getGrpcRequest(), environmentVariables);

        String[] urlParts = processedUrl.split(":");
        String host = urlParts[0];
        int port = Integer.parseInt(urlParts[1]);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        try {
            InputStream protoInputStream = new ByteArrayInputStream(test.getGrpcServiceDefinition().getBytes(StandardCharsets.UTF_8));
            DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(protoInputStream);

            if (descriptorSet.getFileCount() == 0) {
                throw new TestExecutionException("No file descriptors found in gRPC service definition.");
            }
            DescriptorProtos.FileDescriptorProto fileDescriptorProto = descriptorSet.getFile(0);

            if (test.getGrpcServiceName() == null || test.getGrpcMethodName() == null) {
                throw new TestExecutionException("gRPC service name and method name must be specified in the test definition.");
            }
            String serviceName = test.getGrpcServiceName();
            String methodName = test.getGrpcMethodName();

            Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[]{});
            Descriptors.ServiceDescriptor serviceDescriptor = fileDescriptor.findServiceByName(serviceName);
            if (serviceDescriptor == null) {
                throw new TestExecutionException("Service not found: " + serviceName);
            }
            Descriptors.MethodDescriptor methodDescriptor = serviceDescriptor.findMethodByName(methodName);
            if (methodDescriptor == null) {
                throw new TestExecutionException("Method not found: " + methodName);
            }

            DynamicMessage.Builder requestBuilder = DynamicMessage.newBuilder(methodDescriptor.getInputType());
            JsonFormat.parser().merge(processedGrpcRequest, requestBuilder);
            DynamicMessage requestMessage = requestBuilder.build();

            MethodDescriptor<DynamicMessage, DynamicMessage> grpcMethodDescriptor =
                    MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                            .setType(MethodDescriptor.MethodType.UNARY)
                            .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceDescriptor.getFullName(), methodDescriptor.getName()))
                            .setRequestMarshaller(new DynamicMessageMarshaller(methodDescriptor.getInputType()))
                            .setResponseMarshaller(new DynamicMessageMarshaller(methodDescriptor.getOutputType()))
                            .build();

            DynamicMessage responseMessage = ClientCalls.blockingUnaryCall(
                channel,
                grpcMethodDescriptor,
                CallOptions.DEFAULT, // Explicitly provide CallOptions
                requestMessage
            );

            result.setResponseBody(JsonFormat.printer().print(responseMessage));
            result.setStatus(TestStatus.SUCCESS);

        } catch (StatusRuntimeException e) {
            result.setStatus(TestStatus.ERROR);
            result.setErrorMessage("gRPC call failed: " + e.getStatus().toString() + " - " + e.getMessage());
            if (e.getTrailers() != null) {
                Map<String, String> trailersMap = new HashMap<>();
                 for (String key : e.getTrailers().keys()) { // Iterate directly over keys
                    String value = e.getTrailers().get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
                    if (value != null) {
                        trailersMap.put(key, value);
                    }
                }
                result.setResponseHeaders(objectMapper.writeValueAsString(trailersMap));
            }
        } catch (Exception e) {
            result.setStatus(TestStatus.ERROR);
            result.setErrorMessage("Error during gRPC test execution: " + e.getMessage());
            throw e;
        } finally {
            if (channel != null) {
                channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            }
        }
    }

    // Helper class for DynamicMessage marshalling
    private static class DynamicMessageMarshaller implements MethodDescriptor.Marshaller<DynamicMessage> {
        private final Descriptors.Descriptor messageDescriptor;

        DynamicMessageMarshaller(Descriptors.Descriptor messageDescriptor) {
            this.messageDescriptor = messageDescriptor;
        }

        @Override
        public InputStream stream(DynamicMessage value) {
            return value.toByteString().newInput();
        }

        @Override
        public DynamicMessage parse(InputStream stream) {
            try {
                return DynamicMessage.parseFrom(messageDescriptor, stream);
            } catch (IOException e) {
                throw new RuntimeException("Unable to parse dynamic message", e);
            }
        }
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
     * Create an HTTP request for a test with environment variables.
     *
     * @param url the URL
     * @param method the HTTP method
     * @param environmentVariables the environment variables
     * @return the HTTP request
     * @throws Exception if an error occurs
     */
    private HttpUriRequest createHttpRequest(String processedUrl, HttpMethod method, Map<String, String> environmentVariables) throws Exception {
        HttpUriRequest request;

        switch (method) {
            case GET:
                request = new HttpGet(processedUrl);
                break;
            case POST:
                HttpPost post = new HttpPost(processedUrl);
                // We need to get the test object from somewhere else since it's not passed as a parameter anymore
                ApiTest test = getCurrentTest(); // This is a placeholder, you need to implement this method
                if (test.getBody() != null) {
                    String processedBody = processEnvironmentVariables(test.getBody(), environmentVariables);
                    post.setEntity(new StringEntity(processedBody, ContentType.APPLICATION_JSON));
                }
                request = post;
                break;
            case PUT:
                HttpPut put = new HttpPut(processedUrl);
                if (test.getRequestBody() != null) {
                    String processedBody = processEnvironmentVariables(test.getRequestBody(), environmentVariables);
                    put.setEntity(new StringEntity(processedBody, ContentType.APPLICATION_JSON));
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
                    patch.setEntity(new StringEntity(processedBody, ContentType.APPLICATION_JSON));
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

        if (test.getHeaders() != null) {
            Map<String, String> headersMap = objectMapper.readValue(test.getHeaders(), new TypeReference<Map<String, String>>() {});
            for (Map.Entry<String, String> header : headersMap.entrySet()) {
                String processedValue = processEnvironmentVariables(header.getValue(), environmentVariables);
                request.addHeader(header.getKey(), processedValue);
            }
        }
        
        // Auto-inject project tokens if available
        if (test.getProjectId() != null) {
            injectProjectTokens(request, test.getProjectId());
        }
        
        // Timeout is set in executeHttpTest/executeGraphQLTest using RequestConfig
        return request;
    }

    /**
     * Injects project tokens into the request.
     *
     * @param request the HTTP request
     * @param projectId the project ID
     * @throws URISyntaxException if an error occurs
     */
    private void injectProjectTokens(HttpUriRequestBase request, UUID projectId) throws URISyntaxException {
        List<ProjectToken> tokens = projectTokenService.getActiveTokens(projectId);
        
        for (ProjectToken token : tokens) {
            if ("header".equals(token.getTokenLocation())) {
                String headerName = token.getHeaderName();
                String headerValue = token.getTokenValue();
                
                // Format the header value based on token type
                if ("Bearer".equals(token.getTokenType())) {
                    headerValue = "Bearer " + headerValue;
                } else if ("Basic".equals(token.getTokenType())) {
                    headerValue = "Basic " + headerValue;
                }
                
                // Set the header
                request.setHeader(headerName, headerValue);
            } else if ("query".equals(token.getTokenLocation())) {
                // For query parameters, we need to modify the URI
                // This is a simplified implementation - in a real app, you'd need to handle existing query params
                String currentUri = request.getUri().toString();
                String paramName = token.getParameterName();
                String paramValue = token.getTokenValue();
                
                String separator = currentUri.contains("?") ? "&" : "?";
                String newUri = currentUri + separator + paramName + "=" + paramValue;
                
                try {
                    request.setUri(new URI(newUri));
                } catch (URISyntaxException e) {
                    throw new TestExecutionException("Invalid URI: " + newUri, e);
                }
            }
            // Note: cookie and body token locations would require additional implementation
        }
    }

    /**
     * Executes a script (pre-request or post-request).
     *
     * @param script the script to execute
     * @param statusCode the response status code (for post-request scripts)
     * @param responseBody the response body (for post-request scripts)
     * @param responseHeaders the response headers (for post-request scripts)
     * @throws Exception if an error occurs during script execution
     */
    private void executeScript(String script, Integer statusCode, String responseBody, Map<String, String> responseHeaders) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        
        // Set variables for the script
        if (statusCode != null) {
            engine.put("statusCode", statusCode);
        }
        
        if (responseBody != null) {
            engine.put("responseBody", responseBody);
        }
        
        if (responseHeaders != null) {
            engine.put("responseHeaders", responseHeaders);
        }
        
        try {
            // Add utility functions
            engine.eval("function isJson(str) { try { JSON.parse(str); return true; } catch (e) { return false; } }");
            
            // Execute the script
            engine.eval(script);
        } catch (Exception e) {
            throw new Exception("Error executing script: " + e.getMessage(), e);
        }
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
        if (test.getExpectedResponseBody() != null && !test.getExpectedResponseBody().isEmpty() 
                && !test.getExpectedResponseBody().equals(responseBody)) {
            return false;
        }
        
        // Execute validation script if present
        if (test.getValidationScript() != null && !test.getValidationScript().isEmpty()) {
            try {
                return executeValidationScript(test.getValidationScript(), statusCode, responseBody);
            } catch (Exception e) {
                // Log the error and return false
                System.err.println("Error executing validation script: " + e.getMessage());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Executes a validation script and returns the result.
     *
     * @param script the script to execute
     * @param statusCode the response status code
     * @param responseBody the response body
     * @return the result of the script execution (true if valid, false otherwise)
     * @throws Exception if an error occurs during script execution
     */
    private boolean executeValidationScript(String script, int statusCode, String responseBody) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");
        
        // Set variables for the script
        engine.put("statusCode", statusCode);
        engine.put("responseBody", responseBody);
        
        try {
            // Add utility functions
            engine.eval("function isJson(str) { try { JSON.parse(str); return true; } catch (e) { return false; } }");
            
            // Execute the script
            Object result = engine.eval(script);
            
            // Convert result to boolean
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result != null) {
                return Boolean.parseBoolean(result.toString());
            }
            
            return false;
        } catch (Exception e) {
            throw new Exception("Error executing validation script: " + e.getMessage(), e);
        }
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

    /**
     * Record for HTTP test results.
     */
    private record HttpResult(int statusCode, String responseBody, Map<String, String> responseHeaders) {}
}
