package com.codebridge.monitoring.platform.ops.events.controller;

import com.codebridge.monitoring.platform.ops.events.dto.AuditLogDto;
import com.codebridge.monitoring.platform.ops.events.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller for audit log operations.
 * Provides endpoints for querying and managing audit logs.
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Get all audit logs with pagination.
     *
     * @param pageable Pagination information
     * @return Paginated audit logs
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> getAllAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(auditService.getAllAuditLogs(pageable));
    }

    /**
     * Get audit logs by user ID.
     *
     * @param userId User ID
     * @param pageable Pagination information
     * @return Paginated audit logs
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogsByUser(
            @PathVariable Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsByUser(userId, pageable));
    }

    /**
     * Get audit logs by organization ID.
     *
     * @param organizationId Organization ID
     * @param pageable Pagination information
     * @return Paginated audit logs
     */
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogsByOrganization(
            @PathVariable Long organizationId,
            Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsByOrganization(organizationId, pageable));
    }

    /**
     * Get audit logs by date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination information
     * @return Paginated audit logs
     */
    @GetMapping("/date-range")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsByDateRange(startDate, endDate, pageable));
    }

    /**
     * Get audit logs by action type.
     *
     * @param action Action type
     * @param pageable Pagination information
     * @return Paginated audit logs
     */
    @GetMapping("/action/{action}")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogsByAction(
            @PathVariable String action,
            Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsByAction(action, pageable));
    }

    /**
     * Export audit logs to a file.
     *
     * @param startDate Start date
     * @param endDate End date
     * @param format Export format (CSV, JSON, PDF)
     * @return Export response
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportAuditLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "CSV") String format) {
        return auditService.exportAuditLogs(startDate, endDate, format);
    }
}

