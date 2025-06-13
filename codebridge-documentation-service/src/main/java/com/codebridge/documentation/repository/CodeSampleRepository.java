package com.codebridge.documentation.repository;

import com.codebridge.documentation.model.ApiDocumentation;
import com.codebridge.documentation.model.CodeSample;
import com.codebridge.documentation.model.ProgrammingLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing code samples.
 */
@Repository
public interface CodeSampleRepository extends JpaRepository<CodeSample, UUID> {

    /**
     * Find code samples by documentation.
     *
     * @param documentation the API documentation
     * @return the list of code samples
     */
    List<CodeSample> findByDocumentation(ApiDocumentation documentation);

    /**
     * Find code samples by documentation and language.
     *
     * @param documentation the API documentation
     * @param language the programming language
     * @return the list of code samples
     */
    List<CodeSample> findByDocumentationAndLanguage(ApiDocumentation documentation, ProgrammingLanguage language);

    /**
     * Find a code sample by documentation, operation ID, and language.
     *
     * @param documentation the API documentation
     * @param operationId the operation ID
     * @param language the programming language
     * @return the code sample
     */
    Optional<CodeSample> findByDocumentationAndOperationIdAndLanguage(
            ApiDocumentation documentation, String operationId, ProgrammingLanguage language);

    /**
     * Find code samples by documentation and operation ID.
     *
     * @param documentation the API documentation
     * @param operationId the operation ID
     * @return the list of code samples
     */
    List<CodeSample> findByDocumentationAndOperationId(ApiDocumentation documentation, String operationId);
}

