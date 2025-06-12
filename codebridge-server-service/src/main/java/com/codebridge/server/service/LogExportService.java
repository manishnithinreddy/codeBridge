package com.codebridge.server.service;

import com.codebridge.server.model.ServerActivityLog;
import com.codebridge.server.repository.ServerActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for exporting logs.
 */
@Service
public class LogExportService {

    private static final Logger logger = LoggerFactory.getLogger(LogExportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final ServerActivityLogRepository activityLogRepository;
    
    @Autowired
    public LogExportService(ServerActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }
    
    /**
     * Export logs as CSV.
     *
     * @param userId optional user ID to filter by
     * @param serverId optional server ID to filter by
     * @param startDate optional start date to filter by
     * @param endDate optional end date to filter by
     * @return the CSV data as a byte array
     */
    @Transactional(readOnly = true)
    public byte[] exportLogsAsCsv(UUID userId, UUID serverId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Exporting logs as CSV for user: {}, server: {}, startDate: {}, endDate: {}", 
                userId, serverId, startDate, endDate);
        
        List<ServerActivityLog> logs = fetchLogs(userId, serverId, startDate, endDate);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(outputStream)) {
            // Write CSV header
            writer.println("ID,User ID,Server ID,Action,Status,Details,Error Message,IP Address,User Agent,Timestamp");
            
            // Write log entries
            for (ServerActivityLog log : logs) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        log.getId(),
                        log.getUserId(),
                        log.getServerId() != null ? log.getServerId() : "",
                        escapeCsvField(log.getAction()),
                        escapeCsvField(log.getStatus()),
                        escapeCsvField(log.getDetails()),
                        escapeCsvField(log.getErrorMessage()),
                        escapeCsvField(log.getIpAddress()),
                        escapeCsvField(log.getUserAgent()),
                        log.getTimestamp().format(TIMESTAMP_FORMATTER)));
            }
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Export logs as JSON.
     *
     * @param userId optional user ID to filter by
     * @param serverId optional server ID to filter by
     * @param startDate optional start date to filter by
     * @param endDate optional end date to filter by
     * @return the JSON data as a byte array
     */
    @Transactional(readOnly = true)
    public byte[] exportLogsAsJson(UUID userId, UUID serverId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Exporting logs as JSON for user: {}, server: {}, startDate: {}, endDate: {}", 
                userId, serverId, startDate, endDate);
        
        List<ServerActivityLog> logs = fetchLogs(userId, serverId, startDate, endDate);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(outputStream)) {
            // Write JSON array start
            writer.println("[");
            
            // Write log entries
            for (int i = 0; i < logs.size(); i++) {
                ServerActivityLog log = logs.get(i);
                
                writer.println("  {");
                writer.println(String.format("    \"id\": \"%s\",", log.getId()));
                writer.println(String.format("    \"userId\": \"%s\",", log.getUserId()));
                
                if (log.getServerId() != null) {
                    writer.println(String.format("    \"serverId\": \"%s\",", log.getServerId()));
                } else {
                    writer.println("    \"serverId\": null,");
                }
                
                writer.println(String.format("    \"action\": \"%s\",", escapeJsonField(log.getAction())));
                writer.println(String.format("    \"status\": \"%s\",", escapeJsonField(log.getStatus())));
                writer.println(String.format("    \"details\": \"%s\",", escapeJsonField(log.getDetails())));
                
                if (log.getErrorMessage() != null) {
                    writer.println(String.format("    \"errorMessage\": \"%s\",", escapeJsonField(log.getErrorMessage())));
                } else {
                    writer.println("    \"errorMessage\": null,");
                }
                
                writer.println(String.format("    \"ipAddress\": \"%s\",", escapeJsonField(log.getIpAddress())));
                writer.println(String.format("    \"userAgent\": \"%s\",", escapeJsonField(log.getUserAgent())));
                writer.println(String.format("    \"timestamp\": \"%s\"", log.getTimestamp().format(TIMESTAMP_FORMATTER)));
                
                if (i < logs.size() - 1) {
                    writer.println("  },");
                } else {
                    writer.println("  }");
                }
            }
            
            // Write JSON array end
            writer.println("]");
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Export logs as ZIP containing both CSV and JSON formats.
     *
     * @param userId optional user ID to filter by
     * @param serverId optional server ID to filter by
     * @param startDate optional start date to filter by
     * @param endDate optional end date to filter by
     * @return the ZIP data as a byte array
     */
    @Transactional(readOnly = true)
    public byte[] exportLogsAsZip(UUID userId, UUID serverId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Exporting logs as ZIP for user: {}, server: {}, startDate: {}, endDate: {}", 
                userId, serverId, startDate, endDate);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            // Add CSV file to ZIP
            byte[] csvData = exportLogsAsCsv(userId, serverId, startDate, endDate);
            ZipEntry csvEntry = new ZipEntry("logs.csv");
            zipOutputStream.putNextEntry(csvEntry);
            zipOutputStream.write(csvData);
            zipOutputStream.closeEntry();
            
            // Add JSON file to ZIP
            byte[] jsonData = exportLogsAsJson(userId, serverId, startDate, endDate);
            ZipEntry jsonEntry = new ZipEntry("logs.json");
            zipOutputStream.putNextEntry(jsonEntry);
            zipOutputStream.write(jsonData);
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            logger.error("Error creating ZIP file", e);
            throw new RuntimeException("Error creating ZIP file", e);
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Fetch logs based on filters.
     *
     * @param userId optional user ID to filter by
     * @param serverId optional server ID to filter by
     * @param startDate optional start date to filter by
     * @param endDate optional end date to filter by
     * @return the list of logs
     */
    private List<ServerActivityLog> fetchLogs(UUID userId, UUID serverId, LocalDateTime startDate, LocalDateTime endDate) {
        // Set default dates if not provided
        LocalDateTime effectiveStartDate = startDate != null ? startDate : LocalDateTime.now().minusDays(7);
        LocalDateTime effectiveEndDate = endDate != null ? endDate : LocalDateTime.now();
        
        // Fetch logs based on filters
        if (userId != null && serverId != null) {
            return activityLogRepository.findByUserIdAndServerIdAndTimestampBetween(
                    userId, serverId, effectiveStartDate, effectiveEndDate);
        } else if (userId != null) {
            return activityLogRepository.findByUserIdAndTimestampBetween(
                    userId, effectiveStartDate, effectiveEndDate);
        } else if (serverId != null) {
            return activityLogRepository.findByServerIdAndTimestampBetween(
                    serverId, effectiveStartDate, effectiveEndDate);
        } else {
            return activityLogRepository.findByTimestampBetween(
                    effectiveStartDate, effectiveEndDate);
        }
    }
    
    /**
     * Escape a field for CSV format.
     *
     * @param field the field to escape
     * @return the escaped field
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        
        // Escape quotes and wrap in quotes if the field contains commas, quotes, or newlines
        if (field.contains("\"") || field.contains(",") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        
        return field;
    }
    
    /**
     * Escape a field for JSON format.
     *
     * @param field the field to escape
     * @return the escaped field
     */
    private String escapeJsonField(String field) {
        if (field == null) {
            return "";
        }
        
        // Escape backslashes, quotes, and control characters
        return field.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

