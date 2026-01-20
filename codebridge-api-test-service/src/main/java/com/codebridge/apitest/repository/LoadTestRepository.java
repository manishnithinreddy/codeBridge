package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.LoadTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for load tests.
 */
@Repository
public interface LoadTestRepository extends JpaRepository<LoadTest, UUID> {
    
    /**
     * Find all load tests for a user.
     *
     * @param userId the user ID
     * @return the list of load tests
     */
    List<LoadTest> findByUserId(UUID userId);
    
    /**
     * Find a load test by ID and user ID.
     *
     * @param id the load test ID
     * @param userId the user ID
     * @return the load test
     */
    Optional<LoadTest> findByIdAndUserId(UUID id, UUID userId);
}

