package com.codebridge.server.repository;

import com.codebridge.server.model.ServerUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServerUserRepository extends JpaRepository<ServerUser, UUID> {
    Optional<ServerUser> findByServerIdAndPlatformUserId(UUID serverId, UUID platformUserId);
    List<ServerUser> findByPlatformUserId(UUID platformUserId); // Get all server access grants for a user
    List<ServerUser> findByServerId(UUID serverId); // Get all users granted access to a server
    // Consider adding: void deleteByServerIdAndPlatformUserId(UUID serverId, UUID platformUserId);
}
