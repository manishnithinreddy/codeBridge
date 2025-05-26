package com.codebridge.audit.service;

import com.codebridge.audit.dto.AuditEventDto;
import com.codebridge.audit.dto.AuditLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for Audit operations.
 */
public interface AuditService {

    /**
     * Log an audit event.
     *
     * @param auditEventDto the audit event data
     * @return the created audit log
     */
    AuditLogDto logAuditEvent(AuditEventDto auditEventDto);

    /**
     * Get an audit log by its ID.
     *
     * @param id the audit log ID
     * @return the audit log
     */
    AuditLogDto getAuditLogById(UUID id);

    /**
     * Get an audit log by its audit ID.
     *
     * @param auditId the audit ID
     * @return the audit log
     */
    AuditLogDto getAuditLogByAuditId(String auditId);

    /**
     * Get all audit logs with pagination.
     *
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLogDto> getAllAuditLogs(Pageable pageable);

    /**
     * Get all audit logs for a specific user with pagination.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLogDto> getAuditLogsByUser(UUID userId, Pageable pageable);

    /**
     * Get all audit logs for a specific team with pagination.
     *
     * @param teamId the team ID
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLogDto> getAuditLogsByTeam(UUID teamId, Pageable pageable);

    /**
     * Get all audit logs for a specific service with pagination.
     *
     * @param serviceName the service name
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLogDto> getAuditLogsByService(String serviceName, Pageable pageable);

    /**
     * Get all audit logs of a specific type with pagination.
     *
     * @param type the audit log type
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLogDto> getAuditLogsByType(String type, Pageable pageable);

    /**
     * Get all audit logs within a time range with pagination.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLogDto> getAuditLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Search audit logs by multiple criteria with pagination.
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
    Page<AuditLogDto> searchAuditLogs(
            UUID userId,
            UUID teamId,
            String serviceName,
            String type,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable);

    /**
     * Get the most recent audit logs.
     *
     * @param limit the maximum number of logs to return
     * @return list of recent audit logs
     */
    List<AuditLogDto> getRecentAuditLogs(int limit);
}

