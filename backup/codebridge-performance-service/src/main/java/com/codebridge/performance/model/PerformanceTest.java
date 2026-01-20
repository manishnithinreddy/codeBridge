package com.codebridge.performance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a performance test.
 */
@Entity
@Table(name = "performance_tests")
@Data
@NoArgsConstructor
public class PerformanceTest {

    /**
     * Enum representing test types.
     */
    public enum TestType {
        /**
         * HTTP request test.
         */
        HTTP,
        
        /**
         * API test.
         */
        API,
        
        /**
         * Database test.
         */
        DATABASE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TestType type;

    @Column(name = "endpoint", nullable = false)
    private String endpoint;

    @Column(name = "method", nullable = false)
    private String method;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "performance_test_headers", 
                    joinColumns = @JoinColumn(name = "test_id"))
    @MapKeyColumn(name = "header_name")
    @Column(name = "header_value")
    private Map<String, String> headers = new HashMap<>();

    @Column(name = "concurrent_users")
    private int concurrentUsers;

    @Column(name = "ramp_up_period")
    private int rampUpPeriod;

    @Column(name = "duration")
    private int duration;

    @Column(name = "scheduled")
    private boolean scheduled;

    @Column(name = "enabled")
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PerformanceTestStatus status;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

