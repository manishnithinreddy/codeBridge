package com.codebridge.monitoring.performance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a performance SLA.
 */
@Entity
@Table(name = "performance_slas")
@Data
@NoArgsConstructor
public class PerformanceSla {

    /**
     * Enum representing comparison operators for SLAs.
     */
    public enum Operator {
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        EQUAL,
        NOT_EQUAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false)
    private Operator operator;

    @Column(name = "threshold", nullable = false)
    private double threshold;

    @Column(name = "evaluation_period", nullable = false)
    private long evaluationPeriod;

    @Column(name = "aggregation", nullable = false)
    private String aggregation;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

