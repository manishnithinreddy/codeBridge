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
import org.springframework.security.core.Authentication; // Added
import org.springframework.security.oauth2.jwt.Jwt; // Added to get token value
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

// import java.util.Optional;
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

    // getCurrentPlatformUserId() placeholder removed

    @PostMapping("/init")
    public ResponseEntity<SessionResponse> initializeSession(@Valid @RequestBody SshSessionInitRequest request, Authentication authentication) {
        String platformUserIdString = authentication.getName();
        UUID platformUserId = UUID.fromString(platformUserIdString);
        UUID serverId = request.getServerId();
        log.info("User {} authenticated. Initializing SSH session for server: {}", platformUserId, serverId);

        try {
            // 1. Get connection details (includes authorization check) - This uses platformUserId from token
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
                platformUserId, // platformUserId from the validated User JWT
                serverId,
                connectionDetailsPayload
            );

            String url = sessionServiceBaseUrl + "/lifecycle/ssh/init";
            HttpHeaders headers = new HttpHeaders();
            if (authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                headers.setBearerAuth(jwt.getTokenValue());
            }
            HttpEntity<SshSessionServiceInitRequestDto> requestEntity = new HttpEntity<>(sessionServiceRequest, headers);

            ResponseEntity<SessionResponse> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, SessionResponse.class);

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
    public ResponseEntity<KeepAliveResponse> keepAliveSession(@PathVariable String sessionToken, Authentication authentication) {
        String platformUserIdString = authentication.getName(); // For logging/auditing
        log.info("User {} authenticated. Keeping alive SSH session with token: {}", platformUserIdString, sessionToken);

        try {
            String url = sessionServiceBaseUrl + "/lifecycle/ssh/" + sessionToken + "/keepalive";
            HttpHeaders headers = new HttpHeaders();
            if (authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                headers.setBearerAuth(jwt.getTokenValue());
            }
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<KeepAliveResponse> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, KeepAliveResponse.class);
            log.info("Keepalive call to SessionService for token {} returned status {}", sessionToken, responseEntity.getStatusCode());
            return responseEntity;
        } catch (HttpStatusCodeException e) {
            log.error("Error calling SessionService for SSH keepalive (token: {}): {} - {}", sessionToken, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(null);
        } catch (Exception e) {
            log.error("Unexpected error during SSH session keepalive for token {}: {}", sessionToken, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{sessionToken}/release")
    public ResponseEntity<Void> releaseSession(@PathVariable String sessionToken, Authentication authentication) {
        String platformUserIdString = authentication.getName(); // For logging/auditing
        log.info("User {} authenticated. Releasing SSH session with token: {}", platformUserIdString, sessionToken);
        try {
            String url = sessionServiceBaseUrl + "/lifecycle/ssh/" + sessionToken + "/release";
            HttpHeaders headers = new HttpHeaders();
            if (authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                headers.setBearerAuth(jwt.getTokenValue());
            }
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
            log.info("Release call to SessionService for token {} successful", sessionToken);
            return ResponseEntity.noContent().build();
        } catch (HttpStatusCodeException e) {
            log.error("Error calling SessionService for SSH release (token: {}): {} - {}", sessionToken, e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                 return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            log.error("Unexpected error during SSH session release for token {}: {}", sessionToken, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
