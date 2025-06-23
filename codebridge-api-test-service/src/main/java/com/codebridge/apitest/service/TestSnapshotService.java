package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.SnapshotComparisonResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.dto.TestSnapshotRequest;
import com.codebridge.apitest.dto.TestSnapshotResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.TestResult;
import com.codebridge.apitest.model.TestSnapshot;
import com.codebridge.apitest.repository.TestResultRepository;
import com.codebridge.apitest.repository.TestSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for test snapshot operations.
 */
@Service
public class TestSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(TestSnapshotService.class);

    private final TestSnapshotRepository snapshotRepository;
    private final TestResultRepository resultRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public TestSnapshotService(TestSnapshotRepository snapshotRepository,
                              TestResultRepository resultRepository,
                              ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.resultRepository = resultRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all snapshots for a test.
     *
     * @param testId the test ID
     * @return the list of snapshots
     */
    public List<TestSnapshotResponse> getSnapshotsByTestId(Long testId) {
        List<TestSnapshot> snapshots = snapshotRepository.findByTestIdOrderByCreatedAtDesc(testId);
        return snapshots.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a snapshot from a test result.
     *
     * @param request the snapshot request
     * @param userId the user ID
     * @return the created snapshot
     */
    @Transactional
    public TestSnapshotResponse createSnapshot(TestSnapshotRequest request, Long userId) {
        // Validate name uniqueness
        snapshotRepository.findByTestIdAndName(request.getTestId(), request.getName())
                .ifPresent(s -> {
                    throw new IllegalArgumentException("Snapshot with this name already exists for this test");
                });

        TestSnapshot snapshot = new TestSnapshot();
        snapshot.setTestId(request.getTestId());
        snapshot.setName(request.getName());
        snapshot.setDescription(request.getDescription());
        snapshot.setResponseBody(request.getResponseBody());
        snapshot.setResponseHeaders(request.getResponseHeaders());
        snapshot.setStatusCode(request.getStatusCode());
        snapshot.setResponseStatus(request.getResponseStatus());
        snapshot.setCreatedBy(userId);
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setApproved(false);

        return mapToResponse(snapshotRepository.save(snapshot));
    }

    /**
     * Approve a snapshot.
     *
     * @param snapshotId the snapshot ID
     * @param userId the user ID
     * @return the approved snapshot
     */
    @Transactional
    public TestSnapshotResponse approveSnapshot(Long snapshotId, Long userId) {
        TestSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSnapshot", "id", snapshotId.toString()));

        // Remove approval from any existing approved snapshots for this test
        List<TestSnapshot> existingSnapshots = snapshotRepository.findAllByTestIdAndApprovedTrue(snapshot.getTestId());
        existingSnapshots.forEach(s -> {
            s.setApproved(false);
            s.setApprovedBy(null);
            s.setApprovedAt(null);
        });
        snapshotRepository.saveAll(existingSnapshots);

        // Approve this snapshot
        snapshot.setApproved(true);
        snapshot.setApprovedBy(userId);
        snapshot.setApprovedAt(LocalDateTime.now());

        return mapToResponse(snapshotRepository.save(snapshot));
    }

    /**
     * Compare a test result with the approved snapshot.
     *
     * @param testId the test ID
     * @param resultId the result ID
     * @return the comparison result
     */
    public SnapshotComparisonResponse compareWithSnapshot(Long testId, Long resultId) {
        TestResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("TestResult", "id", resultId.toString()));

        SnapshotComparisonResponse comparison = new SnapshotComparisonResponse();
        comparison.setTestId(testId);
        comparison.setResultId(resultId);
        comparison.setHasApprovedSnapshot(false);
        comparison.setMatches(false);

        // Get the approved snapshot
        Optional<TestSnapshot> approvedSnapshotOpt = snapshotRepository.findByTestIdAndApprovedTrue(testId);
        if (approvedSnapshotOpt.isEmpty()) {
            return comparison;
        }
        
        TestSnapshot approvedSnapshot = approvedSnapshotOpt.get();
        comparison.setHasApprovedSnapshot(true);
        comparison.setSnapshotId(approvedSnapshot.getId());
        comparison.setSnapshotName(approvedSnapshot.getName());

        // Compare response body
        boolean bodyMatches = compareJson(result.getResponseBody(), approvedSnapshot.getResponseBody());
        comparison.setBodyMatches(bodyMatches);

        // Compare response headers
        boolean headersMatch = compareJson(result.getResponseHeaders(), approvedSnapshot.getResponseHeaders());
        comparison.setHeadersMatch(headersMatch);

        // Compare status code
        boolean statusMatches = result.getResponseStatus().equals(approvedSnapshot.getResponseStatus());
        comparison.setStatusMatches(statusMatches);

        boolean matches = bodyMatches && headersMatch && statusMatches;
        comparison.setMatches(matches);

        return comparison;
    }

    /**
     * Delete a snapshot.
     *
     * @param snapshotId the snapshot ID
     */
    @Transactional
    public void deleteSnapshot(Long snapshotId) {
        TestSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSnapshot", "id", snapshotId.toString()));

        snapshotRepository.delete(snapshot);
    }

    /**
     * Map a snapshot to a response DTO.
     *
     * @param snapshot the snapshot
     * @return the response DTO
     */
    private TestSnapshotResponse mapToResponse(TestSnapshot snapshot) {
        TestSnapshotResponse response = new TestSnapshotResponse();
        response.setId(snapshot.getId());
        response.setTestId(snapshot.getTestId());
        response.setName(snapshot.getName());
        response.setDescription(snapshot.getDescription());
        response.setResponseBody(snapshot.getResponseBody());
        response.setResponseHeaders(snapshot.getResponseHeaders());
        response.setStatusCode(snapshot.getStatusCode());
        response.setResponseStatus(snapshot.getResponseStatus());
        response.setApproved(snapshot.getApproved());
        response.setApprovedBy(snapshot.getApprovedBy());
        response.setApprovedAt(snapshot.getApprovedAt());
        response.setCreatedBy(snapshot.getCreatedBy());
        response.setCreatedAt(snapshot.getCreatedAt());
        response.setUpdatedAt(snapshot.getUpdatedAt());
        return response;
    }

    /**
     * Compare two JSON strings.
     *
     * @param json1 the first JSON string
     * @param json2 the second JSON string
     * @return true if the JSON strings are equivalent
     */
    private boolean compareJson(String json1, String json2) {
        if (json1 == null && json2 == null) {
            return true;
        }
        if (json1 == null || json2 == null) {
            return false;
        }

        try {
            JsonNode tree1 = objectMapper.readTree(json1);
            JsonNode tree2 = objectMapper.readTree(json2);
            return tree1.equals(tree2);
        } catch (JsonProcessingException e) {
            logger.warn("Error comparing JSON: {}", e.getMessage());
            return json1.equals(json2);
        }
    }
}

