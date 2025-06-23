package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.TestSnapshotRequest;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.ApiTest;
import com.codebridge.apitest.model.TestResult;
import com.codebridge.apitest.model.TestSnapshot;
import com.codebridge.apitest.repository.ApiTestRepository;
import com.codebridge.apitest.repository.TestSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for test snapshot operations.
 */
@Service
public class TestSnapshotService {

    private final TestSnapshotRepository snapshotRepository;
    private final ApiTestRepository testRepository;

    @Autowired
    public TestSnapshotService(TestSnapshotRepository snapshotRepository, ApiTestRepository testRepository) {
        this.snapshotRepository = snapshotRepository;
        this.testRepository = testRepository;
    }

    /**
     * Get all snapshots for a test.
     *
     * @param testId the test ID
     * @return list of snapshots
     */
    public List<TestSnapshot> getSnapshotsByTestId(Long testId) {
        return snapshotRepository.findByTestIdOrderByCreatedAtDesc(testId);
    }

    /**
     * Create a new snapshot for a test.
     *
     * @param testId the test ID
     * @param request the snapshot request
     * @param userId the user ID
     * @return the created snapshot
     */
    @Transactional
    public TestSnapshot createSnapshot(Long testId, TestSnapshotRequest request, Long userId) {
        ApiTest test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", testId));

        TestSnapshot snapshot = new TestSnapshot();
        snapshot.setTestId(testId);
        snapshot.setName(request.getName());
        snapshot.setDescription(request.getDescription());
        snapshot.setResponseBody(request.getResponseBody());
        snapshot.setResponseHeaders(request.getResponseHeaders());
        snapshot.setResponseStatus(request.getResponseStatus());
        snapshot.setCreatedBy(userId);
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setApproved(false);

        return snapshotRepository.save(snapshot);
    }

    /**
     * Get a snapshot by ID.
     *
     * @param snapshotId the snapshot ID
     * @return the snapshot
     * @throws ResourceNotFoundException if the snapshot is not found
     */
    public TestSnapshot getSnapshot(Long snapshotId) {
        TestSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSnapshot", "id", snapshotId));
        
        ApiTest test = testRepository.findById(snapshot.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", snapshot.getTestId()));
        
        return snapshot;
    }

    /**
     * Approve a snapshot.
     *
     * @param snapshotId the snapshot ID
     * @param userId the user ID
     * @return the approved snapshot
     * @throws ResourceNotFoundException if the test is not found
     */
    @Transactional
    public TestSnapshot approveSnapshot(Long snapshotId, Long userId) {
        TestSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSnapshot", "id", snapshotId));
        
        // Unapprove all other snapshots for this test
        List<TestSnapshot> existingSnapshots = snapshotRepository.findByTestIdAndApprovedTrue(snapshot.getTestId());
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
        
        return snapshotRepository.save(snapshot);
    }

    /**
     * Compare a test result with the approved snapshot.
     *
     * @param testId the test ID
     * @param result the test result to compare
     * @return comparison result map
     */
    public Map<String, Object> compareWithSnapshot(Long testId, TestResult result) {
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("hasApprovedSnapshot", false);
        comparison.put("matches", false);
        
        Optional<TestSnapshot> approvedSnapshotOpt = snapshotRepository.findByTestIdAndApprovedTrue(testId);
        if (approvedSnapshotOpt.isEmpty()) {
            return comparison;
        }
        
        TestSnapshot approvedSnapshot = approvedSnapshotOpt.get();
        comparison.put("hasApprovedSnapshot", true);
        comparison.put("snapshotId", approvedSnapshot.getId());
        comparison.put("snapshotName", approvedSnapshot.getName());
        comparison.put("snapshotCreatedAt", approvedSnapshot.getCreatedAt());
        
        // Compare response body, headers, and status
        boolean bodyMatches = result.getResponseBody().equals(approvedSnapshot.getResponseBody());
        boolean headersMatch = result.getResponseHeaders().equals(approvedSnapshot.getResponseHeaders());
        boolean statusMatches = result.getResponseStatus().equals(approvedSnapshot.getResponseStatus());
        boolean matches = bodyMatches && headersMatch && statusMatches;
        
        comparison.put("matches", matches);
        comparison.put("bodyMatches", bodyMatches);
        comparison.put("headersMatch", headersMatch);
        comparison.put("statusMatches", statusMatches);
        
        return comparison;
    }
}

