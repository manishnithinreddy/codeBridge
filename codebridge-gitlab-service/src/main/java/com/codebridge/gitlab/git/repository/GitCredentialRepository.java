package com.codebridge.gitlab.git.repository;

import com.codebridge.gitlab.git.model.GitCredential;
import com.codebridge.gitlab.git.model.GitProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface GitCredentialRepository extends JpaRepository<GitCredential, UUID> {
    
    List<GitCredential> findByUserId(UUID userId);
    
    List<GitCredential> findByTeamId(UUID teamId);
    
    List<GitCredential> findByUserIdAndProvider(UUID userId, GitProvider provider);
    
    List<GitCredential> findByTeamIdAndProvider(UUID teamId, GitProvider provider);
    
    Optional<GitCredential> findByUserIdAndProviderAndIsDefault(UUID userId, GitProvider provider, boolean isDefault);
    
    Optional<GitCredential> findByTeamIdAndProviderAndIsDefault(UUID teamId, GitProvider provider, boolean isDefault);
}
