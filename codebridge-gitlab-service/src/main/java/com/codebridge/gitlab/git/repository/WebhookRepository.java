package com.codebridge.git.repository;

import com.codebridge.git.model.Repository;
import com.codebridge.git.model.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {
    
    List<Webhook> findByRepository(Repository repository);
    
    Optional<Webhook> findByRemoteIdAndRepository(String remoteId, Repository repository);
    
    List<Webhook> findByRepositoryAndStatus(Repository repository, Webhook.WebhookStatus status);
    
    void deleteByRepository(Repository repository);
}

