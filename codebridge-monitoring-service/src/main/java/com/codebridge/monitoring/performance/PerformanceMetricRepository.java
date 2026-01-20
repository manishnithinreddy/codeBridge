package com.codebridge.monitoring.performance.repository;

import com.codebridge.monitoring.performance.model.MetricType;
import com.codebridge.monitoring.performance.model.PerformanceMetric;
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

    /**
     * Find distinct service and metric name pairs.
     *
     * @return list of service and metric name pairs
     */
    @Query("SELECT DISTINCT m.serviceName, m.metricName FROM PerformanceMetric m")
    List<Object[]> findDistinctServiceAndMetricNames();

    /**
     * Find slow endpoints based on response time threshold.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param threshold the response time threshold
     * @param startTime the start time
     * @param endTime the end time
     * @return list of slow endpoints with their average response times
     */
    @Query("SELECT m.serviceName, m.metricName, AVG(m.value) as avgResponseTime " +
           "FROM PerformanceMetric m WHERE m.serviceName = :serviceName " +
           "AND m.metricName = :metricName AND m.value > :threshold " +
           "AND m.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY m.serviceName, m.metricName " +
           "ORDER BY avgResponseTime DESC")
    List<Object[]> findSlowEndpoints(
            @Param("serviceName") String serviceName,
            @Param("metricName") String metricName,
            @Param("threshold") double threshold,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find slow query types for database optimization.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param startTime the start time
     * @param endTime the end time
     * @return list of slow query types with their metrics
     */
    @Query("SELECT m.serviceName, m.metricName, COUNT(m) as queryCount, AVG(m.value) as avgTime " +
           "FROM PerformanceMetric m WHERE m.serviceName = :serviceName " +
           "AND m.metricName = :metricName " +
           "AND m.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY m.serviceName, m.metricName " +
           "ORDER BY avgTime DESC")
    List<Object[]> findSlowQueryTypes(
            @Param("serviceName") String serviceName,
            @Param("metricName") String metricName,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find frequent endpoints based on request count threshold.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param minRequestCount minimum request count threshold
     * @param startTime the start time
     * @param endTime the end time
     * @return list of frequent endpoints with their request counts
     */
    @Query("SELECT m.serviceName, m.metricName, SUM(m.value) as totalRequests " +
           "FROM PerformanceMetric m WHERE m.serviceName = :serviceName " +
           "AND m.metricName = :metricName " +
           "AND m.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY m.serviceName, m.metricName " +
           "HAVING SUM(m.value) > :minRequestCount " +
           "ORDER BY totalRequests DESC")
    List<Object[]> findFrequentEndpoints(
            @Param("serviceName") String serviceName,
            @Param("metricName") String metricName,
            @Param("minRequestCount") int minRequestCount,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find endpoints with high error rates.
     *
     * @param serviceName the service name
     * @param metricName the metric name
     * @param errorRateThreshold minimum error rate threshold
     * @param startTime the start time
     * @param endTime the end time
     * @return list of endpoints with high error rates
     */
    @Query("SELECT m.serviceName, m.metricName, AVG(m.value) as avgErrorRate " +
           "FROM PerformanceMetric m WHERE m.serviceName = :serviceName " +
           "AND m.metricName = :metricName " +
           "AND m.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY m.serviceName, m.metricName " +
           "HAVING AVG(m.value) > :errorRateThreshold " +
           "ORDER BY avgErrorRate DESC")
    List<Object[]> findHighErrorRateEndpoints(
            @Param("serviceName") String serviceName,
            @Param("metricName") String metricName,
            @Param("errorRateThreshold") double errorRateThreshold,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
