package com.codebridge.performance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing an optimization recommendation.
 */
@Entity
@Table(name = "optimization_recommendations")
@Data
@NoArgsConstructor
public class OptimizationRecommendation {

    /**
     * Enum representing recommendation priority.
     */
    public enum Priority {
        /**
         * Low priority recommendation.
         */
        LOW,
        
        /**
         * Medium priority recommendation.
         */
        MEDIUM,
        
        /**
         * High priority recommendation.
         */
        HIGH
    }
    
    /**
     * Enum representing recommendation status.
     */
    public enum Status {
        /**
         * Open recommendation.
         */
        OPEN,
        
        /**
         * In progress recommendation.
         */
        IN_PROGRESS,
        
        /**
         * Implemented recommendation.
         */
        IMPLEMENTED,
        
        /**
         * Dismissed recommendation.
         */
        DISMISSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OptimizationRecommendationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "status_comment")
    private String statusComment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "implemented_at")
    private Instant implementedAt;
}

