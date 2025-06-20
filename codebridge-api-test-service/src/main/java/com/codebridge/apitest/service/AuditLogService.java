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
     * @param resourceType the resource type
     * @param resourceId the resource ID
     * @param details the details
     * @return the created audit log
     */
    public AuditLog logAction(Long userId, String action, String resourceType, Long resourceId, Object details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setIpAddress(getClientIpAddress());
        auditLog.setUserAgent(getUserAgent());
        
        if (details != null) {
            try {
                auditLog.setDetails(objectMapper.writeValueAsString(details));
            } catch (JsonProcessingException e) {
                auditLog.setDetails("Error serializing details: " + e.getMessage());
            }
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
    public Page<AuditLog> getAuditLogsForUser(Long userId, Pageable pageable) {
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
    public Page<AuditLog> getAuditLogsForResource(Long resourceId, String resourceType, Pageable pageable) {
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
     * Get the client IP address from the current request.
     *
     * @return the client IP address
     */
    private String getClientIpAddress() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "unknown";
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        return ip;
    }
    
    /**
     * Get the user agent from the current request.
     *
     * @return the user agent
     */
    private String getUserAgent() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getHeader("User-Agent") : "unknown";
    }
    
    /**
     * Get the current HTTP request.
     *
     * @return the current request
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

