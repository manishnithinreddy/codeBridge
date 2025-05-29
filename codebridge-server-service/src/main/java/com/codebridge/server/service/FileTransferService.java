package com.codebridge.server.service;

import com.codebridge.server.dto.ServerResponse;
import com.codebridge.server.dto.remote.RemoteFileEntry;
import com.codebridge.server.exception.FileTransferException;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

@Service
public class FileTransferService {

    private static final Logger logger = LoggerFactory.getLogger(FileTransferService.class);
    private static final int CONNECT_TIMEOUT_MS = 20000; // 20 seconds
    private static final int CHANNEL_CONNECT_TIMEOUT_MS = 5000; // 5 seconds

    private final ServerManagementService serverManagementService;
    private final SshKeyManagementService sshKeyManagementService;
    // private final ServerActivityLogService activityLogService; // Placeholder

    public FileTransferService(ServerManagementService serverManagementService,
                               SshKeyManagementService sshKeyManagementService
                               /* ServerActivityLogService activityLogService */) {
        this.serverManagementService = serverManagementService;
        this.sshKeyManagementService = sshKeyManagementService;
        // this.activityLogService = activityLogService;
    }

    private record SftpConnection(Session session, ChannelSftp channelSftp) implements AutoCloseable {
        @Override
        public void close() {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
                logger.debug("SFTP channel disconnected.");
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
                logger.debug("Session disconnected.");
            }
        }
    }

    private SftpConnection createSftpConnection(UUID serverId, UUID userId) throws FileTransferException {
        ServerResponse serverDetails = serverManagementService.getServerById(serverId, userId);

        if (serverDetails.getAuthProvider() == null ||
            ServerAuthProvider.valueOf(serverDetails.getAuthProvider()) != ServerAuthProvider.SSH_KEY) {
            throw new FileTransferException("SFTP operation failed: Server '" + serverId + "' is not configured for SSH Key authentication.");
        }
        if (serverDetails.getSshKeyId() == null) {
            throw new FileTransferException("SFTP operation failed: SSH Key ID is missing for server '" + serverId + "'.");
        }

        SshKey sshKey = sshKeyManagementService.getDecryptedSshKey(serverDetails.getSshKeyId(), userId);
        if (sshKey.getPrivateKey() == null || sshKey.getPrivateKey().isBlank()) {
            throw new FileTransferException("SFTP operation failed: Private key is missing or empty for SSH key '" + sshKey.getId() + "'.");
        }

        String hostname = serverDetails.getHostname();
        int port = serverDetails.getPort();
        String remoteUsername = serverDetails.getRemoteUsername();

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            jsch.addIdentity(
                    "sshKey_" + sshKey.getId().toString(),
                    sshKey.getPrivateKey().getBytes(StandardCharsets.UTF_8),
                    sshKey.getPublicKey() != null ? sshKey.getPublicKey().getBytes(StandardCharsets.UTF_8) : null,
                    null // No passphrase for now
            );

            session = jsch.getSession(remoteUsername, hostname, port);
            session.setConfig("StrictHostKeyChecking", "no"); // TODO: Make configurable or use known_hosts
            session.setTimeout(CONNECT_TIMEOUT_MS);
            logger.info("Attempting SFTP session to server: {}@{}:{}", remoteUsername, hostname, port);
            session.connect();
            logger.info("SFTP session connected to server: {}", serverId);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(CHANNEL_CONNECT_TIMEOUT_MS);
            logger.info("SFTP channel connected for server: {}", serverId);
            return new SftpConnection(session, channelSftp);
        } catch (JSchException e) {
            // Disconnect if partially connected
            if (channelSftp != null && channelSftp.isConnected()) channelSftp.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
            logger.error("Failed to create SFTP connection for server {}: {}", serverId, e.getMessage(), e);
            throw new FileTransferException("Failed to establish SFTP connection: " + e.getMessage(), e);
        }
    }

    public List<RemoteFileEntry> listFiles(UUID serverId, UUID userId, String remotePath) {
        List<RemoteFileEntry> fileEntries = new ArrayList<>();
        try (SftpConnection sftpConnection = createSftpConnection(serverId, userId)) {
            ChannelSftp channelSftp = sftpConnection.channelSftp();
            
            String pathToList = (remotePath == null || remotePath.isBlank() || remotePath.equals(".")) ? channelSftp.pwd() : remotePath;
            logger.info("Listing files in path '{}' on server {}", pathToList, serverId);

            @SuppressWarnings("unchecked") // JSch library uses Vector
            Vector<ChannelSftp.LsEntry> vectorOfEntries = channelSftp.ls(pathToList);

            for (ChannelSftp.LsEntry entry : vectorOfEntries) {
                if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) {
                    continue;
                }
                RemoteFileEntry fileEntry = new RemoteFileEntry(
                        entry.getFilename(),
                        entry.getAttrs().isDir(),
                        entry.getAttrs().getSize(),
                        entry.getAttrs().getMtimeString(),
                        entry.getAttrs().getPermissionsString()
                );
                fileEntries.add(fileEntry);
            }
            // logActivity(serverId, userId, "LIST_FILES", "Path: " + pathToList + ", Count: " + fileEntries.size(), "SUCCESS");
            return fileEntries;
        } catch (JSchException | SftpException e) {
            logger.error("Error listing files on server {}: {}", serverId, e.getMessage(), e);
            // logActivity(serverId, userId, "LIST_FILES_FAILED", "Path: " + remotePath + ", Error: " + e.getMessage(), "FAILURE");
            throw new FileTransferException("Failed to list files on server " + serverId + " at path '" + remotePath + "': " + e.getMessage(), e);
        }
    }

    public byte[] downloadFile(UUID serverId, UUID userId, String remotePath) {
        try (SftpConnection sftpConnection = createSftpConnection(serverId, userId);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            ChannelSftp channelSftp = sftpConnection.channelSftp();
            logger.info("Downloading file '{}' from server {}", remotePath, serverId);
            
            SftpATTRS attrs = channelSftp.lstat(remotePath);
            if (attrs.isDir()) {
                 // logActivity(serverId, userId, "DOWNLOAD_FILE_FAILED", "Path is a directory: " + remotePath, "FAILURE");
                throw new FileTransferException("Specified path is a directory, not a file: " + remotePath);
            }
            if (attrs.getSize() > (50 * 1024 * 1024)) { // Example: 50MB limit
                // logActivity(serverId, userId, "DOWNLOAD_FILE_FAILED", "File too large: " + remotePath + ", Size: " + attrs.getSize(), "FAILURE");
                throw new FileTransferException("File is too large to download directly. Size: " + attrs.getSize() + " bytes.");
            }

            channelSftp.get(remotePath, outputStream);
            // logActivity(serverId, userId, "DOWNLOAD_FILE", "Path: " + remotePath + ", Size: " + outputStream.size() , "SUCCESS");
            return outputStream.toByteArray();
        } catch (JSchException | SftpException | IOException e) {
            logger.error("Error downloading file from server {}: {}", serverId, e.getMessage(), e);
            // logActivity(serverId, userId, "DOWNLOAD_FILE_FAILED", "Path: " + remotePath + ", Error: " + e.getMessage(), "FAILURE");
            throw new FileTransferException("Failed to download file '" + remotePath + "' from server " + serverId + ": " + e.getMessage(), e);
        }
    }

    public void uploadFile(UUID serverId, UUID userId, String remotePath, InputStream inputStream, String remoteFileName) {
        try (SftpConnection sftpConnection = createSftpConnection(serverId, userId)) {
            ChannelSftp channelSftp = sftpConnection.channelSftp();

            String targetDir = (remotePath == null || remotePath.isBlank() || remotePath.equals(".")) ? channelSftp.pwd() : remotePath;
            
            try {
                SftpATTRS attrs = channelSftp.lstat(targetDir);
                if (!attrs.isDir()) {
                    // logActivity(serverId, userId, "UPLOAD_FILE_FAILED", "Remote path not a directory: " + targetDir, "FAILURE");
                    throw new FileTransferException("Upload failed: Remote path '" + targetDir + "' is not a directory.");
                }
            } catch (SftpException e) {
                 if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    // logActivity(serverId, userId, "UPLOAD_FILE_FAILED", "Remote directory does not exist: " + targetDir, "FAILURE");
                    throw new FileTransferException("Upload failed: Remote directory '" + targetDir + "' does not exist.");
                }
                throw e; 
            }

            String finalRemoteFileName = Paths.get(remoteFileName).getFileName().toString();
            if (finalRemoteFileName.isEmpty()) {
                throw new FileTransferException("Upload failed: Remote filename cannot be empty or just a directory separator.");
            }
            String fullRemotePath = targetDir.endsWith("/") ? targetDir + finalRemoteFileName : targetDir + "/" + finalRemoteFileName;
            
            logger.info("Uploading file '{}' to '{}' on server {}", finalRemoteFileName, fullRemotePath, serverId);
            channelSftp.put(inputStream, fullRemotePath, ChannelSftp.OVERWRITE);
            // logActivity(serverId, userId, "UPLOAD_FILE", "Path: " + fullRemotePath, "SUCCESS");
        } catch (JSchException | SftpException e) {
            logger.error("Error uploading file to server {}: {}", serverId, e.getMessage(), e);
            // logActivity(serverId, userId, "UPLOAD_FILE_FAILED", "Target: " + remotePath + "/" + remoteFileName + ", Error: " + e.getMessage(), "FAILURE");
            throw new FileTransferException("Failed to upload file to server " + serverId + ": " + e.getMessage(), e);
        } finally {
             // InputStream is managed by the caller (e.g., Spring's MultipartFile's stream)
             // However, if it's not closed by the caller, it should be closed here if possible.
             // For MultipartFile, Spring typically handles closing its stream.
        }
    }
}
