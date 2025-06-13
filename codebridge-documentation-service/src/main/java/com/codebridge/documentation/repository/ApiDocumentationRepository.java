package com.codebridge.documentation.repository;

import com.codebridge.documentation.model.ApiDocumentation;
import com.codebridge.documentation.model.ApiVersion;
import com.codebridge.documentation.model.ServiceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing API documentation.
 */
@Repository
public interface ApiDocumentationRepository extends JpaRepository<ApiDocumentation, UUID> {

    /**
     * Find documentation by service.
     *
     * @param service the service
     * @return the list of API documentation
     */
    List<ApiDocumentation> findByService(ServiceDefinition service);

    /**
     * Find documentation by version.
     *
     * @param version the API version
     * @return the list of API documentation
     */
    List<ApiDocumentation> findByVersion(ApiVersion version);

    /**
     * Find documentation by service and version.
     *
     * @param service the service
     * @param version the API version
     * @return the API documentation
     */
    Optional<ApiDocumentation> findByServiceAndVersion(ServiceDefinition service, ApiVersion version);
}

