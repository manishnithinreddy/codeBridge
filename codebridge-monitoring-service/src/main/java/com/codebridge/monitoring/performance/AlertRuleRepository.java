package com.codebridge.monitoring.performance.repository;

import com.codebridge.monitoring.performance.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing alert rules.
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    /**
     * Find alert rules by enabled status.
     *
     * @param enabled the enabled status
     * @return list of alert rules
     */
    List<AlertRule> findByEnabled(boolean enabled);

    /**
     * Find alert rules by service name.
     *
     * @param serviceName the service name
     * @return list of alert rules
     */
    List<AlertRule> findByServiceName(String serviceName);

    /**
     * Find alert rules by service name and metric name.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @return list of alert rules
     */
    List<AlertRule> findByServiceNameAndMetricName(String serviceName, String metricName);
}

