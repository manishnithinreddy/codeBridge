package com.codebridge.server.repository;

import com.codebridge.server.model.ServerActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ServerActivityLogRepository extends JpaRepository<ServerActivityLog, UUID> {
    // Paginated finders
    Page<ServerActivityLog> findByServerId(UUID serverId, Pageable pageable);
    Page<ServerActivityLog> findByPlatformUserId(UUID platformUserId, Pageable pageable);
    Page<ServerActivityLog> findByServerIdAndPlatformUserId(UUID serverId, UUID platformUserId, Pageable pageable);

    // Non-paginated alternatives (could be useful for specific, limited queries)
    List<ServerActivityLog> findByServerIdOrderByTimestampDesc(UUID serverId);
    List<ServerActivityLog> findByPlatformUserIdOrderByTimestampDesc(UUID platformUserId);
    
    // Added for log export feature
    List<ServerActivityLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<ServerActivityLog> findByUserIdAndTimestampBetween(UUID userId, LocalDateTime startDate, LocalDateTime endDate);
    List<ServerActivityLog> findByServerIdAndTimestampBetween(UUID serverId, LocalDateTime startDate, LocalDateTime endDate);
    List<ServerActivityLog> findByUserIdAndServerIdAndTimestampBetween(UUID userId, UUID serverId, LocalDateTime startDate, LocalDateTime endDate);
}

