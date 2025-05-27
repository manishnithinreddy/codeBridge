package com.codebridge.git.repository;

import com.codebridge.git.model.GitProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitProviderRepository extends JpaRepository<GitProvider, UUID> {
    
    Optional<GitProvider> findByName(String name);
    
    List<GitProvider> findByEnabled(boolean enabled);
    
    Optional<GitProvider> findByTypeAndEnabled(GitProvider.ProviderType type, boolean enabled);
}

