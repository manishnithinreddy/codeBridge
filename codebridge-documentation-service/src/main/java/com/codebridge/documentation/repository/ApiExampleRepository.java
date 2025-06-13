package com.codebridge.documentation.repository;

import com.codebridge.documentation.model.ApiDocumentation;
import com.codebridge.documentation.model.ApiExample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing API examples.
 */
@Repository
public interface ApiExampleRepository extends JpaRepository<ApiExample, UUID> {

    /**
     * Find examples by documentation.
     *
     * @param documentation the API documentation
     * @return the list of API examples
     */
    List<ApiExample> findByDocumentation(ApiDocumentation documentation);

    /**
     * Find an example by documentation and operation ID.
     *
     * @param documentation the API documentation
     * @param operationId the operation ID
     * @return the API example
     */
    Optional<ApiExample> findByDocumentationAndOperationId(ApiDocumentation documentation, String operationId);

    /**
     * Find examples by documentation and path.
     *
     * @param documentation the API documentation
     * @param path the API path
     * @return the list of API examples
     */
    List<ApiExample> findByDocumentationAndPath(ApiDocumentation documentation, String path);

    /**
     * Find examples by documentation, path, and method.
     *
     * @param documentation the API documentation
     * @param path the API path
     * @param method the HTTP method
     * @return the list of API examples
     */
    List<ApiExample> findByDocumentationAndPathAndMethod(ApiDocumentation documentation, String path, String method);
}

