package com.codebridge.session.controller;

import com.codebridge.session.dto.CommandRequest;
import com.codebridge.session.dto.CommandResponse;
import com.codebridge.session.dto.RemoteFileEntry;
import com.codebridge.session.dto.SshSessionMetadata;
import com.codebridge.session.exception.AccessDeniedException; // Assuming this is in com.codebridge.session.exception
import com.codebridge.session.exception.RemoteOperationException; // Create this custom exception
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.SshSessionWrapper;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.ApplicationInstanceIdProvider;
import com.codebridge.session.service.SshSessionLifecycleManager;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/ops/ssh/{sessionToken}")
public class SshOperationController {

    private static final Logger log = LoggerFactory.getLogger(SshOperationController.class);

    private final SshSessionLifecycleManager sessionLifecycleManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationInstanceIdProvider instanceIdProvider;

    private static final int DEFAULT_COMMAND_TIMEOUT_SECONDS = 60;
    private static final int CHANNEL_CONNECT_TIMEOUT_MS = 5000; // Common for exec and sftp channel

    public SshOperationController(SshSessionLifecycleManager sessionLifecycleManager,
                                  JwtTokenProvider jwtTokenProvider,
                                  ApplicationInstanceIdProvider instanceIdProvider) {
        this.sessionLifecycleManager = sessionLifecycleManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.instanceIdProvider = instanceIdProvider;
    }

    private com.jcraft.jsch.Session getValidatedLocalSshSession(String sessionToken, String operationName) throws AccessDeniedException {
        log.debug("Validating session token for {} operation: {}", operationName, sessionToken);
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        if (keyOpt.isEmpty()) {
            throw new AccessDeniedException("Invalid or expired session token for " + operationName + ".");
        }
        SessionKey sessionKey = keyOpt.get();
        log.debug("Session key {} extracted from token for {}", sessionKey, operationName);

        Optional<SshSessionMetadata> metadataOpt = sessionLifecycleManager.getSessionMetadata(sessionKey);
        if (metadataOpt.isEmpty()) {
            sessionLifecycleManager.releaseSshSession(sessionToken); // Clean up token as metadata is gone
            throw new AccessDeniedException("Session metadata not found or expired for " + operationName + ". Please re-initialize.");
        }
        SshSessionMetadata metadata = metadataOpt.get();

        if (!instanceIdProvider.getInstanceId().equals(metadata.getHostingInstanceId())) {
            log.warn("Session for key {} (token: {}) is hosted on instance {} but operation {} requested on instance {}.",
                     sessionKey, sessionToken, metadata.getHostingInstanceId(), operationName, instanceIdProvider.getInstanceId());
            // TODO: Implement request forwarding or re-establishment logic if desired.
            // For now, if an instance gets a keepalive, it takes ownership. If an op request lands on wrong instance, it fails.
            throw new AccessDeniedException("Session not active on this service instance. Please retry. If error persists, re-initialize session.");
        }

        SshSessionWrapper wrapper = sessionLifecycleManager.getLocalSession(sessionKey)
            .orElseThrow(() -> {
                log.warn("Session for key {} (token: {}) metadata indicates local hosting but not found in local map on instance {}. Cleaning up.",
                         sessionKey, sessionToken, instanceIdProvider.getInstanceId());
                // Metadata points here, but session isn't local. This is an inconsistent state.
                // Force release to clean up Redis state.
                sessionLifecycleManager.forceReleaseSessionByKey(sessionKey, true); // true to ensure token from metadata is cleaned
                return new AccessDeniedException("Session not found locally or disconnected. Please re-initialize.");
            });

        if (!wrapper.isConnected()) {
            log.warn("Session for key {} (token: {}) found locally but is disconnected. Cleaning up.", sessionKey, sessionToken);
            sessionLifecycleManager.forceReleaseSessionByKey(sessionKey, true);
            throw new AccessDeniedException("Session disconnected. Please re-initialize.");
        }

        // Crucial: Update access times for both local wrapper and Redis metadata
        sessionLifecycleManager.updateSessionAccessTime(sessionKey);
        log.info("Session token validated for {} operation. Session key: {}. Hosting instance: {}", operationName, sessionKey, metadata.getHostingInstanceId());
        return wrapper.getJschSession();
    }


    @PostMapping("/execute-command")
    public ResponseEntity<CommandResponse> executeCommand(
            @PathVariable String sessionToken,
            @Valid @RequestBody CommandRequest commandRequest) throws RemoteOperationException {

        com.jcraft.jsch.Session jschSession = null;
        SessionKey sessionKey = null; // To hold sessionKey if token is valid
        try {
            // Validate token and get session key first to use in logging/cleanup even if jschSession fails later
            Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
            if(keyOpt.isPresent()) sessionKey = keyOpt.get();

            jschSession = getValidatedLocalSshSession(sessionToken, "execute-command");

            long executionStartTime = System.currentTimeMillis();
            String command = commandRequest.getCommand();
            int commandTimeoutSeconds = commandRequest.getTimeout() != null && commandRequest.getTimeout() > 0
                                        ? commandRequest.getTimeout()
                                        : DEFAULT_COMMAND_TIMEOUT_SECONDS;

            ChannelExec channelExec = null;
            String stdoutOutput = "";
            String stderrOutput = "";
            Integer exitStatus = null;

            try {
                channelExec = (ChannelExec) jschSession.openChannel("exec");
                channelExec.setCommand(command);
                ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
                ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
                channelExec.setOutputStream(stdoutStream);
                channelExec.setErrStream(stderrStream);
                channelExec.setInputStream(null);

                log.info("Executing command via session token (key {}): {}", sessionKey, command);
                channelExec.connect(CHANNEL_CONNECT_TIMEOUT_MS);

                long timeoutEndTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(commandTimeoutSeconds);
                while (!channelExec.isClosed() && System.currentTimeMillis() < timeoutEndTime) {
                    Thread.sleep(100);
                }

                stdoutOutput = stdoutStream.toString(StandardCharsets.UTF_8);
                stderrOutput = stderrStream.toString(StandardCharsets.UTF_8);

                if (!channelExec.isClosed()) {
                    channelExec.sendSignal("KILL");
                    throw new RemoteOperationException("Command execution timed out after " + commandTimeoutSeconds + " seconds. Partial stdout: " + stdoutOutput + " Partial stderr: " + stderrOutput);
                }
                exitStatus = channelExec.getExitStatus();
                log.info("Command executed (key {}). Exit: {}. Stdout: [{}], Stderr: [{}]", sessionKey, exitStatus, stdoutOutput.length(), stderrOutput.length());

                long durationMs = System.currentTimeMillis() - executionStartTime;
                return ResponseEntity.ok(new CommandResponse(stdoutOutput, stderrOutput, exitStatus, durationMs));

            } catch (JSchException e) {
                log.error("JSchException during command execution (key {}): {}", sessionKey, e.getMessage(), e);
                if (jschSession != null && !jschSession.isConnected()) {
                    log.warn("JSch session for key {} found disconnected. Releasing.", sessionKey);
                    sessionLifecycleManager.forceReleaseSessionByKey(sessionKey, true);
                }
                throw new RemoteOperationException("SSH command execution error: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Command execution interrupted (key {}): {}", sessionKey, e.getMessage(), e);
                throw new RemoteOperationException("Command execution interrupted: " + e.getMessage(), e);
            } catch (Exception e) {
                 log.error("Unexpected error during command execution (key {}): {}", sessionKey, e.getMessage(), e);
                throw new RemoteOperationException("Unexpected error during command execution: " + e.getMessage(), e);
            } finally {
                if (channelExec != null) {
                    channelExec.disconnect();
                }
            }
        } catch (AccessDeniedException ade) {
            log.warn("Access denied for execute-command with token {}: {}", sessionToken, ade.getMessage());
            throw ade; // Re-throw to be handled by GlobalExceptionHandler
        } catch (Exception e) {
            // Catch other unexpected errors from getValidatedLocalSshSession or setup
            log.error("Error processing execute-command for token {}: {}", sessionToken, e.getMessage(), e);
            throw new RemoteOperationException("Error processing command request: " + e.getMessage(), e);
        }
    }

    @GetMapping("/sftp/list")
    public ResponseEntity<List<RemoteFileEntry>> listFiles(
            @PathVariable String sessionToken,
            @RequestParam(required = false, defaultValue = ".") @NotBlank String remotePath) throws RemoteOperationException {
        com.jcraft.jsch.Session jschSession = null;
        SessionKey sessionKey = null;
        try {
            Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
            if(keyOpt.isPresent()) sessionKey = keyOpt.get();

            jschSession = getValidatedLocalSshSession(sessionToken, "sftp-list");
            ChannelSftp channelSftp = null;
            try {
                channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
                channelSftp.connect(CHANNEL_CONNECT_TIMEOUT_MS);
                log.info("SFTP channel connected for list (key {})", sessionKey);

                String pathToList = remotePath;
                if (pathToList == null || pathToList.isBlank() || pathToList.equals(".")) {
                    pathToList = channelSftp.pwd();
                }
                log.debug("Listing files in path '{}' (key {})", pathToList, sessionKey);

                @SuppressWarnings("unchecked")
                Vector<ChannelSftp.LsEntry> vectorOfEntries = channelSftp.ls(pathToList);
                List<RemoteFileEntry> fileEntries = new ArrayList<>();
                for (ChannelSftp.LsEntry entry : vectorOfEntries) {
                    if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) continue;
                    fileEntries.add(new RemoteFileEntry(
                        entry.getFilename(), entry.getAttrs().isDir(), entry.getAttrs().getSize(),
                        entry.getAttrs().getMtimeString(), entry.getAttrs().getPermissionsString()
                    ));
                }
                return ResponseEntity.ok(fileEntries);
            } catch (JSchException | SftpException e) {
                log.error("SFTP error during listFiles (key {}): {}", sessionKey, e.getMessage(), e);
                 if (jschSession != null && !jschSession.isConnected()) {
                    log.warn("JSch session for key {} found disconnected during SFTP list. Releasing.", sessionKey);
                    sessionLifecycleManager.forceReleaseSessionByKey(sessionKey, true);
                }
                throw new RemoteOperationException("SFTP list operation failed: " + e.getMessage(), e);
            } finally {
                if (channelSftp != null) channelSftp.disconnect();
            }
        } catch (AccessDeniedException ade) {
             log.warn("Access denied for sftp-list with token {}: {}", sessionToken, ade.getMessage());
            throw ade;
        } catch (Exception e) {
            log.error("Error processing sftp-list for token {}: {}", sessionToken, e.getMessage(), e);
            throw new RemoteOperationException("Error processing SFTP list request: " + e.getMessage(), e);
        }
    }

    @GetMapping("/sftp/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String sessionToken,
            @RequestParam @NotBlank String remotePath) throws RemoteOperationException {
        com.jcraft.jsch.Session jschSession = null;
        SessionKey sessionKey = null;
        try {
            Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
            if(keyOpt.isPresent()) sessionKey = keyOpt.get();

            jschSession = getValidatedLocalSshSession(sessionToken, "sftp-download");
            ChannelSftp channelSftp = null;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
                channelSftp.connect(CHANNEL_CONNECT_TIMEOUT_MS);
                log.info("SFTP channel connected for download (key {})", sessionKey);
                log.debug("Downloading file '{}' (key {})", remotePath, sessionKey);

                channelSftp.get(remotePath, outputStream);
                byte[] fileData = outputStream.toByteArray();
                ByteArrayResource resource = new ByteArrayResource(fileData);
                String filename = Paths.get(remotePath).getFileName().toString();
                String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                        .contentLength(fileData.length)
                        .body(resource);
            } catch (JSchException | SftpException | IOException e) {
                log.error("SFTP error during download (key {}): {}", sessionKey, e.getMessage(), e);
                 if (jschSession != null && !jschSession.isConnected()) {
                    log.warn("JSch session for key {} found disconnected during SFTP download. Releasing.", sessionKey);
                    sessionLifecycleManager.forceReleaseSessionByKey(sessionKey, true);
                }
                throw new RemoteOperationException("SFTP download failed: " + e.getMessage(), e);
            } finally {
                if (channelSftp != null) channelSftp.disconnect();
                try { outputStream.close(); } catch (IOException e) { log.warn("Error closing output stream for download (key {})", sessionKey, e); }
            }
        } catch (AccessDeniedException ade) {
            log.warn("Access denied for sftp-download with token {}: {}", sessionToken, ade.getMessage());
            throw ade;
        } catch (Exception e) {
            log.error("Error processing sftp-download for token {}: {}", sessionToken, e.getMessage(), e);
            throw new RemoteOperationException("Error processing SFTP download request: " + e.getMessage(), e);
        }
    }

    @PostMapping("/sftp/upload")
    public ResponseEntity<Void> uploadFile(
            @PathVariable String sessionToken,
            @RequestParam @NotBlank String remotePath,
            @RequestParam("file") MultipartFile file) throws RemoteOperationException {
        if (file.isEmpty()) {
            throw new RemoteOperationException("Uploaded file cannot be empty.");
        }
        com.jcraft.jsch.Session jschSession = null;
        SessionKey sessionKey = null;
        try {
            Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
            if(keyOpt.isPresent()) sessionKey = keyOpt.get();

            jschSession = getValidatedLocalSshSession(sessionToken, "sftp-upload");
            ChannelSftp channelSftp = null;
            try (InputStream inputStream = file.getInputStream()) {
                channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
                channelSftp.connect(CHANNEL_CONNECT_TIMEOUT_MS);
                log.info("SFTP channel connected for upload (key {})", sessionKey);

                String targetDir = remotePath;
                if (targetDir == null || targetDir.isBlank() || targetDir.equals(".")) {
                    targetDir = channelSftp.pwd();
                }
                // Ensure targetDir ends with a slash if it's meant to be a directory.
                // For simplicity, this example assumes remotePath is the full path including filename, or a directory.
                // More robust logic would check if remotePath is a dir, then append filename.
                // For now, using remotePath as provided by client for the put operation.
                // String finalRemotePath = Paths.get(targetDir, file.getOriginalFilename()).toString(); // Example
                // For `put`, remotePath typically means the full path of the destination file.

                log.debug("Uploading file to '{}' (key {})", remotePath, sessionKey);
                channelSftp.put(inputStream, remotePath, ChannelSftp.OVERWRITE); // Using remotePath directly
                return ResponseEntity.ok().build();
            } catch (JSchException | SftpException | IOException e) {
                log.error("SFTP error during upload (key {}): {}", sessionKey, e.getMessage(), e);
                if (jschSession != null && !jschSession.isConnected()) {
                    log.warn("JSch session for key {} found disconnected during SFTP upload. Releasing.", sessionKey);
                    sessionLifecycleManager.forceReleaseSessionByKey(sessionKey, true);
                }
                throw new RemoteOperationException("SFTP upload failed: " + e.getMessage(), e);
            } finally {
                if (channelSftp != null) channelSftp.disconnect();
            }
        } catch (AccessDeniedException ade) {
            log.warn("Access denied for sftp-upload with token {}: {}", sessionToken, ade.getMessage());
            throw ade;
        } catch (Exception e) {
            log.error("Error processing sftp-upload for token {}: {}", sessionToken, e.getMessage(), e);
            throw new RemoteOperationException("Error processing SFTP upload request: " + e.getMessage(), e);
        }
    }
}
