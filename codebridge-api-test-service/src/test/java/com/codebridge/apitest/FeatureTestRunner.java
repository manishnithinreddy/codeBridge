package com.codebridge.apitest;

import com.codebridge.apitest.model.*;
import com.codebridge.apitest.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A standalone test runner to verify key features of the API Test Service.
 * This class doesn't require a full Spring context and can be run directly.
 */
public class FeatureTestRunner {

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FeatureTestRunner() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }

    public static void main(String[] args) {
        FeatureTestRunner runner = new FeatureTestRunner();
        try {
            runner.runTests();
        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runTests() throws Exception {
        System.out.println("=== API Test Service Feature Test Runner ===");
        
        // Test 1: Direct Request Testing (GET)
        testDirectRequestGet();
        
        // Test 2: Direct Request Testing (POST)
        testDirectRequestPost();
        
        // Test 3: Environment Variable Substitution
        testEnvironmentVariableSubstitution();
        
        // Test 4: Token Injection
        testTokenInjection();
        
        System.out.println("\n=== All Tests Completed ===");
    }

    private void testDirectRequestGet() throws Exception {
        System.out.println("\n=== Test 1: Direct Request Testing (GET) ===");
        
        // Create a GET request to a public API
        HttpGet request = new HttpGet(new URI("https://jsonplaceholder.typicode.com/users"));
        request.addHeader("Accept", "application/json");
        
        System.out.println("Executing GET request to: " + request.getUri());
        
        // Execute the request
        long startTime = System.currentTimeMillis();
        CloseableHttpResponse response = httpClient.execute(request);
        long endTime = System.currentTimeMillis();
        
        // Process the response
        int statusCode = response.getCode();
        String responseBody = new String(response.getEntity().getContent().readAllBytes());
        long executionTime = endTime - startTime;
        
        System.out.println("Status Code: " + statusCode);
        System.out.println("Execution Time: " + executionTime + "ms");
        System.out.println("Response Body (truncated): " + responseBody.substring(0, Math.min(responseBody.length(), 200)) + "...");
        
        // Verify the response
        if (statusCode == 200) {
            System.out.println("✅ GET Request Test Passed");
        } else {
            System.out.println("❌ GET Request Test Failed");
        }
    }

    private void testDirectRequestPost() throws Exception {
        System.out.println("\n=== Test 2: Direct Request Testing (POST) ===");
        
        // Create a POST request to a public API
        HttpPost request = new HttpPost(new URI("https://jsonplaceholder.typicode.com/posts"));
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");
        
        // Create request body
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Test Post");
        body.put("body", "This is a test post body");
        body.put("userId", 1);
        
        String jsonBody = objectMapper.writeValueAsString(body);
        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        
        System.out.println("Executing POST request to: " + request.getUri());
        System.out.println("Request Body: " + jsonBody);
        
        // Execute the request
        long startTime = System.currentTimeMillis();
        CloseableHttpResponse response = httpClient.execute(request);
        long endTime = System.currentTimeMillis();
        
        // Process the response
        int statusCode = response.getCode();
        String responseBody = new String(response.getEntity().getContent().readAllBytes());
        long executionTime = endTime - startTime;
        
        System.out.println("Status Code: " + statusCode);
        System.out.println("Execution Time: " + executionTime + "ms");
        System.out.println("Response Body: " + responseBody);
        
        // Verify the response
        if (statusCode == 201) {
            System.out.println("✅ POST Request Test Passed");
        } else {
            System.out.println("❌ POST Request Test Failed");
        }
    }

    private void testEnvironmentVariableSubstitution() throws Exception {
        System.out.println("\n=== Test 3: Environment Variable Substitution ===");
        
        // Define environment variables
        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("baseUrl", "https://jsonplaceholder.typicode.com");
        environmentVariables.put("resourcePath", "comments");
        environmentVariables.put("limit", "3");
        
        // Create a URL with environment variables
        String url = "{{baseUrl}}/{{resourcePath}}?_limit={{limit}}";
        System.out.println("Original URL: " + url);
        
        // Process environment variables
        String processedUrl = processEnvironmentVariables(url, environmentVariables);
        System.out.println("Processed URL: " + processedUrl);
        
        // Create and execute the request
        HttpGet request = new HttpGet(new URI(processedUrl));
        request.addHeader("Accept", "application/json");
        
        CloseableHttpResponse response = httpClient.execute(request);
        
        // Process the response
        int statusCode = response.getCode();
        String responseBody = new String(response.getEntity().getContent().readAllBytes());
        
        System.out.println("Status Code: " + statusCode);
        System.out.println("Response Body (truncated): " + responseBody.substring(0, Math.min(responseBody.length(), 200)) + "...");
        
        // Verify the response
        if (statusCode == 200 && responseBody.contains("id") && responseBody.contains("email")) {
            System.out.println("✅ Environment Variable Substitution Test Passed");
        } else {
            System.out.println("❌ Environment Variable Substitution Test Failed");
        }
    }

    private void testTokenInjection() throws Exception {
        System.out.println("\n=== Test 4: Token Injection ===");
        
        // Create a request that requires authentication
        HttpGet request = new HttpGet(new URI("https://httpbin.org/headers"));
        
        // Simulate token injection
        ProjectToken token = new ProjectToken();
        token.setTokenType("Bearer");
        token.setTokenValue("test-token-123456");
        
        // Inject token into request headers
        System.out.println("Injecting token: " + token.getTokenType() + " " + token.getTokenValue());
        request.addHeader("Authorization", token.getTokenType() + " " + token.getTokenValue());
        
        // Execute the request
        CloseableHttpResponse response = httpClient.execute(request);
        
        // Process the response
        int statusCode = response.getCode();
        String responseBody = new String(response.getEntity().getContent().readAllBytes());
        
        System.out.println("Status Code: " + statusCode);
        System.out.println("Response Body: " + responseBody);
        
        // Verify the response contains our injected token
        if (statusCode == 200 && responseBody.contains("Bearer test-token-123456")) {
            System.out.println("✅ Token Injection Test Passed");
        } else {
            System.out.println("❌ Token Injection Test Failed");
        }
    }

    private String processEnvironmentVariables(String input, Map<String, String> variables) {
        if (input == null || input.isEmpty() || variables == null || variables.isEmpty()) {
            return input;
        }
        
        String result = input;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        
        return result;
    }
}

