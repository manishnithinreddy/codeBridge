package com.codebridge.server.repository;

import com.codebridge.server.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServerRepository extends JpaRepository<Server, UUID> {
    List<Server> findByUserId(UUID userId);
    Optional<Server> findByIdAndUserId(UUID id, UUID userId);
    // Consider adding: boolean existsByNameAndUserId(String name, UUID userId);
    // Consider adding: List<Server> findByStatus(ServerStatus status);
    // Consider adding: List<Server> findByCloudProvider(ServerCloudProvider cloudProvider);
}
