package com.codebridge.apitest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Demonstration of snapshot testing functionality.
 * This test shows how API responses can be compared with stored snapshots.
 */
public class SnapshotTestingDemo {

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Path snapshotsDir;

    public SnapshotTestingDemo() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
        this.snapshotsDir = Paths.get("src/test/resources/snapshots");
        
        // Create snapshots directory if it doesn't exist
        try {
            Files.createDirectories(snapshotsDir);
        } catch (IOException e) {
            System.err.println("Error creating snapshots directory: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SnapshotTestingDemo demo = new SnapshotTestingDemo();
        try {
            demo.runSnapshotTests();
        } catch (Exception e) {
            System.err.println("Error running snapshot tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runSnapshotTests() throws Exception {
        System.out.println("=== Snapshot Testing Demo ===");
        
        // Test 1: Create a snapshot
        System.out.println("\n=== Test 1: Create a Snapshot ===");
        JsonNode userResponse = executeGetRequest("https://jsonplaceholder.typicode.com/users/1");
        createSnapshot("user1", userResponse);
        
        // Test 2: Compare with matching snapshot
        System.out.println("\n=== Test 2: Compare with Matching Snapshot ===");
        JsonNode sameUserResponse = executeGetRequest("https://jsonplaceholder.typicode.com/users/1");
        compareWithSnapshot("user1", sameUserResponse);
        
        // Test 3: Compare with modified snapshot
        System.out.println("\n=== Test 3: Compare with Modified Response ===");
        JsonNode modifiedResponse = modifyResponse(userResponse);
        compareWithSnapshot("user1", modifiedResponse);
        
        // Test 4: Create and compare a different snapshot
        System.out.println("\n=== Test 4: Create and Compare Different Snapshot ===");
        JsonNode postsResponse = executeGetRequest("https://jsonplaceholder.typicode.com/posts/1");
        createSnapshot("post1", postsResponse);
        JsonNode samePostResponse = executeGetRequest("https://jsonplaceholder.typicode.com/posts/1");
        compareWithSnapshot("post1", samePostResponse);
        
        System.out.println("\n=== Snapshot Testing Demo Completed ===");
    }

    private JsonNode executeGetRequest(String url) throws URISyntaxException, IOException {
        System.out.println("Executing GET request to: " + url);
        
        HttpGet request = new HttpGet(new URI(url));
        request.addHeader("Accept", "application/json");
        
        CloseableHttpResponse response = httpClient.execute(request);
        int statusCode = response.getCode();
        
        System.out.println("Status Code: " + statusCode);
        
        JsonNode responseJson = objectMapper.readTree(response.getEntity().getContent());
        System.out.println("Response: " + responseJson.toString());
        
        return responseJson;
    }

    private void createSnapshot(String name, JsonNode response) throws IOException {
        Path snapshotPath = snapshotsDir.resolve(name + ".json");
        
        // Write the response to the snapshot file
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(snapshotPath.toFile(), response);
        
        System.out.println("Created snapshot: " + snapshotPath);
    }

    private void compareWithSnapshot(String name, JsonNode response) throws IOException {
        Path snapshotPath = snapshotsDir.resolve(name + ".json");
        
        if (!Files.exists(snapshotPath)) {
            System.out.println("Snapshot does not exist: " + snapshotPath);
            return;
        }
        
        // Read the snapshot
        JsonNode snapshot = objectMapper.readTree(snapshotPath.toFile());
        
        // Compare the response with the snapshot
        Map<String, Object> differences = compareJsonNodes(snapshot, response);
        
        if (differences.isEmpty()) {
            System.out.println("✅ Response matches snapshot");
        } else {
            System.out.println("❌ Response differs from snapshot");
            System.out.println("Differences:");
            differences.forEach((path, diff) -> {
                System.out.println("  " + path + ": " + diff);
            });
        }
    }

    private JsonNode modifyResponse(JsonNode response) {
        // Create a copy of the response and modify it
        ObjectNode modifiedResponse = response.deepCopy();
        
        // Modify some fields
        modifiedResponse.put("name", "Modified Name");
        modifiedResponse.put("email", "modified@example.com");
        
        return modifiedResponse;
    }

    private Map<String, Object> compareJsonNodes(JsonNode expected, JsonNode actual) {
        Map<String, Object> differences = new HashMap<>();
        compareNodes("$", expected, actual, differences);
        return differences;
    }

    private void compareNodes(String path, JsonNode expected, JsonNode actual, Map<String, Object> differences) {
        if (expected.equals(actual)) {
            return;
        }
        
        if (expected.getNodeType() != actual.getNodeType()) {
            differences.put(path, "Type mismatch: expected " + expected.getNodeType() + ", got " + actual.getNodeType());
            return;
        }
        
        if (expected.isObject()) {
            // Compare object fields
            Iterator<Map.Entry<String, JsonNode>> fields = expected.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                String fieldPath = path + "." + fieldName;
                
                if (actual.has(fieldName)) {
                    compareNodes(fieldPath, field.getValue(), actual.get(fieldName), differences);
                } else {
                    differences.put(fieldPath, "Field missing in actual");
                }
            }
            
            // Check for extra fields in actual
            fields = actual.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                String fieldPath = path + "." + fieldName;
                
                if (!expected.has(fieldName)) {
                    differences.put(fieldPath, "Extra field in actual");
                }
            }
        } else if (expected.isArray()) {
            // Compare array elements
            if (expected.size() != actual.size()) {
                differences.put(path, "Array size mismatch: expected " + expected.size() + ", got " + actual.size());
                return;
            }
            
            for (int i = 0; i < expected.size(); i++) {
                compareNodes(path + "[" + i + "]", expected.get(i), actual.get(i), differences);
            }
        } else {
            // Compare primitive values
            differences.put(path, "Value mismatch: expected " + expected + ", got " + actual);
        }
    }
}

