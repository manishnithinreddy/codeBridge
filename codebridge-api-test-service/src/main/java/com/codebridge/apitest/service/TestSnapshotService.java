package com.codebridge.apitest.service;

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
import java.util.UUID;

/**
 * Service for test snapshot operations.
 */
@Service
public class TestSnapshotService {
    
    private final TestSnapshotRepository testSnapshotRepository;
    private final ApiTestRepository apiTestRepository;
    private final ProjectSharingService projectSharingService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TestSnapshotService(TestSnapshotRepository testSnapshotRepository,
                              ApiTestRepository apiTestRepository,
                              ProjectSharingService projectSharingService,
                              AuditLogService auditLogService,
                              ObjectMapper objectMapper) {
        this.testSnapshotRepository = testSnapshotRepository;
        this.apiTestRepository = apiTestRepository;
        this.projectSharingService = projectSharingService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create a snapshot from a test result.
     *
     * @param testId the test ID
     * @param name the snapshot name
     * @param description the snapshot description
     * @param result the test result
     * @param userId the user ID
     * @return the created snapshot
     */
    @Transactional
    public TestSnapshot createSnapshot(UUID testId, String name, String description, TestResult result, UUID userId) {
        ApiTest test = apiTestRepository.findById(testId)
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", testId.toString()));
        
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(test.getProjectId(), userId);
        if (permission == null || permission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to create snapshots for test " + testId);
        }
        
        // Create snapshot
        TestSnapshot snapshot = new TestSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setTestId(testId);
        snapshot.setName(name);
        snapshot.setDescription(description);
        snapshot.setResponseBody(result.getResponseBody());
        snapshot.setResponseStatusCode(result.getResponseStatusCode());
        snapshot.setResponseHeaders(result.getResponseHeaders());
        snapshot.setApproved(false);
        snapshot.setCreatedBy(userId);
        
        TestSnapshot savedSnapshot = testSnapshotRepository.save(snapshot);
        
        // Log action
        auditLogService.logAction(
            userId,
            "TEST_SNAPSHOT_CREATION",
            savedSnapshot.getId(),
            "TestSnapshot",
            Map.of("testId", testId, "name", name)
        );
        
        return savedSnapshot;
    }
    
    /**
     * Get all snapshots for a test.
     *
     * @param testId the test ID
     * @param userId the user ID
     * @return the list of snapshots
     */
    @Transactional(readOnly = true)
    public List<TestSnapshot> getSnapshots(UUID testId, UUID userId) {
        ApiTest test = apiTestRepository.findById(testId)
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", testId.toString()));
        
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(test.getProjectId(), userId);
        if (permission == null) {
            throw new AccessDeniedException("User does not have permission to view snapshots for test " + testId);
        }
        
        return testSnapshotRepository.findByTestId(testId);
    }
    
    /**
     * Get a snapshot by ID.
     *
     * @param snapshotId the snapshot ID
     * @param userId the user ID
     * @return the snapshot
     */
    @Transactional(readOnly = true)
    public TestSnapshot getSnapshot(UUID snapshotId, UUID userId) {
        TestSnapshot snapshot = testSnapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new ResourceNotFoundException("TestSnapshot", "id", snapshotId.toString()));
        
        ApiTest test = apiTestRepository.findById(snapshot.getTestId())
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", snapshot.getTestId().toString()));
        
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(test.getProjectId(), userId);
        if (permission == null) {
            throw new AccessDeniedException("User does not have permission to view snapshot " + snapshotId);
        }
        
        return snapshot;
    }
    
    /**
     * Approve a snapshot.
     *
     * @param snapshotId the snapshot ID
     * @param userId the user ID
     * @return the approved snapshot
     */
    @Transactional
    public TestSnapshot approveSnapshot(UUID snapshotId, UUID userId) {
        TestSnapshot snapshot = testSnapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new ResourceNotFoundException("TestSnapshot", "id", snapshotId.toString()));
        
        ApiTest test = apiTestRepository.findById(snapshot.getTestId())
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", snapshot.getTestId().toString()));
        
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(test.getProjectId(), userId);
        if (permission == null || permission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to approve snapshot " + snapshotId);
        }
        
        // Approve snapshot
        snapshot.setApproved(true);
        
        TestSnapshot savedSnapshot = testSnapshotRepository.save(snapshot);
        
        // Log action
        auditLogService.logAction(
            userId,
            "TEST_SNAPSHOT_APPROVAL",
            savedSnapshot.getId(),
            "TestSnapshot",
            Map.of("testId", snapshot.getTestId(), "name", snapshot.getName())
        );
        
        return savedSnapshot;
    }
    
    /**
     * Delete a snapshot.
     *
     * @param snapshotId the snapshot ID
     * @param userId the user ID
     */
    @Transactional
    public void deleteSnapshot(UUID snapshotId, UUID userId) {
        TestSnapshot snapshot = testSnapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new ResourceNotFoundException("TestSnapshot", "id", snapshotId.toString()));
        
        ApiTest test = apiTestRepository.findById(snapshot.getTestId())
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", snapshot.getTestId().toString()));
        
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(test.getProjectId(), userId);
        if (permission == null || permission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to delete snapshot " + snapshotId);
        }
        
        // Log action before deletion
        auditLogService.logAction(
            userId,
            "TEST_SNAPSHOT_DELETION",
            snapshotId,
            "TestSnapshot",
            Map.of("testId", snapshot.getTestId(), "name", snapshot.getName())
        );
        
        // Delete snapshot
        testSnapshotRepository.delete(snapshot);
    }
    
    /**
     * Compare a test result with the latest approved snapshot.
     *
     * @param testId the test ID
     * @param result the test result
     * @param userId the user ID
     * @return the comparison result
     */
    @Transactional(readOnly = true)
    public SnapshotComparisonResult compareWithSnapshot(UUID testId, TestResult result, UUID userId) {
        ApiTest test = apiTestRepository.findById(testId)
            .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", testId.toString()));
        
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(test.getProjectId(), userId);
        if (permission == null) {
            throw new AccessDeniedException("User does not have permission to compare snapshots for test " + testId);
        }
        
        // Get latest approved snapshot
        TestSnapshot snapshot = testSnapshotRepository.findFirstByTestIdAndApprovedTrueOrderByCreatedAtDesc(testId)
            .orElse(null);
        
        if (snapshot == null) {
            return new SnapshotComparisonResult(false, "No approved snapshot found", null);
        }
        
        // Compare status code
        if (!snapshot.getResponseStatusCode().equals(result.getResponseStatusCode())) {
            return new SnapshotComparisonResult(
                false,
                "Status code mismatch: expected " + snapshot.getResponseStatusCode() + ", got " + result.getResponseStatusCode(),
                Map.of("statusCodeDiff", Map.of(
                    "expected", snapshot.getResponseStatusCode(),
                    "actual", result.getResponseStatusCode()
                ))
            );
        }
        
        // Compare response body
        try {
            JsonNode expectedJson = objectMapper.readTree(snapshot.getResponseBody());
            JsonNode actualJson = objectMapper.readTree(result.getResponseBody());
            
            if (!expectedJson.equals(actualJson)) {
                Map<String, Object> diff = compareJsonNodes(expectedJson, actualJson);
                return new SnapshotComparisonResult(
                    false,
                    "Response body mismatch",
                    Map.of("bodyDiff", diff)
                );
            }
        } catch (JsonProcessingException e) {
            // If not JSON, compare as strings
            if (!snapshot.getResponseBody().equals(result.getResponseBody())) {
                return new SnapshotComparisonResult(
                    false,
                    "Response body mismatch",
                    Map.of("bodyDiff", Map.of(
                        "expected", snapshot.getResponseBody(),
                        "actual", result.getResponseBody()
                    ))
                );
            }
        }
        
        return new SnapshotComparisonResult(true, "Snapshot comparison passed", null);
    }
    
    /**
     * Compare two JSON nodes and return the differences.
     *
     * @param expected the expected JSON node
     * @param actual the actual JSON node
     * @return the differences
     */
    private Map<String, Object> compareJsonNodes(JsonNode expected, JsonNode actual) {
        Map<String, Object> diff = new HashMap<>();
        
        // Compare node types
        if (expected.getNodeType() != actual.getNodeType()) {
            diff.put("typeMismatch", Map.of(
                "expected", expected.getNodeType().toString(),
                "actual", actual.getNodeType().toString()
            ));
            return diff;
        }
        
        // Compare objects
        if (expected.isObject()) {
            Map<String, Object> fieldDiffs = new HashMap<>();
            
            // Check for missing or different fields
            expected.fieldNames().forEachRemaining(fieldName -> {
                if (!actual.has(fieldName)) {
                    fieldDiffs.put(fieldName, Map.of("missing", true));
                } else {
                    JsonNode expectedValue = expected.get(fieldName);
                    JsonNode actualValue = actual.get(fieldName);
                    
                    if (!expectedValue.equals(actualValue)) {
                        fieldDiffs.put(fieldName, compareJsonNodes(expectedValue, actualValue));
                    }
                }
            });
            
            // Check for extra fields
            actual.fieldNames().forEachRemaining(fieldName -> {
                if (!expected.has(fieldName)) {
                    fieldDiffs.put(fieldName, Map.of("extra", true));
                }
            });
            
            if (!fieldDiffs.isEmpty()) {
                diff.put("fields", fieldDiffs);
            }
        }
        // Compare arrays
        else if (expected.isArray()) {
            if (expected.size() != actual.size()) {
                diff.put("sizeMismatch", Map.of(
                    "expected", expected.size(),
                    "actual", actual.size()
                ));
            }
            
            int minSize = Math.min(expected.size(), actual.size());
            Map<String, Object> itemDiffs = new HashMap<>();
            
            for (int i = 0; i < minSize; i++) {
                JsonNode expectedItem = expected.get(i);
                JsonNode actualItem = actual.get(i);
                
                if (!expectedItem.equals(actualItem)) {
                    itemDiffs.put(String.valueOf(i), compareJsonNodes(expectedItem, actualItem));
                }
            }
            
            if (!itemDiffs.isEmpty()) {
                diff.put("items", itemDiffs);
            }
        }
        // Compare values
        else {
            diff.put("valueMismatch", Map.of(
                "expected", expected.toString(),
                "actual", actual.toString()
            ));
        }
        
        return diff;
    }
    
    /**
     * Snapshot comparison result.
     */
    public static class SnapshotComparisonResult {
        private final boolean matches;
        private final String message;
        private final Map<String, Object> differences;
        
        public SnapshotComparisonResult(boolean matches, String message, Map<String, Object> differences) {
            this.matches = matches;
            this.message = message;
            this.differences = differences;
        }
        
        public boolean isMatches() {
            return matches;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Map<String, Object> getDifferences() {
            return differences;
        }
    }
}

