package com.codebridge.session.controller;

import com.codebridge.session.dto.CommandResponse;
import com.codebridge.session.dto.SshSessionMetadata;
import com.codebridge.session.exception.AccessDeniedException;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.SshSessionWrapper;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.service.SshSessionLifecycleManager;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/session/ssh")
@Validated
public class SshOperationController {

    private static final Logger logger = LoggerFactory.getLogger(SshOperationController.class);
    private static final int DEFAULT_COMMAND_TIMEOUT_MS = 30000; // 30 seconds default timeout

    private final SshSessionLifecycleManager sessionLifecycleManager;
    private final JwtTokenProvider jwtTokenProvider;

    public SshOperationController(SshSessionLifecycleManager sessionLifecycleManager, JwtTokenProvider jwtTokenProvider) {
        this.sessionLifecycleManager = sessionLifecycleManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/command")
    public ResponseEntity<CommandResponse> executeCommand(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid CommandRequest commandRequest) {

        String sessionToken = extractToken(authHeader);
        SessionKey sessionKey = validateSessionAndGetKey(sessionToken, "command execution");

        // Get the session wrapper and execute the command
        SshSessionWrapper wrapper = getValidatedSessionWrapper(sessionKey, "command execution");
        
        try {
            // Execute command with timeout from request or default
            int timeoutMs = commandRequest.timeoutMs() != null && commandRequest.timeoutMs() > 0 
                    ? commandRequest.timeoutMs() : DEFAULT_COMMAND_TIMEOUT_MS;
            
            CommandResult result = executeRemoteCommand(wrapper, commandRequest.command(), timeoutMs);
            sessionLifecycleManager.updateSessionAccessTime(sessionKey, wrapper);
            
            // Return combined output with exit code
            return ResponseEntity.ok(new CommandResponse(result.output(), result.exitCode()));
        } catch (JSchException e) {
            logger.error("JSch error executing SSH command: {}", e.getMessage(), e);
            throw new RemoteOperationException("SSH connection error: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("I/O error executing SSH command: {}", e.getMessage(), e);
            throw new RemoteOperationException("I/O error during command execution: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error("Command execution interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt(); // Restore interrupted state
            throw new RemoteOperationException("Command execution was interrupted", e);
        } catch (Exception e) {
            logger.error("Unexpected error executing SSH command: {}", e.getMessage(), e);
            throw new RemoteOperationException("Failed to execute SSH command: " + e.getMessage(), e);
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Invalid or missing Authorization header");
        }
        return authHeader.substring(7);
    }

    private SessionKey validateSessionAndGetKey(String sessionToken, String operationName) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(sessionToken);
        if (claims == null) {
            logger.warn("Invalid token for {} operation: {}", operationName, sessionToken);
            throw new AccessDeniedException("Invalid session token");
        }

        String platformUserIdStr = claims.getSubject();
        String resourceIdStr = claims.get("resourceId", String.class);
        String sessionType = claims.get("type", String.class);

        if (platformUserIdStr == null || resourceIdStr == null || sessionType == null) {
            logger.warn("Malformed token claims for {} operation. Token: {}", operationName, sessionToken);
            throw new AccessDeniedException("Invalid token claims");
        }

        if (!sessionType.equals("SSH")) {
            logger.warn("Invalid session type for {} operation: {}", operationName, sessionType);
            throw new AccessDeniedException("Invalid session type for SSH operation");
        }

        UUID platformUserId;
        UUID resourceId;
        try {
            platformUserId = UUID.fromString(platformUserIdStr);
            resourceId = UUID.fromString(resourceIdStr);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID in token claims for {} operation. Token: {}", operationName, sessionToken);
            throw new AccessDeniedException("Invalid UUID in token claims");
        }

        SessionKey sessionKey = new SessionKey(platformUserId, resourceId, sessionType);
        
        // Validate session metadata exists
        SshSessionMetadata metadata = sessionLifecycleManager.getSessionMetadata(sessionKey).orElse(null);
        if (metadata == null) {
            logger.warn("No session metadata found for {} operation. Key: {}, Token: {}", operationName, sessionKey, sessionToken);
            throw new AccessDeniedException("Session metadata not found. Session may have expired or been released.");
        }

        return sessionKey;
    }

    private SshSessionWrapper getValidatedSessionWrapper(SessionKey sessionKey, String operationName) {
        SshSessionWrapper wrapper = sessionLifecycleManager.getLocalSession(sessionKey).orElse(null);
        if (wrapper == null || !wrapper.isConnected()) {
            logger.warn("Local session for {} not found or JSch session disconnected. Key: {}. Releasing.", operationName, sessionKey);
            sessionLifecycleManager.forceReleaseSshSessionByKey(sessionKey, true); // Clean up inconsistent state
            throw new AccessDeniedException("Session not found locally or is disconnected. Please re-initialize the session.");
        }
        return wrapper;
    }

    private CommandResult executeRemoteCommand(SshSessionWrapper wrapper, String command, int timeoutMs) 
            throws JSchException, IOException, InterruptedException {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) wrapper.getJschSession().openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.setOutputStream(outputStream);
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            channel.setErrStream(errorStream);

            InputStream in = channel.getInputStream();
            channel.connect();

            // Read the command output with timeout
            long startTime = System.currentTimeMillis();
            long endTime = startTime + timeoutMs;
            byte[] tmp = new byte[4096]; // Increased buffer size for better performance
            boolean timedOut = false;
            
            while (!timedOut) {
                // Check for timeout
                if (System.currentTimeMillis() > endTime) {
                    timedOut = true;
                    break;
                }
                
                // Read available data
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, tmp.length);
                    if (i < 0) break;
                    outputStream.write(tmp, 0, i);
                }
                
                // Check if channel is closed
                if (channel.isClosed()) {
                    // Read any remaining data
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, tmp.length);
                        if (i < 0) break;
                        outputStream.write(tmp, 0, i);
                    }
                    break;
                }
                
                // Sleep briefly to avoid CPU spinning
                TimeUnit.MILLISECONDS.sleep(100);
            }

            // Get command output and exit code
            String output = outputStream.toString(StandardCharsets.UTF_8);
            String error = errorStream.toString(StandardCharsets.UTF_8);
            int exitCode = timedOut ? -1 : channel.getExitStatus();

            // Combine output and error streams
            if (!error.isEmpty()) {
                logger.debug("SSH command produced error output: {}", error);
                output = output + "\n" + error;
            }
            
            if (timedOut) {
                logger.warn("SSH command timed out after {} ms: {}", timeoutMs, command);
                output = output + "\n*** Command execution timed out after " + timeoutMs + " ms ***";
            }

            return new CommandResult(output, exitCode);
        } finally {
            if (channel != null) {
                try {
                    if (channel.isConnected()) {
                        channel.disconnect();
                    }
                } catch (Exception e) {
                    logger.warn("Error disconnecting channel: {}", e.getMessage());
                }
            }
        }
    }

    public record CommandRequest(
            @NotBlank(message = "Command cannot be blank") String command,
            Integer timeoutMs) {}

    private record CommandResult(String output, int exitCode) {}
}
