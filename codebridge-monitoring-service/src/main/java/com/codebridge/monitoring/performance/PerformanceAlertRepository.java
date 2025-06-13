package com.codebridge.monitoring.performance.repository;

import com.codebridge.monitoring.performance.model.AlertRule;
import com.codebridge.monitoring.performance.model.AlertStatus;
import com.codebridge.monitoring.performance.model.PerformanceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing performance alerts.
 */
@Repository
public interface PerformanceAlertRepository extends JpaRepository<PerformanceAlert, UUID> {

    /**
     * Find alerts by status.
     *
     * @param status the alert status
     * @return list of alerts
     */
    List<PerformanceAlert> findByStatus(AlertStatus status);

    /**
     * Find alerts by alert rule.
     *
     * @param alertRule the alert rule
     * @return list of alerts
     */
    List<PerformanceAlert> findByAlertRule(AlertRule alertRule);

    /**
     * Find the most recent alert for a specific alert rule.
     *
     * @param alertRule the alert rule
     * @return the most recent alert
     */
    PerformanceAlert findTopByAlertRuleOrderByCreatedAtDesc(AlertRule alertRule);

    /**
     * Find alerts created after a specific time.
     *
     * @param createdAt the creation time
     * @return list of alerts
     */
    List<PerformanceAlert> findByCreatedAtAfter(Instant createdAt);

    /**
     * Find alerts by status and created after a specific time.
     *
     * @param status the alert status
     * @param createdAt the creation time
     * @return list of alerts
     */
    List<PerformanceAlert> findByStatusAndCreatedAtAfter(AlertStatus status, Instant createdAt);
}

