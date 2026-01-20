package com.codebridge.core.repository;

import com.codebridge.core.model.UserTeamRole;
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
public interface UserTeamRoleRepository extends JpaRepository<UserTeamRole, UUID> {
    
    List<UserTeamRole> findByUserId(UUID userId);
    
    Page<UserTeamRole> findByUserId(UUID userId, Pageable pageable);
    
    List<UserTeamRole> findByTeamId(UUID teamId);
    
    Page<UserTeamRole> findByTeamId(UUID teamId, Pageable pageable);
    
    List<UserTeamRole> findByRoleId(UUID roleId);
    
    Page<UserTeamRole> findByRoleId(UUID roleId, Pageable pageable);
    
    List<UserTeamRole> findByUserIdAndTeamId(UUID userId, UUID teamId);
    
    Optional<UserTeamRole> findByUserIdAndTeamIdAndRoleId(UUID userId, UUID teamId, UUID roleId);
    
    @Query("SELECT utr FROM UserTeamRole utr WHERE utr.user.id = :userId AND utr.team.id IN " +
           "(SELECT t.id FROM Team t WHERE t.id = :teamId OR t.parentTeam.id = :teamId)")
    List<UserTeamRole> findByUserIdAndTeamIdIncludingChildTeams(@Param("userId") UUID userId, @Param("teamId") UUID teamId);
    
    @Query("SELECT CASE WHEN COUNT(utr) > 0 THEN true ELSE false END FROM UserTeamRole utr " +
           "JOIN utr.role r JOIN r.permissions p " +
           "WHERE utr.user.id = :userId AND utr.team.id = :teamId AND p.name = :permission")
    boolean hasPermission(@Param("userId") UUID userId, @Param("teamId") UUID teamId, @Param("permission") String permission);
}

