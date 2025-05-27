package com.codebridge.usermanagement.team.repository;

import com.codebridge.usermanagement.team.model.TeamMembership;
import com.codebridge.usermanagement.team.model.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TeamMembership entity operations.
 */
@Repository
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, UUID> {

    /**
     * Find all memberships for a user.
     *
     * @param userId the user ID
     * @return list of team memberships
     */
    List<TeamMembership> findByUserId(UUID userId);

    /**
     * Find all active memberships for a user.
     *
     * @param userId the user ID
     * @param isActive the active status
     * @return list of active team memberships
     */
    List<TeamMembership> findByUserIdAndIsActive(UUID userId, boolean isActive);

    /**
     * Find all memberships for a team.
     *
     * @param teamId the team ID
     * @return list of team memberships
     */
    List<TeamMembership> findByTeamId(UUID teamId);

    /**
     * Find all active memberships for a team.
     *
     * @param teamId the team ID
     * @param isActive the active status
     * @return list of active team memberships
     */
    List<TeamMembership> findByTeamIdAndIsActive(UUID teamId, boolean isActive);

    /**
     * Find all memberships for a team with a specific role.
     *
     * @param teamId the team ID
     * @param role the team role
     * @return list of team memberships
     */
    List<TeamMembership> findByTeamIdAndRole(UUID teamId, TeamRole role);

    /**
     * Find a specific membership for a user and team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return the team membership if found
     */
    Optional<TeamMembership> findByUserIdAndTeamId(UUID userId, UUID teamId);

    /**
     * Find all memberships with last active time after a specific date.
     *
     * @param dateTime the date time
     * @return list of team memberships
     */
    List<TeamMembership> findByLastActiveAtAfter(LocalDateTime dateTime);

    /**
     * Delete all memberships for a user.
     *
     * @param userId the user ID
     * @return the number of deleted memberships
     */
    int deleteByUserId(UUID userId);

    /**
     * Delete all memberships for a team.
     *
     * @param teamId the team ID
     * @return the number of deleted memberships
     */
    int deleteByTeamId(UUID teamId);
}
