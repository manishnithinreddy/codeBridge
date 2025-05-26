package com.codebridge.git.repository;

import com.codebridge.git.model.GitCredential;
import com.codebridge.git.model.GitProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitCredentialRepository extends JpaRepository<GitCredential, UUID> {
    
    List<GitCredential> findByUserId(UUID userId);
    
    List<GitCredential> findByTeamId(UUID teamId);
    
    List<GitCredential> findByUserIdAndProvider(UUID userId, GitProvider provider);
    
    List<GitCredential> findByTeamIdAndProvider(UUID teamId, GitProvider provider);
    
    Optional<GitCredential> findByUserIdAndProviderAndIsDefault(UUID userId, GitProvider provider, boolean isDefault);
    
    Optional<GitCredential> findByTeamIdAndProviderAndIsDefault(UUID teamId, GitProvider provider, boolean isDefault);
}

