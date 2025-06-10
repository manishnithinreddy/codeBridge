package com.codebridge.session.service;

import com.codebridge.session.dto.ChunkMetadata;
import com.codebridge.session.dto.FileTransferRequest;
import com.codebridge.session.dto.FileTransferResponse;
import com.codebridge.session.dto.TransferStatus;
import com.codebridge.session.exception.FileTransferException;
import com.codebridge.session.model.FileTransferRecord;
import com.codebridge.session.repository.FileTransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling chunked file transfers with resume capability
 */
@Service
public class ChunkedFileTransferService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkedFileTransferService.class);
    
    @Value("${file.upload.temp.directory:/tmp/codebridge/uploads}")
    private String tempUploadDirectory;
    
    @Value("${file.chunk.size:1048576}") // Default 1MB
    private int defaultChunkSize;
    
    @Value("${file.transfer.rate.limit.bytes.per.second:0}")
    private long rateLimitBytesPerSecond; // 0 means no limit
    
    @Autowired
    private FileTransferRepository fileTransferRepository;
    
    @Autowired
    private SessionService sessionService;

    /**
     * Initialize a new file upload
     * @param request File transfer request with metadata
     * @return FileTransferResponse with transfer ID and status
     */
    public FileTransferResponse initializeUpload(FileTransferRequest request) {
        logger.info("Initializing file upload: {}", request.getFileName());
        
        // Validate session
        if (!sessionService.isSessionActive(request.getSessionId())) {
            throw new FileTransferException("Invalid or inactive session");
        }
        
        // Create a unique transfer ID
        String transferId = UUID.randomUUID().toString();
        
        // Create directory if it doesn't exist
        createDirectoryIfNotExists();
        
        // Calculate number of chunks
        int totalChunks = calculateTotalChunks(request.getFileSize(), request.getChunkSize() != null ? 
                request.getChunkSize() : defaultChunkSize);
        
        // Create and save transfer record
        FileTransferRecord record = new FileTransferRecord();
        record.setTransferId(transferId);
        record.setSessionId(request.getSessionId());
        record.setFileName(request.getFileName());
        record.setFileSize(request.getFileSize());
        record.setRemotePath(request.getRemotePath());
        record.setChunkSize(request.getChunkSize() != null ? request.getChunkSize() : defaultChunkSize);
        record.setTotalChunks(totalChunks);
        record.setCompletedChunks(0);
        record.setStatus(TransferStatus.INITIALIZED);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        
        fileTransferRepository.save(record);
        
        return FileTransferResponse.builder()
                .transferId(transferId)
                .fileName(request.getFileName())
                .status(TransferStatus.INITIALIZED)
                .totalChunks(totalChunks)
                .completedChunks(0)
                .message("Upload initialized successfully")
                .build();
    }
    
    /**
     * Upload a chunk of a file
     * @param transferId The transfer ID
     * @param chunkNumber The chunk number (0-based)
     * @param chunkFile The chunk data
     * @return FileTransferResponse with updated status
     */
    public FileTransferResponse uploadChunk(String transferId, int chunkNumber, MultipartFile chunkFile) {
        logger.info("Uploading chunk {} for transfer {}", chunkNumber, transferId);
        
        // Get transfer record
        FileTransferRecord record = getTransferRecord(transferId);
        
        // Validate chunk number
        if (chunkNumber < 0 || chunkNumber >= record.getTotalChunks()) {
            throw new FileTransferException("Invalid chunk number: " + chunkNumber);
        }
        
        // Check if chunk already uploaded
        if (isChunkUploaded(transferId, chunkNumber)) {
            logger.info("Chunk {} already uploaded for transfer {}", chunkNumber, transferId);
            return getTransferStatus(transferId);
        }
        
        try {
            // Save chunk to temp file
            String chunkPath = getChunkPath(transferId, chunkNumber);
            File chunkDestination = new File(chunkPath);
            chunkFile.transferTo(chunkDestination);
            
            // Apply rate limiting if configured
            applyRateLimit(chunkFile.getSize());
            
            // Update record
            record.setCompletedChunks(record.getCompletedChunks() + 1);
            record.setUpdatedAt(LocalDateTime.now());
            
            // Check if all chunks are uploaded
            if (record.getCompletedChunks() == record.getTotalChunks()) {
                record.setStatus(TransferStatus.COMPLETED);
            } else {
                record.setStatus(TransferStatus.IN_PROGRESS);
            }
            
            fileTransferRepository.save(record);
            
            return FileTransferResponse.builder()
                    .transferId(transferId)
                    .fileName(record.getFileName())
                    .status(record.getStatus())
                    .totalChunks(record.getTotalChunks())
                    .completedChunks(record.getCompletedChunks())
                    .message("Chunk uploaded successfully")
                    .build();
            
        } catch (IOException e) {
            logger.error("Error uploading chunk", e);
            throw new FileTransferException("Failed to upload chunk: " + e.getMessage());
        }
    }
    
    /**
     * Get the status of a file transfer
     * @param transferId The transfer ID
     * @return FileTransferResponse with current status
     */
    public FileTransferResponse getTransferStatus(String transferId) {
        FileTransferRecord record = getTransferRecord(transferId);
        
        return FileTransferResponse.builder()
                .transferId(transferId)
                .fileName(record.getFileName())
                .status(record.getStatus())
                .totalChunks(record.getTotalChunks())
                .completedChunks(record.getCompletedChunks())
                .message("Transfer status retrieved")
                .build();
    }
    
    /**
     * Complete the file transfer by assembling chunks and uploading to the remote server
     * @param transferId The transfer ID
     * @return FileTransferResponse with final status
     */
    public FileTransferResponse completeTransfer(String transferId) {
        logger.info("Completing transfer {}", transferId);
        
        FileTransferRecord record = getTransferRecord(transferId);
        
        // Check if all chunks are uploaded
        if (record.getCompletedChunks() < record.getTotalChunks()) {
            throw new FileTransferException("Cannot complete transfer: not all chunks uploaded");
        }
        
        try {
            // Assemble file from chunks
            File assembledFile = assembleFile(transferId, record);
            
            // TODO: Upload assembled file to remote server using SessionService
            // This would involve using the existing SSH/SFTP connection to upload the file
            
            // Update record
            record.setStatus(TransferStatus.COMPLETED);
            record.setUpdatedAt(LocalDateTime.now());
            fileTransferRepository.save(record);
            
            // Clean up chunks
            cleanupChunks(transferId, record.getTotalChunks());
            
            return FileTransferResponse.builder()
                    .transferId(transferId)
                    .fileName(record.getFileName())
                    .status(TransferStatus.COMPLETED)
                    .totalChunks(record.getTotalChunks())
                    .completedChunks(record.getCompletedChunks())
                    .message("File transfer completed successfully")
                    .build();
            
        } catch (Exception e) {
            logger.error("Error completing transfer", e);
            
            // Update record
            record.setStatus(TransferStatus.FAILED);
            record.setUpdatedAt(LocalDateTime.now());
            fileTransferRepository.save(record);
            
            throw new FileTransferException("Failed to complete transfer: " + e.getMessage());
        }
    }
    
    /**
     * Cancel a file transfer
     * @param transferId The transfer ID
     * @return FileTransferResponse with final status
     */
    public FileTransferResponse cancelTransfer(String transferId) {
        logger.info("Cancelling transfer {}", transferId);
        
        FileTransferRecord record = getTransferRecord(transferId);
        
        // Update record
        record.setStatus(TransferStatus.CANCELLED);
        record.setUpdatedAt(LocalDateTime.now());
        fileTransferRepository.save(record);
        
        // Clean up chunks
        cleanupChunks(transferId, record.getTotalChunks());
        
        return FileTransferResponse.builder()
                .transferId(transferId)
                .fileName(record.getFileName())
                .status(TransferStatus.CANCELLED)
                .totalChunks(record.getTotalChunks())
                .completedChunks(record.getCompletedChunks())
                .message("File transfer cancelled")
                .build();
    }
    
    /**
     * Get metadata about uploaded chunks for resuming a transfer
     * @param transferId The transfer ID
     * @return List of chunk metadata
     */
    public List<ChunkMetadata> getUploadedChunks(String transferId) {
        FileTransferRecord record = getTransferRecord(transferId);
        List<ChunkMetadata> chunks = new ArrayList<>();
        
        for (int i = 0; i < record.getTotalChunks(); i++) {
            File chunkFile = new File(getChunkPath(transferId, i));
            if (chunkFile.exists()) {
                chunks.add(ChunkMetadata.builder()
                        .chunkNumber(i)
                        .size(chunkFile.length())
                        .uploaded(true)
                        .build());
            } else {
                chunks.add(ChunkMetadata.builder()
                        .chunkNumber(i)
                        .uploaded(false)
                        .build());
            }
        }
        
        return chunks;
    }
    
    /**
     * Calculate MD5 checksum for a file
     * @param file The file to calculate checksum for
     * @return MD5 checksum as hex string
     */
    public String calculateChecksum(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] digest = md.digest(fileBytes);
            
            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("Error calculating checksum", e);
            throw new FileTransferException("Failed to calculate checksum: " + e.getMessage());
        }
    }
    
    // Helper methods
    
    private void createDirectoryIfNotExists() {
        File directory = new File(tempUploadDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
    
    private int calculateTotalChunks(long fileSize, int chunkSize) {
        return (int) Math.ceil((double) fileSize / chunkSize);
    }
    
    private FileTransferRecord getTransferRecord(String transferId) {
        Optional<FileTransferRecord> recordOpt = fileTransferRepository.findById(transferId);
        if (!recordOpt.isPresent()) {
            throw new FileTransferException("Transfer not found: " + transferId);
        }
        return recordOpt.get();
    }
    
    private String getChunkPath(String transferId, int chunkNumber) {
        return tempUploadDirectory + File.separator + transferId + "_" + chunkNumber + ".part";
    }
    
    private boolean isChunkUploaded(String transferId, int chunkNumber) {
        File chunkFile = new File(getChunkPath(transferId, chunkNumber));
        return chunkFile.exists() && chunkFile.length() > 0;
    }
    
    private File assembleFile(String transferId, FileTransferRecord record) throws IOException {
        String outputPath = tempUploadDirectory + File.separator + record.getFileName();
        File outputFile = new File(outputPath);
        
        try (RandomAccessFile output = new RandomAccessFile(outputFile, "rw")) {
            // Set the file size
            output.setLength(record.getFileSize());
            
            // Copy each chunk to the right position
            for (int i = 0; i < record.getTotalChunks(); i++) {
                File chunkFile = new File(getChunkPath(transferId, i));
                byte[] buffer = Files.readAllBytes(chunkFile.toPath());
                
                output.seek((long) i * record.getChunkSize());
                output.write(buffer);
            }
        }
        
        return outputFile;
    }
    
    private void cleanupChunks(String transferId, int totalChunks) {
        for (int i = 0; i < totalChunks; i++) {
            File chunkFile = new File(getChunkPath(transferId, i));
            if (chunkFile.exists()) {
                chunkFile.delete();
            }
        }
    }
    
    /**
     * Apply rate limiting to control transfer speed
     * @param bytesTransferred The number of bytes transferred
     */
    private void applyRateLimit(long bytesTransferred) {
        if (rateLimitBytesPerSecond > 0) {
            try {
                // Simple rate limiting by sleeping
                long sleepTime = (bytesTransferred * 1000) / rateLimitBytesPerSecond;
                if (sleepTime > 0) {
                    logger.debug("Rate limiting applied: sleeping for {} ms after transferring {} bytes", 
                            sleepTime, bytesTransferred);
                    Thread.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Rate limiting interrupted", e);
            }
        }
    }
}
