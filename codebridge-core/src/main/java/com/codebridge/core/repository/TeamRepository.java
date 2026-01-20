package com.codebridge.core.repository;

import com.codebridge.core.model.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    
    Optional<Team> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Team> findByParentTeamId(UUID parentTeamId);
    
    Page<Team> findByParentTeamId(UUID parentTeamId, Pageable pageable);
    
    List<Team> findByOwnerId(UUID ownerId);
    
    Page<Team> findByOwnerId(UUID ownerId, Pageable pageable);
    
    @Query("SELECT t FROM Team t WHERE t.parentTeam IS NULL")
    List<Team> findRootTeams();
    
    @Query("SELECT t FROM Team t WHERE t.parentTeam IS NULL")
    Page<Team> findRootTeams(Pageable pageable);
    
    @Query("SELECT t FROM Team t JOIN t.members u WHERE u.id = :userId")
    List<Team> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT t FROM Team t JOIN t.members u WHERE u.id = :userId")
    Page<Team> findByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT t FROM Team t WHERE t.name LIKE %:search% OR t.description LIKE %:search%")
    Page<Team> searchTeams(@Param("search") String search, Pageable pageable);
}

