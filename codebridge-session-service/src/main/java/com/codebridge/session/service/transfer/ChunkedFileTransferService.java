package com.codebridge.session.service.transfer;

import com.codebridge.session.dto.FileChunkRequest;
import com.codebridge.session.dto.FileChunkResponse;
import com.codebridge.session.dto.FileTransferInitRequest;
import com.codebridge.session.dto.FileTransferInitResponse;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.SshSessionWrapper;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.SshSessionLifecycleManager;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChunkedFileTransferService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkedFileTransferService.class);
    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    private final SshSessionLifecycleManager sessionLifecycleManager;
    private final JwtTokenProvider jwtTokenProvider;

    // In-memory tracking of active file transfers
    private final Map<String, FileTransferContext> activeTransfers = new ConcurrentHashMap<>();

    public ChunkedFileTransferService(SshSessionLifecycleManager sessionLifecycleManager, JwtTokenProvider jwtTokenProvider) {
        this.sessionLifecycleManager = sessionLifecycleManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public FileTransferInitResponse initializeFileTransfer(String sessionToken, FileTransferInitRequest request) {
        // Validate session token and get session key
        SessionKey sessionKey = validateSessionToken(sessionToken);
        
        // Generate a unique transfer ID
        String transferId = UUID.randomUUID().toString();
        
        // Create a new transfer context
        FileTransferContext context = new FileTransferContext(
                transferId,
                sessionKey,
                request.getRemotePath(),
                request.getDirection(),
                request.getTotalSize(),
                request.getFileName()
        );
        
        // For uploads, prepare the remote file
        if (context.getDirection() == TransferDirection.UPLOAD) {
            try {
                prepareRemoteFile(context);
            } catch (Exception e) {
                logger.error("Failed to prepare remote file for upload: {}", e.getMessage(), e);
                throw new RemoteOperationException("Failed to prepare remote file: " + e.getMessage(), e);
            }
        } else {
            // For downloads, check if the file exists and get its size
            try {
                validateRemoteFile(context);
            } catch (Exception e) {
                logger.error("Failed to validate remote file for download: {}", e.getMessage(), e);
                throw new RemoteOperationException("Failed to validate remote file: " + e.getMessage(), e);
            }
        }
        
        // Store the context
        activeTransfers.put(transferId, context);
        
        return new FileTransferInitResponse(
                transferId,
                context.getDirection(),
                context.getTotalSize(),
                context.getFileName(),
                context.getRemotePath()
        );
    }

    public FileChunkResponse transferChunk(String sessionToken, String transferId, FileChunkRequest request) {
        // Validate session token
        SessionKey sessionKey = validateSessionToken(sessionToken);
        
        // Get the transfer context
        FileTransferContext context = activeTransfers.get(transferId);
        if (context == null) {
            throw new RemoteOperationException("Transfer not found or expired: " + transferId);
        }
        
        // Verify the session key matches
        if (!context.getSessionKey().equals(sessionKey)) {
            throw new RemoteOperationException("Session key mismatch for transfer: " + transferId);
        }
        
        try {
            if (context.getDirection() == TransferDirection.UPLOAD) {
                return handleUploadChunk(context, request);
            } else {
                return handleDownloadChunk(context, request);
            }
        } catch (Exception e) {
            logger.error("Error during chunk transfer: {}", e.getMessage(), e);
            throw new RemoteOperationException("Failed to transfer chunk: " + e.getMessage(), e);
        }
    }

    private FileChunkResponse handleUploadChunk(FileTransferContext context, FileChunkRequest request) throws JSchException, IOException {
        // Get the SSH session
        SshSessionWrapper wrapper = sessionLifecycleManager.getLocalSession(context.getSessionKey()).orElse(null);
        if (wrapper == null || !wrapper.isConnected()) {
            throw new RemoteOperationException("SSH session is not connected");
        }
        
        // Update session access time
        sessionLifecycleManager.updateSessionAccessTime(context.getSessionKey(), wrapper);
        
        // Append the chunk to the remote file
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) wrapper.getJschSession().openChannel("exec");
            
            // Use dd to append to the file at the correct offset
            String command = String.format(
                    "dd of='%s' bs=1 seek=%d conv=notrunc",
                    context.getRemotePath().replace("'", "'\\''"), // Escape single quotes
                    request.getOffset()
            );
            
            channel.setCommand(command);
            
            // Connect the channel
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect();
            
            // Write the chunk data
            out.write(request.getData());
            out.flush();
            out.close();
            
            // Wait for the command to complete
            StringBuilder errorOutput = new StringBuilder();
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                while (err.available() > 0) {
                    int i = err.read(buffer, 0, BUFFER_SIZE);
                    if (i < 0) break;
                    errorOutput.append(new String(buffer, 0, i, StandardCharsets.UTF_8));
                }
                
                if (channel.isClosed()) {
                    int exitStatus = channel.getExitStatus();
                    if (exitStatus != 0) {
                        throw new RemoteOperationException("Failed to write chunk: " + errorOutput.toString());
                    }
                    break;
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RemoteOperationException("Interrupted during chunk upload");
                }
            }
            
            // Update the context with the new offset
            context.setCurrentOffset(request.getOffset() + request.getData().length);
            
            return new FileChunkResponse(
                    context.getTransferId(),
                    request.getChunkIndex(),
                    context.getCurrentOffset(),
                    context.getTotalSize(),
                    context.getCurrentOffset() >= context.getTotalSize()
            );
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private FileChunkResponse handleDownloadChunk(FileTransferContext context, FileChunkRequest request) throws JSchException, IOException {
        // Get the SSH session
        SshSessionWrapper wrapper = sessionLifecycleManager.getLocalSession(context.getSessionKey()).orElse(null);
        if (wrapper == null || !wrapper.isConnected()) {
            throw new RemoteOperationException("SSH session is not connected");
        }
        
        // Update session access time
        sessionLifecycleManager.updateSessionAccessTime(context.getSessionKey(), wrapper);
        
        // Read the chunk from the remote file
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) wrapper.getJschSession().openChannel("exec");
            
            // Use dd to read from the file at the correct offset
            long chunkSize = Math.min(request.getChunkSize(), context.getTotalSize() - request.getOffset());
            String command = String.format(
                    "dd if='%s' bs=1 skip=%d count=%d",
                    context.getRemotePath().replace("'", "'\\''"), // Escape single quotes
                    request.getOffset(),
                    chunkSize
            );
            
            channel.setCommand(command);
            
            // Connect the channel
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect();
            
            // Read the chunk data
            ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, BUFFER_SIZE);
                    if (i < 0) break;
                    dataBuffer.write(buffer, 0, i);
                }
                
                if (channel.isClosed()) {
                    int exitStatus = channel.getExitStatus();
                    if (exitStatus != 0) {
                        StringBuilder errorOutput = new StringBuilder();
                        while (err.available() > 0) {
                            int i = err.read(buffer, 0, BUFFER_SIZE);
                            if (i < 0) break;
                            errorOutput.append(new String(buffer, 0, i, StandardCharsets.UTF_8));
                        }
                        throw new RemoteOperationException("Failed to read chunk: " + errorOutput.toString());
                    }
                    break;
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RemoteOperationException("Interrupted during chunk download");
                }
            }
            
            byte[] chunkData = dataBuffer.toByteArray();
            
            // Update the context with the new offset
            context.setCurrentOffset(request.getOffset() + chunkData.length);
            
            return new FileChunkResponse(
                    context.getTransferId(),
                    request.getChunkIndex(),
                    context.getCurrentOffset(),
                    context.getTotalSize(),
                    context.getCurrentOffset() >= context.getTotalSize(),
                    chunkData
            );
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void prepareRemoteFile(FileTransferContext context) throws JSchException, IOException {
        // Get the SSH session
        SshSessionWrapper wrapper = sessionLifecycleManager.getLocalSession(context.getSessionKey()).orElse(null);
        if (wrapper == null || !wrapper.isConnected()) {
            throw new RemoteOperationException("SSH session is not connected");
        }
        
        // Create an empty file of the correct size
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) wrapper.getJschSession().openChannel("exec");
            
            // Create the directory if it doesn't exist
            String directory = context.getRemotePath().substring(0, context.getRemotePath().lastIndexOf('/'));
            String command = String.format(
                    "mkdir -p '%s' && truncate -s %d '%s'",
                    directory.replace("'", "'\\''"), // Escape single quotes
                    context.getTotalSize(),
                    context.getRemotePath().replace("'", "'\\''") // Escape single quotes
            );
            
            channel.setCommand(command);
            
            // Connect the channel
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect();
            
            // Wait for the command to complete
            StringBuilder errorOutput = new StringBuilder();
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                while (err.available() > 0) {
                    int i = err.read(buffer, 0, BUFFER_SIZE);
                    if (i < 0) break;
                    errorOutput.append(new String(buffer, 0, i, StandardCharsets.UTF_8));
                }
                
                if (channel.isClosed()) {
                    int exitStatus = channel.getExitStatus();
                    if (exitStatus != 0) {
                        throw new RemoteOperationException("Failed to prepare remote file: " + errorOutput.toString());
                    }
                    break;
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RemoteOperationException("Interrupted during file preparation");
                }
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void validateRemoteFile(FileTransferContext context) throws JSchException, IOException {
        // Get the SSH session
        SshSessionWrapper wrapper = sessionLifecycleManager.getLocalSession(context.getSessionKey()).orElse(null);
        if (wrapper == null || !wrapper.isConnected()) {
            throw new RemoteOperationException("SSH session is not connected");
        }
        
        // Check if the file exists and get its size
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) wrapper.getJschSession().openChannel("exec");
            
            String command = String.format(
                    "if [ -f '%s' ]; then stat -c %%s '%s'; else echo 'File not found' >&2; exit 1; fi",
                    context.getRemotePath().replace("'", "'\\''"), // Escape single quotes
                    context.getRemotePath().replace("'", "'\\''") // Escape single quotes
            );
            
            channel.setCommand(command);
            
            // Connect the channel
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect();
            
            // Read the output (file size)
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, BUFFER_SIZE);
                    if (i < 0) break;
                    output.append(new String(buffer, 0, i, StandardCharsets.UTF_8));
                }
                
                while (err.available() > 0) {
                    int i = err.read(buffer, 0, BUFFER_SIZE);
                    if (i < 0) break;
                    errorOutput.append(new String(buffer, 0, i, StandardCharsets.UTF_8));
                }
                
                if (channel.isClosed()) {
                    int exitStatus = channel.getExitStatus();
                    if (exitStatus != 0) {
                        throw new RemoteOperationException("Failed to validate remote file: " + errorOutput.toString());
                    }
                    break;
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RemoteOperationException("Interrupted during file validation");
                }
            }
            
            // Parse the file size
            String sizeStr = output.toString().trim();
            try {
                long fileSize = Long.parseLong(sizeStr);
                context.setTotalSize(fileSize);
            } catch (NumberFormatException e) {
                throw new RemoteOperationException("Failed to parse file size: " + sizeStr);
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private SessionKey validateSessionToken(String sessionToken) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(sessionToken);
        if (claims == null) {
            throw new RemoteOperationException("Invalid session token");
        }
        
        String sessionType = claims.get("type", String.class);
        if (!"SSH".equals(sessionType)) {
            throw new RemoteOperationException("Invalid session type for file transfer");
        }
        
        // Get the session key from the token
        String platformUserIdStr = claims.getSubject();
        String resourceIdStr = claims.get("resourceId", String.class);
        
        if (platformUserIdStr == null || resourceIdStr == null) {
            throw new RemoteOperationException("Invalid token claims");
        }
        
        try {
            UUID platformUserId = UUID.fromString(platformUserIdStr);
            UUID resourceId = UUID.fromString(resourceIdStr);
            SessionKey sessionKey = new SessionKey(platformUserId, resourceId, sessionType);
            
            // Verify the session exists
            if (sessionLifecycleManager.getLocalSession(sessionKey).isEmpty()) {
                throw new RemoteOperationException("Session not found or expired");
            }
            
            return sessionKey;
        } catch (IllegalArgumentException e) {
            throw new RemoteOperationException("Invalid UUID in token claims");
        }
    }

    // Inner classes for file transfer context
    public static class FileTransferContext {
        private final String transferId;
        private final SessionKey sessionKey;
        private final String remotePath;
        private final TransferDirection direction;
        private long totalSize;
        private final String fileName;
        private long currentOffset = 0;
        
        public FileTransferContext(String transferId, SessionKey sessionKey, String remotePath, 
                                  TransferDirection direction, long totalSize, String fileName) {
            this.transferId = transferId;
            this.sessionKey = sessionKey;
            this.remotePath = remotePath;
            this.direction = direction;
            this.totalSize = totalSize;
            this.fileName = fileName;
        }
        
        // Getters and setters
        public String getTransferId() {
            return transferId;
        }
        
        public SessionKey getSessionKey() {
            return sessionKey;
        }
        
        public String getRemotePath() {
            return remotePath;
        }
        
        public TransferDirection getDirection() {
            return direction;
        }
        
        public long getTotalSize() {
            return totalSize;
        }
        
        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public long getCurrentOffset() {
            return currentOffset;
        }
        
        public void setCurrentOffset(long currentOffset) {
            this.currentOffset = currentOffset;
        }
    }
    
    public enum TransferDirection {
        UPLOAD,
        DOWNLOAD
    }
}

