package com.codebridge.monitoring.platform.ops.events.service.impl;

import com.codebridge.monitoring.platform.ops.events.dto.AuditLogDto;
import com.codebridge.monitoring.platform.ops.events.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of AuditService.
 * Provides audit log management functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<AuditLogDto> auditLogs = new ArrayList<>();

    @Override
    public Page<AuditLogDto> getAllAuditLogs(Pageable pageable) {
        log.debug("Getting all audit logs with pagination: {}", pageable);
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), auditLogs.size());
        
        List<AuditLogDto> pageContent = start < auditLogs.size() ? 
            auditLogs.subList(start, end) : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, auditLogs.size());
    }

    @Override
    public Page<AuditLogDto> getAuditLogsByUser(Long userId, Pageable pageable) {
        log.debug("Getting audit logs for user: {}", userId);
        
        List<AuditLogDto> userLogs = auditLogs.stream()
            .filter(log -> userId.equals(log.getUserId()))
            .toList();
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), userLogs.size());
        
        List<AuditLogDto> pageContent = start < userLogs.size() ? 
            userLogs.subList(start, end) : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, userLogs.size());
    }

    @Override
    public Page<AuditLogDto> getAuditLogsByOrganization(Long organizationId, Pageable pageable) {
        log.debug("Getting audit logs for organization: {}", organizationId);
        
        List<AuditLogDto> orgLogs = auditLogs.stream()
            .filter(log -> organizationId.equals(log.getOrganizationId()))
            .toList();
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), orgLogs.size());
        
        List<AuditLogDto> pageContent = start < orgLogs.size() ? 
            orgLogs.subList(start, end) : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, orgLogs.size());
    }

    @Override
    public Page<AuditLogDto> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Getting audit logs for date range: {} to {}", startDate, endDate);
        
        List<AuditLogDto> dateLogs = auditLogs.stream()
            .filter(log -> log.getTimestamp().isAfter(startDate) && log.getTimestamp().isBefore(endDate))
            .toList();
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dateLogs.size());
        
        List<AuditLogDto> pageContent = start < dateLogs.size() ? 
            dateLogs.subList(start, end) : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, dateLogs.size());
    }

    @Override
    public Page<AuditLogDto> getAuditLogsByAction(String action, Pageable pageable) {
        log.debug("Getting audit logs for action: {}", action);
        
        List<AuditLogDto> actionLogs = auditLogs.stream()
            .filter(log -> action.equals(log.getAction()))
            .toList();
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), actionLogs.size());
        
        List<AuditLogDto> pageContent = start < actionLogs.size() ? 
            actionLogs.subList(start, end) : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, actionLogs.size());
    }

    @Override
    public ResponseEntity<?> exportAuditLogs(LocalDateTime startDate, LocalDateTime endDate, String format) {
        log.debug("Exporting audit logs from {} to {} in format: {}", startDate, endDate, format);
        
        List<AuditLogDto> exportLogs = auditLogs.stream()
            .filter(log -> log.getTimestamp().isAfter(startDate) && log.getTimestamp().isBefore(endDate))
            .toList();
        
        switch (format.toUpperCase()) {
            case "CSV":
                return exportToCsv(exportLogs);
            case "JSON":
                return exportToJson(exportLogs);
            case "PDF":
                return exportToPdf(exportLogs);
            default:
                return ResponseEntity.badRequest().body("Unsupported format: " + format);
        }
    }

    @Override
    public AuditLogDto createAuditLog(AuditLogDto auditLogDto) {
        log.debug("Creating audit log: {}", auditLogDto);
        
        auditLogDto.setId(idGenerator.getAndIncrement());
        auditLogDto.setTimestamp(LocalDateTime.now());
        auditLogs.add(auditLogDto);
        
        return auditLogDto;
    }

    @Override
    public void logAuditEvent(String action, String entityType, Long entityId, Long userId, Long organizationId, Object details) {
        log.debug("Logging audit event: action={}, entityType={}, entityId={}, userId={}, organizationId={}", 
                 action, entityType, entityId, userId, organizationId);
        
        Map<String, Object> detailsMap = new HashMap<>();
        if (details != null) {
            detailsMap.put("details", details);
        }
        
        AuditLogDto auditLog = AuditLogDto.builder()
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .userId(userId)
            .organizationId(organizationId)
            .details(detailsMap)
            .timestamp(LocalDateTime.now())
            .build();
        
        createAuditLog(auditLog);
    }

    private ResponseEntity<String> exportToCsv(List<AuditLogDto> logs) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Action,EntityType,EntityId,UserId,OrganizationId,Timestamp\n");
        
        for (AuditLogDto log : logs) {
            csv.append(String.format("%d,%s,%s,%d,%d,%d,%s\n",
                log.getId(), log.getAction(), log.getEntityType(),
                log.getEntityId(), log.getUserId(), log.getOrganizationId(),
                log.getTimestamp()));
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "audit-logs.csv");
        
        return ResponseEntity.ok().headers(headers).body(csv.toString());
    }

    private ResponseEntity<String> exportToJson(List<AuditLogDto> logs) {
        // Simple JSON export - in production, use Jackson or similar
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        
        for (int i = 0; i < logs.size(); i++) {
            AuditLogDto log = logs.get(i);
            json.append("  {\n");
            json.append(String.format("    \"id\": %d,\n", log.getId()));
            json.append(String.format("    \"action\": \"%s\",\n", log.getAction()));
            json.append(String.format("    \"entityType\": \"%s\",\n", log.getEntityType()));
            json.append(String.format("    \"entityId\": %d,\n", log.getEntityId()));
            json.append(String.format("    \"userId\": %d,\n", log.getUserId()));
            json.append(String.format("    \"organizationId\": %d,\n", log.getOrganizationId()));
            json.append(String.format("    \"timestamp\": \"%s\"\n", log.getTimestamp()));
            json.append("  }");
            if (i < logs.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("]");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "audit-logs.json");
        
        return ResponseEntity.ok().headers(headers).body(json.toString());
    }

    private ResponseEntity<String> exportToPdf(List<AuditLogDto> logs) {
        // Simplified PDF export - in production, use iText or similar
        String pdfContent = "PDF Export not fully implemented. Use CSV or JSON format.";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "audit-logs.txt");
        
        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }
}
