package com.codebridge.monitoring.performance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a performance metric.
 */
@Entity
@Table(name = "performance_metrics", 
       indexes = {
           @Index(name = "idx_service_metric", columnList = "service_name,metric_name"),
           @Index(name = "idx_timestamp", columnList = "timestamp")
       })
@Data
@NoArgsConstructor
public class PerformanceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType;

    @Column(name = "value", nullable = false)
    private double value;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "performance_metric_tags", 
                    joinColumns = @JoinColumn(name = "metric_id"))
    @MapKeyColumn(name = "tag_key")
    @Column(name = "tag_value")
    private Map<String, String> tags = new HashMap<>();
}

