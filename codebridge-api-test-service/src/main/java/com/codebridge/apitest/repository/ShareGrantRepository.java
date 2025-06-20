package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.ShareGrant; // Updated to use apitest.model
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShareGrantRepository extends JpaRepository<ShareGrant, Long> {

    Optional<ShareGrant> findByProjectIdAndGranteeUserId(Long projectId, Long granteeUserId);

    List<ShareGrant> findByProjectId(Long projectId);

    List<ShareGrant> findByGranteeUserId(Long granteeUserId);

    @Modifying
    @Transactional
    void deleteByProjectId(Long projectId);
}

