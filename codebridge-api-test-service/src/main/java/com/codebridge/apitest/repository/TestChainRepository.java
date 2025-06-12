package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.TestChain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for test chains.
 */
@Repository
public interface TestChainRepository extends JpaRepository<TestChain, UUID> {
    
    /**
     * Find all test chains for a project.
     *
     * @param projectId the project ID
     * @return the list of test chains
     */
    List<TestChain> findByProjectId(UUID projectId);
    
    /**
     * Find all active test chains for a project.
     *
     * @param projectId the project ID
     * @return the list of active test chains
     */
    List<TestChain> findByProjectIdAndActiveTrue(UUID projectId);
}

