package com.codebridge.session.repository;

import com.codebridge.session.dto.TransferStatus;
import com.codebridge.session.model.FileTransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for file transfer records
 */
@Repository
public interface FileTransferRepository extends JpaRepository<FileTransferRecord, String> {
    
    /**
     * Find all transfers for a session
     * @param sessionId The session ID
     * @return List of file transfer records
     */
    List<FileTransferRecord> findBySessionId(String sessionId);
    
    /**
     * Find all transfers with a specific status
     * @param status The transfer status
     * @return List of file transfer records
     */
    List<FileTransferRecord> findByStatus(TransferStatus status);
    
    /**
     * Find all transfers for a session with a specific status
     * @param sessionId The session ID
     * @param status The transfer status
     * @return List of file transfer records
     */
    List<FileTransferRecord> findBySessionIdAndStatus(String sessionId, TransferStatus status);
    
    /**
     * Find all transfers created before a specific time
     * @param dateTime The cutoff time
     * @return List of file transfer records
     */
    List<FileTransferRecord> findByCreatedAtBefore(LocalDateTime dateTime);
    
    /**
     * Find all transfers that have not been updated since a specific time
     * @param dateTime The cutoff time
     * @return List of file transfer records
     */
    List<FileTransferRecord> findByUpdatedAtBeforeAndStatusNot(LocalDateTime dateTime, TransferStatus status);
}

