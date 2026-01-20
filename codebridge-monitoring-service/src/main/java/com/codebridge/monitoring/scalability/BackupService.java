package com.codebridge.monitoring.scalability.resilience;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing database backups.
 */
public interface BackupService {

    /**
     * Performs a full backup of the database.
     *
     * @return the backup ID
     */
    String performFullBackup();

    /**
     * Performs an incremental backup of the database.
     *
     * @param baseBackupId the ID of the base backup
     * @return the backup ID
     */
    String performIncrementalBackup(String baseBackupId);

    /**
     * Restores the database from a backup.
     *
     * @param backupId the ID of the backup to restore
     * @return true if the restore was successful, false otherwise
     */
    boolean restoreFromBackup(String backupId);

    /**
     * Verifies a backup.
     *
     * @param backupId the ID of the backup to verify
     * @return true if the backup is valid, false otherwise
     */
    boolean verifyBackup(String backupId);

    /**
     * Gets a list of all backups.
     *
     * @return a list of backup IDs
     */
    List<String> getBackups();

    /**
     * Gets information about a backup.
     *
     * @param backupId the ID of the backup
     * @return a map of backup information
     */
    BackupInfo getBackupInfo(String backupId);

    /**
     * Deletes a backup.
     *
     * @param backupId the ID of the backup to delete
     * @return true if the backup was deleted, false otherwise
     */
    boolean deleteBackup(String backupId);

    /**
     * Cleans up old backups based on the retention policy.
     *
     * @return the number of backups deleted
     */
    int cleanupOldBackups();

    /**
     * Represents information about a backup.
     */
    class BackupInfo {
        private String backupId;
        private String type; // FULL or INCREMENTAL
        private LocalDateTime timestamp;
        private long sizeBytes;
        private String status; // COMPLETED, FAILED, VERIFYING, VERIFIED, CORRUPTED
        private String baseBackupId; // For incremental backups
        
        // Getters and setters
        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getBaseBackupId() { return baseBackupId; }
        public void setBaseBackupId(String baseBackupId) { this.baseBackupId = baseBackupId; }
    }
}

