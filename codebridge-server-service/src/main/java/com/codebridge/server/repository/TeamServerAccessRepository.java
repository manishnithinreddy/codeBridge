package com.codebridge.server.repository;

import com.codebridge.server.model.TeamServerAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for team-based server access.
 */
@Repository
public interface TeamServerAccessRepository extends JpaRepository<TeamServerAccess, Long> {

    /**
     * Finds all team-based access entries for a server.
     *
     * @param serverId The server ID
     * @return A list of team-based access entries
     */
    List<TeamServerAccess> findByServerId(UUID serverId);

    /**
     * Finds all team-based access entries for a team.
     *
     * @param teamId The team ID
     * @return A list of team-based access entries
     */
    List<TeamServerAccess> findByTeamId(UUID teamId);

    /**
     * Finds a team-based access entry for a server and team.
     *
     * @param serverId The server ID
     * @param teamId The team ID
     * @return The team-based access entry, if found
     */
    Optional<TeamServerAccess> findByServerIdAndTeamId(UUID serverId, UUID teamId);

    /**
     * Finds all valid team-based access entries for a server.
     * A valid entry is one that has not expired.
     *
     * @param serverId The server ID
     * @param now The current time
     * @return A list of valid team-based access entries
     */
    @Query("SELECT tsa FROM TeamServerAccess tsa WHERE tsa.serverId = :serverId AND (tsa.expiresAt IS NULL OR tsa.expiresAt > :now)")
    List<TeamServerAccess> findValidAccessByServerId(@Param("serverId") UUID serverId, @Param("now") LocalDateTime now);

    /**
     * Finds all valid team-based access entries for a team.
     * A valid entry is one that has not expired.
     *
     * @param teamId The team ID
     * @param now The current time
     * @return A list of valid team-based access entries
     */
    @Query("SELECT tsa FROM TeamServerAccess tsa WHERE tsa.teamId = :teamId AND (tsa.expiresAt IS NULL OR tsa.expiresAt > :now)")
    List<TeamServerAccess> findValidAccessByTeamId(@Param("teamId") UUID teamId, @Param("now") LocalDateTime now);

    /**
     * Finds all valid team-based access entries for a user's teams.
     * A valid entry is one that has not expired.
     *
     * @param teamIds The team IDs
     * @param now The current time
     * @return A list of valid team-based access entries
     */
    @Query("SELECT tsa FROM TeamServerAccess tsa WHERE tsa.teamId IN :teamIds AND (tsa.expiresAt IS NULL OR tsa.expiresAt > :now)")
    List<TeamServerAccess> findValidAccessByTeamIds(@Param("teamIds") List<UUID> teamIds, @Param("now") LocalDateTime now);

    /**
     * Finds all expired team-based access entries.
     * An expired entry is one that has an expiration time in the past.
     *
     * @param now The current time
     * @return A list of expired team-based access entries
     */
    @Query("SELECT tsa FROM TeamServerAccess tsa WHERE tsa.expiresAt IS NOT NULL AND tsa.expiresAt <= :now")
    List<TeamServerAccess> findExpiredAccess(@Param("now") LocalDateTime now);

    /**
     * Deletes all expired team-based access entries.
     * An expired entry is one that has an expiration time in the past.
     *
     * @param now The current time
     * @return The number of entries deleted
     */
    @Query("DELETE FROM TeamServerAccess tsa WHERE tsa.expiresAt IS NOT NULL AND tsa.expiresAt <= :now")
    int deleteExpiredAccess(@Param("now") LocalDateTime now);
}

