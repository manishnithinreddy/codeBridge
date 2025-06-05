package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.Project; // Updated to use apitest.model
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndPlatformUserId(UUID id, UUID platformUserId);

    List<Project> findByPlatformUserId(UUID platformUserId);

    boolean existsByNameAndPlatformUserId(String name, UUID platformUserId);
}
