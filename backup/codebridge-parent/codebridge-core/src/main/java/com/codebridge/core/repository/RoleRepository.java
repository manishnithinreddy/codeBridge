package com.codebridge.core.repository;

import com.codebridge.core.model.Role;
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
public interface RoleRepository extends JpaRepository<Role, UUID> {
    
    Optional<Role> findByName(String name);
    
    boolean existsByName(String name);
    
    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    List<Role> findByPermissionId(@Param("permissionId") UUID permissionId);
    
    @Query("SELECT r FROM Role r JOIN r.teamRoles utr WHERE utr.user.id = :userId AND utr.team.id = :teamId")
    List<Role> findByUserIdAndTeamId(@Param("userId") UUID userId, @Param("teamId") UUID teamId);
    
    @Query("SELECT r FROM Role r WHERE r.name LIKE %:search% OR r.description LIKE %:search%")
    Page<Role> searchRoles(@Param("search") String search, Pageable pageable);
}

