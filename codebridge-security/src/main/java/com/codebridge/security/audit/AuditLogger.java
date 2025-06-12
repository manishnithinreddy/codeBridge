package com.codebridge.security.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Logger for audit events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogger {

    private final ObjectMapper objectMapper;
    
    @Value("${security.audit.enabled:true}")
    private boolean auditEnabled;
    
    @Value("${security.audit.log-directory:${user.home}/codebridge/audit-logs}")
    private String logDirectory;

    /**
     * Logs a security event.
     *
     * @param eventType The event type
     * @param message The message
     * @param metadata The metadata
     */
    public void logSecurityEvent(String eventType, String message, Map<String, Object> metadata) {
        if (!auditEnabled) {
            return;
        }
        
        try {
            // Create log directory if it doesn't exist
            Path logPath = Paths.get(logDirectory);
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath);
            }
            
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            // Create audit event
            Map<String, Object> auditEvent = new HashMap<>();
            auditEvent.put("id", UUID.randomUUID().toString());
            auditEvent.put("timestamp", LocalDateTime.now().toString());
            auditEvent.put("eventType", eventType);
            auditEvent.put("message", message);
            auditEvent.put("username", username);
            
            if (metadata != null) {
                auditEvent.put("metadata", metadata);
            }
            
            // Write to log file
            String logFileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log";
            File logFile = new File(logPath.toFile(), logFileName);
            
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(objectMapper.writeValueAsString(auditEvent) + System.lineSeparator());
            }
            
            // Also log to application logs
            log.info("AUDIT: {}: {}", eventType, message);
        } catch (IOException e) {
            log.error("Failed to write audit log", e);
        }
    }

    /**
     * Logs a security event.
     *
     * @param eventType The event type
     * @param message The message
     */
    public void logSecurityEvent(String eventType, String message) {
        logSecurityEvent(eventType, message, null);
    }
}

