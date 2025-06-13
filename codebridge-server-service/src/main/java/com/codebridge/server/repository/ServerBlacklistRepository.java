package com.codebridge.server.repository;

import com.codebridge.server.model.ServerBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for server blacklist entries.
 */
@Repository
public interface ServerBlacklistRepository extends JpaRepository<ServerBlacklist, UUID> {

    /**
     * Find active blacklist entry by IP address.
     *
     * @param ipAddress the IP address
     * @return the blacklist entry, if found
     */
    @Query("SELECT sb FROM ServerBlacklist sb WHERE sb.ipAddress = :ipAddress AND sb.isActive = true " +
           "AND (sb.expiresAt IS NULL OR sb.expiresAt > :now)")
    Optional<ServerBlacklist> findActiveByIpAddress(@Param("ipAddress") String ipAddress, @Param("now") LocalDateTime now);

    /**
     * Find all active blacklist entries.
     *
     * @return the list of active blacklist entries
     */
    @Query("SELECT sb FROM ServerBlacklist sb WHERE sb.isActive = true " +
           "AND (sb.expiresAt IS NULL OR sb.expiresAt > :now)")
    List<ServerBlacklist> findAllActive(@Param("now") LocalDateTime now);

    /**
     * Find all blacklist entries by creator.
     *
     * @param createdBy the creator ID
     * @return the list of blacklist entries
     */
    List<ServerBlacklist> findByCreatedBy(UUID createdBy);

    /**
     * Find all expired blacklist entries.
     *
     * @param now the current time
     * @return the list of expired blacklist entries
     */
    @Query("SELECT sb FROM ServerBlacklist sb WHERE sb.isActive = true " +
           "AND sb.expiresAt IS NOT NULL AND sb.expiresAt <= :now")
    List<ServerBlacklist> findExpired(@Param("now") LocalDateTime now);
}

