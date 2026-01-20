package com.codebridge.performance.repository;

import com.codebridge.performance.model.OptimizationRecommendation;
import com.codebridge.performance.model.OptimizationRecommendationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing optimization recommendations.
 */
@Repository
public interface OptimizationRecommendationRepository extends JpaRepository<OptimizationRecommendation, UUID> {

    /**
     * Find recommendations by type.
     *
     * @param type the recommendation type
     * @return list of recommendations
     */
    List<OptimizationRecommendation> findByType(OptimizationRecommendationType type);

    /**
     * Find recommendations by status.
     *
     * @param status the recommendation status
     * @return list of recommendations
     */
    List<OptimizationRecommendation> findByStatus(OptimizationRecommendation.Status status);

    /**
     * Find recommendations by priority.
     *
     * @param priority the recommendation priority
     * @return list of recommendations
     */
    List<OptimizationRecommendation> findByPriority(OptimizationRecommendation.Priority priority);

    /**
     * Find recommendations by type and status.
     *
     * @param type the recommendation type
     * @param status the recommendation status
     * @return list of recommendations
     */
    List<OptimizationRecommendation> findByTypeAndStatus(
            OptimizationRecommendationType type, OptimizationRecommendation.Status status);

    /**
     * Find recommendations by type, title, and status.
     *
     * @param type the recommendation type
     * @param title the recommendation title
     * @param status the recommendation status
     * @return list of recommendations
     */
    List<OptimizationRecommendation> findByTypeAndTitleAndStatus(
            OptimizationRecommendationType type, String title, OptimizationRecommendation.Status status);

    /**
     * Find recommendations created after a specific time.
     *
     * @param createdAt the creation time
     * @return list of recommendations
     */
    List<OptimizationRecommendation> findByCreatedAtAfter(Instant createdAt);

    /**
     * Find recommendations by status and created after a specific time.
     *
     * @param status the recommendation status
     * @param createdAt the creation time
     * @return list of recommendations
     */
    List<OptimizationRecommendation> findByStatusAndCreatedAtAfter(
            OptimizationRecommendation.Status status, Instant createdAt);
}

