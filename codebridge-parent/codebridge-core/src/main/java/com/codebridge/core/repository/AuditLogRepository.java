package com.codebridge.core.repository;

import com.codebridge.core.model.AuditLog;
import com.codebridge.core.model.AuditLog.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    List<AuditLog> findByUserId(UUID userId);
    
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);
    
    List<AuditLog> findByTeamId(UUID teamId);
    
    Page<AuditLog> findByTeamId(UUID teamId, Pageable pageable);
    
    List<AuditLog> findByActionType(ActionType actionType);
    
    Page<AuditLog> findByActionType(ActionType actionType, Pageable pageable);
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
    
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByUserIdAndDateRange(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.teamId = :teamId AND a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByTeamIdAndDateRange(@Param("teamId") UUID teamId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
}

