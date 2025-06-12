package com.codebridge.performance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a performance SLA violation.
 */
@Entity
@Table(name = "performance_sla_violations")
@Data
@NoArgsConstructor
public class PerformanceSlaViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "sla_id", nullable = false)
    private PerformanceSla sla;

    @Column(name = "current_value", nullable = false)
    private double currentValue;

    @Column(name = "threshold", nullable = false)
    private double threshold;

    @Column(name = "violation_time", nullable = false)
    private Instant violationTime;

    @Column(name = "message", nullable = false)
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

