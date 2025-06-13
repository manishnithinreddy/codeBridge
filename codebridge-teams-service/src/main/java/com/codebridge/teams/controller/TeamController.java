package com.codebridge.teams.controller;

import com.codebridge.teams.dto.TeamDto;
import com.codebridge.teams.dto.TeamMemberDto;
import com.codebridge.teams.dto.TeamRequest;
import com.codebridge.teams.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing team operations.
 * Provides endpoints for team CRUD operations and member management.
 */
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * Get all teams.
     *
     * @return List of teams
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeamDto>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    /**
     * Get teams by organization ID.
     *
     * @param organizationId Organization ID
     * @return List of teams
     */
    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasRole('ADMIN') or @teamSecurity.isOrganizationMember(#organizationId)")
    public ResponseEntity<List<TeamDto>> getTeamsByOrganization(@PathVariable Long organizationId) {
        return ResponseEntity.ok(teamService.getTeamsByOrganization(organizationId));
    }

    /**
     * Get team by ID.
     *
     * @param id Team ID
     * @return Team details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @teamSecurity.isTeamMember(#id)")
    public ResponseEntity<TeamDto> getTeamById(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    /**
     * Create a new team.
     *
     * @param request Team creation request
     * @return Created team details
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<TeamDto> createTeam(@Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(teamService.createTeam(request));
    }

    /**
     * Update a team.
     *
     * @param id Team ID
     * @param request Team update request
     * @return Updated team details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @teamSecurity.isTeamAdmin(#id)")
    public ResponseEntity<TeamDto> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(teamService.updateTeam(id, request));
    }

    /**
     * Delete a team.
     *
     * @param id Team ID
     * @return Success response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @teamSecurity.isTeamAdmin(#id)")
    public ResponseEntity<?> deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all members of a team.
     *
     * @param id Team ID
     * @return List of team members
     */
    @GetMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN') or @teamSecurity.isTeamMember(#id)")
    public ResponseEntity<List<TeamMemberDto>> getTeamMembers(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamMembers(id));
    }

    /**
     * Add a user to a team.
     *
     * @param id Team ID
     * @param userId User ID
     * @param role Role in the team
     * @return Success response
     */
    @PostMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @teamSecurity.isTeamAdmin(#id)")
    public ResponseEntity<?> addTeamMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "MEMBER") String role) {
        teamService.addTeamMember(id, userId, role);
        return ResponseEntity.ok().build();
    }

    /**
     * Update a team member's role.
     *
     * @param id Team ID
     * @param userId User ID
     * @param role New role
     * @return Success response
     */
    @PutMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @teamSecurity.isTeamAdmin(#id)")
    public ResponseEntity<?> updateTeamMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam String role) {
        teamService.updateTeamMemberRole(id, userId, role);
        return ResponseEntity.ok().build();
    }

    /**
     * Remove a user from a team.
     *
     * @param id Team ID
     * @param userId User ID
     * @return Success response
     */
    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @teamSecurity.isTeamAdmin(#id)")
    public ResponseEntity<?> removeTeamMember(@PathVariable Long id, @PathVariable Long userId) {
        teamService.removeTeamMember(id, userId);
        return ResponseEntity.ok().build();
    }
}

