package com.codebridge.server.service;

import com.codebridge.server.dto.ServerResponse;
import com.codebridge.server.dto.remote.CommandRequest;
import com.codebridge.server.dto.remote.CommandResponse;
import com.codebridge.server.exception.RemoteCommandException;
import com.codebridge.server.exception.ResourceNotFoundException;
// Server model might not be directly used if ServerResponse from ServerManagementService has all needed info
// import com.codebridge.server.model.Server; 
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RemoteExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(RemoteExecutionService.class);
    private static final int DEFAULT_COMMAND_TIMEOUT_SECONDS = 60;
    private static final int CONNECT_TIMEOUT_MS = 20000; // 20 seconds
    private static final int CHANNEL_CONNECT_TIMEOUT_MS = 5000; // 5 seconds for channel connection

    private final ServerManagementService serverManagementService;
    private final SshKeyManagementService sshKeyManagementService;
    // private final ServerActivityLogService activityLogService; // Placeholder for future integration

    public RemoteExecutionService(ServerManagementService serverManagementService,
                                  SshKeyManagementService sshKeyManagementService
                                  /* ServerActivityLogService activityLogService */) {
        this.serverManagementService = serverManagementService;
        this.sshKeyManagementService = sshKeyManagementService;
        // this.activityLogService = activityLogService;
    }

    public CommandResponse executeCommand(UUID serverId, UUID userId, CommandRequest commandRequest) {
        long executionStartTime = System.currentTimeMillis();
        String command = commandRequest.getCommand();
        int commandTimeoutSeconds = commandRequest.getTimeout() != null && commandRequest.getTimeout() > 0 
                                    ? commandRequest.getTimeout() 
                                    : DEFAULT_COMMAND_TIMEOUT_SECONDS;

        ServerResponse serverDetails = serverManagementService.getServerById(serverId, userId);

        if (serverDetails.getAuthProvider() == null || 
            ServerAuthProvider.valueOf(serverDetails.getAuthProvider()) != ServerAuthProvider.SSH_KEY) {
            // logActivity(serverId, userId, command, "Unsupported auth provider: " + serverDetails.getAuthProvider(), "FAILURE");
            throw new RemoteCommandException("Command execution failed: Server is not configured for SSH Key authentication.");
        }
        if (serverDetails.getSshKeyId() == null) {
            // logActivity(serverId, userId, command, "SSH Key ID is missing for server.", "FAILURE");
            throw new RemoteCommandException("Command execution failed: SSH Key ID is missing for server " + serverId);
        }

        SshKey sshKey = sshKeyManagementService.getDecryptedSshKey(serverDetails.getSshKeyId(), userId);
        if (sshKey.getPrivateKey() == null || sshKey.getPrivateKey().isBlank()) {
            // logActivity(serverId, userId, command, "Decrypted private key is empty.", "FAILURE");
            throw new RemoteCommandException("Command execution failed: Private key is missing or empty for SSH key " + sshKey.getId());
        }

        String hostname = serverDetails.getHostname();
        int port = serverDetails.getPort();
        String remoteUsername = serverDetails.getRemoteUsername();

        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channelExec = null;

        String stdoutOutput = "";
        String stderrOutput = "";
        Integer exitStatus = null;

        try {
            jsch.addIdentity(
                    "sshKey_" + sshKey.getId().toString(), // Unique name for the key
                    sshKey.getPrivateKey().getBytes(StandardCharsets.UTF_8),
                    sshKey.getPublicKey() != null ? sshKey.getPublicKey().getBytes(StandardCharsets.UTF_8) : null,
                    null // Passphrase bytes - assuming no passphrase for now
            );

            session = jsch.getSession(remoteUsername, hostname, port);
            session.setConfig("StrictHostKeyChecking", "no"); // TODO: Make configurable or use known_hosts
            session.setTimeout(CONNECT_TIMEOUT_MS);
            logger.info("Attempting to connect to server: {}@{}:{} for command execution.", remoteUsername, hostname, port);
            session.connect();
            logger.info("Successfully connected to server: {}.", serverId);

            channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(command);

            ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
            channelExec.setOutputStream(stdoutStream);
            channelExec.setErrStream(stderrStream);
            channelExec.setInputStream(null); // No input stream for simple commands

            logger.info("Executing command on server {}: {}", serverId, command);
            channelExec.connect(CHANNEL_CONNECT_TIMEOUT_MS); 

            long timeoutEndTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(commandTimeoutSeconds);
            while (!channelExec.isClosed() && System.currentTimeMillis() < timeoutEndTime) {
                Thread.sleep(100); // Polling interval
            }
            
            // Read streams after channel is closed or timed out
            stdoutOutput = stdoutStream.toString(StandardCharsets.UTF_8);
            stderrOutput = stderrStream.toString(StandardCharsets.UTF_8);

            if (!channelExec.isClosed()) {
                channelExec.sendSignal("KILL"); // Attempt to kill the process if timed out
                // logActivity(serverId, userId, command, "Command execution timed out.", "FAILURE", stdoutOutput, stderrOutput, null);
                throw new RemoteCommandException("Command execution timed out after " + commandTimeoutSeconds + " seconds. Partial stdout: " + stdoutOutput + " Partial stderr: " + stderrOutput);
            }
            
            exitStatus = channelExec.getExitStatus();
            logger.info("Command executed on server {}. Exit status: {}. Stdout: [{}], Stderr: [{}]", serverId, exitStatus, stdoutOutput, stderrOutput);
            // logActivity(serverId, userId, command, "Command executed.", "SUCCESS", stdoutOutput, stderrOutput, exitStatus);

        } catch (JSchException e) {
            logger.error("JSchException during command execution on server {}: {}", serverId, e.getMessage(), e);
            // logActivity(serverId, userId, command, "JSchException: " + e.getMessage(), "FAILURE", stdoutOutput, stderrOutput, null);
            throw new RemoteCommandException("SSH connection or command execution error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Command execution interrupted on server {}: {}", serverId, e.getMessage(), e);
            // logActivity(serverId, userId, command, "Command execution interrupted.", "FAILURE", stdoutOutput, stderrOutput, null);
            throw new RemoteCommandException("Command execution interrupted: " + e.getMessage(), e);
        } catch (Exception e) { // Catch other potential exceptions (e.g., IOException from streams)
            logger.error("Unexpected exception during command execution on server {}: {}", serverId, e.getMessage(), e);
            // logActivity(serverId, userId, command, "Unexpected Exception: " + e.getMessage(), "FAILURE", stdoutOutput, stderrOutput, null);
            throw new RemoteCommandException("Error during command execution: " + e.getMessage(), e);
        } finally {
            if (channelExec != null) {
                channelExec.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
            logger.info("Disconnected from server: {} after command execution.", serverId);
        }

        long durationMs = System.currentTimeMillis() - executionStartTime;
        return new CommandResponse(stdoutOutput, stderrOutput, exitStatus, durationMs);
    }

    // Placeholder for activity logging method - to be implemented when ServerActivityLogService is available
    /*
    private void logActivity(UUID serverId, UUID userId, String command, String details, String status) {
        logActivity(serverId, userId, command, details, status, null, null, null);
    }

    private void logActivity(UUID serverId, UUID userId, String command, String details, String status, String stdout, String stderr, Integer exitCode) {
        // if (activityLogService != null) {
        // ServerActivityLogRequest logRequest = new ServerActivityLogRequest();
        // logRequest.setServerId(serverId);
        // logRequest.setPlatformUserId(userId);
        // logRequest.setAction("EXECUTE_COMMAND");
        // String logDetails = "Command: " + command + ". Details: " + details;
        // if (stdout != null) logDetails += ". Stdout: " + stdout.substring(0, Math.min(stdout.length(), 200)); // Truncate for logging
        // if (stderr != null) logDetails += ". Stderr: " + stderr.substring(0, Math.min(stderr.length(), 200));
        // if (exitCode != null) logDetails += ". ExitCode: " + exitCode;
        // logRequest.setDetails(logDetails);
        // logRequest.setStatus(status);
        // if("FAILURE".equals(status) && details != null && !details.contains("Command execution timed out")) {
        //     logRequest.setErrorMessage(details);
        // }
        // activityLogService.createLogEntry(logRequest);
        // }
    }
    */
}
