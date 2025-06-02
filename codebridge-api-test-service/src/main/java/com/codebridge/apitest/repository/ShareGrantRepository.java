package com.codebridge.apitest.repository;

import com.codebridge.apitester.model.ShareGrant; // Corrected: Point to apitester.model
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShareGrantRepository extends JpaRepository<ShareGrant, UUID> {

    Optional<ShareGrant> findByProjectIdAndGranteeUserId(UUID projectId, UUID granteeUserId);

    List<ShareGrant> findByProjectId(UUID projectId);

    List<ShareGrant> findByGranteeUserId(UUID granteeUserId);

    // Custom query for delete often requires @Modifying and @Transactional
    // However, Spring Data JPA can derive simple deletes by method name.
    // For consistency and clarity if more complex logic were needed, @Query could be used.
    @Transactional
    void deleteByProjectIdAndGranteeUserId(UUID projectId, UUID granteeUserId);

    @Transactional
    @Modifying // Required for DML operations that are not part of standard JpaRepository methods
    @Query("DELETE FROM ShareGrant sg WHERE sg.project.id = :projectId")
    void deleteByProjectId(UUID projectId);
}
