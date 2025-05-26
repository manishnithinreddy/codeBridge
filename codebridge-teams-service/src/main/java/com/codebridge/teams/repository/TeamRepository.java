package com.codebridge.teams.repository;

import com.codebridge.teams.model.Team;
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
 * Repository for Team entity operations.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    /**
     * Find a team by its name.
     *
     * @param name the team name
     * @return the team if found
     */
    Optional<Team> findByName(String name);

    /**
     * Find all active teams.
     *
     * @param pageable pagination information
     * @return a page of active teams
     */
    Page<Team> findByActiveTrue(Pageable pageable);

    /**
     * Find all child teams of a parent team.
     *
     * @param parentTeam the parent team
     * @return list of child teams
     */
    List<Team> findByParentTeam(Team parentTeam);

    /**
     * Find all teams owned by a specific user.
     *
     * @param ownerId the owner's ID
     * @param pageable pagination information
     * @return a page of teams owned by the user
     */
    Page<Team> findByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Search teams by name or description containing the search term.
     *
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return a page of matching teams
     */
    @Query("SELECT t FROM Team t WHERE " +
            "LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Team> searchTeams(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Check if a team with the given name already exists.
     *
     * @param name the team name
     * @return true if a team with the name exists
     */
    boolean existsByName(String name);
}

