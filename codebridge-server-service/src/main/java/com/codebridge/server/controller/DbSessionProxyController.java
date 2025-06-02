package com.codebridge.server.controller;

import com.codebridge.server.dto.client.DbSessionCredentials; // Assuming this DTO exists
import com.codebridge.server.dto.client.DbSessionServiceApiInitRequestDto;
// Assuming a DTO for session response from SessionService, e.g., SessionResponse
// For now, using Map as placeholder.
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/db") // Standardized path for DB sessions
public class DbSessionProxyController {

    private final RestTemplate restTemplate;
    private final String sessionServiceBaseUrl;
    // ServerAccessControlService might be needed if serverId is involved for DB sessions
    // For this initial restoration, assuming DB sessions might not be strictly tied to a managed Server entity

    public DbSessionProxyController(RestTemplate restTemplate,
                                    @Value("${codebridge.service-urls.session-service}") String sessionServiceBaseUrl) {
        this.restTemplate = restTemplate;
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

    // Client provides DB connection alias and credentials directly
    // This DTO would be defined in codebridge-server-service or a shared DTO lib
    public static class DbSessionClientInitRequest {
        @jakarta.validation.constraints.NotBlank
        public String dbConnectionAlias;
        @jakarta.validation.constraints.NotNull
        public DbSessionCredentials credentials;
        public UUID serverId; // Optional: if this DB is related to a managed server
    }


    @PostMapping("/init")
    public ResponseEntity<?> initDbSession(@Valid @RequestBody DbSessionClientInitRequest clientRequest,
                                           Authentication authentication,
                                           HttpServletRequest request) {
        UUID platformUserId = getPlatformUserId(authentication);

        DbSessionServiceApiInitRequestDto sessionServiceRequest = new DbSessionServiceApiInitRequestDto(
            platformUserId,
            clientRequest.dbConnectionAlias,
            clientRequest.credentials
        );
        sessionServiceRequest.setServerId(clientRequest.serverId); // Pass along if provided

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String userJwt = extractJwtFromRequest(request);
        if (userJwt != null) {
            headers.setBearerAuth(userJwt);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","User JWT not found in request."));
        }
        
        HttpEntity<DbSessionServiceApiInitRequestDto> entity = new HttpEntity<>(sessionServiceRequest, headers);
        String url = sessionServiceBaseUrl + "/lifecycle/db/init";

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @PostMapping("/{sessionToken}/keepalive")
    public ResponseEntity<?> keepaliveDbSession(@PathVariable String sessionToken, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        String userJwt = extractJwtFromRequest(request);
        if (userJwt != null) {
            headers.setBearerAuth(userJwt);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = sessionServiceBaseUrl + "/lifecycle/db/" + sessionToken + "/keepalive";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @PostMapping("/{sessionToken}/release")
    public ResponseEntity<?> releaseDbSession(@PathVariable String sessionToken, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        String userJwt = extractJwtFromRequest(request);
        if (userJwt != null) {
            headers.setBearerAuth(userJwt);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = sessionServiceBaseUrl + "/lifecycle/db/" + sessionToken + "/release";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
