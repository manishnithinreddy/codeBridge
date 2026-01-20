package com.codebridge.core.repository;

import com.codebridge.core.model.Permission;
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
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    
    Optional<Permission> findByName(String name);
    
    boolean existsByName(String name);
    
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId")
    List<Permission> findByRoleId(@Param("roleId") UUID roleId);
    
    @Query("SELECT p FROM Permission p JOIN p.roles r JOIN r.teamRoles utr WHERE utr.user.id = :userId AND utr.team.id = :teamId")
    List<Permission> findByUserIdAndTeamId(@Param("userId") UUID userId, @Param("teamId") UUID teamId);
    
    @Query("SELECT p FROM Permission p WHERE p.name LIKE %:search% OR p.description LIKE %:search%")
    Page<Permission> searchPermissions(@Param("search") String search, Pageable pageable);
}

