package com.codebridge.apitest.service;

import com.codebridge.apitest.model.AuditLog;
import com.codebridge.apitest.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for audit logging.
 */
@Service
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Log an action.
     *
     * @param userId the user ID
     * @param action the action
     * @param resourceId the resource ID
     * @param resourceType the resource type
     * @param details the details
     * @return the created audit log
     */
    public AuditLog logAction(UUID userId, String action, UUID resourceId, String resourceType, Object details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID());
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setResourceId(resourceId);
        auditLog.setResourceType(resourceType);
        
        // Convert details to JSON
        if (details != null) {
            try {
                auditLog.setDetails(objectMapper.writeValueAsString(details));
            } catch (JsonProcessingException e) {
                auditLog.setDetails("Error serializing details: " + e.getMessage());
            }
        }
        
        // Get request information if available
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            auditLog.setIpAddress(request.getRemoteAddr());
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        }
        
        return auditLogRepository.save(auditLog);
    }
    
    /**
     * Get audit logs for a user.
     *
     * @param userId the user ID
     * @param pageable the pagination information
     * @return the page of audit logs
     */
    public Page<AuditLog> getAuditLogsForUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }
    
    /**
     * Get audit logs for a resource.
     *
     * @param resourceId the resource ID
     * @param resourceType the resource type
     * @param pageable the pagination information
     * @return the page of audit logs
     */
    public Page<AuditLog> getAuditLogsForResource(UUID resourceId, String resourceType, Pageable pageable) {
        return auditLogRepository.findByResourceIdAndResourceType(resourceId, resourceType, pageable);
    }
    
    /**
     * Get audit logs for a specific action.
     *
     * @param action the action
     * @param pageable the pagination information
     * @return the page of audit logs
     */
    public Page<AuditLog> getAuditLogsForAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }
    
    /**
     * Get audit logs within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable the pagination information
     * @return the page of audit logs
     */
    public Page<AuditLog> getAuditLogsInDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetween(startDate, endDate, pageable);
    }
    
    /**
     * Get the current HTTP request.
     *
     * @return the current HTTP request, or null if not available
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

