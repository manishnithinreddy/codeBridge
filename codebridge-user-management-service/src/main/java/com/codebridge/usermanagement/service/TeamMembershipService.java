package com.codebridge.usermanagement.service;

import com.codebridge.usermanagement.exception.ResourceNotFoundException;
import com.codebridge.usermanagement.model.TeamMembership;
import com.codebridge.usermanagement.model.TeamRole;
import com.codebridge.usermanagement.repository.TeamMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for team membership operations.
 */
@Service
public class TeamMembershipService {

    private static final Logger logger = LoggerFactory.getLogger(TeamMembershipService.class);

    private final TeamMembershipRepository teamMembershipRepository;

    @Autowired
    public TeamMembershipService(TeamMembershipRepository teamMembershipRepository) {
        this.teamMembershipRepository = teamMembershipRepository;
    }

    /**
     * Get all team memberships.
     *
     * @return list of team memberships
     */
    public List<TeamMembership> getAllTeamMemberships() {
        return teamMembershipRepository.findAll();
    }

    /**
     * Get a team membership by ID.
     *
     * @param id the team membership ID
     * @return the team membership
     * @throws ResourceNotFoundException if the team membership is not found
     */
    public TeamMembership getTeamMembershipById(UUID id) {
        return teamMembershipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TeamMembership", "id", id));
    }

    /**
     * Get all memberships for a user.
     *
     * @param userId the user ID
     * @return list of team memberships
     */
    public List<TeamMembership> getUserTeamMemberships(UUID userId) {
        return teamMembershipRepository.findByUserId(userId);
    }

    /**
     * Get all active memberships for a user.
     *
     * @param userId the user ID
     * @return list of active team memberships
     */
    public List<TeamMembership> getUserActiveTeamMemberships(UUID userId) {
        return teamMembershipRepository.findByUserIdAndIsActive(userId, true);
    }

    /**
     * Get all memberships for a team.
     *
     * @param teamId the team ID
     * @return list of team memberships
     */
    public List<TeamMembership> getTeamMemberships(UUID teamId) {
        return teamMembershipRepository.findByTeamId(teamId);
    }

    /**
     * Get all active memberships for a team.
     *
     * @param teamId the team ID
     * @return list of active team memberships
     */
    public List<TeamMembership> getActiveTeamMemberships(UUID teamId) {
        return teamMembershipRepository.findByTeamIdAndIsActive(teamId, true);
    }

    /**
     * Get all memberships for a team with a specific role.
     *
     * @param teamId the team ID
     * @param role the team role
     * @return list of team memberships
     */
    public List<TeamMembership> getTeamMembershipsByRole(UUID teamId, TeamRole role) {
        return teamMembershipRepository.findByTeamIdAndRole(teamId, role);
    }

    /**
     * Get a specific membership for a user and team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return the team membership
     * @throws ResourceNotFoundException if the team membership is not found
     */
    public TeamMembership getUserTeamMembership(UUID userId, UUID teamId) {
        return teamMembershipRepository.findByUserIdAndTeamId(userId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("TeamMembership", "userId and teamId", userId + " and " + teamId));
    }

    /**
     * Create a new team membership.
     *
     * @param teamMembership the team membership to create
     * @return the created team membership
     */
    @Transactional
    public TeamMembership createTeamMembership(TeamMembership teamMembership) {
        if (teamMembership.getId() == null) {
            teamMembership.setId(UUID.randomUUID());
        }
        
        if (teamMembership.getJoinedAt() == null) {
            teamMembership.setJoinedAt(LocalDateTime.now());
        }
        
        if (teamMembership.getLastActiveAt() == null) {
            teamMembership.setLastActiveAt(LocalDateTime.now());
        }
        
        teamMembership.setActive(true);
        
        logger.info("Creating team membership for user ID: {} in team ID: {}", 
                teamMembership.getUserId(), teamMembership.getTeamId());
        return teamMembershipRepository.save(teamMembership);
    }

    /**
     * Update a team membership.
     *
     * @param id the team membership ID
     * @param teamMembershipDetails the team membership details
     * @return the updated team membership
     * @throws ResourceNotFoundException if the team membership is not found
     */
    @Transactional
    public TeamMembership updateTeamMembership(UUID id, TeamMembership teamMembershipDetails) {
        TeamMembership teamMembership = getTeamMembershipById(id);
        
        teamMembership.setRole(teamMembershipDetails.getRole());
        teamMembership.setActive(teamMembershipDetails.isActive());
        teamMembership.setLastActiveAt(LocalDateTime.now());
        
        logger.info("Updating team membership for user ID: {} in team ID: {}", 
                teamMembership.getUserId(), teamMembership.getTeamId());
        return teamMembershipRepository.save(teamMembership);
    }

    /**
     * Update a user's role in a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @param role the new role
     * @return the updated team membership
     * @throws ResourceNotFoundException if the team membership is not found
     */
    @Transactional
    public TeamMembership updateUserRole(UUID userId, UUID teamId, TeamRole role) {
        TeamMembership teamMembership = getUserTeamMembership(userId, teamId);
        teamMembership.setRole(role);
        teamMembership.setLastActiveAt(LocalDateTime.now());
        
        logger.info("Updating role for user ID: {} in team ID: {} to {}", userId, teamId, role);
        return teamMembershipRepository.save(teamMembership);
    }

    /**
     * Update a user's active status in a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @param active the new active status
     * @return the updated team membership
     * @throws ResourceNotFoundException if the team membership is not found
     */
    @Transactional
    public TeamMembership updateUserActiveStatus(UUID userId, UUID teamId, boolean active) {
        TeamMembership teamMembership = getUserTeamMembership(userId, teamId);
        teamMembership.setActive(active);
        teamMembership.setLastActiveAt(LocalDateTime.now());
        
        logger.info("Updating active status for user ID: {} in team ID: {} to {}", userId, teamId, active);
        return teamMembershipRepository.save(teamMembership);
    }

    /**
     * Update a user's last active time in a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return the updated team membership
     * @throws ResourceNotFoundException if the team membership is not found
     */
    @Transactional
    public TeamMembership updateUserLastActiveTime(UUID userId, UUID teamId) {
        TeamMembership teamMembership = getUserTeamMembership(userId, teamId);
        teamMembership.setLastActiveAt(LocalDateTime.now());
        
        logger.info("Updating last active time for user ID: {} in team ID: {}", userId, teamId);
        return teamMembershipRepository.save(teamMembership);
    }

    /**
     * Delete a team membership.
     *
     * @param id the team membership ID
     * @return true if the team membership was deleted, false otherwise
     */
    @Transactional
    public boolean deleteTeamMembership(UUID id) {
        Optional<TeamMembership> teamMembership = teamMembershipRepository.findById(id);
        if (teamMembership.isPresent()) {
            teamMembershipRepository.delete(teamMembership.get());
            logger.info("Deleted team membership with ID: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Delete a specific membership for a user and team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return true if the team membership was deleted, false otherwise
     */
    @Transactional
    public boolean deleteUserTeamMembership(UUID userId, UUID teamId) {
        Optional<TeamMembership> teamMembership = teamMembershipRepository.findByUserIdAndTeamId(userId, teamId);
        if (teamMembership.isPresent()) {
            teamMembershipRepository.delete(teamMembership.get());
            logger.info("Deleted team membership for user ID: {} in team ID: {}", userId, teamId);
            return true;
        }
        return false;
    }

    /**
     * Delete all memberships for a user.
     *
     * @param userId the user ID
     * @return the number of deleted memberships
     */
    @Transactional
    public int deleteAllUserTeamMemberships(UUID userId) {
        int count = teamMembershipRepository.deleteByUserId(userId);
        logger.info("Deleted {} team memberships for user ID: {}", count, userId);
        return count;
    }

    /**
     * Delete all memberships for a team.
     *
     * @param teamId the team ID
     * @return the number of deleted memberships
     */
    @Transactional
    public int deleteAllTeamMemberships(UUID teamId) {
        int count = teamMembershipRepository.deleteByTeamId(teamId);
        logger.info("Deleted {} team memberships for team ID: {}", count, teamId);
        return count;
    }

    /**
     * Check if a user is a member of a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return true if the user is a member of the team, false otherwise
     */
    public boolean isUserTeamMember(UUID userId, UUID teamId) {
        return teamMembershipRepository.findByUserIdAndTeamId(userId, teamId).isPresent();
    }

    /**
     * Check if a user is an active member of a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return true if the user is an active member of the team, false otherwise
     */
    public boolean isUserActiveTeamMember(UUID userId, UUID teamId) {
        Optional<TeamMembership> teamMembership = teamMembershipRepository.findByUserIdAndTeamId(userId, teamId);
        return teamMembership.isPresent() && teamMembership.get().isActive();
    }

    /**
     * Check if a user has a specific role in a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @param role the team role
     * @return true if the user has the specified role in the team, false otherwise
     */
    public boolean hasUserRole(UUID userId, UUID teamId, TeamRole role) {
        Optional<TeamMembership> teamMembership = teamMembershipRepository.findByUserIdAndTeamId(userId, teamId);
        return teamMembership.isPresent() && teamMembership.get().getRole() == role;
    }

    /**
     * Get all recently active team members.
     *
     * @param since the time since users were last active
     * @return list of team memberships
     */
    public List<TeamMembership> getRecentlyActiveTeamMembers(LocalDateTime since) {
        return teamMembershipRepository.findByLastActiveAtAfter(since);
    }
}

