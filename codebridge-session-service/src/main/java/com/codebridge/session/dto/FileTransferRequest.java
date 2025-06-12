package com.codebridge.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for file transfer initialization request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTransferRequest {
    
    /**
     * Session ID for the connection
     */
    @NotBlank(message = "Session ID is required")
    private String sessionId;
    
    /**
     * Name of the file to be transferred
     */
    @NotBlank(message = "File name is required")
    private String fileName;
    
    /**
     * Size of the file in bytes
     */
    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;
    
    /**
     * Remote path where the file should be saved
     */
    @NotBlank(message = "Remote path is required")
    private String remotePath;
    
    /**
     * Size of each chunk in bytes (optional, default will be used if not provided)
     */
    private Integer chunkSize;
    
    /**
     * MD5 checksum of the file (optional)
     */
    private String checksum;
    
    /**
     * Whether to overwrite existing file (optional, default is false)
     */
    private Boolean overwrite;
    
    /**
     * Direction of transfer (UPLOAD or DOWNLOAD)
     */
    @NotNull(message = "Transfer direction is required")
    private TransferDirection direction;
}
