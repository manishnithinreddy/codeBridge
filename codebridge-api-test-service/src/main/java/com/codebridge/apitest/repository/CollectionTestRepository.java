package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.CollectionTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CollectionTest entities.
 */
@Repository
public interface CollectionTestRepository extends JpaRepository<CollectionTest, Long> {

    /**
     * Find collection tests by collection ID.
     *
     * @param collectionId the collection ID
     * @return list of collection tests
     */
    List<CollectionTest> findByCollectionIdOrderByOrder(Long collectionId);

    /**
     * Find collection test by collection ID and test ID.
     *
     * @param collectionId the collection ID
     * @param testId the test ID
     * @return optional collection test
     */
    Optional<CollectionTest> findByCollectionIdAndTestId(Long collectionId, Long testId);

    /**
     * Delete collection tests by collection ID.
     *
     * @param collectionId the collection ID
     */
    void deleteByCollectionId(Long collectionId);

    /**
     * Delete collection test by collection ID and test ID.
     *
     * @param collectionId the collection ID
     * @param testId the test ID
     */
    void deleteByCollectionIdAndTestId(Long collectionId, Long testId);
}

