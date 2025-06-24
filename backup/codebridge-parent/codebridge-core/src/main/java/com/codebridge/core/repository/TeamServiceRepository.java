package com.codebridge.core.repository;

import com.codebridge.core.model.TeamService;
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
public interface TeamServiceRepository extends JpaRepository<TeamService, UUID> {
    
    List<TeamService> findByTeamId(UUID teamId);
    
    Page<TeamService> findByTeamId(UUID teamId, Pageable pageable);
    
    List<TeamService> findByServiceId(UUID serviceId);
    
    Page<TeamService> findByServiceId(UUID serviceId, Pageable pageable);
    
    Optional<TeamService> findByTeamIdAndServiceId(UUID teamId, UUID serviceId);
    
    List<TeamService> findByEnabled(boolean enabled);
    
    Page<TeamService> findByEnabled(boolean enabled, Pageable pageable);
    
    @Query("SELECT ts FROM TeamService ts WHERE ts.team.id = :teamId AND ts.enabled = true ORDER BY ts.displayOrder ASC")
    List<TeamService> findEnabledServicesByTeamIdOrderByDisplayOrder(@Param("teamId") UUID teamId);
}

