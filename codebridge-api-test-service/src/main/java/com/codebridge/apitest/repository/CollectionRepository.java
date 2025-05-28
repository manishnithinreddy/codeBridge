package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Collection entities.
 */
@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    /**
     * Find collections by user ID.
     *
     * @param userId the user ID
     * @return list of collections
     */
    List<Collection> findByUserId(UUID userId);

    /**
     * Find collections by team ID.
     *
     * @param teamId the team ID
     * @return list of collections
     */
    List<Collection> findByTeamId(UUID teamId);

    /**
     * Find collection by ID and user ID.
     *
     * @param id the collection ID
     * @param userId the user ID
     * @return optional collection
     */
    Optional<Collection> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find shared collections.
     *
     * @param shared true for shared collections
     * @return list of shared collections
     */
    List<Collection> findByShared(boolean shared);

    /**
     * Find shared collections by team ID.
     *
     * @param teamId the team ID
     * @param shared true for shared collections
     * @return list of shared collections
     */
    List<Collection> findByTeamIdAndShared(UUID teamId, boolean shared);
}

