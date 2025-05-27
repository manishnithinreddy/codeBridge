package com.codebridge.docker.repository;

import com.codebridge.docker.model.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Container entities.
 */
@Repository
public interface ContainerRepository extends JpaRepository<Container, UUID> {

    /**
     * Finds all containers by user ID.
     *
     * @param userId the user ID
     * @return the list of containers
     */
    List<Container> findByUserId(UUID userId);

    /**
     * Finds all containers by team ID.
     *
     * @param teamId the team ID
     * @return the list of containers
     */
    List<Container> findByTeamId(UUID teamId);

    /**
     * Finds a container by ID and user ID.
     *
     * @param id the container ID
     * @param userId the user ID
     * @return the container, if found
     */
    Optional<Container> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Finds a container by name and user ID.
     *
     * @param name the container name
     * @param userId the user ID
     * @return the container, if found
     */
    Optional<Container> findByNameAndUserId(String name, UUID userId);
}

