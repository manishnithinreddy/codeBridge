package com.codebridge.gitlab.git.repository;

import com.codebridge.gitlab.git.model.Repository;
import com.codebridge.gitlab.git.model.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {
    
    List<Webhook> findByRepository(Repository repository);
    
    Optional<Webhook> findByRemoteIdAndRepository(String remoteId, Repository repository);
    
    List<Webhook> findByRepositoryAndStatus(Repository repository, Webhook.WebhookStatus status);
    
    void deleteByRepository(Repository repository);
}
