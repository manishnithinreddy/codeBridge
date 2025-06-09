package com.codebridge.server.repository;

import com.codebridge.server.model.ServerUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServerUserRepository extends JpaRepository<ServerUser, UUID> {
    Optional<ServerUser> findByServerIdAndPlatformUserId(UUID serverId, UUID platformUserId);
    List<ServerUser> findByPlatformUserId(UUID platformUserId); // Get all server access grants for a user
    List<ServerUser> findByServerId(UUID serverId); // Get all users granted access to a server
    
    // Find all active and valid (not expired) grants for a user
    @Query("SELECT su FROM ServerUser su WHERE su.platformUserId = ?1 AND su.isActive = true AND (su.expiresAt IS NULL OR su.expiresAt > ?2)")
    List<ServerUser> findActiveAndValidByPlatformUserId(UUID platformUserId, LocalDateTime now);
    
    // Find all active and valid (not expired) grants for a server
    @Query("SELECT su FROM ServerUser su WHERE su.server.id = ?1 AND su.isActive = true AND (su.expiresAt IS NULL OR su.expiresAt > ?2)")
    List<ServerUser> findActiveAndValidByServerId(UUID serverId, LocalDateTime now);
    
    // Find all expired grants
    @Query("SELECT su FROM ServerUser su WHERE su.isActive = true AND su.expiresAt IS NOT NULL AND su.expiresAt <= ?1")
    List<ServerUser> findExpiredGrants(LocalDateTime now);
}
