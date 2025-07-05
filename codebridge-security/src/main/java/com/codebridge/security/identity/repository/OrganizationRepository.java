package com.codebridge.security.identity.repository;

import com.codebridge.security.identity.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    Optional<Organization> findByName(String name);
    
    List<Organization> findByActiveTrue();
    
    @Query("SELECT o FROM Organization o JOIN o.users u WHERE u.id = :userId")
    List<Organization> findByUserId(@Param("userId") Long userId);
    
    boolean existsByName(String name);
}

