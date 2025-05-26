package com.codebridge.audit.repository;

import com.codebridge.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AuditLog entity operations.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find an audit log by its audit ID.
     *
     * @param auditId the audit ID
     * @return the audit log if found
     */
    Optional<AuditLog> findByAuditId(String auditId);

    /**
     * Find all audit logs for a specific user.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find all audit logs for a specific team.
     *
     * @param teamId the team ID
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLog> findByTeamId(UUID teamId, Pageable pageable);

    /**
     * Find all audit logs for a specific service.
     *
     * @param serviceName the service name
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLog> findByServiceName(String serviceName, Pageable pageable);

    /**
     * Find all audit logs of a specific type.
     *
     * @param type the audit log type
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLog> findByType(String type, Pageable pageable);

    /**
     * Find all audit logs within a time range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLog> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Search audit logs by multiple criteria.
     *
     * @param userId the user ID (optional)
     * @param teamId the team ID (optional)
     * @param serviceName the service name (optional)
     * @param type the audit log type (optional)
     * @param startTime the start time (optional)
     * @param endTime the end time (optional)
     * @param pageable pagination information
     * @return a page of matching audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:teamId IS NULL OR a.teamId = :teamId) AND " +
            "(:serviceName IS NULL OR a.serviceName = :serviceName) AND " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:startTime IS NULL OR a.timestamp >= :startTime) AND " +
            "(:endTime IS NULL OR a.timestamp <= :endTime)")
    Page<AuditLog> searchAuditLogs(
            @Param("userId") UUID userId,
            @Param("teamId") UUID teamId,
            @Param("serviceName") String serviceName,
            @Param("type") String type,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * Find the most recent audit logs.
     *
     * @param limit the maximum number of logs to return
     * @return list of recent audit logs
     */
    @Query(value = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit", nativeQuery = true)
    List<AuditLog> findRecentAuditLogs(@Param("limit") int limit);
}

