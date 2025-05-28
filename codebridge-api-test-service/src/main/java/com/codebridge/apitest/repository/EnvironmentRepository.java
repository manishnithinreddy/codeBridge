package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Environment entities.
 */
@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {

    /**
     * Find environments by user ID.
     *
     * @param userId the user ID
     * @return list of environments
     */
    List<Environment> findByUserId(UUID userId);

    /**
     * Find environments by team ID.
     *
     * @param teamId the team ID
     * @return list of environments
     */
    List<Environment> findByTeamId(UUID teamId);

    /**
     * Find environment by ID and user ID.
     *
     * @param id the environment ID
     * @param userId the user ID
     * @return optional environment
     */
    Optional<Environment> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find default environment by user ID.
     *
     * @param userId the user ID
     * @param isDefault true for default environment
     * @return optional environment
     */
    Optional<Environment> findByUserIdAndIsDefault(UUID userId, boolean isDefault);
}

