package com.codebridge.monitoring.performance.repository;

import com.codebridge.monitoring.performance.model.PerformanceSla;
import com.codebridge.monitoring.performance.model.PerformanceSlaViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing performance SLA violations.
 */
@Repository
public interface PerformanceSlaViolationRepository extends JpaRepository<PerformanceSlaViolation, UUID> {

    /**
     * Find violations by SLA.
     *
     * @param sla the SLA
     * @return list of violations
     */
    List<PerformanceSlaViolation> findBySla(PerformanceSla sla);

    /**
     * Find violations by SLA and violation time between.
     *
     * @param sla the SLA
     * @param startTime the start time
     * @param endTime the end time
     * @return list of violations
     */
    List<PerformanceSlaViolation> findBySlaAndViolationTimeBetween(
            PerformanceSla sla, Instant startTime, Instant endTime);

    /**
     * Find violations by violation time between.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @return list of violations
     */
    List<PerformanceSlaViolation> findByViolationTimeBetween(Instant startTime, Instant endTime);

    /**
     * Count violations by SLA.
     *
     * @param sla the SLA
     * @return the count of violations
     */
    long countBySla(PerformanceSla sla);

    /**
     * Count violations by SLA and violation time between.
     *
     * @param sla the SLA
     * @param startTime the start time
     * @param endTime the end time
     * @return the count of violations
     */
    long countBySlaAndViolationTimeBetween(PerformanceSla sla, Instant startTime, Instant endTime);
}

