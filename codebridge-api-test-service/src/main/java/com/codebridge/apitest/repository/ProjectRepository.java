package com.codebridge.apitest.repository;

import com.codebridge.apitest.model.Project; // Updated to use apitest.model
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndUserId(Long id, Long userId);

    List<Project> findByUserId(Long userId);

    boolean existsByNameAndUserId(String name, Long userId);
}

