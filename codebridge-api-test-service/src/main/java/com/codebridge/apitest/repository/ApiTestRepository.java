package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.ApiTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for API tests with optimized queries.
 */
@Repository
public interface ApiTestRepository extends JpaRepository<ApiTest, UUID> {

    /**
     * Finds API tests by project ID.
     *
     * @param projectId The project ID
     * @return The list of API tests
     */
    List<ApiTest> findByProjectId(UUID projectId);

    /**
     * Finds API tests by project ID with pagination.
     *
     * @param projectId The project ID
     * @param pageable The pagination information
     * @return The page of API tests
     */
    Page<ApiTest> findByProjectId(UUID projectId, Pageable pageable);

    /**
     * Finds API tests by name containing the given text.
     *
     * @param name The name to search for
     * @param pageable The pagination information
     * @return The page of API tests
     */
    Page<ApiTest> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Finds API tests by project ID and name containing the given text.
     *
     * @param projectId The project ID
     * @param name The name to search for
     * @param pageable The pagination information
     * @return The page of API tests
     */
    Page<ApiTest> findByProjectIdAndNameContainingIgnoreCase(UUID projectId, String name, Pageable pageable);

    /**
     * Finds API tests by project ID and created after the given date.
     *
     * @param projectId The project ID
     * @param createdAt The date to search after
     * @param pageable The pagination information
     * @return The page of API tests
     */
    Page<ApiTest> findByProjectIdAndCreatedAtAfter(UUID projectId, LocalDateTime createdAt, Pageable pageable);

    /**
     * Finds API tests by project ID with optimized query.
     *
     * @param projectId The project ID
     * @return The list of API tests
     */
    @Query("SELECT a FROM ApiTest a LEFT JOIN FETCH a.headers LEFT JOIN FETCH a.queryParams WHERE a.projectId = :projectId")
    List<ApiTest> findByProjectIdWithDetails(@Param("projectId") UUID projectId);

    /**
     * Finds an API test by ID with optimized query.
     *
     * @param id The API test ID
     * @return The API test
     */
    @Query("SELECT a FROM ApiTest a LEFT JOIN FETCH a.headers LEFT JOIN FETCH a.queryParams LEFT JOIN FETCH a.formParams WHERE a.id = :id")
    Optional<ApiTest> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Counts API tests by project ID.
     *
     * @param projectId The project ID
     * @return The count of API tests
     */
    @Query("SELECT COUNT(a) FROM ApiTest a WHERE a.projectId = :projectId")
    long countByProjectId(@Param("projectId") UUID projectId);

    /**
     * Finds the latest API tests by project ID.
     *
     * @param projectId The project ID
     * @param limit The maximum number of results
     * @return The list of API tests
     */
    @Query(value = "SELECT * FROM api_test WHERE project_id = :projectId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<ApiTest> findLatestByProjectId(@Param("projectId") UUID projectId, @Param("limit") int limit);

    /**
     * Finds API tests by user ID.
     *
     * @param userId The user ID
     * @return The list of API tests
     */
    List<ApiTest> findByUserId(UUID userId);

    /**
     * Finds an API test by ID and user ID.
     *
     * @param id The API test ID
     * @param userId The user ID
     * @return The API test
     */
    Optional<ApiTest> findByIdAndUserId(UUID id, UUID userId);
}
