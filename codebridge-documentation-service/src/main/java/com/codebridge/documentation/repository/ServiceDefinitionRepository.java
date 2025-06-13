package com.codebridge.documentation.repository;

import com.codebridge.documentation.model.ServiceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing service definitions.
 */
@Repository
public interface ServiceDefinitionRepository extends JpaRepository<ServiceDefinition, UUID> {

    /**
     * Find a service by name.
     *
     * @param name the service name
     * @return the service definition
     */
    Optional<ServiceDefinition> findByName(String name);

    /**
     * Find services by enabled status.
     *
     * @param enabled the enabled status
     * @return the list of service definitions
     */
    List<ServiceDefinition> findByEnabled(boolean enabled);

    /**
     * Find services by enabled status and scan flag.
     *
     * @param enabled the enabled status
     * @param scan the scan flag
     * @return the list of service definitions
     */
    List<ServiceDefinition> findByEnabledAndScanTrue(boolean enabled);
}

