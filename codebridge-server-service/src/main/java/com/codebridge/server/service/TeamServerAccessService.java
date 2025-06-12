package com.codebridge.server.service;

import com.codebridge.server.dto.TeamServerAccessDTO;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.TeamServerAccess;
import com.codebridge.server.repository.TeamServerAccessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing team-based server access.
 */
@Service
public class TeamServerAccessService {
    private static final Logger logger = LoggerFactory.getLogger(TeamServerAccessService.class);

    private final TeamServerAccessRepository teamServerAccessRepository;

    @Autowired
    public TeamServerAccessService(TeamServerAccessRepository teamServerAccessRepository) {
        this.teamServerAccessRepository = teamServerAccessRepository;
    }

    /**
     * Grants access to a server for a team.
     *
     * @param serverId The server ID
     * @param teamId The team ID
     * @param accessLevel The access level
     * @param createdBy The user ID of the user granting access
     * @param expiresAt The expiration time (can be null for permanent access)
     * @return The created team server access
     */
    @Transactional
    public TeamServerAccessDTO grantAccess(
            UUID serverId,
            UUID teamId,
            TeamServerAccess.AccessLevel accessLevel,
            UUID createdBy,
            LocalDateTime expiresAt) {
        
        // Check if access already exists
        teamServerAccessRepository.findByServerIdAndTeamId(serverId, teamId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Team already has access to this server");
                });
        
        TeamServerAccess teamServerAccess = new TeamServerAccess();
        teamServerAccess.setServerId(serverId);
        teamServerAccess.setTeamId(teamId);
        teamServerAccess.setAccessLevel(accessLevel);
        teamServerAccess.setCreatedBy(createdBy);
        teamServerAccess.setExpiresAt(expiresAt);
        
        TeamServerAccess saved = teamServerAccessRepository.save(teamServerAccess);
        logger.info("Granted {} access to server {} for team {}, expires at: {}", 
                accessLevel, serverId, teamId, expiresAt);
        
        return mapToDTO(saved);
    }

    /**
     * Updates access to a server for a team.
     *
     * @param serverId The server ID
     * @param teamId The team ID
     * @param accessLevel The new access level
     * @param expiresAt The new expiration time (can be null for permanent access)
     * @return The updated team server access
     */
    @Transactional
    public TeamServerAccessDTO updateAccess(
            UUID serverId,
            UUID teamId,
            TeamServerAccess.AccessLevel accessLevel,
            LocalDateTime expiresAt) {
        
        TeamServerAccess teamServerAccess = teamServerAccessRepository.findByServerIdAndTeamId(serverId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team does not have access to this server"));
        
        teamServerAccess.setAccessLevel(accessLevel);
        teamServerAccess.setExpiresAt(expiresAt);
        
        TeamServerAccess saved = teamServerAccessRepository.save(teamServerAccess);
        logger.info("Updated access to server {} for team {} to {}, expires at: {}", 
                serverId, teamId, accessLevel, expiresAt);
        
        return mapToDTO(saved);
    }

    /**
     * Revokes access to a server for a team.
     *
     * @param serverId The server ID
     * @param teamId The team ID
     */
    @Transactional
    public void revokeAccess(UUID serverId, UUID teamId) {
        TeamServerAccess teamServerAccess = teamServerAccessRepository.findByServerIdAndTeamId(serverId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team does not have access to this server"));
        
        teamServerAccessRepository.delete(teamServerAccess);
        logger.info("Revoked access to server {} for team {}", serverId, teamId);
    }

    /**
     * Gets all team-based access entries for a server.
     *
     * @param serverId The server ID
     * @return A list of team-based access entries
     */
    @Transactional(readOnly = true)
    public List<TeamServerAccessDTO> getAccessByServerId(UUID serverId) {
        return teamServerAccessRepository.findByServerId(serverId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets all team-based access entries for a team.
     *
     * @param teamId The team ID
     * @return A list of team-based access entries
     */
    @Transactional(readOnly = true)
    public List<TeamServerAccessDTO> getAccessByTeamId(UUID teamId) {
        return teamServerAccessRepository.findByTeamId(teamId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets all valid team-based access entries for a server.
     * A valid entry is one that has not expired.
     *
     * @param serverId The server ID
     * @return A list of valid team-based access entries
     */
    @Transactional(readOnly = true)
    public List<TeamServerAccessDTO> getValidAccessByServerId(UUID serverId) {
        return teamServerAccessRepository.findValidAccessByServerId(serverId, LocalDateTime.now()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets all valid team-based access entries for a team.
     * A valid entry is one that has not expired.
     *
     * @param teamId The team ID
     * @return A list of valid team-based access entries
     */
    @Transactional(readOnly = true)
    public List<TeamServerAccessDTO> getValidAccessByTeamId(UUID teamId) {
        return teamServerAccessRepository.findValidAccessByTeamId(teamId, LocalDateTime.now()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets all valid team-based access entries for a user's teams.
     * A valid entry is one that has not expired.
     *
     * @param teamIds The team IDs
     * @return A list of valid team-based access entries
     */
    @Transactional(readOnly = true)
    public List<TeamServerAccessDTO> getValidAccessByTeamIds(List<UUID> teamIds) {
        return teamServerAccessRepository.findValidAccessByTeamIds(teamIds, LocalDateTime.now()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a team has access to a server.
     *
     * @param serverId The server ID
     * @param teamId The team ID
     * @return True if the team has access to the server, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(UUID serverId, UUID teamId) {
        return teamServerAccessRepository.findByServerIdAndTeamId(serverId, teamId).isPresent();
    }

    /**
     * Checks if a team has valid access to a server.
     * Valid access is access that has not expired.
     *
     * @param serverId The server ID
     * @param teamId The team ID
     * @return True if the team has valid access to the server, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasValidAccess(UUID serverId, UUID teamId) {
        return teamServerAccessRepository.findByServerIdAndTeamId(serverId, teamId)
                .map(access -> access.getExpiresAt() == null || access.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    /**
     * Checks if a team has a specific access level to a server.
     *
     * @param serverId The server ID
     * @param teamId The team ID
     * @param accessLevel The access level
     * @return True if the team has the specified access level to the server, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasAccessLevel(UUID serverId, UUID teamId, TeamServerAccess.AccessLevel accessLevel) {
        return teamServerAccessRepository.findByServerIdAndTeamId(serverId, teamId)
                .map(access -> access.getAccessLevel().ordinal() >= accessLevel.ordinal() &&
                        (access.getExpiresAt() == null || access.getExpiresAt().isAfter(LocalDateTime.now())))
                .orElse(false);
    }

    /**
     * Scheduled task to clean up expired team-based access entries.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void cleanupExpiredAccess() {
        int deleted = teamServerAccessRepository.deleteExpiredAccess(LocalDateTime.now());
        if (deleted > 0) {
            logger.info("Cleaned up {} expired team server access entries", deleted);
        }
    }

    /**
     * Maps a TeamServerAccess entity to a TeamServerAccessDTO.
     *
     * @param teamServerAccess The TeamServerAccess entity
     * @return The TeamServerAccessDTO
     */
    private TeamServerAccessDTO mapToDTO(TeamServerAccess teamServerAccess) {
        return new TeamServerAccessDTO(
                teamServerAccess.getId(),
                teamServerAccess.getServerId(),
                teamServerAccess.getTeamId(),
                teamServerAccess.getAccessLevel().name(),
                teamServerAccess.getCreatedBy(),
                teamServerAccess.getExpiresAt(),
                teamServerAccess.getCreatedAt(),
                teamServerAccess.getUpdatedAt()
        );
    }
}

