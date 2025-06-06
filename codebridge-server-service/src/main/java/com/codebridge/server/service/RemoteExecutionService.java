package com.codebridge.server.service;

import com.codebridge.server.dto.ServerResponse;
import com.codebridge.server.dto.remote.CommandRequest;
import com.codebridge.server.dto.remote.CommandResponse;
import com.codebridge.server.exception.RemoteCommandException;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.exception.AccessDeniedException; // Added
import com.codebridge.server.util.JwtUtil; // Assuming a utility for JWT parsing
import io.jsonwebtoken.Claims; // Added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // Added
import org.springframework.http.HttpEntity; // Added
import org.springframework.http.HttpHeaders; // Added
import org.springframework.http.HttpMethod; // Added
import org.springframework.http.ResponseEntity; // Added
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException; // Added
import org.springframework.web.client.RestTemplate; // Added

import java.util.UUID;

@Service
public class RemoteExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(RemoteExecutionService.class);
    // Removed JSch related constants

    private final RestTemplate restTemplate;
    private final ServerAccessControlService serverAccessControlService;
    private final ServerActivityLogService activityLogService;
    private final String sessionServiceBaseUrl;
    // Assuming a JwtUtil for parsing SessionService issued JWTs (sessionToken)
    // This is a simplified approach for restoration. In a full setup, this might involve more robust token validation.
    private final JwtUtil jwtUtil = new JwtUtil(); // Placeholder instantiation

    public RemoteExecutionService(RestTemplate restTemplate,
                                  ServerAccessControlService serverAccessControlService,
                                  ServerActivityLogService activityLogService,
                                  @Value("${codebridge.service-urls.session-service}") String sessionServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.serverAccessControlService = serverAccessControlService;
        this.activityLogService = activityLogService;
        this.sessionServiceBaseUrl = sessionServiceBaseUrl;
    }

    // Method signature changed: platformUserId is now derived from sessionToken
    public CommandResponse executeCommand(UUID serverId, String sessionToken, CommandRequest commandRequest) {
        long startTime = System.currentTimeMillis();
        String logStatus = "FAILED";
        String errorMessage = null;
        String logDetails = String.format("ServerID: %s, Command: '%s'", serverId, commandRequest.getCommand());
        UUID platformUserIdFromToken = null;

        try {
            // 1. Validate sessionToken and extract claims (simplified)
            // In a real scenario, use a proper JWT library and validate signature, expiry, issuer etc.
            // For restoration, assume JwtUtil can parse claims.
            Claims claims = jwtUtil.extractAllClaims(sessionToken); // Placeholder for actual JWT parsing
            platformUserIdFromToken = UUID.fromString(claims.getSubject());
            UUID resourceIdFromToken = UUID.fromString(claims.get("resourceId", String.class));
            String sessionType = claims.get("type", String.class);

            if (!"SSH".equals(sessionType)) {
                throw new AccessDeniedException("Invalid session type for SSH command execution.");
            }
            if (!resourceIdFromToken.equals(serverId)) {
                throw new AccessDeniedException("Session token resourceId mismatch with target serverId.");
            }

            // 2. Authorize if user still has access (optional step, SessionService might be sole authority)
            // For this restoration, we re-validate with ServerAccessControlService.
            // This provides an extra layer of security if grants change after session issuance.
            serverAccessControlService.getValidatedConnectionDetails(platformUserIdFromToken, serverId);
            // The above call throws AccessDeniedException if user no longer has access.

            // 3. Make RestTemplate call to SessionService
            String url = sessionServiceBaseUrl + "/ops/ssh/" + sessionToken + "/execute-command";
            HttpHeaders headers = new HttpHeaders();
            // SessionService's operational endpoints are secured by its own JWT validation (SessionToken).
            // No need to propagate original User JWT here.
            HttpEntity<CommandRequest> entity = new HttpEntity<>(commandRequest, headers);

            ResponseEntity<CommandResponse> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, CommandResponse.class);

            logStatus = response.getBody() != null && response.getBody().getExitStatus() == 0 ? "SUCCESS" : "FAILED_WITH_STDERR";
            if (response.getBody() != null && response.getBody().getExitStatus() != 0) {
                errorMessage = response.getBody().getStderr();
            }

            activityLogService.createLog(platformUserIdFromToken, "REMOTE_COMMAND_EXECUTE_PROXY", serverId, logDetails, logStatus, errorMessage);
            return response.getBody();

        } catch (HttpStatusCodeException e) {
            errorMessage = e.getResponseBodyAsString();
            logger.error("Error calling SessionService for command execution on server {}: {} - {}", serverId, e.getStatusCode(), errorMessage, e);
            activityLogService.createLog(platformUserIdFromToken, "REMOTE_COMMAND_EXECUTE_PROXY_FAILED", serverId, logDetails, "FAILED", errorMessage);
            throw new RemoteCommandException("Failed to execute command via SessionService: " + errorMessage, e);
        } catch (AccessDeniedException e) {
            errorMessage = e.getMessage();
            logger.warn("Access denied for command execution on server {}: {}", serverId, errorMessage);
            activityLogService.createLog(platformUserIdFromToken, "REMOTE_COMMAND_EXECUTE_PROXY_DENIED", serverId, logDetails, "FAILED", errorMessage);
            throw e; // Re-throw
        }
        catch (Exception e) { // Catch other potential errors (e.g., JWT parsing)
            errorMessage = e.getMessage();
            logger.error("Unexpected error during remote command execution proxy for server {}: {}", serverId, errorMessage, e);
            activityLogService.createLog(platformUserIdFromToken, "REMOTE_COMMAND_EXECUTE_PROXY_ERROR", serverId, logDetails, "FAILED", errorMessage);
            throw new RemoteCommandException("Unexpected error during command execution: " + errorMessage, e);
        }
    }
}
