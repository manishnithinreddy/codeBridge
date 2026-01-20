package com.codebridge.performance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a performance test result.
 */
@Entity
@Table(name = "performance_test_results")
@Data
@NoArgsConstructor
public class PerformanceTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    private PerformanceTest test;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PerformanceTestStatus status;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "total_requests")
    private int totalRequests;

    @Column(name = "successful_requests")
    private int successfulRequests;

    @Column(name = "failed_requests")
    private int failedRequests;

    @Column(name = "average_response_time")
    private double averageResponseTime;

    @Column(name = "min_response_time")
    private double minResponseTime;

    @Column(name = "max_response_time")
    private double maxResponseTime;

    @Column(name = "throughput")
    private double throughput;

    @Column(name = "percentile_90")
    private double percentile90;

    @Column(name = "percentile_95")
    private double percentile95;

    @Column(name = "percentile_99")
    private double percentile99;

    @Column(name = "error_rate")
    private double errorRate;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "has_regression")
    private boolean hasRegression;

    @Column(name = "regression_message", columnDefinition = "TEXT")
    private String regressionMessage;

    @Column(name = "detailed_results", columnDefinition = "json")
    private Map<String, Object> detailedResults;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

