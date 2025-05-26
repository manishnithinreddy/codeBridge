package com.codebridge.teams.repository;

import com.codebridge.teams.model.Team;
import com.codebridge.teams.model.TeamMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TeamMember entity operations.
 */
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    /**
     * Find all members of a specific team.
     *
     * @param team the team
     * @param pageable pagination information
     * @return a page of team members
     */
    Page<TeamMember> findByTeam(Team team, Pageable pageable);

    /**
     * Find all active members of a specific team.
     *
     * @param team the team
     * @param active the active status
     * @param pageable pagination information
     * @return a page of active team members
     */
    Page<TeamMember> findByTeamAndActive(Team team, boolean active, Pageable pageable);

    /**
     * Find all teams that a user is a member of.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return a page of team memberships
     */
    Page<TeamMember> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find all active teams that a user is a member of.
     *
     * @param userId the user ID
     * @param active the active status
     * @param pageable pagination information
     * @return a page of active team memberships
     */
    Page<TeamMember> findByUserIdAndActive(UUID userId, boolean active, Pageable pageable);

    /**
     * Find a specific team membership for a user and team.
     *
     * @param team the team
     * @param userId the user ID
     * @return the team membership if found
     */
    Optional<TeamMember> findByTeamAndUserId(Team team, UUID userId);

    /**
     * Check if a user is a member of a specific team.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return true if the user is a member of the team
     */
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Find all team IDs that a user is a member of.
     *
     * @param userId the user ID
     * @return list of team IDs
     */
    @Query("SELECT tm.team.id FROM TeamMember tm WHERE tm.userId = :userId AND tm.active = true")
    List<UUID> findTeamIdsByUserId(@Param("userId") UUID userId);

    /**
     * Find all user IDs that are members of a specific team.
     *
     * @param teamId the team ID
     * @return list of user IDs
     */
    @Query("SELECT tm.userId FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.active = true")
    List<UUID> findUserIdsByTeamId(@Param("teamId") UUID teamId);
}

