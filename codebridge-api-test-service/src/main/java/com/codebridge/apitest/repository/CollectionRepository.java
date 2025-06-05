package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.Collection; // Updated to use apitest.model
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    // Finders for standalone collections (not associated with a project)
    List<Collection> findByUserId(UUID userId);
    Optional<Collection> findByIdAndUserId(UUID id, UUID userId);

    // Finders for collections associated with a project
    List<Collection> findByProjectId(UUID projectId);

    // Finds collections within a specific project that are also primarily owned by a specific user.
    // The 'userId' on Collection entity is the direct owner.
    // This can be useful if you want to find collections in a project that a specific user "owns"
    // even if they have access to the project via sharing.
    List<Collection> findByProjectIdAndUserId(UUID projectId, UUID userId);

    // Finds a specific collection by its ID, within a specific project, and owned by a specific user.
    Optional<Collection> findByIdAndProjectIdAndUserId(UUID id, UUID projectId, UUID userId);
}
