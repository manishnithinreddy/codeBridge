import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A standalone demo of the API Test Service features.
 * This program doesn't depend on the codebase but demonstrates the key features.
 */
public class ApiTestDemo {

    private final HttpClient httpClient;
    private final Map<String, String> environmentVariables;
    private final Map<String, String> chainVariables;

    public ApiTestDemo() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        this.environmentVariables = new HashMap<>();
        this.chainVariables = new HashMap<>();
        
        // Initialize environment variables
        environmentVariables.put("baseUrl", "https://jsonplaceholder.typicode.com");
        environmentVariables.put("postsPath", "posts");
        environmentVariables.put("usersPath", "users");
        environmentVariables.put("limit", "3");
    }

    public static void main(String[] args) {
        ApiTestDemo demo = new ApiTestDemo();
        
        try {
            System.out.println("=== API Test Service Feature Demo ===\n");
            
            // Test 1: Direct Request Testing (GET)
            demo.testDirectRequestGet();
            
            // Test 2: Direct Request Testing (POST)
            demo.testDirectRequestPost();
            
            // Test 3: Environment Variable Substitution
            demo.testEnvironmentVariableSubstitution();
            
            // Test 4: Token Injection
            demo.testTokenInjection();
            
            // Test 5: Chained Requests
            demo.testChainedRequests();
            
            System.out.println("\n=== All Tests Completed Successfully ===");
            
        } catch (Exception e) {
            System.err.println("Error running demo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testDirectRequestGet() throws Exception {
        System.out.println("=== Test 1: Direct Request Testing (GET) ===");
        
        // Create a GET request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://jsonplaceholder.typicode.com/users/1"))
                .header("Accept", "application/json")
                .GET()
                .build();
        
        System.out.println("Executing GET request to: " + request.uri());
        
        // Execute the request
        long startTime = System.currentTimeMillis();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();
        
        // Process the response
        int statusCode = response.statusCode();
        String responseBody = response.body();
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
        
        System.out.println();
    }

    private void testDirectRequestPost() throws Exception {
        System.out.println("=== Test 2: Direct Request Testing (POST) ===");
        
        // Create request body
        String jsonBody = "{"
            + "\"title\": \"Test Post\","
            + "\"body\": \"This is a test post body\","
            + "\"userId\": 1"
            + "}";
        
        // Create a POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://jsonplaceholder.typicode.com/posts"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        
        System.out.println("Executing POST request to: " + request.uri());
        System.out.println("Request Body: " + jsonBody);
        
        // Execute the request
        long startTime = System.currentTimeMillis();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();
        
        // Process the response
        int statusCode = response.statusCode();
        String responseBody = response.body();
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
        
        System.out.println();
    }

    private void testEnvironmentVariableSubstitution() throws Exception {
        System.out.println("=== Test 3: Environment Variable Substitution ===");
        
        // Create a URL with environment variables
        String url = "{{baseUrl}}/{{usersPath}}?_limit={{limit}}";
        System.out.println("Original URL: " + url);
        
        // Process environment variables
        String processedUrl = processEnvironmentVariables(url);
        System.out.println("Processed URL: " + processedUrl);
        
        // Create and execute the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(processedUrl))
                .header("Accept", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Process the response
        int statusCode = response.statusCode();
        String responseBody = response.body();
        
        System.out.println("Status Code: " + statusCode);
        System.out.println("Response Body (truncated): " + responseBody.substring(0, Math.min(responseBody.length(), 200)) + "...");
        
        // Verify the response
        if (statusCode == 200 && responseBody.contains("id") && responseBody.contains("name")) {
            System.out.println("✅ Environment Variable Substitution Test Passed");
        } else {
            System.out.println("❌ Environment Variable Substitution Test Failed");
        }
        
        System.out.println();
    }

    private void testTokenInjection() throws Exception {
        System.out.println("=== Test 4: Token Injection ===");
        
        // Simulate token data
        String tokenType = "Bearer";
        String tokenValue = "test-token-" + UUID.randomUUID().toString().substring(0, 8);
        
        // Create a request that requires authentication
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://httpbin.org/headers"))
                .header("Authorization", tokenType + " " + tokenValue)
                .GET()
                .build();
        
        System.out.println("Injecting token: " + tokenType + " " + tokenValue);
        
        // Execute the request
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Process the response
        int statusCode = response.statusCode();
        String responseBody = response.body();
        
        System.out.println("Status Code: " + statusCode);
        System.out.println("Response Body: " + responseBody);
        
        // Verify the response contains our injected token
        if (statusCode == 200 && responseBody.contains(tokenValue)) {
            System.out.println("✅ Token Injection Test Passed");
        } else {
            System.out.println("❌ Token Injection Test Failed");
        }
        
        System.out.println();
    }

    private void testChainedRequests() throws Exception {
        System.out.println("=== Test 5: Chained Requests ===");
        
        // Step 1: Get a user
        System.out.println("Step 1: Get user data");
        HttpRequest userRequest = HttpRequest.newBuilder()
                .uri(new URI("https://jsonplaceholder.typicode.com/users/1"))
                .header("Accept", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());
        String userResponseBody = userResponse.body();
        
        System.out.println("User Response: " + userResponseBody);
        
        // Extract user ID and name from response
        // Note: In a real implementation, we would use a JSON parser
        String userId = extractValue(userResponseBody, "\"id\":\\s*(\\d+)");
        String userName = extractValue(userResponseBody, "\"name\":\\s*\"([^\"]*)\"");
        
        chainVariables.put("userId", userId);
        chainVariables.put("userName", userName);
        
        System.out.println("Extracted variables:");
        System.out.println("  userId: " + userId);
        System.out.println("  userName: " + userName);
        
        // Step 2: Create a post using the extracted user ID
        System.out.println("\nStep 2: Create a post using extracted user ID");
        
        String postBody = "{"
            + "\"title\": \"Post by {{userName}}\","
            + "\"body\": \"This is a test post created for user with ID {{userId}}\","
            + "\"userId\": {{userId}}"
            + "}";
        
        String processedBody = processChainVariables(postBody);
        System.out.println("Processed Body: " + processedBody);
        
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(new URI("https://jsonplaceholder.typicode.com/posts"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(processedBody))
                .build();
        
        HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
        String postResponseBody = postResponse.body();
        
        System.out.println("Post Response: " + postResponseBody);
        
        // Extract post ID from response
        String postId = extractValue(postResponseBody, "\"id\":\\s*(\\d+)");
        chainVariables.put("postId", postId);
        
        System.out.println("Extracted postId: " + postId);
        
        // Step 3: Get comments for the post
        System.out.println("\nStep 3: Get comments for the post");
        
        String commentsUrl = "https://jsonplaceholder.typicode.com/posts/{{postId}}/comments";
        String processedUrl = processChainVariables(commentsUrl);
        
        System.out.println("Processed URL: " + processedUrl);
        
        HttpRequest commentsRequest = HttpRequest.newBuilder()
                .uri(new URI(processedUrl))
                .header("Accept", "application/json")
                .GET()
                .build();
        
        HttpResponse<String> commentsResponse = httpClient.send(commentsRequest, HttpResponse.BodyHandlers.ofString());
        String commentsResponseBody = commentsResponse.body();
        
        System.out.println("Comments Response (truncated): " + 
                commentsResponseBody.substring(0, Math.min(commentsResponseBody.length(), 200)) + "...");
        
        // Verify the chain worked correctly
        if (commentsResponse.statusCode() == 200 && commentsResponseBody.contains("email")) {
            System.out.println("✅ Chained Requests Test Passed");
        } else {
            System.out.println("❌ Chained Requests Test Failed");
        }
    }

    private String processEnvironmentVariables(String input) {
        if (input == null || input.isEmpty() || environmentVariables.isEmpty()) {
            return input;
        }
        
        String result = input;
        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        
        return result;
    }

    private String processChainVariables(String input) {
        if (input == null || input.isEmpty() || chainVariables.isEmpty()) {
            return input;
        }
        
        String result = input;
        for (Map.Entry<String, String> entry : chainVariables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        
        return result;
    }

    private String extractValue(String json, String pattern) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}

