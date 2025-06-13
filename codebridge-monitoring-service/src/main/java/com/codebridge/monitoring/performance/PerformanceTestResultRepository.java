package com.codebridge.monitoring.performance.repository;

import com.codebridge.monitoring.performance.model.PerformanceTest;
import com.codebridge.monitoring.performance.model.PerformanceTestResult;
import com.codebridge.monitoring.performance.model.PerformanceTestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing performance test results.
 */
@Repository
public interface PerformanceTestResultRepository extends JpaRepository<PerformanceTestResult, UUID> {

    /**
     * Find results by test.
     *
     * @param test the test
     * @return list of results
     */
    List<PerformanceTestResult> findByTest(PerformanceTest test);

    /**
     * Find results by test ordered by start time.
     *
     * @param test the test
     * @return list of results
     */
    List<PerformanceTestResult> findByTestOrderByStartTimeDesc(PerformanceTest test);

    /**
     * Find results by status.
     *
     * @param status the result status
     * @return list of results
     */
    List<PerformanceTestResult> findByStatus(PerformanceTestStatus status);

    /**
     * Find results by test and status.
     *
     * @param test the test
     * @param status the result status
     * @return list of results
     */
    List<PerformanceTestResult> findByTestAndStatus(PerformanceTest test, PerformanceTestStatus status);

    /**
     * Find results by test and has regression.
     *
     * @param test the test
     * @param hasRegression whether the result has regression
     * @return list of results
     */
    List<PerformanceTestResult> findByTestAndHasRegression(PerformanceTest test, boolean hasRegression);

    /**
     * Find results by start time after.
     *
     * @param startTime the start time
     * @return list of results
     */
    List<PerformanceTestResult> findByStartTimeAfter(Instant startTime);

    /**
     * Find the most recent result for a test with a specific status, excluding a specific result.
     *
     * @param test the test
     * @param status the result status
     * @param id the result ID to exclude
     * @return the most recent result
     */
    PerformanceTestResult findTopByTestAndStatusAndIdNotOrderByEndTimeDesc(
            PerformanceTest test, PerformanceTestStatus status, UUID id);
}

