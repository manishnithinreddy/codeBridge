package com.codebridge.server.controller;

import com.codebridge.server.dto.remote.RemoteFileEntry;
import com.codebridge.server.service.FileTransferService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servers/{serverId}/files")
public class FileTransferController {

    private final FileTransferService fileTransferService;

    public FileTransferController(FileTransferService fileTransferService) {
        this.fileTransferService = fileTransferService;
    }

    // getCurrentUserId() removed as session token will provide user context

    @GetMapping("/list")
    public ResponseEntity<List<RemoteFileEntry>> listFiles(
            @PathVariable UUID serverId,
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestParam(required = false, defaultValue = ".") String remotePath) {
        List<RemoteFileEntry> fileEntries = fileTransferService.listFiles(serverId, sessionToken, remotePath);
        return ResponseEntity.ok(fileEntries);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID serverId,
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestParam @NotBlank String remotePath) {
        byte[] fileData = fileTransferService.downloadFile(serverId, sessionToken, remotePath);
        ByteArrayResource resource = new ByteArrayResource(fileData);

        // Extract filename from path for Content-Disposition header
        String filename = "";
        if (remotePath != null && !remotePath.isEmpty()) {
            // Basic extraction, consider more robust path normalization if needed
            filename = Paths.get(remotePath).getFileName().toString();
        }
        // Ensure the filename is properly encoded for the Content-Disposition header
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentLength(fileData.length)
                .body(resource);
    }

    @PostMapping("/upload")
    public ResponseEntity<Void> uploadFile(
            @PathVariable UUID serverId,
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestParam(required = false, defaultValue = ".") String remotePath,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            // Consider throwing a custom exception handled by GlobalExceptionHandler
            return ResponseEntity.badRequest().build();
        }
        fileTransferService.uploadFile(
                serverId,
                sessionToken,
                remotePath,
                file.getInputStream(),
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded_file"
        );
        return ResponseEntity.ok().build();
    }
}
