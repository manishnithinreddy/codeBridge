package com.codebridge.performance.repository;

import com.codebridge.performance.model.MetricType;
import com.codebridge.performance.model.PerformanceMetric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing performance metrics.
 */
@Repository
public interface PerformanceMetricRepository extends JpaRepository<PerformanceMetric, UUID> {

    /**
     * Find metrics by service name.
     *
     * @param serviceName the service name
     * @return list of metrics
     */
    List<PerformanceMetric> findByServiceName(String serviceName);

    /**
     * Find metrics by service name and metric name.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @return list of metrics
     */
    List<PerformanceMetric> findByServiceNameAndMetricName(String serviceName, String metricName);

    /**
     * Find metrics by service name, metric name, and time range.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of metrics
     */
    Page<PerformanceMetric> findByServiceNameAndMetricNameAndTimestampBetween(
            String serviceName, String metricName, Instant startTime, Instant endTime, Pageable pageable);

    /**
     * Find metrics by service name, metric name, metric type, and time range.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param metricType the metric type
     * @param startTime the start time
     * @param endTime the end time
     * @return list of metrics
     */
    List<PerformanceMetric> findByServiceNameAndMetricNameAndMetricTypeAndTimestampBetween(
            String serviceName, String metricName, MetricType metricType, Instant startTime, Instant endTime);

    /**
     * Calculate average metric value for a specific service, metric, and time range.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param startTime the start time
     * @param endTime the end time
     * @return the average value
     */
    @Query("SELECT AVG(m.value) FROM PerformanceMetric m WHERE m.serviceName = :serviceName " +
           "AND m.metricName = :metricName AND m.timestamp BETWEEN :startTime AND :endTime")
    Double calculateAverageValue(
            @Param("serviceName") String serviceName,
            @Param("metricName") String metricName,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Calculate maximum metric value for a specific service, metric, and time range.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param startTime the start time
     * @param endTime the end time
     * @return the maximum value
     */
    @Query("SELECT MAX(m.value) FROM PerformanceMetric m WHERE m.serviceName = :serviceName " +
           "AND m.metricName = :metricName AND m.timestamp BETWEEN :startTime AND :endTime")
    Double calculateMaxValue(
            @Param("serviceName") String serviceName,
            @Param("metricName") String metricName,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Calculate minimum metric value for a specific service, metric, and time range.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param startTime the start time
     * @param endTime the end time
     * @return the minimum value
     */
    @Query("SELECT MIN(m.value) FROM PerformanceMetric m WHERE m.serviceName = :serviceName " +
           "AND m.metricName = :metricName AND m.timestamp BETWEEN :startTime AND :endTime")
    Double calculateMinValue(
            @Param("serviceName") String serviceName,
            @Param("metricName") String metricName,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Calculate sum of metric values for a specific service, metric, and time range.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param startTime the start time
     * @param endTime the end time
     * @return the sum of values
     */
    @Query("SELECT SUM(m.value) FROM PerformanceMetric m WHERE m.serviceName = :serviceName " +
           "AND m.metricName = :metricName AND m.timestamp BETWEEN :startTime AND :endTime")
    Double calculateSumValue(
            @Param("serviceName") String serviceName,
            @Param("metricName") String metricName,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Count metrics for a specific service, metric, and time range.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param startTime the start time
     * @param endTime the end time
     * @return the count of metrics
     */
    @Query("SELECT COUNT(m) FROM PerformanceMetric m WHERE m.serviceName = :serviceName " +
           "AND m.metricName = :metricName AND m.timestamp BETWEEN :startTime AND :endTime")
    Long countMetrics(
            @Param("serviceName") String serviceName,
            @Param("metricName") String metricName,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Delete metrics older than a specific time.
     *
     * @param timestamp the cutoff timestamp
     * @return the number of deleted metrics
     */
    long deleteByTimestampBefore(Instant timestamp);
}

