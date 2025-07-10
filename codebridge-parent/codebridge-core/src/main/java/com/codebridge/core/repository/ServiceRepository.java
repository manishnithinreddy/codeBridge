package com.codebridge.core.repository;

import com.codebridge.core.model.Service;
import com.codebridge.core.model.Service.ServiceType;
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
public interface ServiceRepository extends JpaRepository<Service, UUID> {
    
    Optional<Service> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Service> findByType(ServiceType type);
    
    List<Service> findByEnabled(boolean enabled);
    
    Page<Service> findByEnabled(boolean enabled, Pageable pageable);
    
    @Query("SELECT s FROM Service s JOIN s.teamServices ts WHERE ts.team.id = :teamId")
    List<Service> findByTeamId(@Param("teamId") UUID teamId);
    
    @Query("SELECT s FROM Service s JOIN s.teamServices ts WHERE ts.team.id = :teamId")
    Page<Service> findByTeamId(@Param("teamId") UUID teamId, Pageable pageable);
    
    @Query("SELECT s FROM Service s WHERE s.name LIKE %:search% OR s.description LIKE %:search%")
    Page<Service> searchServices(@Param("search") String search, Pageable pageable);
}

