package com.codebridge.session.controller;

import com.codebridge.session.dto.FileChunkRequest;
import com.codebridge.session.dto.FileChunkResponse;
import com.codebridge.session.dto.FileTransferInitRequest;
import com.codebridge.session.dto.FileTransferInitResponse;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.SshSessionLifecycleManager;
import com.codebridge.session.service.transfer.ChunkedFileTransferService;
import com.codebridge.session.service.transfer.ChunkedFileTransferService.TransferDirection;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
     * Initialize a file transfer (upload or download).
     *
     * @param sessionToken The session token
     * @param request The file transfer initialization request
     * @return A response entity with the transfer ID
     */
    @PostMapping("/init")
    public ResponseEntity<FileTransferInitResponse> initializeTransfer(
            @PathVariable String sessionToken,
            @Valid @RequestBody FileTransferInitRequest request) {
        
        logger.info("Initializing {} transfer for file: {}", 
                request.getDirection(), request.getRemotePath());
        
        FileTransferInitResponse response = fileTransferService.initializeFileTransfer(
                sessionToken, request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Transfer a chunk of a file (upload or download).
     *
     * @param sessionToken The session token
     * @param transferId The transfer ID
     * @param request The chunk request
     * @return A response entity with the chunk response
     */
    @PostMapping("/{transferId}/chunk")
    public ResponseEntity<FileChunkResponse> transferChunk(
            @PathVariable String sessionToken,
            @PathVariable String transferId,
            @Valid @RequestBody FileChunkRequest request) {
        
        logger.debug("Processing chunk {} for transfer {}", 
                request.getChunkIndex(), transferId);
        
        FileChunkResponse response = fileTransferService.transferChunk(
                sessionToken, transferId, request);
        
        return ResponseEntity.ok(response);
    }
}

