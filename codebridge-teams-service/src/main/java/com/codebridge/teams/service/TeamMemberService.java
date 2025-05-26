package com.codebridge.teams.service;

import com.codebridge.teams.dto.TeamMemberDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for TeamMember operations.
 */
public interface TeamMemberService {

    /**
     * Add a user to a team.
     *
     * @param teamMemberDto the team member data
     * @return the created team membership
     */
    TeamMemberDto addMemberToTeam(TeamMemberDto teamMemberDto);

    /**
     * Get a team membership by its ID.
     *
     * @param id the team membership ID
     * @return the team membership
     */
    TeamMemberDto getTeamMemberById(UUID id);

    /**
     * Update a team membership.
     *
     * @param id the team membership ID
     * @param teamMemberDto the updated team membership data
     * @return the updated team membership
     */
    TeamMemberDto updateTeamMember(UUID id, TeamMemberDto teamMemberDto);

    /**
     * Remove a user from a team.
     *
     * @param id the team membership ID
     */
    void removeTeamMember(UUID id);

    /**
     * Get all members of a specific team with pagination.
     *
     * @param teamId the team ID
     * @param pageable pagination information
     * @return a page of team members
     */
    Page<TeamMemberDto> getTeamMembers(UUID teamId, Pageable pageable);

    /**
     * Get all active members of a specific team with pagination.
     *
     * @param teamId the team ID
     * @param pageable pagination information
     * @return a page of active team members
     */
    Page<TeamMemberDto> getActiveTeamMembers(UUID teamId, Pageable pageable);

    /**
     * Get all teams that a user is a member of with pagination.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return a page of team memberships
     */
    Page<TeamMemberDto> getUserTeams(UUID userId, Pageable pageable);

    /**
     * Get all active teams that a user is a member of with pagination.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return a page of active team memberships
     */
    Page<TeamMemberDto> getUserActiveTeams(UUID userId, Pageable pageable);

    /**
     * Check if a user is a member of a specific team.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return true if the user is a member of the team
     */
    boolean isUserTeamMember(UUID teamId, UUID userId);

    /**
     * Get all team IDs that a user is a member of.
     *
     * @param userId the user ID
     * @return list of team IDs
     */
    List<UUID> getUserTeamIds(UUID userId);

    /**
     * Get all user IDs that are members of a specific team.
     *
     * @param teamId the team ID
     * @return list of user IDs
     */
    List<UUID> getTeamUserIds(UUID teamId);

    /**
     * Activate or deactivate a team membership.
     *
     * @param id the team membership ID
     * @param active the active status
     * @return the updated team membership
     */
    TeamMemberDto setTeamMemberActive(UUID id, boolean active);
}

