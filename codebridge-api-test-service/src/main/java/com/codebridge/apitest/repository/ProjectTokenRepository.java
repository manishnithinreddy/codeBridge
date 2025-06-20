package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.ProjectToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for project tokens.
 */
@Repository
public interface ProjectTokenRepository extends JpaRepository<ProjectToken, Long> {
    
    /**
     * Find all tokens for a project.
     *
     * @param projectId the project ID
     * @return the list of tokens
     */
    List<ProjectToken> findByProjectId(Long projectId);
    
    /**
     * Find a specific token by name for a project.
     *
     * @param projectId the project ID
     * @param name the token name
     * @return the token if found
     */
    Optional<ProjectToken> findByProjectIdAndName(Long projectId, String name);
    
    /**
     * Delete all tokens for a project.
     *
     * @param projectId the project ID
     */
    void deleteByProjectId(Long projectId);
}

