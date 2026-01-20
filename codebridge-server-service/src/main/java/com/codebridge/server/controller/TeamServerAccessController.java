package com.codebridge.server.controller;

import com.codebridge.server.dto.TeamServerAccessDTO;
import com.codebridge.server.model.TeamServerAccess;
import com.codebridge.server.service.TeamServerAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller for team-based server access.
 */
@RestController
@RequestMapping("/api/team-server-access")
public class TeamServerAccessController {

    private final TeamServerAccessService teamServerAccessService;

    @Autowired
    public TeamServerAccessController(TeamServerAccessService teamServerAccessService) {
        this.teamServerAccessService = teamServerAccessService;
    }

    /**
     * Grants access to a server for a team.
     *
     * @param request The grant access request
     * @return The created team server access
     */
    @PostMapping
    public ResponseEntity<TeamServerAccessDTO> grantAccess(@Valid @RequestBody GrantAccessRequest request) {
        TeamServerAccessDTO teamServerAccess = teamServerAccessService.grantAccess(
                request.serverId(),
                request.teamId(),
                TeamServerAccess.AccessLevel.valueOf(request.accessLevel()),
                request.createdBy(),
                request.expiresAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(teamServerAccess);
    }

    /**
     * Updates access to a server for a team.
     *
     * @param serverId The server ID
     * @param teamId The team ID
     * @param request The update access request
     * @return The updated team server access
     */
    @PutMapping("/{serverId}/{teamId}")
    public ResponseEntity<TeamServerAccessDTO> updateAccess(
            @PathVariable UUID serverId,
            @PathVariable UUID teamId,
            @Valid @RequestBody UpdateAccessRequest request) {
        
        TeamServerAccessDTO teamServerAccess = teamServerAccessService.updateAccess(
                serverId,
                teamId,
                TeamServerAccess.AccessLevel.valueOf(request.accessLevel()),
                request.expiresAt()
        );
        return ResponseEntity.ok(teamServerAccess);
    }

    /**
     * Revokes access to a server for a team.
     *
     * @param serverId The server ID
     * @param teamId The team ID
     * @return No content
     */
    @DeleteMapping("/{serverId}/{teamId}")
    public ResponseEntity<Void> revokeAccess(
            @PathVariable UUID serverId,
            @PathVariable UUID teamId) {
        
        teamServerAccessService.revokeAccess(serverId, teamId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets all team-based access entries for a server.
     *
     * @param serverId The server ID
     * @return A list of team-based access entries
     */
    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<TeamServerAccessDTO>> getAccessByServerId(@PathVariable UUID serverId) {
        List<TeamServerAccessDTO> teamServerAccess = teamServerAccessService.getAccessByServerId(serverId);
        return ResponseEntity.ok(teamServerAccess);
    }

    /**
     * Gets all team-based access entries for a team.
     *
     * @param teamId The team ID
     * @return A list of team-based access entries
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<TeamServerAccessDTO>> getAccessByTeamId(@PathVariable UUID teamId) {
        List<TeamServerAccessDTO> teamServerAccess = teamServerAccessService.getAccessByTeamId(teamId);
        return ResponseEntity.ok(teamServerAccess);
    }

    /**
     * Gets all valid team-based access entries for a server.
     * A valid entry is one that has not expired.
     *
     * @param serverId The server ID
     * @return A list of valid team-based access entries
     */
    @GetMapping("/server/{serverId}/valid")
    public ResponseEntity<List<TeamServerAccessDTO>> getValidAccessByServerId(@PathVariable UUID serverId) {
        List<TeamServerAccessDTO> teamServerAccess = teamServerAccessService.getValidAccessByServerId(serverId);
        return ResponseEntity.ok(teamServerAccess);
    }

    /**
     * Gets all valid team-based access entries for a team.
     * A valid entry is one that has not expired.
     *
     * @param teamId The team ID
     * @return A list of valid team-based access entries
     */
    @GetMapping("/team/{teamId}/valid")
    public ResponseEntity<List<TeamServerAccessDTO>> getValidAccessByTeamId(@PathVariable UUID teamId) {
        List<TeamServerAccessDTO> teamServerAccess = teamServerAccessService.getValidAccessByTeamId(teamId);
        return ResponseEntity.ok(teamServerAccess);
    }

    /**
     * Checks if a team has access to a server.
     *
     * @param serverId The server ID
     * @param teamId The team ID
     * @return True if the team has access to the server, false otherwise
     */
    @GetMapping("/{serverId}/{teamId}/check")
    public ResponseEntity<AccessCheckResponse> checkAccess(
            @PathVariable UUID serverId,
            @PathVariable UUID teamId,
            @RequestParam(required = false) String accessLevel) {
        
        boolean hasAccess;
        if (accessLevel != null) {
            hasAccess = teamServerAccessService.hasAccessLevel(
                    serverId,
                    teamId,
                    TeamServerAccess.AccessLevel.valueOf(accessLevel)
            );
        } else {
            hasAccess = teamServerAccessService.hasValidAccess(serverId, teamId);
        }
        
        return ResponseEntity.ok(new AccessCheckResponse(hasAccess));
    }

    /**
     * Request for granting access to a server for a team.
     */
    public record GrantAccessRequest(
            @NotNull UUID serverId,
            @NotNull UUID teamId,
            @NotNull String accessLevel,
            @NotNull UUID createdBy,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt
    ) {}

    /**
     * Request for updating access to a server for a team.
     */
    public record UpdateAccessRequest(
            @NotNull String accessLevel,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt
    ) {}

    /**
     * Response for checking if a team has access to a server.
     */
    public record AccessCheckResponse(boolean hasAccess) {}
}

