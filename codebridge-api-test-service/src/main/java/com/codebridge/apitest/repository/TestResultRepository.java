package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for TestResult entities.
 */
@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID> {

    /**
     * Finds all test results by test ID, ordered by creation date (descending).
     *
     * @param testId the test ID
     * @return the list of test results
     */
    List<TestResult> findByTestIdOrderByCreatedAtDesc(UUID testId);
}

