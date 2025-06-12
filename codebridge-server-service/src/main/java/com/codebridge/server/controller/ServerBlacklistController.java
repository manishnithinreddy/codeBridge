package com.codebridge.server.controller;

import com.codebridge.server.dto.ServerBlacklistRequest;
import com.codebridge.server.dto.ServerBlacklistResponse;
import com.codebridge.server.service.ServerBlacklistService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for server blacklist operations.
 */
@RestController
@RequestMapping("/api/v1/servers/blacklist")
public class ServerBlacklistController {

    private static final Logger logger = LoggerFactory.getLogger(ServerBlacklistController.class);
    
    private final ServerBlacklistService blacklistService;
    
    @Autowired
    public ServerBlacklistController(ServerBlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }
    
    /**
     * Add a server to the blacklist.
     *
     * @param request the blacklist request
     * @param userDetails the authenticated user details
     * @return the blacklist response
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServerBlacklistResponse> addToBlacklist(
            @Valid @RequestBody ServerBlacklistRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        logger.info("Adding server to blacklist: {}", request.getIpAddress());
        
        // Extract user ID from UserDetails
        UUID userId = UUID.fromString(userDetails.getUsername());
        
        ServerBlacklistResponse response = blacklistService.addToBlacklist(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Remove a server from the blacklist.
     *
     * @param id the blacklist entry ID
     * @return no content if successful, not found otherwise
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeFromBlacklist(@PathVariable UUID id) {
        logger.info("Removing server from blacklist: {}", id);
        
        boolean removed = blacklistService.removeFromBlacklist(id);
        
        if (removed) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get all active blacklist entries.
     *
     * @return the list of blacklist responses
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServerBlacklistResponse>> getAllActiveBlacklist() {
        logger.info("Getting all active blacklist entries");
        
        List<ServerBlacklistResponse> responses = blacklistService.getAllActiveBlacklist();
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get blacklist entry by ID.
     *
     * @param id the blacklist entry ID
     * @return the blacklist response if found, not found otherwise
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServerBlacklistResponse> getBlacklistById(@PathVariable UUID id) {
        logger.info("Getting blacklist entry by ID: {}", id);
        
        return blacklistService.getBlacklistById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Check if a server is blacklisted.
     *
     * @param ipAddress the IP address to check
     * @return true if blacklisted, false otherwise
     */
    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Boolean> isBlacklisted(@RequestParam String ipAddress) {
        logger.info("Checking if server is blacklisted: {}", ipAddress);
        
        boolean isBlacklisted = blacklistService.isBlacklisted(ipAddress);
        return ResponseEntity.ok(isBlacklisted);
    }
}

