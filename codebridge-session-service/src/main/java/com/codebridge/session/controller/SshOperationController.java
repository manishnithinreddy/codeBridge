package com.codebridge.session.controller;

import com.codebridge.session.config.ApplicationInstanceIdProvider;
import com.codebridge.session.dto.SshSessionMetadata;
import com.codebridge.session.dto.ops.CommandRequest;
import com.codebridge.session.dto.ops.CommandResponse;
import com.codebridge.session.dto.ops.RemoteFileEntry;
import com.codebridge.session.exception.AccessDeniedException;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.exception.ResourceNotFoundException;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.SshSessionWrapper;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.SshSessionLifecycleManager;
import com.jcraft.jsch.*;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

@RestController
@RequestMapping("/api/sessions/ops/ssh/{sessionToken}") // Base path includes sessionToken
public class SshOperationController {

    private static final Logger logger = LoggerFactory.getLogger(SshOperationController.class);
    private static final int DEFAULT_COMMAND_TIMEOUT_MS = 60000; // 60 seconds
    private static final int JSCH_CHANNEL_CONNECT_TIMEOUT_MS = 5000; // 5 seconds for channel

    private final SshSessionLifecycleManager sessionLifecycleManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationInstanceIdProvider instanceIdProvider;
    private final String applicationInstanceId;


    public SshOperationController(SshSessionLifecycleManager sessionLifecycleManager,
                                  JwtTokenProvider jwtTokenProvider,
                                  ApplicationInstanceIdProvider instanceIdProvider) {
        this.sessionLifecycleManager = sessionLifecycleManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.instanceIdProvider = instanceIdProvider;
        this.applicationInstanceId = this.instanceIdProvider.getInstanceId();
    }

    // Helper method to get and validate the local JSch session
    private Session getValidatedLocalSshSession(String sessionToken, String operationName) {
        if (!jwtTokenProvider.validateToken(sessionToken)) {
            logger.warn("{} attempt with invalid token: {}", operationName, sessionToken);
            throw new AccessDeniedException("Invalid or expired session token for " + operationName + ".");
        }

        Claims claims = jwtTokenProvider.getClaimsFromToken(sessionToken);
        SessionKey sessionKey = new SessionKey(
            UUID.fromString(claims.getSubject()), // platformUserId
            UUID.fromString(claims.get("resourceId", String.class)), // serverId
            claims.get("type", String.class) // Should be "SSH"
        );

        if (!"SSH".equals(sessionKey.sessionType())) {
             throw new AccessDeniedException("Invalid session type for " + operationName + ". Expected SSH.");
        }

        SshSessionMetadata metadata = sessionLifecycleManager.getSessionMetadata(sessionKey);
        if (metadata == null) {
            logger.warn("No session metadata found for {} operation. Key: {}, Token: {}", operationName, sessionKey, sessionToken);
            throw new AccessDeniedException("Session metadata not found. Session may have expired or been released.");
        }

        if (!applicationInstanceId.equals(metadata.hostingInstanceId())) {
            logger.warn("Session for {} is not hosted on this instance. Key: {}, Expected Host: {}, Actual Host: {}.", 
                        operationName, sessionKey, metadata.hostingInstanceId(), applicationInstanceId);
            throw new AccessDeniedException("Session is not active on this service instance. Please reconnect or try another instance.");
        }
        
        if (metadata.expiresAt() < Instant.now().toEpochMilli()) {
            logger.warn("Session for {} has expired based on metadata. Key: {}, Token: {}", operationName, sessionKey, sessionToken);
            sessionLifecycleManager.forceReleaseSshSessionByKey(sessionKey, true); // Clean up
            throw new AccessDeniedException("Session has expired.");
        }

        SshSessionWrapper wrapper = sessionLifecycleManager.getLocalSession(sessionKey);
        if (wrapper == null || !wrapper.isConnected()) {
            logger.warn("Local session for {} not found or JSch session disconnected. Key: {}. Releasing.", operationName, sessionKey);
            sessionLifecycleManager.forceReleaseSshSessionByKey(sessionKey, true); // Clean up inconsistent state
            throw new AccessDeniedException("Session not found locally or is disconnected. Please re-initialize the session.");
        }

        sessionLifecycleManager.updateSessionAccessTime(sessionKey, wrapper); // Update last accessed time for keepalive
        return wrapper.getJschSession();
    }

    @PostMapping("/execute-command")
    public ResponseEntity<CommandResponse> executeCommand(
            @PathVariable String sessionToken,
            @Valid @RequestBody CommandRequest commandRequest) {
        
        long startTime = System.currentTimeMillis();
        Session jschSession = getValidatedLocalSshSession(sessionToken, "execute-command");
        ChannelExec channel = null;
        String stdoutString = "";
        String stderrString = "";
        int exitStatus = -1;

        try {
            channel = (ChannelExec) jschSession.openChannel("exec");
            channel.setCommand(commandRequest.getCommand());

            ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
            channel.setOutputStream(stdoutStream);
            channel.setErrStream(stderrStream);

            logger.debug("Executing command via token {}: '{}'", sessionToken, commandRequest.getCommand());
            channel.connect(JSCH_CHANNEL_CONNECT_TIMEOUT_MS);

            int timeout = commandRequest.getTimeout() != null ? commandRequest.getTimeout() : DEFAULT_COMMAND_TIMEOUT_MS;
            long endTime = System.currentTimeMillis() + timeout;
            while (!channel.isClosed() && System.currentTimeMillis() < endTime) {
                Thread.sleep(100); // Poll interval
            }

            if (!channel.isClosed()) {
                logger.warn("Command timeout for session token {}: {}", sessionToken, commandRequest.getCommand());
                throw new RemoteOperationException("Command execution timed out after " + timeout + "ms.");
            }
            
            exitStatus = channel.getExitStatus();
            stdoutString = stdoutStream.toString(StandardCharsets.UTF_8);
            stderrString = stderrStream.toString(StandardCharsets.UTF_8);
            logger.info("Command via token {} executed with exit status: {}", sessionToken, exitStatus);

        } catch (JSchException e) {
            logger.error("JSchException for session token {}: {}", sessionToken, e.getMessage(), e);
            // Consider releasing the session if JSchException indicates a critical connection problem
            throw new RemoteOperationException("SSH operation failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.warn("Command execution interrupted for session token {}: {}", sessionToken, e.getMessage());
            Thread.currentThread().interrupt();
            throw new RemoteOperationException("Command execution was interrupted.", e);
        } catch (Exception e) {
            logger.error("Unexpected error during remote command for session token {}: {}", sessionToken, e.getMessage(), e);
            throw new RemoteOperationException("An unexpected error occurred during command execution: " + e.getMessage(), e);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
        long durationMs = System.currentTimeMillis() - startTime;
        return ResponseEntity.ok(new CommandResponse(stdoutString, stderrString, exitStatus, durationMs));
    }

    @GetMapping("/sftp/list")
    public ResponseEntity<List<RemoteFileEntry>> listFiles(
            @PathVariable String sessionToken,
            @RequestParam @NotBlank String remotePath) {
        Session jschSession = getValidatedLocalSshSession(sessionToken, "sftp-list");
        ChannelSftp channelSftp = null;
        List<RemoteFileEntry> fileEntries = new ArrayList<>();
        try {
            channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
            channelSftp.connect(JSCH_CHANNEL_CONNECT_TIMEOUT_MS);
            
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(remotePath);
            for (ChannelSftp.LsEntry entry : entries) {
                if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) continue;
                SftpATTRS attrs = entry.getAttrs();
                fileEntries.add(new RemoteFileEntry(
                    entry.getFilename(), attrs.isDir(), attrs.getSize(),
                    Instant.ofEpochSecond(attrs.getMTime()).toString(), attrs.getPermissionsString()
                ));
            }
            return ResponseEntity.ok(fileEntries);
        } catch (JSchException | SftpException e) {
            logger.error("SFTP list error for session token {}: {}", sessionToken, e.getMessage(), e);
            throw new RemoteOperationException("SFTP list operation failed: " + e.getMessage(), e);
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }
    }

    @GetMapping("/sftp/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String sessionToken,
            @RequestParam @NotBlank String remotePath) {
        Session jschSession = getValidatedLocalSshSession(sessionToken, "sftp-download");
        ChannelSftp channelSftp = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
            channelSftp.connect(JSCH_CHANNEL_CONNECT_TIMEOUT_MS);
            
            SftpATTRS attrs = channelSftp.lstat(remotePath); // Check if it's a directory
            if (attrs.isDir()) {
                throw new RemoteOperationException("Specified remote path is a directory, not a file for download.");
            }
            channelSftp.get(remotePath, baos);
            
            ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());
            String filename = remotePath.substring(remotePath.lastIndexOf('/') + 1);
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.contentLength()))
                .body(resource);
        } catch (JSchException | SftpException | IOException e) {
            logger.error("SFTP download error for session token {}: {}", sessionToken, e.getMessage(), e);
            throw new RemoteOperationException("SFTP download operation failed: " + e.getMessage(), e);
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
             try { baos.close(); } catch (IOException e) { /* ignore */ }
        }
    }

    @PostMapping("/sftp/upload")
    public ResponseEntity<Void> uploadFile(
            @PathVariable String sessionToken,
            @RequestParam @NotBlank String remotePath, // Target directory on remote server
            @RequestParam("file") MultipartFile file) {
        Session jschSession = getValidatedLocalSshSession(sessionToken, "sftp-upload");
        ChannelSftp channelSftp = null;
        if (file.isEmpty()) {
            throw new RemoteOperationException("Cannot upload an empty file.");
        }
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded_file";
        
        try (InputStream inputStream = file.getInputStream()) {
            channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
            channelSftp.connect(JSCH_CHANNEL_CONNECT_TIMEOUT_MS);
            
            // Ensure remotePath is a directory and ends with a slash
            String targetDir = remotePath.endsWith("/") ? remotePath : remotePath + "/";
            try {
                SftpATTRS attrs = channelSftp.lstat(targetDir);
                if (!attrs.isDir()) {
                     throw new RemoteOperationException("Target remote path for upload is not a directory: " + targetDir);
                }
            } catch (SftpException e) {
                // If directory does not exist, create it (simplified, no recursive creation here)
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    logger.info("Target directory {} does not exist, attempting to create.", targetDir);
                    channelSftp.mkdir(targetDir); // mkdir only creates one level
                } else {
                    throw e;
                }
            }
            channelSftp.cd(targetDir); // Change to target directory
            channelSftp.put(inputStream, originalFilename);
            
            return ResponseEntity.ok().build();
        } catch (JSchException | SftpException | IOException e) {
            logger.error("SFTP upload error for session token {}: {}", sessionToken, e.getMessage(), e);
            throw new RemoteOperationException("SFTP upload operation failed: " + e.getMessage(), e);
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }
    }
}
