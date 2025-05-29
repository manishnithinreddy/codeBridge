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

    // Placeholder for userId extraction - replace with actual Spring Security principal
    private UUID getCurrentUserId() {
        // In a real app with Spring Security:
        // UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // return UUID.fromString(userDetails.getUsername()); // Assuming username is UUID
        // For now, using a placeholder. This MUST be replaced.
        return UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"); // Example UUID
    }

    @GetMapping("/list")
    public ResponseEntity<List<RemoteFileEntry>> listFiles(
            @PathVariable UUID serverId,
            @RequestParam(required = false, defaultValue = ".") String remotePath) {
        UUID userId = getCurrentUserId();
        List<RemoteFileEntry> fileEntries = fileTransferService.listFiles(serverId, userId, remotePath);
        return ResponseEntity.ok(fileEntries);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID serverId,
            @RequestParam @NotBlank String remotePath) {
        UUID userId = getCurrentUserId();
        byte[] fileData = fileTransferService.downloadFile(serverId, userId, remotePath);
        ByteArrayResource resource = new ByteArrayResource(fileData);

        String filename = Paths.get(remotePath).getFileName().toString();
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
            @RequestParam(required = false, defaultValue = ".") String remotePath,
            @RequestParam("file") MultipartFile file) throws IOException {
        UUID userId = getCurrentUserId();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build(); // Or throw custom exception
        }
        fileTransferService.uploadFile(
                serverId,
                userId,
                remotePath,
                file.getInputStream(),
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded_file"
        );
        return ResponseEntity.ok().build();
    }
}
