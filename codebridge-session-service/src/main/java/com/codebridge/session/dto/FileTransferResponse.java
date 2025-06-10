package com.codebridge.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for file transfer response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTransferResponse {
    
    /**
     * Unique ID for the transfer
     */
    private String transferId;
    
    /**
     * Name of the file being transferred
     */
    private String fileName;
    
    /**
     * Current status of the transfer
     */
    private TransferStatus status;
    
    /**
     * Total number of chunks for the file
     */
    private Integer totalChunks;
    
    /**
     * Number of chunks that have been completed
     */
    private Integer completedChunks;
    
    /**
     * Progress percentage (0-100)
     */
    private Integer progressPercentage;
    
    /**
     * Message about the transfer status
     */
    private String message;
    
    /**
     * Error message if any
     */
    private String error;
    
    /**
     * MD5 checksum of the completed file (only provided when transfer is complete)
     */
    private String checksum;
    
    /**
     * Calculate progress percentage based on completed and total chunks
     * @return Progress percentage (0-100)
     */
    public Integer getProgressPercentage() {
        if (totalChunks == null || totalChunks == 0 || completedChunks == null) {
            return 0;
        }
        return (int) Math.floor((double) completedChunks / totalChunks * 100);
    }
}

