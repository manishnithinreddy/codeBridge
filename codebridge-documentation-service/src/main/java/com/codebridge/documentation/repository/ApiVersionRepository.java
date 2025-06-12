package com.codebridge.documentation.repository;

import com.codebridge.documentation.model.ApiVersion;
import com.codebridge.documentation.model.ServiceDefinition;
import com.codebridge.documentation.model.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing API versions.
 */
@Repository
public interface ApiVersionRepository extends JpaRepository<ApiVersion, UUID> {

    /**
     * Find versions by service.
     *
     * @param service the service
     * @return the list of API versions
     */
    List<ApiVersion> findByService(ServiceDefinition service);

    /**
     * Find a version by service and name.
     *
     * @param service the service
     * @param name the version name
     * @return the API version
     */
    Optional<ApiVersion> findByServiceAndName(ServiceDefinition service, String name);

    /**
     * Find versions by status.
     *
     * @param status the version status
     * @return the list of API versions
     */
    List<ApiVersion> findByStatus(VersionStatus status);

    /**
     * Find versions by service and status.
     *
     * @param service the service
     * @param status the version status
     * @return the list of API versions
     */
    List<ApiVersion> findByServiceAndStatus(ServiceDefinition service, VersionStatus status);
}

