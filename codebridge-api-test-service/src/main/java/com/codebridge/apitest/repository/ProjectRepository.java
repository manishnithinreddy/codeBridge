package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.Project; // Updated to use apitest.model
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndPlatformUserId(Long id, Long platformUserId);

    List<Project> findByPlatformUserId(Long platformUserId);

    boolean existsByNameAndPlatformUserId(String name, Long platformUserId);
}
