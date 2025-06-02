package com.codebridge.server.controller;

import com.codebridge.server.dto.client.DbSessionServiceApiInitRequest;
import com.codebridge.server.dto.sessions.DbSessionInitRequest; // Request DTO for this controller's endpoint
import com.codebridge.server.dto.sessions.KeepAliveResponse;
import com.codebridge.server.dto.sessions.SessionResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity; // Added
import org.springframework.http.HttpHeaders; // Added
import org.springframework.http.HttpMethod; // Added
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Added
import org.springframework.security.oauth2.jwt.Jwt; // Added
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@RestController
@RequestMapping("/api/db/sessions") // Same path as the old DbSessionController
public class DbSessionProxyController {

    private static final Logger log = LoggerFactory.getLogger(DbSessionProxyController.class);

    private final RestTemplate restTemplate;

    @Value("${codebridge.service-urls.session-service}")
    private String sessionServiceBaseUrl;

    public DbSessionProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // getCurrentPlatformUserId() placeholder removed

    @PostMapping("/init")
    public ResponseEntity<SessionResponse> initializeDbSession(@Valid @RequestBody DbSessionInitRequest localRequest, Authentication authentication) {
        String platformUserIdString = authentication.getName();
        UUID platformUserId = UUID.fromString(platformUserIdString);
        log.info("User {} authenticated. Proxying request to initialize DB session for alias: {}",
                 platformUserId, localRequest.getDbConnectionAlias());

        // Prepare the request for SessionService
        DbSessionServiceApiInitRequest sessionServiceRequest = new DbSessionServiceApiInitRequest(
            platformUserId,
            localRequest.getDbConnectionAlias(),
            localRequest.getCredentials()
        );

        String url = sessionServiceBaseUrl + "/lifecycle/db/init";
        try {
            HttpHeaders headers = new HttpHeaders();
            if (authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                headers.setBearerAuth(jwt.getTokenValue());
            }
            HttpEntity<DbSessionServiceApiInitRequest> requestEntity = new HttpEntity<>(sessionServiceRequest, headers);

            ResponseEntity<SessionResponse> responseEntity =
                restTemplate.exchange(url, HttpMethod.POST, requestEntity, SessionResponse.class);

            if(responseEntity.getStatusCode() == HttpStatus.CREATED && responseEntity.getBody() != null) {
                 log.info("Successfully proxied DB init request for alias: {}, user: {}. SessionService Token: {}",
                    localRequest.getDbConnectionAlias(), platformUserId,
                    responseEntity.getBody().getSessionToken() != null ? "[PRESENT]" : "[MISSING]");
            } else {
                log.warn("SessionService call for DB init returned status: {} for alias: {}, user: {}",
                    responseEntity.getStatusCode(), localRequest.getDbConnectionAlias(), platformUserId);
            }
            return responseEntity;
        } catch (HttpStatusCodeException e) {
            log.error("Error proxying DB init request to SessionService: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(null); // Or map to a specific error DTO
        } catch (Exception e) {
            log.error("Unexpected error proxying DB session initialization for alias {}: {}", localRequest.getDbConnectionAlias(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{sessionToken}/keepalive")
    public ResponseEntity<KeepAliveResponse> keepAliveDbSession(@PathVariable String sessionToken, Authentication authentication) {
        String platformUserIdString = authentication.getName();
        log.info("User {} authenticated. Proxying request to keep alive DB session with token: {}", platformUserIdString, sessionToken);
        try {
            String url = sessionServiceBaseUrl + "/lifecycle/db/" + sessionToken + "/keepalive";
            HttpHeaders headers = new HttpHeaders();
            if (authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                headers.setBearerAuth(jwt.getTokenValue());
            }
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<KeepAliveResponse> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, KeepAliveResponse.class);
            log.info("Keepalive proxy call to SessionService for token {} returned status {}", sessionToken, responseEntity.getStatusCode());
            return responseEntity;
        } catch (HttpStatusCodeException e) {
            log.error("Error proxying DB keepalive to SessionService (token: {}): {} - {}", sessionToken, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(null);
        } catch (Exception e) {
            log.error("Unexpected error proxying DB session keepalive for token {}: {}", sessionToken, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{sessionToken}/release")
    public ResponseEntity<Void> releaseDbSession(@PathVariable String sessionToken, Authentication authentication) {
        String platformUserIdString = authentication.getName();
        log.info("User {} authenticated. Proxying request to release DB session with token: {}", platformUserIdString, sessionToken);
        try {
            String url = sessionServiceBaseUrl + "/lifecycle/db/" + sessionToken + "/release";
            HttpHeaders headers = new HttpHeaders();
            if (authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                headers.setBearerAuth(jwt.getTokenValue());
            }
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
            log.info("Release proxy call to SessionService for token {} successful", sessionToken);
            return ResponseEntity.noContent().build();
        } catch (HttpStatusCodeException e) {
            log.error("Error proxying DB release to SessionService (token: {}): {} - {}", sessionToken, e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                 return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            log.error("Unexpected error proxying DB session release for token {}: {}", sessionToken, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
