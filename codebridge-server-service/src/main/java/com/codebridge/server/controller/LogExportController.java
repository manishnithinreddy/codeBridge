package com.codebridge.server.controller;

import com.codebridge.server.service.LogExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Controller for log export operations.
 */
@RestController
@RequestMapping("/api/v1/logs/export")
public class LogExportController {

    private static final Logger logger = LoggerFactory.getLogger(LogExportController.class);
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final LogExportService logExportService;
    
    @Autowired
    public LogExportController(LogExportService logExportService) {
        this.logExportService = logExportService;
    }
    
    /**
     * Export logs as CSV.
     *
     * @param userId optional user ID to filter by
     * @param serverId optional server ID to filter by
     * @param startDate optional start date to filter by
     * @param endDate optional end date to filter by
     * @return the CSV file
     */
    @GetMapping("/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<byte[]> exportLogsAsCsv(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID serverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.info("Exporting logs as CSV for user: {}, server: {}, startDate: {}, endDate: {}", 
                userId, serverId, startDate, endDate);
        
        byte[] csvData = logExportService.exportLogsAsCsv(userId, serverId, startDate, endDate);
        
        String filename = "logs_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".csv";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(csvData.length)
                .body(csvData);
    }
    
    /**
     * Export logs as JSON.
     *
     * @param userId optional user ID to filter by
     * @param serverId optional server ID to filter by
     * @param startDate optional start date to filter by
     * @param endDate optional end date to filter by
     * @return the JSON file
     */
    @GetMapping("/json")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<byte[]> exportLogsAsJson(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID serverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.info("Exporting logs as JSON for user: {}, server: {}, startDate: {}, endDate: {}", 
                userId, serverId, startDate, endDate);
        
        byte[] jsonData = logExportService.exportLogsAsJson(userId, serverId, startDate, endDate);
        
        String filename = "logs_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".json";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(jsonData.length)
                .body(jsonData);
    }
    
    /**
     * Export logs as ZIP containing both CSV and JSON formats.
     *
     * @param userId optional user ID to filter by
     * @param serverId optional server ID to filter by
     * @param startDate optional start date to filter by
     * @param endDate optional end date to filter by
     * @return the ZIP file
     */
    @GetMapping("/zip")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<byte[]> exportLogsAsZip(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID serverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        logger.info("Exporting logs as ZIP for user: {}, server: {}, startDate: {}, endDate: {}", 
                userId, serverId, startDate, endDate);
        
        byte[] zipData = logExportService.exportLogsAsZip(userId, serverId, startDate, endDate);
        
        String filename = "logs_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".zip";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zipData.length)
                .body(zipData);
    }
}

