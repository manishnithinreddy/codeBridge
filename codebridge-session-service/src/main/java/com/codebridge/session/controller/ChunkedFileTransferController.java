package com.codebridge.session.controller;

import com.codebridge.session.model.SessionKey;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.SshSessionLifecycleManager;
import com.codebridge.session.service.transfer.ChunkedFileTransferService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for optimized file transfers using chunking.
 * This improves performance and reliability for large file transfers.
 */
@RestController
@RequestMapping("/api/sessions/ops/ssh/{sessionToken}/sftp/chunked")
public class ChunkedFileTransferController {
    private static final Logger logger = LoggerFactory.getLogger(ChunkedFileTransferController.class);

    private final ChunkedFileTransferService fileTransferService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SshSessionLifecycleManager sessionLifecycleManager;

    @Autowired
    public ChunkedFileTransferController(
            ChunkedFileTransferService fileTransferService,
            JwtTokenProvider jwtTokenProvider,
            SshSessionLifecycleManager sessionLifecycleManager) {
        this.fileTransferService = fileTransferService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionLifecycleManager = sessionLifecycleManager;
    }

    /**
     * Uploads a file to a remote server using chunking.
     *
     * @param sessionToken The session token
     * @param remotePath The remote path
     * @param file The file to upload
     * @return A response entity
     */
    @PostMapping("/upload")
    public CompletableFuture<ResponseEntity<Void>> uploadFile(
            @PathVariable String sessionToken,
            @RequestParam String remotePath,
            @RequestParam("file") MultipartFile file) {
        
        SessionKey sessionKey = getSessionKeyFromToken(sessionToken);
        
        return fileTransferService.uploadFile(sessionKey, file, remotePath)
                .thenApply(result -> ResponseEntity.ok().build());
    }

    /**
     * Downloads a file from a remote server using chunking.
     *
     * @param sessionToken The session token
     * @param remotePath The remote path
     * @return A response entity with the file data
     */
    @GetMapping("/download")
    public CompletableFuture<ResponseEntity<Resource>> downloadFile(
            @PathVariable String sessionToken,
            @RequestParam String remotePath) {
        
        SessionKey sessionKey = getSessionKeyFromToken(sessionToken);
        
        return fileTransferService.downloadFile(sessionKey, remotePath)
                .thenApply(fileData -> {
                    ByteArrayResource resource = new ByteArrayResource(fileData);
                    String filename = remotePath.substring(remotePath.lastIndexOf('/') + 1);
                    String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
                    
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.contentLength()))
                            .body(resource);
                });
    }

    /**
     * Gets the session key from a session token.
     *
     * @param sessionToken The session token
     * @return The session key
     */
    private SessionKey getSessionKeyFromToken(String sessionToken) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(sessionToken);
        if (claims == null) {
            throw new IllegalArgumentException("Invalid session token");
        }
        
        return new SessionKey(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get("resourceId", String.class)),
                claims.get("type", String.class)
        );
    }
}

