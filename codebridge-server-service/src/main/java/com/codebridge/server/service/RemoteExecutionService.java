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
import com.codebridge.server.model.Server;
import com.codebridge.server.sessions.SessionKey;
// import com.codebridge.server.model.Server; // Not directly used in this refactored version
// import com.codebridge.server.model.SshKey; // Not directly used
// import com.codebridge.server.model.enums.ServerAuthProvider; // Not directly used
// import com.jcraft.jsch.ChannelExec; // JSch logic moves to SessionService
// import com.jcraft.jsch.JSchException; // JSch logic moves to SessionService
import com.codebridge.server.sessions.SessionKey;
// import com.codebridge.server.sessions.SessionManager; // Removed
// import com.codebridge.server.sessions.SshSessionWrapper; // Removed
import com.codebridge.server.exception.AccessDeniedException;
import com.codebridge.server.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;


import java.util.Optional;
import java.util.UUID;
// import java.util.concurrent.TimeUnit; // No longer managing timeouts directly
// import java.io.ByteArrayOutputStream; // No longer managing streams directly
// import java.nio.charset.StandardCharsets; // No longer managing streams directly
// import java.util.function.Supplier; // No longer creating sessions here

@Service
public class RemoteExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RemoteExecutionService.class);
    // DEFAULT_COMMAND_TIMEOUT_SECONDS, CONNECT_TIMEOUT_MS, CHANNEL_CONNECT_TIMEOUT_MS removed as JSch logic is gone

    private final ServerAccessControlService serverAccessControlService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final ServerActivityLogService activityLogService; // For logging

    @Value("${codebridge.service-urls.session-service}")
    private String sessionServiceBaseUrl;

    @Autowired
    public RemoteExecutionService(ServerAccessControlService serverAccessControlService,
                                  JwtTokenProvider jwtTokenProvider,
                                  RestTemplate restTemplate,
                                  ServerActivityLogService activityLogService) {
        this.serverAccessControlService = serverAccessControlService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.restTemplate = restTemplate;
        this.activityLogService = activityLogService;
    }

    public CommandResponse executeCommand(UUID serverId, String sessionToken, CommandRequest commandRequest) {
        // 1. Validate token and extract SessionKey
        Optional<SessionKey> keyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(sessionToken);
        if (keyOpt.isEmpty()) {
            logActivity(serverId, null, commandRequest.getCommand(), "Invalid or expired session token.", "FAILURE", null, null, null);
            throw new AccessDeniedException("Invalid or expired session token.");
        }
        SessionKey sessionKey = keyOpt.get();
        UUID platformUserId = sessionKey.userId();

        // 2. Verify token matches requested server
        if (!sessionKey.resourceId().equals(serverId) || !"SSH".equals(sessionKey.resourceType())) {
            logActivity(serverId, platformUserId, commandRequest.getCommand(), "Session token mismatch.", "FAILURE", null, null, null);
            throw new AccessDeniedException("Session token mismatch: Token is not valid for the requested server or resource type.");
        }

        // 3. Perform business authorization check
        try {
            serverAccessControlService.checkUserAccessAndGetConnectionDetails(platformUserId, serverId);
            // We don't need the details themselves here, just the confirmation of access.
            // SessionService will use the details it received during session init.
            log.debug("User {} authorized for server {} via ServerAccessControlService", platformUserId, serverId);
        } catch (AccessDeniedException | ResourceNotFoundException e) {
            logActivity(serverId, platformUserId, commandRequest.getCommand(), "Authorization failed: " + e.getMessage(), "FAILURE", null, null, null);
            throw e; // Re-throw to be handled by GlobalExceptionHandler
        }

        // 4. Make HTTP POST call to SessionService
        String url = sessionServiceBaseUrl + "/ops/ssh/" + sessionToken + "/execute-command";
        HttpHeaders headers = new HttpHeaders();
        // Potentially add an inter-service auth token if SessionService requires it from ServerService
        HttpEntity<CommandRequest> requestEntity = new HttpEntity<>(commandRequest, headers);

        try {
            log.info("Delegating command execution for server {} (token: {}) to SessionService at {}", serverId, sessionToken, url);
            ResponseEntity<CommandResponse> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                CommandResponse.class
            );

            CommandResponse response = responseEntity.getBody();
            if (response != null) {
                 logActivity(serverId, platformUserId, commandRequest.getCommand(),
                            "Command executed via SessionService. Exit: " + response.getExitStatus(),
                            response.getExitStatus() == 0 ? "SUCCESS" : "FAILURE",
                            response.getStdout(), response.getStderr(), response.getExitStatus());
            } else {
                 logActivity(serverId, platformUserId, commandRequest.getCommand(),
                            "Command execution via SessionService returned null body.", "FAILURE", null, null, null);
            }
            return response;

        } catch (HttpStatusCodeException e) {
            log.error("SessionService call failed for command execution (server {}): {} - {}", serverId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            // Log failure activity
            logActivity(serverId, platformUserId, commandRequest.getCommand(),
                        "SessionService error: " + e.getStatusCode(), "FAILURE", null, e.getResponseBodyAsString(), null);
            // Map to a local exception type
            throw new RemoteCommandException("Failed to execute command via SessionService: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error during command execution delegation (server {}): {}", serverId, e.getMessage(), e);
            logActivity(serverId, platformUserId, commandRequest.getCommand(),
                        "Unexpected error: " + e.getMessage(), "FAILURE", null, null, null);
            throw new RemoteCommandException("Unexpected error delegating command execution: " + e.getMessage(), e);
        }
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
