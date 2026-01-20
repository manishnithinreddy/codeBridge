package com.codebridge.gitlab.git.repository;

import com.codebridge.gitlab.git.model.GitProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface GitProviderRepository extends JpaRepository<GitProvider, UUID> {
    
    Optional<GitProvider> findByName(String name);
    
    List<GitProvider> findByEnabled(boolean enabled);
    
    Optional<GitProvider> findByTypeAndEnabled(GitProvider.ProviderType type, boolean enabled);
}
