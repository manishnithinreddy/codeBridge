package com.codebridge.monitoring.performance.repository;

import com.codebridge.monitoring.performance.model.PerformanceTest;
import com.codebridge.monitoring.performance.model.PerformanceTestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing performance tests.
 */
@Repository
public interface PerformanceTestRepository extends JpaRepository<PerformanceTest, UUID> {

    /**
     * Find tests by enabled status.
     *
     * @param enabled the enabled status
     * @return list of tests
     */
    List<PerformanceTest> findByEnabled(boolean enabled);

    /**
     * Find tests by scheduled status.
     *
     * @param scheduled the scheduled status
     * @return list of tests
     */
    List<PerformanceTest> findByScheduled(boolean scheduled);

    /**
     * Find tests by enabled and scheduled status.
     *
     * @param enabled the enabled status
     * @param scheduled the scheduled status
     * @return list of tests
     */
    List<PerformanceTest> findByEnabledAndScheduled(boolean enabled, boolean scheduled);

    /**
     * Find tests by status.
     *
     * @param status the test status
     * @return list of tests
     */
    List<PerformanceTest> findByStatus(PerformanceTestStatus status);

    /**
     * Find tests by type.
     *
     * @param type the test type
     * @return list of tests
     */
    List<PerformanceTest> findByType(PerformanceTest.TestType type);
}

