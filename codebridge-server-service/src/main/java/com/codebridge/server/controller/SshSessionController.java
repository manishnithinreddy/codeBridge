package com.codebridge.server.controller;

import com.codebridge.server.dto.UserSpecificConnectionDetailsDto;
import com.codebridge.server.dto.client.ClientUserProvidedConnectionDetails;
import com.codebridge.server.dto.client.SshSessionServiceApiInitRequestDto;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.service.ServerAccessControlService;
// Assuming a DTO for session response from SessionService, e.g., SessionResponse
// For now, let's use a generic ResponseEntity<Map<String, Object>> or define a simple SessionResponse DTO.
// Let's define a simple SessionResponse for now.
// package com.codebridge.server.dto.client;
// public record SessionResponse(String sessionToken, String type, String status, long createdAt, long expiresAt) {}
// For now, using Map as placeholder. A proper shared DTO would be better.
import com.codebridge.server.util.JwtUtil; // Placeholder for actual JWT extraction utility
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/ssh") // Matches common pattern for proxying to session service
public class SshSessionController {

    private final RestTemplate restTemplate;
    private final ServerAccessControlService serverAccessControlService;
    private final String sessionServiceBaseUrl;

    public SshSessionController(RestTemplate restTemplate,
                                ServerAccessControlService serverAccessControlService,
                                @Value("${codebridge.service-urls.session-service}") String sessionServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.serverAccessControlService = serverAccessControlService;
        this.sessionServiceBaseUrl = sessionServiceBaseUrl;
    }

    private UUID getPlatformUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Authentication principal not found.");
        }
        return UUID.fromString(authentication.getName());
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }


    @PostMapping("/init")
    public ResponseEntity<?> initSshSession(@RequestParam UUID serverId,
                                            Authentication authentication,
                                            HttpServletRequest request) {
        UUID platformUserId = getPlatformUserId(authentication);
        UserSpecificConnectionDetailsDto connDetailsDto =
            serverAccessControlService.getValidatedConnectionDetails(platformUserId, serverId);

        ClientUserProvidedConnectionDetails clientDetails = new ClientUserProvidedConnectionDetails(
            connDetailsDto.getHostname(),
            connDetailsDto.getPort(),
            connDetailsDto.getUsername(),
            connDetailsDto.getAuthProvider()
        );

        if (connDetailsDto.getAuthProvider() == ServerAuthProvider.SSH_KEY) {
            SshKey decryptedKey = connDetailsDto.getDecryptedSshKey();
            if (decryptedKey == null || decryptedKey.getPrivateKey() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Decrypted SSH key not available for connection."));
            }
            clientDetails.setDecryptedPrivateKey(decryptedKey.getPrivateKey());
            clientDetails.setSshKeyName(decryptedKey.getName());
        } else if (connDetailsDto.getAuthProvider() == ServerAuthProvider.PASSWORD) {
            if (connDetailsDto.getDecryptedPassword() == null) {
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Decrypted password not available for connection."));
            }
            clientDetails.setDecryptedPassword(connDetailsDto.getDecryptedPassword());
        } else {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Unsupported auth provider for SSH session init."));
        }

        SshSessionServiceApiInitRequestDto sessionServiceRequest =
            new SshSessionServiceApiInitRequestDto(platformUserId, serverId, clientDetails);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String userJwt = extractJwtFromRequest(request);
        if (userJwt != null) {
            headers.setBearerAuth(userJwt);
        } else {
            // This should ideally not happen if Spring Security is correctly configured
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","User JWT not found in request."));
        }

        HttpEntity<SshSessionServiceApiInitRequestDto> entity = new HttpEntity<>(sessionServiceRequest, headers);
        String url = sessionServiceBaseUrl + "/lifecycle/ssh/init"; // Target SessionService endpoint

        try {
            // Assuming SessionResponse is a Map or a defined DTO like:
            // public record SessionResponse(String sessionToken, String type, String status, long createdAt, long expiresAt) {}
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @PostMapping("/{sessionToken}/keepalive")
    public ResponseEntity<?> keepaliveSshSession(@PathVariable String sessionToken, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        String userJwt = extractJwtFromRequest(request);
         if (userJwt != null) {
            headers.setBearerAuth(userJwt);
        } // else: SessionService will reject if it needs it and it's missing
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = sessionServiceBaseUrl + "/lifecycle/ssh/" + sessionToken + "/keepalive";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @PostMapping("/{sessionToken}/release")
    public ResponseEntity<?> releaseSshSession(@PathVariable String sessionToken, HttpServletRequest request) {
         HttpHeaders headers = new HttpHeaders();
        String userJwt = extractJwtFromRequest(request);
        if (userJwt != null) {
            headers.setBearerAuth(userJwt);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = sessionServiceBaseUrl + "/lifecycle/ssh/" + sessionToken + "/release";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
