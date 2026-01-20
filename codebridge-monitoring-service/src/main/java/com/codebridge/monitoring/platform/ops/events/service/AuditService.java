package com.codebridge.monitoring.platform.ops.events.service;

import com.codebridge.monitoring.platform.ops.events.dto.AuditLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

/**
 * Service interface for audit log operations.
 * Provides methods for querying, managing, and exporting audit logs.
 */
public interface AuditService {

    /**
     * Get all audit logs with pagination.
     *
     * @param pageable Pagination information
     * @return Paginated audit logs
     */
    Page<AuditLogDto> getAllAuditLogs(Pageable pageable);

    /**
     * Get audit logs by user ID.
     *
     * @param userId User ID
     * @param pageable Pagination information
     * @return Paginated audit logs
     */
    Page<AuditLogDto> getAuditLogsByUser(Long userId, Pageable pageable);

    /**
     * Get audit logs by organization ID.
     *
     * @param organizationId Organization ID
     * @param pageable Pagination information
     * @return Paginated audit logs
     */
    Page<AuditLogDto> getAuditLogsByOrganization(Long organizationId, Pageable pageable);

    /**
     * Get audit logs by date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination information
     * @return Paginated audit logs
     */
    Page<AuditLogDto> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Get audit logs by action type.
     *
     * @param action Action type
     * @param pageable Pagination information
     * @return Paginated audit logs
     */
    Page<AuditLogDto> getAuditLogsByAction(String action, Pageable pageable);

    /**
     * Export audit logs to a file.
     *
     * @param startDate Start date
     * @param endDate End date
     * @param format Export format (CSV, JSON, PDF)
     * @return Export response
     */
    ResponseEntity<?> exportAuditLogs(LocalDateTime startDate, LocalDateTime endDate, String format);

    /**
     * Create an audit log entry.
     *
     * @param auditLogDto Audit log data
     * @return Created audit log
     */
    AuditLogDto createAuditLog(AuditLogDto auditLogDto);

    /**
     * Log an audit event.
     *
     * @param action Action performed
     * @param entityType Type of entity
     * @param entityId Entity ID
     * @param userId User ID
     * @param organizationId Organization ID
     * @param details Additional details
     */
    void logAuditEvent(String action, String entityType, Long entityId, Long userId, Long organizationId, Object details);
}
