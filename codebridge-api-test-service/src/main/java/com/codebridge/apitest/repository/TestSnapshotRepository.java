package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.TestSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for test snapshots.
 */
@Repository
public interface TestSnapshotRepository extends JpaRepository<TestSnapshot, UUID> {
    
    /**
     * Find all snapshots for a test.
     *
     * @param testId the test ID
     * @return the list of snapshots
     */
    List<TestSnapshot> findByTestId(UUID testId);
    
    /**
     * Find a snapshot by test ID and name.
     *
     * @param testId the test ID
     * @param name the snapshot name
     * @return the snapshot if found
     */
    Optional<TestSnapshot> findByTestIdAndName(UUID testId, String name);
    
    /**
     * Find the latest approved snapshot for a test.
     *
     * @param testId the test ID
     * @return the latest approved snapshot if found
     */
    Optional<TestSnapshot> findFirstByTestIdAndApprovedTrueOrderByCreatedAtDesc(UUID testId);
}

