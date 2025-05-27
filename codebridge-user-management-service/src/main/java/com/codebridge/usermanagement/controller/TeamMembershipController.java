package com.codebridge.usermanagement.controller;

import com.codebridge.usermanagement.model.TeamMembership;
import com.codebridge.usermanagement.model.TeamRole;
import com.codebridge.usermanagement.service.TeamMembershipService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for team membership operations.
 */
@RestController
@RequestMapping("/team-memberships")
public class TeamMembershipController {

    private static final Logger logger = LoggerFactory.getLogger(TeamMembershipController.class);

    private final TeamMembershipService teamMembershipService;

    @Autowired
    public TeamMembershipController(TeamMembershipService teamMembershipService) {
        this.teamMembershipService = teamMembershipService;
    }

    /**
     * Get all team memberships.
     *
     * @return list of team memberships
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeamMembership>> getAllTeamMemberships() {
        logger.info("Getting all team memberships");
        List<TeamMembership> memberships = teamMembershipService.getAllTeamMemberships();
        return ResponseEntity.ok(memberships);
    }

    /**
     * Get a team membership by ID.
     *
     * @param id the team membership ID
     * @return the team membership
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<TeamMembership> getTeamMembershipById(@PathVariable UUID id) {
        logger.info("Getting team membership by ID: {}", id);
        TeamMembership membership = teamMembershipService.getTeamMembershipById(id);
        return ResponseEntity.ok(membership);
    }

    /**
     * Get all memberships for a user.
     *
     * @param userId the user ID
     * @return list of team memberships
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<TeamMembership>> getUserTeamMemberships(@PathVariable UUID userId) {
        logger.info("Getting team memberships for user ID: {}", userId);
        List<TeamMembership> memberships = teamMembershipService.getUserTeamMemberships(userId);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Get all active memberships for a user.
     *
     * @param userId the user ID
     * @return list of active team memberships
     */
    @GetMapping("/user/{userId}/active")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<TeamMembership>> getUserActiveTeamMemberships(@PathVariable UUID userId) {
        logger.info("Getting active team memberships for user ID: {}", userId);
        List<TeamMembership> memberships = teamMembershipService.getUserActiveTeamMemberships(userId);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Get all memberships for a team.
     *
     * @param teamId the team ID
     * @return list of team memberships
     */
    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<TeamMembership>> getTeamMemberships(@PathVariable UUID teamId) {
        logger.info("Getting team memberships for team ID: {}", teamId);
        List<TeamMembership> memberships = teamMembershipService.getTeamMemberships(teamId);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Get all active memberships for a team.
     *
     * @param teamId the team ID
     * @return list of active team memberships
     */
    @GetMapping("/team/{teamId}/active")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<TeamMembership>> getActiveTeamMemberships(@PathVariable UUID teamId) {
        logger.info("Getting active team memberships for team ID: {}", teamId);
        List<TeamMembership> memberships = teamMembershipService.getActiveTeamMemberships(teamId);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Get all memberships for a team with a specific role.
     *
     * @param teamId the team ID
     * @param role the team role
     * @return list of team memberships
     */
    @GetMapping("/team/{teamId}/role/{role}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<TeamMembership>> getTeamMembershipsByRole(
            @PathVariable UUID teamId, @PathVariable TeamRole role) {
        logger.info("Getting team memberships for team ID: {} with role: {}", teamId, role);
        List<TeamMembership> memberships = teamMembershipService.getTeamMembershipsByRole(teamId, role);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Get a specific membership for a user and team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return the team membership
     */
    @GetMapping("/user/{userId}/team/{teamId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<TeamMembership> getUserTeamMembership(
            @PathVariable UUID userId, @PathVariable UUID teamId) {
        logger.info("Getting team membership for user ID: {} in team ID: {}", userId, teamId);
        TeamMembership membership = teamMembershipService.getUserTeamMembership(userId, teamId);
        return ResponseEntity.ok(membership);
    }

    /**
     * Create a new team membership.
     *
     * @param teamMembership the team membership to create
     * @return the created team membership
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamMembership> createTeamMembership(@Valid @RequestBody TeamMembership teamMembership) {
        logger.info("Creating team membership for user ID: {} in team ID: {}", 
                teamMembership.getUserId(), teamMembership.getTeamId());
        TeamMembership createdMembership = teamMembershipService.createTeamMembership(teamMembership);
        return new ResponseEntity<>(createdMembership, HttpStatus.CREATED);
    }

    /**
     * Update a team membership.
     *
     * @param id the team membership ID
     * @param teamMembershipDetails the team membership details
     * @return the updated team membership
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamMembership> updateTeamMembership(
            @PathVariable UUID id, @Valid @RequestBody TeamMembership teamMembershipDetails) {
        logger.info("Updating team membership with ID: {}", id);
        TeamMembership updatedMembership = teamMembershipService.updateTeamMembership(id, teamMembershipDetails);
        return ResponseEntity.ok(updatedMembership);
    }

    /**
     * Update a user's role in a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @param role the new role
     * @return the updated team membership
     */
    @PatchMapping("/user/{userId}/team/{teamId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamMembership> updateUserRole(
            @PathVariable UUID userId, @PathVariable UUID teamId, @RequestParam TeamRole role) {
        logger.info("Updating role for user ID: {} in team ID: {} to {}", userId, teamId, role);
        TeamMembership updatedMembership = teamMembershipService.updateUserRole(userId, teamId, role);
        return ResponseEntity.ok(updatedMembership);
    }

    /**
     * Update a user's active status in a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @param active the new active status
     * @return the updated team membership
     */
    @PatchMapping("/user/{userId}/team/{teamId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamMembership> updateUserActiveStatus(
            @PathVariable UUID userId, @PathVariable UUID teamId, @RequestParam boolean active) {
        logger.info("Updating active status for user ID: {} in team ID: {} to {}", userId, teamId, active);
        TeamMembership updatedMembership = teamMembershipService.updateUserActiveStatus(userId, teamId, active);
        return ResponseEntity.ok(updatedMembership);
    }

    /**
     * Update a user's last active time in a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return the updated team membership
     */
    @PatchMapping("/user/{userId}/team/{teamId}/last-active")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<TeamMembership> updateUserLastActiveTime(
            @PathVariable UUID userId, @PathVariable UUID teamId) {
        logger.info("Updating last active time for user ID: {} in team ID: {}", userId, teamId);
        TeamMembership updatedMembership = teamMembershipService.updateUserLastActiveTime(userId, teamId);
        return ResponseEntity.ok(updatedMembership);
    }

    /**
     * Delete a team membership.
     *
     * @param id the team membership ID
     * @return no content if the team membership was deleted
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTeamMembership(@PathVariable UUID id) {
        logger.info("Deleting team membership with ID: {}", id);
        boolean deleted = teamMembershipService.deleteTeamMembership(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Delete a specific membership for a user and team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return no content if the team membership was deleted
     */
    @DeleteMapping("/user/{userId}/team/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserTeamMembership(@PathVariable UUID userId, @PathVariable UUID teamId) {
        logger.info("Deleting team membership for user ID: {} in team ID: {}", userId, teamId);
        boolean deleted = teamMembershipService.deleteUserTeamMembership(userId, teamId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Delete all memberships for a user.
     *
     * @param userId the user ID
     * @return the number of deleted memberships
     */
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> deleteAllUserTeamMemberships(@PathVariable UUID userId) {
        logger.info("Deleting all team memberships for user ID: {}", userId);
        int count = teamMembershipService.deleteAllUserTeamMemberships(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Delete all memberships for a team.
     *
     * @param teamId the team ID
     * @return the number of deleted memberships
     */
    @DeleteMapping("/team/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> deleteAllTeamMemberships(@PathVariable UUID teamId) {
        logger.info("Deleting all team memberships for team ID: {}", teamId);
        int count = teamMembershipService.deleteAllTeamMemberships(teamId);
        return ResponseEntity.ok(count);
    }

    /**
     * Check if a user is a member of a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return true if the user is a member of the team, false otherwise
     */
    @GetMapping("/user/{userId}/team/{teamId}/is-member")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> isUserTeamMember(@PathVariable UUID userId, @PathVariable UUID teamId) {
        logger.info("Checking if user ID: {} is a member of team ID: {}", userId, teamId);
        boolean isMember = teamMembershipService.isUserTeamMember(userId, teamId);
        return ResponseEntity.ok(isMember);
    }

    /**
     * Check if a user is an active member of a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return true if the user is an active member of the team, false otherwise
     */
    @GetMapping("/user/{userId}/team/{teamId}/is-active")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> isUserActiveTeamMember(@PathVariable UUID userId, @PathVariable UUID teamId) {
        logger.info("Checking if user ID: {} is an active member of team ID: {}", userId, teamId);
        boolean isActive = teamMembershipService.isUserActiveTeamMember(userId, teamId);
        return ResponseEntity.ok(isActive);
    }

    /**
     * Check if a user has a specific role in a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @param role the team role
     * @return true if the user has the specified role in the team, false otherwise
     */
    @GetMapping("/user/{userId}/team/{teamId}/has-role/{role}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> hasUserRole(
            @PathVariable UUID userId, @PathVariable UUID teamId, @PathVariable TeamRole role) {
        logger.info("Checking if user ID: {} has role: {} in team ID: {}", userId, role, teamId);
        boolean hasRole = teamMembershipService.hasUserRole(userId, teamId, role);
        return ResponseEntity.ok(hasRole);
    }

    /**
     * Get all recently active team members.
     *
     * @param since the time since users were last active
     * @return list of team memberships
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeamMembership>> getRecentlyActiveTeamMembers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        logger.info("Getting recently active team members since: {}", since);
        List<TeamMembership> memberships = teamMembershipService.getRecentlyActiveTeamMembers(since);
        return ResponseEntity.ok(memberships);
    }
}

