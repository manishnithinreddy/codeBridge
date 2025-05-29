package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.ApiTestRequest;
import com.codebridge.apitest.dto.ApiTestResponse;
import com.codebridge.apitest.dto.EnvironmentResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.exception.TestExecutionException;
import com.codebridge.apitest.model.*;
import com.codebridge.apitest.repository.ApiTestRepository;
import com.codebridge.apitest.repository.TestResultRepository;
import com.codebridge.apitest.scripting.MutableRequestData;
import com.codebridge.apitest.scripting.ResponseData;
import com.codebridge.apitest.service.JavaScriptExecutorService.ScriptExecutionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hc.core5.net.URIBuilder;
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
    private final JavaScriptExecutorService javaScriptExecutorService;

    @Autowired
    public ApiTestService(
            ApiTestRepository apiTestRepository,
            TestResultRepository testResultRepository,
            ObjectMapper objectMapper,
            RestTemplate restTemplate,
            EnvironmentService environmentService,
            JavaScriptExecutorService javaScriptExecutorService) {
        this.apiTestRepository = apiTestRepository;
        this.testResultRepository = testResultRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.environmentService = environmentService;
        this.javaScriptExecutorService = javaScriptExecutorService;
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

        // Map AuthType
        if (request.getAuthType() != null) {
            try {
                apiTest.setAuthType(AuthType.valueOf(request.getAuthType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                apiTest.setAuthType(AuthType.NONE); // Default or log error
            }
        } else {
            apiTest.setAuthType(AuthType.NONE);
        }
        apiTest.setAuthToken(request.getAuthToken());
        apiTest.setApiKeyName(request.getApiKeyName());
        apiTest.setApiKeyValue(request.getApiKeyValue());
        if (request.getApiKeyLocation() != null) {
            try {
                apiTest.setApiKeyLocation(ApiKeyLocation.valueOf(request.getApiKeyLocation().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Default or log error, if AuthType is API_KEY, this might be an issue
                if (apiTest.getAuthType() == AuthType.API_KEY) {
                     // Consider setting AuthType to NONE or logging a warning
                }
            }
        }
        
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

        // Map AuthType
        if (request.getAuthType() != null) {
            try {
                test.setAuthType(AuthType.valueOf(request.getAuthType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                test.setAuthType(AuthType.NONE); // Default or log error
            }
        } else {
            test.setAuthType(AuthType.NONE);
        }
        test.setAuthToken(request.getAuthToken());
        test.setApiKeyName(request.getApiKeyName());
        test.setApiKeyValue(request.getApiKeyValue());
        if (request.getApiKeyLocation() != null) {
            try {
                test.setApiKeyLocation(ApiKeyLocation.valueOf(request.getApiKeyLocation().toUpperCase()));
            } catch (IllegalArgumentException e) {
                if (test.getAuthType() == AuthType.API_KEY) {
                    // Consider setting AuthType to NONE or logging
                }
            }
        } else {
            if (test.getAuthType() == AuthType.API_KEY) {
                // API_KEY type needs a location. Default or handle error.
                // test.setApiKeyLocation(null); // Or a default if appropriate
            }
        }
        
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
        return executeTest(id, userId, null); // Or Collections.emptyMap()
    }

    @Transactional
    public TestResultResponse executeTest(UUID id, UUID userId, Map<String, String> collectionVariables) {
        ApiTest test = apiTestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", id));

        TestResult result = new TestResult();
        result.setId(UUID.randomUUID());
        result.setTestId(test.getId());

        long startTime = System.currentTimeMillis();
        Map<String, String> environmentVariables = null;
        Map<String, String> scriptVariables = new HashMap<>(); // Merged variables

        MutableRequestData mutableRequestData = null;
        String processedUrl = test.getUrl();
        String processedRequestBody = test.getRequestBody();
        Map<String, String> processedHeaders = parseHeaders(test.getHeaders());
        HttpMethod processedMethod = test.getMethod();

        try {
            // Fetch environment variables
            if (test.getEnvironmentId() != null) {
                EnvironmentResponse environment = environmentService.getEnvironmentById(test.getEnvironmentId(), userId);
                environmentVariables = environment.getVariables();
            }

            // Merge environment and collection variables
            if (environmentVariables != null) {
                scriptVariables.putAll(environmentVariables);
            }
            if (collectionVariables != null) {
                scriptVariables.putAll(collectionVariables); // Collection vars override environment vars
            }
            
            // Resolve variables in auth fields
            String resolvedAuthToken = resolveVariablesInString(test.getAuthToken(), scriptVariables);
            String resolvedApiKeyName = resolveVariablesInString(test.getApiKeyName(), scriptVariables);
            String resolvedApiKeyValue = resolveVariablesInString(test.getApiKeyValue(), scriptVariables);


            // Initial processing of URL, headers, and body with merged variables BEFORE pre-request script
            String initialProcessedUrl = resolveVariablesInString(test.getUrl(), scriptVariables);
            Map<String, String> initialProcessedHeaders = parseHeaders(resolveVariablesInHeaders(test.getHeaders(), scriptVariables));
            String initialProcessedRequestBody = resolveVariablesInString(test.getRequestBody(), scriptVariables);
            String initialProcessedGraphQLQuery = resolveVariablesInString(test.getGraphqlQuery(), scriptVariables);
            String initialProcessedGraphQLVars = resolveVariablesInString(test.getGraphqlVariables(), scriptVariables);


            if (test.getPreRequestScript() != null && !test.getPreRequestScript().isEmpty()) {
                Map<String, Object> bindings = new HashMap<>();
                // For GraphQL, the "body" in MutableRequestData might be the composed GraphQL query + variables
                // or a raw JSON body if the user intends to send that.
                // Here, we assume if it's GraphQL, the script might want to manipulate query/variables individually
                // or the whole body. We'll provide the initially resolved body.
                String bodyForScript = (test.getProtocolType() == ProtocolType.GRAPHQL) ? 
                                       composeGraphQLBody(initialProcessedGraphQLQuery, initialProcessedGraphQLVars) : 
                                       initialProcessedRequestBody;

                mutableRequestData = new MutableRequestData(
                        initialProcessedUrl,
                        test.getMethod().name(),
                        initialProcessedHeaders,
                        bodyForScript
                );
                bindings.put("request", mutableRequestData);
                bindings.put("variables", new HashMap<>(scriptVariables)); // Pass a copy of merged variables to script

                try {
                    javaScriptExecutorService.executeScript(test.getPreRequestScript(), bindings);
                    // Apply changes from mutableRequestData
                    processedUrl = mutableRequestData.getUrl();
                    processedMethod = HttpMethod.valueOf(mutableRequestData.getMethod().toUpperCase());
                    processedHeaders = mutableRequestData.getHeaders();
                    processedRequestBody = mutableRequestData.getBody(); // This body is used by both HTTP and GraphQL if modified

                    // Update test object in memory for this execution - this is for current execution context only
                    // The actual ApiTest entity in DB is not changed by script execution.
                    // test.setUrl(processedUrl); // Not strictly needed as processedUrl is passed directly
                    // test.setMethod(processedMethod); // Not strictly needed
                    // test.setHeaders(serializeHeaders(processedHeaders)); // Not strictly needed
                    // test.setRequestBody(processedRequestBody); // Not strictly needed

                } catch (ScriptExecutionException e) {
                    result.setStatus(TestStatus.ERROR);
                    result.setErrorMessage("Pre-request script execution failed: " + e.getMessage());
                    testResultRepository.save(result);
                    return mapToTestResultResponse(result); 
                }
            } else {
                // If no pre-request script, use the initially processed values
                processedUrl = initialProcessedUrl;
                processedHeaders = initialProcessedHeaders;
                // For GraphQL, compose body from query and variables if not modified by script
                processedRequestBody = (test.getProtocolType() == ProtocolType.GRAPHQL) ? 
                                       composeGraphQLBody(initialProcessedGraphQLQuery, initialProcessedGraphQLVars) : 
                                       initialProcessedRequestBody;
                // processedMethod remains as is from test definition
            }

            // Execute the test based on protocol type
            if (test.getProtocolType() == null || test.getProtocolType() == ProtocolType.HTTP) {
                executeHttpTest(test, result, scriptVariables, processedUrl, processedMethod, processedHeaders, processedRequestBody,
                                resolvedAuthToken, resolvedApiKeyName, resolvedApiKeyValue);
            } else if (test.getProtocolType() == ProtocolType.GRAPHQL) {
                // For GraphQL, processedRequestBody already contains the composed query and variables
                executeGraphQLTest(test, result, scriptVariables, processedUrl, processedHeaders, processedRequestBody,
                                   resolvedAuthToken, resolvedApiKeyName, resolvedApiKeyValue);
            } else if (test.getProtocolType() == ProtocolType.GRPC) {
                executeGrpcTest(test, result, scriptVariables); // Assuming gRPC might need auth differently or not at all via this mechanism
            } else if (test.getProtocolType() == ProtocolType.WEBSOCKET) {
                executeWebSocketTest(test, result, scriptVariables); // Same for WebSocket
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
     * @param scriptVariables Merged environment and collection variables
     * @param processedUrl URL after pre-request script and variable resolution
     * @param processedMethod HTTP method after pre-request script
     * @param processedHeaders Headers after pre-request script and variable resolution
     * @param processedRequestBody Request body after pre-request script and variable resolution
     * @throws Exception if an error occurs
     */
    private void executeHttpTest(ApiTest test, TestResult result, Map<String, String> scriptVariables,
                                 String processedUrl, HttpMethod processedMethod, Map<String, String> processedHeaders, String processedRequestBody,
                                 String resolvedAuthToken, String resolvedApiKeyName, String resolvedApiKeyValue) throws Exception {

        HttpUriRequest request = createHttpRequest(
                test.getAuthType(), test.getApiKeyLocation(),
                processedMethod, processedUrl, processedHeaders, processedRequestBody,
                resolvedAuthToken, resolvedApiKeyName, resolvedApiKeyValue,
                test.getTimeoutMs()
        );

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(test.getTimeoutMs()).setSocketTimeout(test.getTimeoutMs()).build()).build()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
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

                // Validation
                boolean basicValidationPassed = validateResponse(test, statusCode, responseBody);
                boolean scriptValidationPassed = true;
                String scriptErrorMessage = null;

                if (test.getValidationScript() != null && !test.getValidationScript().isEmpty()) {
                    ResponseData responseData = new ResponseData(statusCode, headerMap, responseBody);
                    Map<String, Object> scriptBindings = new HashMap<>();
                    scriptBindings.put("response", responseData);
                    if (scriptVariables != null) {
                        scriptBindings.put("variables", new HashMap<>(scriptVariables)); // Pass a copy
                    } else {
                        scriptBindings.put("variables", new HashMap<>());
                    }
                    // TODO: Inject 'pm' object for assertions in future

                    try {
                        javaScriptExecutorService.executeScript(test.getValidationScript(), scriptBindings);
                        // If script executes without error, it's considered a pass from script's perspective
                    } catch (ScriptExecutionException e) {
                        scriptValidationPassed = false;
                        scriptErrorMessage = "Validation script execution failed: " + e.getMessage();
                        result.setErrorMessage(scriptErrorMessage); // Log script error
                    } catch (Exception e) { // Catch any other unexpected errors from script execution
                        scriptValidationPassed = false;
                        scriptErrorMessage = "Unexpected error during validation script execution: " + e.getMessage();
                        result.setErrorMessage(scriptErrorMessage);
                    }
                }

                if (basicValidationPassed && scriptValidationPassed) {
                    result.setStatus(TestStatus.SUCCESS);
                } else {
                    result.setStatus(TestStatus.FAILURE);
                    if (!basicValidationPassed && scriptErrorMessage != null) {
                        // Append to existing message if basic validation also failed
                        result.setErrorMessage("Basic validation failed. " + scriptErrorMessage);
                    } else if (!basicValidationPassed) {
                        result.setErrorMessage("Basic validation failed (e.g., status code or expected body mismatch).");
                    }
                    // If only script validation failed, its error is already set.
                }
            }
        }
    }

    /**
     * Execute a GraphQL API test.
     *
     * @param test the API test
     * @param result the test result
     * @param environmentVariables the environment variables
     * @param scriptVariables Merged environment and collection variables
     * @param processedUrl URL after pre-request script and variable resolution
     * @param processedHeaders Headers after pre-request script and variable resolution
     * @param processedRequestBody Request body (GraphQL payload) after pre-request script and variable resolution
     * @throws Exception if an error occurs
     */
    private void executeGraphQLTest(ApiTest test, TestResult result, Map<String, String> scriptVariables,
                                    String processedUrl, Map<String, String> processedHeaders, String processedRequestBody,
                                    String resolvedAuthToken, String resolvedApiKeyName, String resolvedApiKeyValue) throws Exception {
        
        // For GraphQL, the authorization is typically done via headers, similar to other HTTP POST requests.
        // The createHttpRequest method will handle adding auth headers or modifying URL for API key in query.
        // We treat GraphQL as a specialized HTTP POST.

        // Create the basic HttpPost request object; createHttpRequest will then add auth.
        HttpPost httpPost = (HttpPost) createHttpRequest(
            test.getAuthType(), test.getApiKeyLocation(),
            HttpMethod.POST, // GraphQL is always POST
            processedUrl, 
            processedHeaders, // These are script-modified headers
            processedRequestBody, // This is the GraphQL JSON payload
            resolvedAuthToken, resolvedApiKeyName, resolvedApiKeyValue,
            test.getTimeoutMs()
        );
        
        // Ensure Content-Type for GraphQL if not already set by script or auth logic
        if (httpPost.getFirstHeader("Content-Type") == null) {
             httpPost.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        }


        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(test.getTimeoutMs()).setSocketTimeout(test.getTimeoutMs()).build()).build()) {
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
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

                // Validation
                boolean basicValidationPassed = validateResponse(test, statusCode, responseBody);
                boolean scriptValidationPassed = true;
                String scriptErrorMessage = null;

                if (test.getValidationScript() != null && !test.getValidationScript().isEmpty()) {
                    ResponseData responseData = new ResponseData(statusCode, headerMap, responseBody);
                    Map<String, Object> scriptBindings = new HashMap<>();
                    scriptBindings.put("response", responseData);
                    if (scriptVariables != null) {
                        scriptBindings.put("variables", new HashMap<>(scriptVariables)); // Pass a copy
                    } else {
                        scriptBindings.put("variables", new HashMap<>());
                    }

                    try {
                        javaScriptExecutorService.executeScript(test.getValidationScript(), scriptBindings);
                    } catch (ScriptExecutionException e) {
                        scriptValidationPassed = false;
                        scriptErrorMessage = "Validation script execution failed: " + e.getMessage();
                        result.setErrorMessage(scriptErrorMessage);
                    } catch (Exception e) {
                        scriptValidationPassed = false;
                        scriptErrorMessage = "Unexpected error during validation script execution: " + e.getMessage();
                        result.setErrorMessage(scriptErrorMessage);
                    }
                }

                if (basicValidationPassed && scriptValidationPassed) {
                    result.setStatus(TestStatus.SUCCESS);
                } else {
                    result.setStatus(TestStatus.FAILURE);
                    if (!basicValidationPassed && scriptErrorMessage != null) {
                        result.setErrorMessage("Basic validation failed. " + scriptErrorMessage);
                    } else if (!basicValidationPassed) {
                        result.setErrorMessage("Basic validation failed (e.g., status code or expected body mismatch).");
                    }
                }
            }
        }
    }

    /**
     * Execute a gRPC API test.
     * Note: This is a placeholder implementation. Actual gRPC implementation would require additional dependencies.
     *
     * @param test the API test
     * @param result the test result
     * @param scriptVariables the merged script variables
     * @throws Exception if an error occurs
     */
    private void executeGrpcTest(ApiTest test, TestResult result, Map<String, String> scriptVariables) throws Exception {
        // This is a placeholder for gRPC implementation
        // TODO: Use scriptVariables for any variable substitution if needed for gRPC
        result.setStatus(TestStatus.ERROR);
        result.setErrorMessage("gRPC testing is not yet implemented");
    }

    /**
     * Execute a WebSocket API test.
     * Note: This is a placeholder implementation. Actual WebSocket implementation would require additional dependencies.
     *
     * @param test the API test
     * @param result the test result
     * @param scriptVariables the merged script variables
     * @throws Exception if an error occurs
     */
    private void executeWebSocketTest(ApiTest test, TestResult result, Map<String, String> scriptVariables) throws Exception {
        // This is a placeholder for WebSocket implementation
        // TODO: Use scriptVariables for any variable substitution if needed for WebSocket
        result.setStatus(TestStatus.ERROR);
        result.setErrorMessage("WebSocket testing is not yet implemented");
    }
    
    private String composeGraphQLBody(String query, String variablesString) throws JsonProcessingException {
        Map<String, Object> graphQLRequestBody = new HashMap<>();
        graphQLRequestBody.put("query", query);
        if (variablesString != null && !variablesString.trim().isEmpty()) {
            try {
                Map<String, Object> variablesMap = objectMapper.readValue(variablesString, new TypeReference<Map<String, Object>>() {});
                graphQLRequestBody.put("variables", variablesMap);
            } catch (JsonProcessingException e) {
                throw new TestExecutionException("Invalid GraphQL variables JSON: " + variablesString + "; error: " + e.getMessage());
            }
        }
        return objectMapper.writeValueAsString(graphQLRequestBody);
    }


    /**
     * Resolves variables in a string.
     *
     * @param input the input string
     * @param variables the map of variables to resolve
     * @return the processed string
     */
    private String resolveVariablesInString(String input, Map<String, String> variables) {
        if (input == null || variables == null || variables.isEmpty()) {
            return input;
        }
        
        String result = input;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        
        return result;
    }

    /**
     * Create an HTTP request for an API test.
     *
     * @param test the API test
     * @param processedUrl the processed URL
      * @param processedUrl         the processed URL
      * @param processedHeaders     the processed headers map
      * @param processedRequestBody the processed request body
      * @param resolvedAuthToken    resolved bearer token
      * @param resolvedApiKeyName   resolved API key name
      * @param resolvedApiKeyValue  resolved API key value
      * @param timeoutMs            timeout in milliseconds
     * @return the HTTP request
      * @throws JsonProcessingException if an error occurs during header processing
      * @throws TestExecutionException if URL is malformed for API key in query
     */
    private HttpUriRequest createHttpRequest(
            AuthType authType, ApiKeyLocation apiKeyLocation,
            HttpMethod method, String processedUrl, Map<String, String> processedHeaders, String processedRequestBody,
            String resolvedAuthToken, String resolvedApiKeyName, String resolvedApiKeyValue,
            int timeoutMs) throws JsonProcessingException, TestExecutionException {
        
        HttpUriRequestBase request;
        String urlToUse = processedUrl; // Start with the URL potentially modified by pre-request script

        // Placeholder for if API key in query modifies the URL before request object creation
        // This is tricky because HttpUriRequestBase objects take URL at construction.
        // So, if API key is in query, we need to modify urlToUse first.

        switch (method) {
            case GET:
                request = new HttpGet(urlToUse);
                break;
            case POST:
                HttpPost post = new HttpPost(urlToUse);
                if (processedRequestBody != null) {
                    post.setEntity(new StringEntity(processedRequestBody, ContentType.APPLICATION_JSON));
                }
                request = post;
                break;
            case PUT:
                HttpPut put = new HttpPut(urlToUse);
                if (processedRequestBody != null) {
                    put.setEntity(new StringEntity(processedRequestBody, ContentType.APPLICATION_JSON));
                }
                request = put;
                break;
            case DELETE:
                request = new HttpDelete(urlToUse);
                // Apache HttpClient 5 HttpDelete does not directly support setEntity.
                // If body is needed, it's a non-standard use.
                break;
            case PATCH:
                HttpPatch patch = new HttpPatch(urlToUse);
                if (processedRequestBody != null) {
                    patch.setEntity(new StringEntity(processedRequestBody, ContentType.APPLICATION_JSON));
                }
                request = patch;
                break;
            case HEAD:
                request = new HttpHead(urlToUse);
                break;
            case OPTIONS:
                request = new HttpOptions(urlToUse);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        // 1. Apply API Key to Query Parameter if needed (modifies request URI)
        if (authType == AuthType.API_KEY && apiKeyLocation == ApiKeyLocation.QUERY_PARAMETER &&
            resolvedApiKeyName != null && !resolvedApiKeyName.isEmpty() &&
            resolvedApiKeyValue != null /* allow empty value for key? */) {
            try {
                URIBuilder uriBuilder = new URIBuilder(request.getUri());
                uriBuilder.addParameter(resolvedApiKeyName, resolvedApiKeyValue);
                request.setUri(uriBuilder.build());
            } catch (URISyntaxException e) {
                throw new TestExecutionException("Error adding API key to URL query parameter: " + e.getMessage(), e);
            }
        }

        // 2. Set headers from processedHeaders (potentially modified by pre-request script)
        if (processedHeaders != null) {
            for (Map.Entry<String, String> header : processedHeaders.entrySet()) {
                request.addHeader(header.getKey(), header.getValue());
            }
        }

        // 3. Apply AuthType.BEARER_TOKEN or API_KEY (Header)
        // This will overwrite any header with the same name (e.g. "Authorization") set by script or initial headers.
        if (authType == AuthType.BEARER_TOKEN && resolvedAuthToken != null && !resolvedAuthToken.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + resolvedAuthToken);
        } else if (authType == AuthType.API_KEY && apiKeyLocation == ApiKeyLocation.HEADER &&
                   resolvedApiKeyName != null && !resolvedApiKeyName.isEmpty() &&
                   resolvedApiKeyValue != null) {
            request.setHeader(resolvedApiKeyName, resolvedApiKeyValue);
        }
        
        // Timeout can be further configured if needed, but it's primarily set on HttpClient
        // RequestConfig.Builder configBuilder = RequestConfig.custom()
        //         .setConnectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        //         .setResponseTimeout(timeoutMs, TimeUnit.MILLISECONDS) // For HttpClient 5.x
        //         .build();
        // request.setConfig(requestConfig);
        
        return request;
    }
    
    private Map<String, String> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(headersJson, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing headers JSON: " + headersJson, e);
        }
    }

    private String serializeHeaders(Map<String, String> headersMap) {
        if (headersMap == null || headersMap.isEmpty()) {
            return null; // Or "{}", depending on desired representation for empty headers
        }
        try {
            return objectMapper.writeValueAsString(headersMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing headers map to JSON", e);
        }
    }

    private String resolveVariablesInHeaders(String headersJson, Map<String, String> scriptVariables) {
        if (headersJson == null || scriptVariables == null || scriptVariables.isEmpty()) {
            return headersJson;
        }
        Map<String, String> headersMap = parseHeaders(headersJson);
        Map<String, String> processedHeadersMap = new HashMap<>();
        for (Map.Entry<String, String> entry : headersMap.entrySet()) {
            processedHeadersMap.put(entry.getKey(), resolveVariablesInString(entry.getValue(), scriptVariables));
        }
        return serializeHeaders(processedHeadersMap);
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
        // Note: Validation script execution is handled directly in executeHttpTest and executeGraphQLTest methods.
        // This method focuses on basic, declarative validations.
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

        // Map AuthType and ApiKeyLocation to String for response
        if (apiTest.getAuthType() != null) {
            response.setAuthType(apiTest.getAuthType().name());
        }
        response.setAuthToken(apiTest.getAuthToken()); // Already string
        response.setApiKeyName(apiTest.getApiKeyName()); // Already string
        response.setApiKeyValue(apiTest.getApiKeyValue()); // Already string
        if (apiTest.getApiKeyLocation() != null) {
            response.setApiKeyLocation(apiTest.getApiKeyLocation().name());
        }
        
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
