package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.TestSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for test snapshots.
 */
@Repository
public interface TestSnapshotRepository extends JpaRepository<TestSnapshot, Long> {
    
    /**
     * Find all snapshots for a test ordered by creation date.
     *
     * @param testId the test ID
     * @return the list of snapshots
     */
    List<TestSnapshot> findByTestIdOrderByCreatedAtDesc(Long testId);
    
    /**
     * Find a snapshot by test ID and name.
     *
     * @param testId the test ID
     * @param name the snapshot name
     * @return the snapshot if found
     */
    Optional<TestSnapshot> findByTestIdAndName(Long testId, String name);
    
    /**
     * Find all approved snapshots for a test.
     *
     * @param testId the test ID
     * @return the list of approved snapshots
     */
    List<TestSnapshot> findByTestIdAndApprovedTrue(Long testId);
    
    /**
     * Find the approved snapshot for a test.
     *
     * @param testId the test ID
     * @return the approved snapshot if found
     */
    Optional<TestSnapshot> findByTestIdAndApprovedTrue(Long testId);
}

