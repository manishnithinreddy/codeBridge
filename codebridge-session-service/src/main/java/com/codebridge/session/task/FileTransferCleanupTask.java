package com.codebridge.session.task;

import com.codebridge.session.dto.TransferStatus;
import com.codebridge.session.model.FileTransferRecord;
import com.codebridge.session.repository.FileTransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to clean up old file transfers
 */
@Component
public class FileTransferCleanupTask {
    
    private static final Logger logger = LoggerFactory.getLogger(FileTransferCleanupTask.class);
    
    @Autowired
    private FileTransferRepository fileTransferRepository;
    
    @Value("${file.upload.temp.directory:/tmp/codebridge/uploads}")
    private String tempUploadDirectory;
    
    @Value("${file.transfer.cleanup.days:7}")
    private int cleanupDays;
    
    /**
     * Clean up completed transfers older than the configured number of days
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldTransfers() {
        logger.info("Running file transfer cleanup task");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDays);
        
        // Find completed transfers older than cutoff date
        List<FileTransferRecord> oldTransfers = fileTransferRepository.findByUpdatedAtBeforeAndStatusNot(
                cutoffDate, TransferStatus.IN_PROGRESS);
        
        logger.info("Found {} old transfers to clean up", oldTransfers.size());
        
        for (FileTransferRecord transfer : oldTransfers) {
            try {
                // Clean up any remaining chunk files
                for (int i = 0; i < transfer.getTotalChunks(); i++) {
                    String chunkPath = tempUploadDirectory + File.separator + 
                            transfer.getTransferId() + "_" + i + ".part";
                    File chunkFile = new File(chunkPath);
                    if (chunkFile.exists()) {
                        chunkFile.delete();
                    }
                }
                
                // Clean up any assembled file
                String filePath = tempUploadDirectory + File.separator + transfer.getFileName();
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
                
                // Delete the record
                fileTransferRepository.delete(transfer);
                
                logger.info("Cleaned up transfer {}", transfer.getTransferId());
                
            } catch (Exception e) {
                logger.error("Error cleaning up transfer {}", transfer.getTransferId(), e);
            }
        }
        
        logger.info("File transfer cleanup task completed");
    }
    
    /**
     * Clean up stalled transfers that haven't been updated in 24 hours
     * Runs every 6 hours
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void cleanupStalledTransfers() {
        logger.info("Running stalled transfer cleanup task");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(24);
        
        // Find in-progress transfers that haven't been updated in 24 hours
        List<FileTransferRecord> stalledTransfers = fileTransferRepository.findByUpdatedAtBeforeAndStatusNot(
                cutoffDate, TransferStatus.COMPLETED);
        
        logger.info("Found {} stalled transfers to clean up", stalledTransfers.size());
        
        for (FileTransferRecord transfer : stalledTransfers) {
            try {
                // Update status to FAILED
                transfer.setStatus(TransferStatus.FAILED);
                transfer.setErrorMessage("Transfer stalled and was automatically cancelled");
                transfer.setUpdatedAt(LocalDateTime.now());
                fileTransferRepository.save(transfer);
                
                logger.info("Marked stalled transfer {} as FAILED", transfer.getTransferId());
                
            } catch (Exception e) {
                logger.error("Error handling stalled transfer {}", transfer.getTransferId(), e);
            }
        }
        
        logger.info("Stalled transfer cleanup task completed");
    }
}

