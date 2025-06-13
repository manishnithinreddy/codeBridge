package com.codebridge.server.service;

import com.codebridge.server.dto.ServerBlacklistRequest;
import com.codebridge.server.dto.ServerBlacklistResponse;
import com.codebridge.server.model.ServerBlacklist;
import com.codebridge.server.repository.ServerBlacklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for server blacklist operations.
 */
@Service
public class ServerBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(ServerBlacklistService.class);
    
    private final ServerBlacklistRepository blacklistRepository;
    
    @Autowired
    public ServerBlacklistService(ServerBlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }
    
    /**
     * Add a server to the blacklist.
     *
     * @param request the blacklist request
     * @param userId the user ID of the creator
     * @return the blacklist response
     */
    @Transactional
    public ServerBlacklistResponse addToBlacklist(ServerBlacklistRequest request, UUID userId) {
        logger.info("Adding server to blacklist: {}", request.getIpAddress());
        
        // Check if the IP is already blacklisted
        Optional<ServerBlacklist> existingEntry = blacklistRepository.findActiveByIpAddress(
                request.getIpAddress(), LocalDateTime.now());
        
        if (existingEntry.isPresent()) {
            // Update the existing entry
            ServerBlacklist entry = existingEntry.get();
            entry.setReason(request.getReason());
            entry.setExpiresAt(request.getExpiresAt());
            entry.setHostname(request.getHostname());
            
            return mapToResponse(blacklistRepository.save(entry));
        } else {
            // Create a new entry
            ServerBlacklist entry = new ServerBlacklist(request.getIpAddress(), userId);
            entry.setReason(request.getReason());
            entry.setExpiresAt(request.getExpiresAt());
            entry.setHostname(request.getHostname());
            
            return mapToResponse(blacklistRepository.save(entry));
        }
    }
    
    /**
     * Remove a server from the blacklist.
     *
     * @param id the blacklist entry ID
     * @return true if removed, false otherwise
     */
    @Transactional
    public boolean removeFromBlacklist(UUID id) {
        logger.info("Removing server from blacklist: {}", id);
        
        Optional<ServerBlacklist> entry = blacklistRepository.findById(id);
        
        if (entry.isPresent()) {
            ServerBlacklist blacklist = entry.get();
            blacklist.setActive(false);
            blacklistRepository.save(blacklist);
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a server is blacklisted.
     *
     * @param ipAddress the IP address to check
     * @return true if blacklisted, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String ipAddress) {
        return blacklistRepository.findActiveByIpAddress(ipAddress, LocalDateTime.now()).isPresent();
    }
    
    /**
     * Get all active blacklist entries.
     *
     * @return the list of blacklist responses
     */
    @Transactional(readOnly = true)
    public List<ServerBlacklistResponse> getAllActiveBlacklist() {
        return blacklistRepository.findAllActive(LocalDateTime.now()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get blacklist entry by ID.
     *
     * @param id the blacklist entry ID
     * @return the blacklist response, if found
     */
    @Transactional(readOnly = true)
    public Optional<ServerBlacklistResponse> getBlacklistById(UUID id) {
        return blacklistRepository.findById(id)
                .map(this::mapToResponse);
    }
    
    /**
     * Scheduled task to clean up expired blacklist entries.
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void cleanupExpiredEntries() {
        logger.info("Cleaning up expired blacklist entries");
        
        LocalDateTime now = LocalDateTime.now();
        List<ServerBlacklist> expiredEntries = blacklistRepository.findExpired(now);
        
        for (ServerBlacklist entry : expiredEntries) {
            entry.setActive(false);
            blacklistRepository.save(entry);
            logger.info("Deactivated expired blacklist entry: {}", entry.getId());
        }
    }
    
    /**
     * Map a ServerBlacklist entity to a ServerBlacklistResponse DTO.
     *
     * @param blacklist the blacklist entity
     * @return the blacklist response
     */
    private ServerBlacklistResponse mapToResponse(ServerBlacklist blacklist) {
        ServerBlacklistResponse response = new ServerBlacklistResponse();
        response.setId(blacklist.getId());
        response.setIpAddress(blacklist.getIpAddress());
        response.setHostname(blacklist.getHostname());
        response.setReason(blacklist.getReason());
        response.setCreatedAt(blacklist.getCreatedAt());
        response.setExpiresAt(blacklist.getExpiresAt());
        response.setCreatedBy(blacklist.getCreatedBy());
        response.setActive(blacklist.isActive());
        
        return response;
    }
}

