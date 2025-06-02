package com.codebridge.server.controller;

import com.codebridge.server.dto.file.RemoteFileEntry;
import com.codebridge.server.service.FileTransferService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    private UUID getPlatformUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Authentication principal not found or username is null.");
        }
        return UUID.fromString(authentication.getName());
    }

    @GetMapping("/list")
    public ResponseEntity<List<RemoteFileEntry>> listFiles(
            @PathVariable UUID serverId,
            @RequestParam @NotBlank String remotePath,
            Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        List<RemoteFileEntry> files = fileTransferService.listFiles(serverId, platformUserId, remotePath);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID serverId,
            @RequestParam @NotBlank String remotePath,
            Authentication authentication) {
        UUID platformUserId = getPlatformUserId(authentication);
        byte[] fileData = fileTransferService.downloadFile(serverId, platformUserId, remotePath);
        
        ByteArrayResource resource = new ByteArrayResource(fileData);
        
        String filename = remotePath.substring(remotePath.lastIndexOf('/') + 1);
        // Ensure filename is properly encoded for the Content-Disposition header
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileData.length))
                .body(resource);
    }

    @PostMapping("/upload")
    public ResponseEntity<Void> uploadFile(
            @PathVariable UUID serverId,
            @RequestParam @NotBlank String remotePath, // This is the target directory
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        UUID platformUserId = getPlatformUserId(authentication);
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build(); // Or throw custom exception
        }
        
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded_file";

        fileTransferService.uploadFile(
                serverId,
                platformUserId,
                remotePath,
                file.getInputStream(),
                originalFilename
        );
        return ResponseEntity.ok().build(); // Or ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
