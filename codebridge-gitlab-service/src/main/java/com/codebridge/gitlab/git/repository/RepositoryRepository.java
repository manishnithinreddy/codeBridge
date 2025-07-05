package com.codebridge.gitlab.git.repository;

import com.codebridge.gitlab.git.model.GitProvider;
import com.codebridge.gitlab.git.model.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface RepositoryRepository extends JpaRepository<Repository, UUID> {
    
    List<Repository> findByTeamId(UUID teamId);
    
    Page<Repository> findByTeamId(UUID teamId, Pageable pageable);
    
    Optional<Repository> findByRemoteIdAndProvider(String remoteId, GitProvider provider);
    
    @Query("SELECT r FROM Repository r WHERE r.teamId = :teamId AND " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Repository> searchByTeamId(@Param("teamId") UUID teamId, 
                                   @Param("searchTerm") String searchTerm, 
                                   Pageable pageable);
    
    List<Repository> findByProviderAndTeamId(GitProvider provider, UUID teamId);
    
    @Query("SELECT COUNT(r) FROM Repository r WHERE r.teamId = :teamId")
    long countByTeamId(@Param("teamId") UUID teamId);
}
