package com.codebridge.teams.repository;

import com.codebridge.teams.model.Team;
import com.codebridge.teams.model.TeamService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TeamService entity operations.
 */
@Repository
public interface TeamServiceRepository extends JpaRepository<TeamService, UUID> {

    /**
     * Find all service associations for a specific team.
     *
     * @param team the team
     * @return list of team service associations
     */
    List<TeamService> findByTeam(Team team);

    /**
     * Find all active service associations for a specific team.
     *
     * @param team the team
     * @param active the active status
     * @return list of active team service associations
     */
    List<TeamService> findByTeamAndActive(Team team, boolean active);

    /**
     * Find a specific service association for a team and service.
     *
     * @param team the team
     * @param serviceId the service ID
     * @return the team service association if found
     */
    Optional<TeamService> findByTeamAndServiceId(Team team, UUID serviceId);

    /**
     * Check if a team has access to a specific service.
     *
     * @param teamId the team ID
     * @param serviceId the service ID
     * @return true if the team has access to the service
     */
    boolean existsByTeamIdAndServiceIdAndActive(UUID teamId, UUID serviceId, boolean active);

    /**
     * Find all service IDs that a team has access to.
     *
     * @param teamId the team ID
     * @return list of service IDs
     */
    @Query("SELECT ts.serviceId FROM TeamService ts WHERE ts.team.id = :teamId AND ts.active = true")
    List<UUID> findServiceIdsByTeamId(@Param("teamId") UUID teamId);

    /**
     * Find all team IDs that have access to a specific service.
     *
     * @param serviceId the service ID
     * @return list of team IDs
     */
    @Query("SELECT ts.team.id FROM TeamService ts WHERE ts.serviceId = :serviceId AND ts.active = true")
    List<UUID> findTeamIdsByServiceId(@Param("serviceId") UUID serviceId);
}

