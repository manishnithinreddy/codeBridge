package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Environment entities.
 */
@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    /**
     * Find environments by project ID.
     *
     * @param projectId the project ID
     * @return list of environments
     */
    List<Environment> findByProjectId(Long projectId);

    /**
     * Find environment by ID and project ID.
     *
     * @param id the environment ID
     * @param projectId the project ID
     * @return optional environment
     */
    Optional<Environment> findByIdAndProjectId(Long id, Long projectId);

    /**
     * Find default environment by project ID.
     *
     * @param projectId the project ID
     * @param isDefault true for default environment
     * @return optional environment
     */
    Optional<Environment> findByProjectIdAndIsDefault(Long projectId, boolean isDefault);
}

