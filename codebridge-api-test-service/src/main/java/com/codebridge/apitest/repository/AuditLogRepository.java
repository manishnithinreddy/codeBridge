package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for audit logs.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    /**
     * Find all audit logs for a user.
     *
     * @param userId the user ID
     * @param pageable the pagination information
     * @return the page of audit logs
     */
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);
    
    /**
     * Find all audit logs for a resource.
     *
     * @param resourceId the resource ID
     * @param resourceType the resource type
     * @param pageable the pagination information
     * @return the page of audit logs
     */
    Page<AuditLog> findByResourceIdAndResourceType(UUID resourceId, String resourceType, Pageable pageable);
    
    /**
     * Find all audit logs for a specific action.
     *
     * @param action the action
     * @param pageable the pagination information
     * @return the page of audit logs
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);
    
    /**
     * Find all audit logs within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable the pagination information
     * @return the page of audit logs
     */
    Page<AuditLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}

