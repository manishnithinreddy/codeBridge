package com.codebridge.teams.service;

import com.codebridge.teams.dto.TeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Team operations.
 */
public interface TeamService {

    /**
     * Create a new team.
     *
     * @param teamDto the team data
     * @return the created team
     */
    TeamDto createTeam(TeamDto teamDto);

    /**
     * Get a team by its ID.
     *
     * @param id the team ID
     * @return the team
     */
    TeamDto getTeamById(UUID id);

    /**
     * Get a team by its name.
     *
     * @param name the team name
     * @return the team
     */
    TeamDto getTeamByName(String name);

    /**
     * Update a team.
     *
     * @param id the team ID
     * @param teamDto the updated team data
     * @return the updated team
     */
    TeamDto updateTeam(UUID id, TeamDto teamDto);

    /**
     * Delete a team.
     *
     * @param id the team ID
     */
    void deleteTeam(UUID id);

    /**
     * Get all teams with pagination.
     *
     * @param pageable pagination information
     * @return a page of teams
     */
    Page<TeamDto> getAllTeams(Pageable pageable);

    /**
     * Get all active teams with pagination.
     *
     * @param pageable pagination information
     * @return a page of active teams
     */
    Page<TeamDto> getActiveTeams(Pageable pageable);

    /**
     * Get all teams owned by a specific user.
     *
     * @param ownerId the owner's ID
     * @param pageable pagination information
     * @return a page of teams owned by the user
     */
    Page<TeamDto> getTeamsByOwner(UUID ownerId, Pageable pageable);

    /**
     * Get all child teams of a parent team.
     *
     * @param parentId the parent team ID
     * @return list of child teams
     */
    List<TeamDto> getChildTeams(UUID parentId);

    /**
     * Search teams by name or description.
     *
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return a page of matching teams
     */
    Page<TeamDto> searchTeams(String searchTerm, Pageable pageable);

    /**
     * Check if a team with the given name exists.
     *
     * @param name the team name
     * @return true if a team with the name exists
     */
    boolean teamExistsByName(String name);

    /**
     * Activate or deactivate a team.
     *
     * @param id the team ID
     * @param active the active status
     * @return the updated team
     */
    TeamDto setTeamActive(UUID id, boolean active);
}

