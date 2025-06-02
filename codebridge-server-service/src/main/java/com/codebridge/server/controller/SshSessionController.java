package com.codebridge.server.controller;

import com.codebridge.server.dto.client.ClientUserProvidedConnectionDetails;
import com.codebridge.server.dto.client.SshSessionServiceInitRequestDto;
import com.codebridge.server.dto.sessions.KeepAliveResponse;
import com.codebridge.server.dto.sessions.SessionResponse;
import com.codebridge.server.dto.sessions.SshSessionInitRequest; // This is the request to this controller
import com.codebridge.server.model.SshKey;
import com.codebridge.server.service.ServerAccessControlService;
import com.codebridge.server.service.SshKeyManagementService;
// Old session manager removed
// import com.codebridge.server.sessions.InMemorySessionManagerImpl;
// import com.codebridge.server.sessions.SshSessionWrapper;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

// import java.util.Optional; // No longer directly used from local manager
import java.util.UUID;

@RestController
@RequestMapping("/api/ssh/sessions")
public class SshSessionController {

    private static final Logger log = LoggerFactory.getLogger(SshSessionController.class);

    private final RestTemplate restTemplate;
    private final ServerAccessControlService serverAccessControlService;
    private final SshKeyManagementService sshKeyManagementService;

    @Value("${codebridge.service-urls.session-service}")
    private String sessionServiceBaseUrl;

    public SshSessionController(RestTemplate restTemplate,
                                ServerAccessControlService serverAccessControlService,
                                SshKeyManagementService sshKeyManagementService) {
        this.restTemplate = restTemplate;
        this.serverAccessControlService = serverAccessControlService;
        this.sshKeyManagementService = sshKeyManagementService;
    }

    // TODO: Replace with actual user ID from Spring Security context
    private UUID getCurrentPlatformUserId() {
        // For example, using Spring Security:
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
        //     UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        //     // Assuming your UserDetails implementation has a method to get the UUID
        //     // return ((CustomUserDetails) userDetails).getId();
        // }
        return UUID.fromString("00000000-0000-0000-0000-000000000000"); // Placeholder
    }

    @PostMapping("/init")
    public ResponseEntity<SessionResponse> initializeSession(@Valid @RequestBody SshSessionInitRequest request) {
        UUID platformUserId = getCurrentPlatformUserId();
        UUID serverId = request.getServerId();
        log.info("Received request to initialize SSH session for server: {} by user: {}", serverId, platformUserId);

        try {
            // 1. Get connection details (includes authorization check)
            ServerAccessControlService.UserSpecificConnectionDetails userAccessDetails =
                serverAccessControlService.checkUserAccessAndGetConnectionDetails(platformUserId, serverId);

            if (userAccessDetails.server().getAuthProvider() != com.codebridge.server.model.enums.ServerAuthProvider.SSH_KEY) {
                throw new IllegalArgumentException("Server is not configured for SSH key authentication.");
            }
            if (userAccessDetails.sshKeyIdToUse() == null) {
                 throw new IllegalArgumentException("No SSH key is assigned for this user on the specified server.");
            }

            // 2. Fetch decrypted SSH key
            SshKey decryptedSshKey = sshKeyManagementService.getDecryptedSshKey(userAccessDetails.sshKeyIdToUse(), platformUserId);

            // 3. Construct payload for SessionService
            ClientUserProvidedConnectionDetails connectionDetailsPayload = new ClientUserProvidedConnectionDetails(
                userAccessDetails.server().getHostname(),
                userAccessDetails.server().getPort(),
                userAccessDetails.remoteUsername(),
                decryptedSshKey.getPrivateKeyBytes(),
                decryptedSshKey.getPublicKeyBytes()
            );

            SshSessionServiceInitRequestDto sessionServiceRequest = new SshSessionServiceInitRequestDto(
                platformUserId,
                serverId,
                connectionDetailsPayload
            );

            String url = sessionServiceBaseUrl + "/lifecycle/ssh/init";
            ResponseEntity<SessionResponse> responseEntity = restTemplate.postForEntity(url, sessionServiceRequest, SessionResponse.class);

            // Log successful call to session service before returning its response
            if(responseEntity.getStatusCode() == HttpStatus.CREATED && responseEntity.getBody() != null) {
                 log.info("Successfully initialized session via SessionService for server: {}, user: {}. Token: {}",
                    serverId, platformUserId, responseEntity.getBody().getSessionToken() != null ? "[PRESENT]" : "[MISSING]");
            } else {
                log.warn("SessionService call for init returned status: {} for server: {}, user: {}",
                    responseEntity.getStatusCode(), serverId, platformUserId);
            }
            return responseEntity;

        } catch (HttpStatusCodeException e) {
            log.error("Error calling SessionService for SSH init: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            // Convert HttpStatusCodeException to a suitable local response
            return ResponseEntity.status(e.getStatusCode()).body(null); // Or map to an error DTO
        } catch (Exception e) {
            log.error("Unexpected error during SSH session initialization for server {}: {}", serverId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // Or map to an error DTO
        }
    }

    @PostMapping("/{sessionToken}/keepalive")
    public ResponseEntity<KeepAliveResponse> keepAliveSession(@PathVariable String sessionToken) {
        log.info("Received request to keep alive SSH session with token: {}", sessionToken);

        // Similar to init, keepAliveSshSession in InMemorySessionManagerImpl was designed to use its own
        // injected sshSessionConfigProperties.
        Optional<KeepAliveResponse> keepAliveResponseOpt = sshSessionManager.keepAliveSshSession(sessionToken);

        if (keepAliveResponseOpt.isPresent()) {
            return ResponseEntity.ok(keepAliveResponseOpt.get());
        } else {
            log.warn("Keepalive failed for session token: {}. Session not found or expired.", sessionToken);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{sessionToken}/release")
    public ResponseEntity<Void> releaseSession(@PathVariable String sessionToken) {
        log.info("Received request to release SSH session with token: {}", sessionToken);
        sshSessionManager.releaseSshSession(sessionToken);
        return ResponseEntity.noContent().build();
    }
}
