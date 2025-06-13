package com.codebridge.server.repository;

import com.codebridge.server.model.SshKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SshKeyRepository extends JpaRepository<SshKey, UUID> {
    List<SshKey> findByUserId(UUID userId);
    // Consider adding: Optional<SshKey> findByIdAndUserId(UUID id, UUID userId);
    // Consider adding: boolean existsByNameAndUserId(String name, UUID userId);
}
