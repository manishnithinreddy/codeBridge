package com.codebridge.security.apikey.repository;

import com.codebridge.security.apikey.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for API key entities.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /**
     * Finds an API key by key prefix.
     *
     * @param keyPrefix The key prefix
     * @return The API key, if found
     */
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    /**
     * Finds API keys by user ID.
     *
     * @param userId The user ID
     * @return The API keys
     */
    List<ApiKey> findByUserId(Long userId);

    /**
     * Finds active API keys by user ID.
     *
     * @param userId The user ID
     * @return The active API keys
     */
    @Query("SELECT k FROM ApiKey k WHERE k.userId = :userId AND k.enabled = true AND (k.expirationDate IS NULL OR k.expirationDate > :now) AND k.revokedAt IS NULL")
    List<ApiKey> findActiveKeysByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Counts active API keys by user ID.
     *
     * @param userId The user ID
     * @return The count of active API keys
     */
    @Query("SELECT COUNT(k) FROM ApiKey k WHERE k.userId = :userId AND k.enabled = true AND (k.expirationDate IS NULL OR k.expirationDate > :now) AND k.revokedAt IS NULL")
    long countActiveKeysByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Finds expired API keys.
     *
     * @param now The current time
     * @return The expired API keys
     */
    @Query("SELECT k FROM ApiKey k WHERE k.enabled = true AND k.expirationDate < :now AND k.revokedAt IS NULL")
    List<ApiKey> findExpiredKeys(@Param("now") LocalDateTime now);

    /**
     * Finds API keys that have not been used since a specific time.
     *
     * @param lastUsedBefore The time before which the API keys have not been used
     * @return The unused API keys
     */
    @Query("SELECT k FROM ApiKey k WHERE k.enabled = true AND (k.lastUsed IS NULL OR k.lastUsed < :lastUsedBefore) AND k.revokedAt IS NULL")
    List<ApiKey> findUnusedKeys(@Param("lastUsedBefore") LocalDateTime lastUsedBefore);
}

