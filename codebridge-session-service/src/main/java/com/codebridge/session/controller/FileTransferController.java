package com.codebridge.session.controller;

import com.codebridge.session.dto.ChunkMetadata;
import com.codebridge.session.dto.FileTransferRequest;
import com.codebridge.session.dto.FileTransferResponse;
import com.codebridge.session.service.ChunkedFileTransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller for file transfer operations
 */
@RestController
@RequestMapping("/api/transfers")
public class FileTransferController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTransferController.class);
    
    @Autowired
    private ChunkedFileTransferService fileTransferService;
    
    /**
     * Initialize a new file upload
     * @param request File transfer request
     * @return FileTransferResponse with transfer ID and status
     */
    @PostMapping("/initialize")
    public ResponseEntity<FileTransferResponse> initializeUpload(@Valid @RequestBody FileTransferRequest request) {
        logger.info("Initializing file upload: {}", request.getFileName());
        FileTransferResponse response = fileTransferService.initializeUpload(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /**
     * Upload a chunk of a file
     * @param transferId The transfer ID
     * @param chunkNumber The chunk number (0-based)
     * @param chunk The chunk data
     * @return FileTransferResponse with updated status
     */
    @PostMapping("/{transferId}/chunks/{chunkNumber}")
    public ResponseEntity<FileTransferResponse> uploadChunk(
            @PathVariable String transferId,
            @PathVariable int chunkNumber,
            @RequestParam("chunk") MultipartFile chunk) {
        
        logger.info("Uploading chunk {} for transfer {}", chunkNumber, transferId);
        FileTransferResponse response = fileTransferService.uploadChunk(transferId, chunkNumber, chunk);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /**
     * Get the status of a file transfer
     * @param transferId The transfer ID
     * @return FileTransferResponse with current status
     */
    @GetMapping("/{transferId}/status")
    public ResponseEntity<FileTransferResponse> getTransferStatus(@PathVariable String transferId) {
        logger.info("Getting status for transfer {}", transferId);
        FileTransferResponse response = fileTransferService.getTransferStatus(transferId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /**
     * Complete a file transfer
     * @param transferId The transfer ID
     * @return FileTransferResponse with final status
     */
    @PostMapping("/{transferId}/complete")
    public ResponseEntity<FileTransferResponse> completeTransfer(@PathVariable String transferId) {
        logger.info("Completing transfer {}", transferId);
        FileTransferResponse response = fileTransferService.completeTransfer(transferId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /**
     * Cancel a file transfer
     * @param transferId The transfer ID
     * @return FileTransferResponse with final status
     */
    @PostMapping("/{transferId}/cancel")
    public ResponseEntity<FileTransferResponse> cancelTransfer(@PathVariable String transferId) {
        logger.info("Cancelling transfer {}", transferId);
        FileTransferResponse response = fileTransferService.cancelTransfer(transferId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    /**
     * Get metadata about uploaded chunks for resuming a transfer
     * @param transferId The transfer ID
     * @return List of chunk metadata
     */
    @GetMapping("/{transferId}/chunks")
    public ResponseEntity<List<ChunkMetadata>> getUploadedChunks(@PathVariable String transferId) {
        logger.info("Getting uploaded chunks for transfer {}", transferId);
        List<ChunkMetadata> chunks = fileTransferService.getUploadedChunks(transferId);
        return ResponseEntity.status(HttpStatus.OK).body(chunks);
    }
}

