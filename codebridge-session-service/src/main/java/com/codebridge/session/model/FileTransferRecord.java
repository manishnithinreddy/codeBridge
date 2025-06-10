package com.codebridge.session.model;

import com.codebridge.session.dto.TransferDirection;
import com.codebridge.session.dto.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entity for tracking file transfers
 */
@Entity
@Table(name = "file_transfers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileTransferRecord {
    
    /**
     * Unique ID for the transfer
     */
    @Id
    @Column(name = "transfer_id")
    private String transferId;
    
    /**
     * Session ID associated with this transfer
     */
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    /**
     * Name of the file being transferred
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    /**
     * Size of the file in bytes
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    /**
     * Remote path where the file is being transferred to/from
     */
    @Column(name = "remote_path", nullable = false)
    private String remotePath;
    
    /**
     * Size of each chunk in bytes
     */
    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;
    
    /**
     * Total number of chunks for the file
     */
    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;
    
    /**
     * Number of chunks that have been completed
     */
    @Column(name = "completed_chunks", nullable = false)
    private Integer completedChunks;
    
    /**
     * Current status of the transfer
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransferStatus status;
    
    /**
     * Direction of the transfer (UPLOAD or DOWNLOAD)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private TransferDirection direction;
    
    /**
     * MD5 checksum of the file (optional)
     */
    @Column(name = "checksum")
    private String checksum;
    
    /**
     * Error message if any
     */
    @Column(name = "error_message")
    private String errorMessage;
    
    /**
     * When the transfer was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * When the transfer was last updated
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * When the transfer was completed (if applicable)
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}

