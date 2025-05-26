package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.ApiTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ApiTest entities.
 */
@Repository
public interface ApiTestRepository extends JpaRepository<ApiTest, UUID> {

    /**
     * Finds all API tests by user ID.
     *
     * @param userId the user ID
     * @return the list of API tests
     */
    List<ApiTest> findByUserId(UUID userId);

    /**
     * Finds all API tests by team ID.
     *
     * @param teamId the team ID
     * @return the list of API tests
     */
    List<ApiTest> findByTeamId(UUID teamId);

    /**
     * Finds an API test by ID and user ID.
     *
     * @param id the API test ID
     * @param userId the user ID
     * @return the API test, if found
     */
    Optional<ApiTest> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Finds an API test by name and user ID.
     *
     * @param name the API test name
     * @param userId the user ID
     * @return the API test, if found
     */
    Optional<ApiTest> findByNameAndUserId(String name, UUID userId);
}

