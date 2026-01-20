package com.codebridge.monitoring.performance.repository;

import com.codebridge.monitoring.performance.model.PerformanceSla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing performance SLAs.
 */
@Repository
public interface PerformanceSlaRepository extends JpaRepository<PerformanceSla, UUID> {

    /**
     * Find SLAs by enabled status.
     *
     * @param enabled the enabled status
     * @return list of SLAs
     */
    List<PerformanceSla> findByEnabled(boolean enabled);

    /**
     * Find SLAs by service name.
     *
     * @param serviceName the service name
     * @return list of SLAs
     */
    List<PerformanceSla> findByServiceName(String serviceName);

    /**
     * Find SLAs by service name and metric name.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @return list of SLAs
     */
    List<PerformanceSla> findByServiceNameAndMetricName(String serviceName, String metricName);
}

