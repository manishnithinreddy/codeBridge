package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TestResult entities.
 */
@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    /**
     * Finds all test results by test ID, ordered by execution date (descending).
     *
     * @param testId the test ID
     * @return the list of test results
     */
    List<TestResult> findByTestIdOrderByExecutedAtDesc(Long testId);
}

