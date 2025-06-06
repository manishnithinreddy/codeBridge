package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.CollectionTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CollectionTest entities.
 */
@Repository
public interface CollectionTestRepository extends JpaRepository<CollectionTest, UUID> {

    /**
     * Find collection tests by collection ID.
     *
     * @param collectionId the collection ID
     * @return list of collection tests
     */
    List<CollectionTest> findByCollectionIdOrderByOrder(UUID collectionId);

    /**
     * Find collection test by collection ID and test ID.
     *
     * @param collectionId the collection ID
     * @param testId the test ID
     * @return optional collection test
     */
    Optional<CollectionTest> findByCollectionIdAndTestId(UUID collectionId, UUID testId);

    /**
     * Delete collection tests by collection ID.
     *
     * @param collectionId the collection ID
     */
    void deleteByCollectionId(UUID collectionId);

    /**
     * Delete collection test by collection ID and test ID.
     *
     * @param collectionId the collection ID
     * @param testId the test ID
     */
    void deleteByCollectionIdAndTestId(UUID collectionId, UUID testId);
}

