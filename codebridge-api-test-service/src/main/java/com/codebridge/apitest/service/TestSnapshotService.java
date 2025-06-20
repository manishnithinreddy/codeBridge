package com.codebridge.apitest.service;

import com.codebridge.apitest.exception.AccessDeniedException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.ApiTest;
import com.codebridge.apitest.model.TestResult;
import com.codebridge.apitest.model.TestSnapshot;
import com.codebridge.apitest.model.enums.SharePermissionLevel;
import com.codebridge.apitest.repository.ApiTestRepository;
import com.codebridge.apitest.repository.TestSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing test snapshots.
 */
@Service
public class TestSnapshotService {

    private final TestSnapshotRepository testSnapshotRepository;
    private final ApiTestRepository apiTestRepository;
    private final ProjectSharingService projectSharingService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TestSnapshotService(
            TestSnapshotRepository testSnapshotRepository,
            ApiTestRepository apiTestRepository,
            ProjectSharingService projectSharingService,
            ObjectMapper objectMapper) {
        this.testSnapshotRepository = testSnapshotRepository;
        this.apiTestRepository = apiTestRepository;
        this.projectSharingService = projectSharingService;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all snapshots for a test.
     *
     * @param testId the test ID
     * @param userId the user ID
     * @return list of snapshots
     */
    public List<TestSnapshot> getSnapshots(Long testId, Long userId) {
        ApiTest test = apiTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));
        
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(test.getProjectId(), userId);
        
        return testSnapshotRepository.findByTestId(testId);
    }

    /**
     * Get a snapshot by ID.
     *
     * @param snapshotId the snapshot ID
     * @param userId the user ID
     * @return the snapshot
     * @throws ResourceNotFoundException if the snapshot is not found
     */
    public TestSnapshot getSnapshot(Long snapshotId, Long userId) {
        TestSnapshot snapshot = testSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Snapshot not found"));
        
        ApiTest test = apiTestRepository.findById(snapshot.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));
        
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(test.getProjectId(), userId);
        
        return snapshot;
    }

    /**
     * Create a snapshot from a test result.
     *
     * @param testId the test ID
     * @param result the test result
     * @param name the snapshot name
     * @param userId the user ID
     * @return the created snapshot
     * @throws ResourceNotFoundException if the test is not found
     */
    @Transactional
    public TestSnapshot createSnapshot(Long testId, TestResult result, String name, Long userId) {
        ApiTest test = apiTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));
        
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(test.getProjectId(), userId, SharePermissionLevel.WRITE);
        
        // Check if a snapshot with this name already exists
        Optional<TestSnapshot> existingSnapshot = testSnapshotRepository.findByTestIdAndName(testId, name);
        if (existingSnapshot.isPresent()) {
            throw new IllegalArgumentException("A snapshot with this name already exists");
        }
        
        TestSnapshot snapshot = new TestSnapshot();
        snapshot.setTestId(testId);
        snapshot.setName(name);
        snapshot.setCreatedBy(userId);
        snapshot.setApproved(false);
        
        // Copy relevant data from the test result
        snapshot.setRequestBody(result.getRequestBody());
        snapshot.setResponseBody(result.getResponseBody());
        snapshot.setResponseStatus(result.getResponseStatus());
        snapshot.setResponseTime(result.getResponseTime());
        
        // Convert headers to JSON
        try {
            if (result.getRequestHeaders() != null) {
                snapshot.setRequestHeaders(objectMapper.writeValueAsString(result.getRequestHeaders()));
            }
            if (result.getResponseHeaders() != null) {
                snapshot.setResponseHeaders(objectMapper.writeValueAsString(result.getResponseHeaders()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON", e);
        }
        
        return testSnapshotRepository.save(snapshot);
    }

    /**
     * Approve a snapshot.
     *
     * @param snapshotId the snapshot ID
     * @param userId the user ID
     * @return the approved snapshot
     * @throws ResourceNotFoundException if the snapshot is not found
     */
    @Transactional
    public TestSnapshot approveSnapshot(Long snapshotId, Long userId) {
        TestSnapshot snapshot = testSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Snapshot not found"));
        
        ApiTest test = apiTestRepository.findById(snapshot.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));
        
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(test.getProjectId(), userId, SharePermissionLevel.WRITE);
        
        // Unapprove any previously approved snapshots for this test
        testSnapshotRepository.findFirstByTestIdAndApprovedTrueOrderByCreatedAtDesc(snapshot.getTestId())
                .ifPresent(previousSnapshot -> {
                    previousSnapshot.setApproved(false);
                    testSnapshotRepository.save(previousSnapshot);
                });
        
        // Approve this snapshot
        snapshot.setApproved(true);
        snapshot.setApprovedBy(userId);
        snapshot.setApprovedAt(LocalDateTime.now());
        
        return testSnapshotRepository.save(snapshot);
    }

    /**
     * Delete a snapshot.
     *
     * @param snapshotId the snapshot ID
     * @param userId the user ID
     * @throws ResourceNotFoundException if the snapshot is not found
     */
    @Transactional
    public void deleteSnapshot(Long snapshotId, Long userId) {
        TestSnapshot snapshot = testSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Snapshot not found"));
        
        ApiTest test = apiTestRepository.findById(snapshot.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));
        
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(test.getProjectId(), userId, SharePermissionLevel.WRITE);
        
        testSnapshotRepository.delete(snapshot);
    }

    /**
     * Compare a test result with the latest approved snapshot.
     *
     * @param testId the test ID
     * @param result the test result
     * @return comparison result map
     */
    public Map<String, Object> compareWithSnapshot(Long testId, TestResult result) {
        Optional<TestSnapshot> snapshotOpt = testSnapshotRepository.findFirstByTestIdAndApprovedTrueOrderByCreatedAtDesc(testId);
        
        if (snapshotOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("hasSnapshot", false);
            response.put("message", "No approved snapshot found for this test");
            return response;
        }
        
        TestSnapshot snapshot = snapshotOpt.get();
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("hasSnapshot", true);
        comparison.put("snapshotId", snapshot.getId());
        comparison.put("snapshotName", snapshot.getName());
        
        // Compare status code
        boolean statusMatch = snapshot.getResponseStatus() == result.getResponseStatus();
        comparison.put("statusMatch", statusMatch);
        
        // Compare response bodies
        boolean bodyMatch = false;
        String bodyDiff = null;
        
        try {
            if (snapshot.getResponseBody() != null && result.getResponseBody() != null) {
                JsonNode snapshotJson = objectMapper.readTree(snapshot.getResponseBody());
                JsonNode resultJson = objectMapper.readTree(result.getResponseBody());
                
                bodyMatch = snapshotJson.equals(resultJson);
                if (!bodyMatch) {
                    // Generate a simple diff
                    bodyDiff = generateJsonDiff(snapshotJson, resultJson);
                }
            } else {
                bodyMatch = (snapshot.getResponseBody() == null && result.getResponseBody() == null) ||
                        (snapshot.getResponseBody() != null && snapshot.getResponseBody().equals(result.getResponseBody()));
            }
        } catch (JsonProcessingException e) {
            // If we can't parse as JSON, do a simple string comparison
            bodyMatch = (snapshot.getResponseBody() == null && result.getResponseBody() == null) ||
                    (snapshot.getResponseBody() != null && snapshot.getResponseBody().equals(result.getResponseBody()));
        }
        
        comparison.put("bodyMatch", bodyMatch);
        if (bodyDiff != null) {
            comparison.put("bodyDiff", bodyDiff);
        }
        
        // Overall match
        comparison.put("match", statusMatch && bodyMatch);
        
        return comparison;
    }

    /**
     * Generate a simple JSON diff.
     *
     * @param expected the expected JSON
     * @param actual the actual JSON
     * @return a string representation of the diff
     */
    private String generateJsonDiff(JsonNode expected, JsonNode actual) {
        // This is a simplified diff generator
        // In a real implementation, you might use a more sophisticated diff library
        StringBuilder diff = new StringBuilder();
        
        if (expected.getNodeType() != actual.getNodeType()) {
            return "Different types: expected " + expected.getNodeType() + ", got " + actual.getNodeType();
        }
        
        if (expected.isObject()) {
            // Compare objects
            expected.fieldNames().forEachRemaining(fieldName -> {
                if (!actual.has(fieldName)) {
                    diff.append("Missing field in actual: ").append(fieldName).append("\n");
                } else if (!expected.get(fieldName).equals(actual.get(fieldName))) {
                    diff.append("Different values for field ").append(fieldName).append(":\n")
                            .append("  Expected: ").append(expected.get(fieldName)).append("\n")
                            .append("  Actual: ").append(actual.get(fieldName)).append("\n");
                }
            });
            
            actual.fieldNames().forEachRemaining(fieldName -> {
                if (!expected.has(fieldName)) {
                    diff.append("Extra field in actual: ").append(fieldName).append("\n");
                }
            });
        } else if (expected.isArray()) {
            // Compare arrays
            if (expected.size() != actual.size()) {
                diff.append("Array size mismatch: expected ").append(expected.size())
                        .append(", got ").append(actual.size()).append("\n");
            }
            
            int minSize = Math.min(expected.size(), actual.size());
            for (int i = 0; i < minSize; i++) {
                if (!expected.get(i).equals(actual.get(i))) {
                    diff.append("Different values at index ").append(i).append(":\n")
                            .append("  Expected: ").append(expected.get(i)).append("\n")
                            .append("  Actual: ").append(actual.get(i)).append("\n");
                }
            }
        } else {
            // Compare primitive values
            if (!expected.equals(actual)) {
                diff.append("Value mismatch:\n")
                        .append("  Expected: ").append(expected).append("\n")
                        .append("  Actual: ").append(actual).append("\n");
            }
        }
        
        return diff.toString();
    }
}

