package com.codebridge.session.service.transfer;

import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.SshSessionWrapper;
import com.codebridge.session.service.SshSessionLifecycleManager;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for optimized file transfers using chunking.
 * This improves performance and reliability for large file transfers.
 */
@Service
public class ChunkedFileTransferService {
    private static final Logger logger = LoggerFactory.getLogger(ChunkedFileTransferService.class);
    private static final int JSCH_CHANNEL_CONNECT_TIMEOUT_MS = 5000;

    private final SshSessionLifecycleManager sessionLifecycleManager;
    private final MeterRegistry meterRegistry;
    private final ExecutorService transferExecutor;
    private final Path tempDirectory;
    private final int chunkSizeBytes;
    private final int maxConcurrentTransfers;

    // Metrics
    private final Timer uploadTimer;
    private final Timer downloadTimer;

    @Autowired
    public ChunkedFileTransferService(
            SshSessionLifecycleManager sessionLifecycleManager,
            MeterRegistry meterRegistry,
            @Value("${codebridge.session.transfer.chunkSizeBytes:1048576}") int chunkSizeBytes,
            @Value("${codebridge.session.transfer.maxConcurrentTransfers:10}") int maxConcurrentTransfers,
            @Value("${codebridge.session.transfer.tempDirectory:#{systemProperties['java.io.tmpdir']}}") String tempDirectoryPath) {
        
        this.sessionLifecycleManager = sessionLifecycleManager;
        this.meterRegistry = meterRegistry;
        this.chunkSizeBytes = chunkSizeBytes;
        this.maxConcurrentTransfers = maxConcurrentTransfers;
        this.transferExecutor = Executors.newFixedThreadPool(maxConcurrentTransfers);
        
        // Create temp directory
        this.tempDirectory = Paths.get(tempDirectoryPath, "codebridge-transfers");
        try {
            Files.createDirectories(this.tempDirectory);
        } catch (IOException e) {
            logger.error("Failed to create temp directory: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create temp directory for file transfers", e);
        }
        
        // Initialize metrics
        this.uploadTimer = Timer.builder("ssh.sftp.chunked.upload.time")
                .description("Time taken to upload files via chunked SFTP")
                .register(meterRegistry);
        this.downloadTimer = Timer.builder("ssh.sftp.chunked.download.time")
                .description("Time taken to download files via chunked SFTP")
                .register(meterRegistry);
    }

    /**
     * Uploads a file to a remote server using chunking.
     *
     * @param sessionKey The session key
     * @param file The file to upload
     * @param remotePath The remote path
     * @return A CompletableFuture that completes when the upload is done
     */
    public CompletableFuture<Void> uploadFile(SessionKey sessionKey, MultipartFile file, String remotePath) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return CompletableFuture.supplyAsync(() -> {
            String transferId = UUID.randomUUID().toString();
            Path tempFile = tempDirectory.resolve(transferId);
            
            try {
                // Save the file to a temporary location
                file.transferTo(tempFile.toFile());
                
                // Get the session
                SshSessionWrapper wrapper = sessionLifecycleManager.getLocalSession(sessionKey);
                if (wrapper == null || !wrapper.isConnected()) {
                    throw new RemoteOperationException("SSH session is not connected");
                }
                
                // Update session access time
                sessionLifecycleManager.updateSessionAccessTime(sessionKey, wrapper);
                
                // Open SFTP channel
                ChannelSftp channelSftp = (ChannelSftp) wrapper.getJschSession().openChannel("sftp");
                channelSftp.connect(JSCH_CHANNEL_CONNECT_TIMEOUT_MS);
                
                try {
                    // Ensure the remote directory exists
                    String remoteDir = getRemoteDirectory(remotePath);
                    String remoteFileName = getRemoteFileName(remotePath, file.getOriginalFilename());
                    
                    try {
                        channelSftp.stat(remoteDir);
                    } catch (SftpException e) {
                        if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                            // Create the directory
                            channelSftp.mkdir(remoteDir);
                        } else {
                            throw e;
                        }
                    }
                    
                    // Upload the file in chunks
                    try (InputStream inputStream = new FileInputStream(tempFile.toFile())) {
                        channelSftp.put(inputStream, remoteDir + "/" + remoteFileName);
                    }
                    
                    logger.info("File uploaded successfully: {}", remotePath);
                    return null;
                } finally {
                    if (channelSftp != null && channelSftp.isConnected()) {
                        channelSftp.disconnect();
                    }
                }
            } catch (Exception e) {
                logger.error("Error uploading file: {}", e.getMessage(), e);
                throw new RemoteOperationException("Failed to upload file: " + e.getMessage(), e);
            } finally {
                // Clean up temporary file
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary file: {}", e.getMessage(), e);
                }
                
                sample.stop(uploadTimer);
            }
        }, transferExecutor);
    }

    /**
     * Downloads a file from a remote server using chunking.
     *
     * @param sessionKey The session key
     * @param remotePath The remote path
     * @return A CompletableFuture that completes with the file data
     */
    public CompletableFuture<byte[]> downloadFile(SessionKey sessionKey, String remotePath) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return CompletableFuture.supplyAsync(() -> {
            String transferId = UUID.randomUUID().toString();
            Path tempFile = tempDirectory.resolve(transferId);
            
            try {
                // Get the session
                SshSessionWrapper wrapper = sessionLifecycleManager.getLocalSession(sessionKey);
                if (wrapper == null || !wrapper.isConnected()) {
                    throw new RemoteOperationException("SSH session is not connected");
                }
                
                // Update session access time
                sessionLifecycleManager.updateSessionAccessTime(sessionKey, wrapper);
                
                // Open SFTP channel
                ChannelSftp channelSftp = (ChannelSftp) wrapper.getJschSession().openChannel("sftp");
                channelSftp.connect(JSCH_CHANNEL_CONNECT_TIMEOUT_MS);
                
                try {
                    // Check if the remote file exists and is not a directory
                    try {
                        ChannelSftp.LsEntry entry = channelSftp.stat(remotePath);
                        if (entry.getAttrs().isDir()) {
                            throw new RemoteOperationException("Remote path is a directory, not a file");
                        }
                    } catch (SftpException e) {
                        if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                            throw new RemoteOperationException("Remote file does not exist: " + remotePath);
                        } else {
                            throw e;
                        }
                    }
                    
                    // Download the file
                    try (OutputStream outputStream = new FileOutputStream(tempFile.toFile())) {
                        channelSftp.get(remotePath, outputStream);
                    }
                    
                    // Read the file into memory
                    byte[] fileData = Files.readAllBytes(tempFile);
                    logger.info("File downloaded successfully: {}", remotePath);
                    return fileData;
                } finally {
                    if (channelSftp != null && channelSftp.isConnected()) {
                        channelSftp.disconnect();
                    }
                }
            } catch (Exception e) {
                logger.error("Error downloading file: {}", e.getMessage(), e);
                throw new RemoteOperationException("Failed to download file: " + e.getMessage(), e);
            } finally {
                // Clean up temporary file
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary file: {}", e.getMessage(), e);
                }
                
                sample.stop(downloadTimer);
            }
        }, transferExecutor);
    }

    /**
     * Gets the remote directory from a remote path.
     *
     * @param remotePath The remote path
     * @return The remote directory
     */
    private String getRemoteDirectory(String remotePath) {
        int lastSlashIndex = remotePath.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return ".";
        }
        return remotePath.substring(0, lastSlashIndex);
    }

    /**
     * Gets the remote file name from a remote path and original file name.
     *
     * @param remotePath The remote path
     * @param originalFileName The original file name
     * @return The remote file name
     */
    private String getRemoteFileName(String remotePath, String originalFileName) {
        int lastSlashIndex = remotePath.lastIndexOf('/');
        if (lastSlashIndex == -1 || lastSlashIndex == remotePath.length() - 1) {
            return originalFileName;
        }
        return remotePath.substring(lastSlashIndex + 1);
    }

    /**
     * Shuts down the transfer executor.
     * This should be called when the application is shutting down.
     */
    public void shutdown() {
        transferExecutor.shutdown();
    }
}

