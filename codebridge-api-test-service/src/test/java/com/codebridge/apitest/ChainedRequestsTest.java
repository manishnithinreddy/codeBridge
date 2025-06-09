package com.codebridge.apitest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test for chained requests feature.
 * This test demonstrates how variables from one request can be used in subsequent requests.
 */
public class ChainedRequestsTest {

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> chainVariables;

    public ChainedRequestsTest() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
        this.chainVariables = new HashMap<>();
    }

    public static void main(String[] args) {
        ChainedRequestsTest test = new ChainedRequestsTest();
        try {
            test.runChainedRequests();
        } catch (Exception e) {
            System.err.println("Error running chained requests test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runChainedRequests() throws Exception {
        System.out.println("=== Chained Requests Test ===");
        
        // Step 1: Get a user from the API
        JsonNode userResponse = executeGetRequest("https://jsonplaceholder.typicode.com/users/1");
        
        // Extract variables from the response
        extractVariables(userResponse, Map.of(
            "$.id", "userId",
            "$.name", "userName"
        ));
        
        System.out.println("Extracted variables:");
        chainVariables.forEach((key, value) -> System.out.println("  " + key + ": " + value));
        
        // Step 2: Use the extracted variables in a new request
        String postBody = "{"
            + "\"title\": \"Post by {{userName}}\","
            + "\"body\": \"This is a test post created for user with ID {{userId}}\","
            + "\"userId\": {{userId}}"
            + "}";
        
        // Process variables in the body
        String processedBody = processVariables(postBody);
        System.out.println("\nProcessed request body: " + processedBody);
        
        // Execute the second request
        JsonNode postResponse = executePostRequest(
            "https://jsonplaceholder.typicode.com/posts",
            processedBody
        );
        
        // Extract variables from the second response
        extractVariables(postResponse, Map.of(
            "$.id", "postId",
            "$.title", "postTitle"
        ));
        
        System.out.println("\nUpdated variables after second request:");
        chainVariables.forEach((key, value) -> System.out.println("  " + key + ": " + value));
        
        // Step 3: Use variables from both previous requests
        String url = "https://jsonplaceholder.typicode.com/posts/{{postId}}/comments?userId={{userId}}";
        String processedUrl = processVariables(url);
        
        System.out.println("\nProcessed URL for third request: " + processedUrl);
        
        // Execute the third request
        JsonNode commentsResponse = executeGetRequest(processedUrl);
        
        System.out.println("\nChained requests completed successfully!");
        System.out.println("This demonstrates how variables can be extracted from responses and used in subsequent requests.");
    }

    private JsonNode executeGetRequest(String url) throws URISyntaxException, IOException {
        System.out.println("\nExecuting GET request to: " + url);
        
        HttpGet request = new HttpGet(new URI(url));
        request.addHeader("Accept", "application/json");
        
        CloseableHttpResponse response = httpClient.execute(request);
        int statusCode = response.getCode();
        
        System.out.println("Status Code: " + statusCode);
        
        JsonNode responseJson = objectMapper.readTree(response.getEntity().getContent());
        System.out.println("Response (truncated): " + responseJson.toString().substring(0, Math.min(responseJson.toString().length(), 200)) + "...");
        
        return responseJson;
    }

    private JsonNode executePostRequest(String url, String body) throws URISyntaxException, IOException {
        System.out.println("\nExecuting POST request to: " + url);
        System.out.println("Request Body: " + body);
        
        HttpPost request = new HttpPost(new URI(url));
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        
        CloseableHttpResponse response = httpClient.execute(request);
        int statusCode = response.getCode();
        
        System.out.println("Status Code: " + statusCode);
        
        JsonNode responseJson = objectMapper.readTree(response.getEntity().getContent());
        System.out.println("Response: " + responseJson.toString());
        
        return responseJson;
    }

    private void extractVariables(JsonNode response, Map<String, String> variableMappings) {
        variableMappings.forEach((jsonPath, variableName) -> {
            String value = extractValueFromJsonPath(response, jsonPath);
            if (value != null) {
                chainVariables.put(variableName, value);
            }
        });
    }

    private String extractValueFromJsonPath(JsonNode json, String jsonPath) {
        // Simple JsonPath implementation for demonstration
        // Only supports basic paths like $.id, $.name, etc.
        if (jsonPath.startsWith("$.")) {
            String[] parts = jsonPath.substring(2).split("\\.");
            JsonNode current = json;
            
            for (String part : parts) {
                if (current.has(part)) {
                    current = current.get(part);
                } else {
                    return null;
                }
            }
            
            return current.isTextual() ? current.asText() : current.toString();
        }
        
        return null;
    }

    private String processVariables(String input) {
        if (input == null || input.isEmpty() || chainVariables.isEmpty()) {
            return input;
        }
        
        String result = input;
        for (Map.Entry<String, String> entry : chainVariables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        
        return result;
    }
}

