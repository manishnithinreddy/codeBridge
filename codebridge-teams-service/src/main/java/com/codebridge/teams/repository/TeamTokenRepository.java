package com.codebridge.teams.repository;

import com.codebridge.teams.model.Team;
import com.codebridge.teams.model.TeamToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TeamToken entity operations.
 */
@Repository
public interface TeamTokenRepository extends JpaRepository<TeamToken, UUID> {

    /**
     * Find all tokens for a specific team.
     *
     * @param team the team
     * @return list of team tokens
     */
    List<TeamToken> findByTeam(Team team);

    /**
     * Find all active tokens for a specific team.
     *
     * @param team the team
     * @param active the active status
     * @return list of active team tokens
     */
    List<TeamToken> findByTeamAndActive(Team team, boolean active);

    /**
     * Find a token by its value.
     *
     * @param tokenValue the token value
     * @return the token if found
     */
    Optional<TeamToken> findByTokenValue(String tokenValue);

    /**
     * Find a token by its name for a specific team.
     *
     * @param team the team
     * @param name the token name
     * @return the token if found
     */
    Optional<TeamToken> findByTeamAndName(Team team, String name);

    /**
     * Find all valid tokens (active and not expired).
     *
     * @param now the current time
     * @return list of valid tokens
     */
    @Query("SELECT t FROM TeamToken t WHERE t.active = true AND (t.expiresAt IS NULL OR t.expiresAt > :now)")
    List<TeamToken> findValidTokens(@Param("now") LocalDateTime now);

    /**
     * Find all expired tokens.
     *
     * @param now the current time
     * @return list of expired tokens
     */
    @Query("SELECT t FROM TeamToken t WHERE t.active = true AND t.expiresAt IS NOT NULL AND t.expiresAt <= :now")
    List<TeamToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Validate a token (check if it exists, is active, and not expired).
     *
     * @param tokenValue the token value
     * @param now the current time
     * @return the token if valid
     */
    @Query("SELECT t FROM TeamToken t WHERE t.tokenValue = :tokenValue AND t.active = true AND (t.expiresAt IS NULL OR t.expiresAt > :now)")
    Optional<TeamToken> validateToken(@Param("tokenValue") String tokenValue, @Param("now") LocalDateTime now);
}

