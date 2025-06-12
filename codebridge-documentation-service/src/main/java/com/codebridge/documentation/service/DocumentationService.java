package com.codebridge.documentation.service;

import com.codebridge.documentation.model.ApiDocumentation;
import com.codebridge.documentation.model.ApiVersion;
import com.codebridge.documentation.model.DocumentationFormat;
import com.codebridge.documentation.model.ServiceDefinition;
import com.codebridge.documentation.repository.ApiDocumentationRepository;
import com.codebridge.documentation.repository.ApiVersionRepository;
import com.codebridge.documentation.repository.ServiceDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing API documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationService {

    private final ApiDocumentationRepository documentationRepository;
    private final ApiVersionRepository versionRepository;
    private final ServiceDefinitionRepository serviceRepository;
    private final OpenApiService openApiService;
    private final VersioningService versioningService;
    private final StorageService storageService;
    private final PublishingService publishingService;
    private final CodeGenerationService codeGenerationService;

    @Value("${documentation.openapi.scan-enabled:true}")
    private boolean scanEnabled;

    @Value("${documentation.versioning.enabled:true}")
    private boolean versioningEnabled;

    @Value("${documentation.publishing.auto-publish:true}")
    private boolean autoPublish;

    @Value("${documentation.code-generation.enabled:true}")
    private boolean codeGenerationEnabled;

    /**
     * Generate documentation for all registered services.
     */
    @Scheduled(fixedDelayString = "${documentation.openapi.scan-interval:3600000}")
    @Transactional
    public void generateDocumentation() {
        if (!scanEnabled) {
            log.info("OpenAPI scanning is disabled. Skipping documentation generation.");
            return;
        }

        log.info("Starting documentation generation for all services");
        List<ServiceDefinition> services = serviceRepository.findByEnabledAndScanTrue(true);

        for (ServiceDefinition service : services) {
            try {
                generateDocumentationForService(service);
            } catch (Exception e) {
                log.error("Error generating documentation for service {}: {}", service.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Generate documentation for a specific service.
     *
     * @param service the service definition
     * @return the generated API documentation
     */
    @Transactional
    public ApiDocumentation generateDocumentationForService(ServiceDefinition service) {
        log.info("Generating documentation for service: {}", service.getName());

        // Fetch OpenAPI specification
        String openApiSpec = openApiService.fetchOpenApiSpec(service);
        if (openApiSpec == null || openApiSpec.isEmpty()) {
            log.warn("No OpenAPI specification found for service: {}", service.getName());
            return null;
        }

        // Create or update API version
        ApiVersion version;
        if (versioningEnabled) {
            version = versioningService.createOrUpdateVersion(service, openApiSpec);
        } else {
            version = versioningService.getLatestVersion(service);
            if (version == null) {
                version = versioningService.createDefaultVersion(service);
            }
        }

        // Create or update documentation
        ApiDocumentation documentation = createOrUpdateDocumentation(service, version, openApiSpec);

        // Store documentation files
        storeDocumentationFiles(documentation);

        // Generate code samples if enabled
        if (codeGenerationEnabled) {
            codeGenerationService.generateCodeSamples(documentation);
        }

        // Auto-publish if enabled
        if (autoPublish) {
            publishingService.publishDocumentation(documentation);
        }

        return documentation;
    }

    /**
     * Create or update API documentation.
     *
     * @param service the service definition
     * @param version the API version
     * @param openApiSpec the OpenAPI specification
     * @return the created or updated API documentation
     */
    private ApiDocumentation createOrUpdateDocumentation(ServiceDefinition service, ApiVersion version, String openApiSpec) {
        Optional<ApiDocumentation> existingDoc = documentationRepository.findByServiceAndVersion(service, version);

        if (existingDoc.isPresent()) {
            ApiDocumentation doc = existingDoc.get();
            doc.setOpenApiSpec(openApiSpec);
            doc.setUpdatedAt(Instant.now());
            return documentationRepository.save(doc);
        } else {
            ApiDocumentation doc = new ApiDocumentation();
            doc.setService(service);
            doc.setVersion(version);
            doc.setOpenApiSpec(openApiSpec);
            doc.setFormat(DocumentationFormat.OPENAPI);
            doc.setCreatedAt(Instant.now());
            doc.setUpdatedAt(Instant.now());
            return documentationRepository.save(doc);
        }
    }

    /**
     * Store documentation files.
     *
     * @param documentation the API documentation
     */
    private void storeDocumentationFiles(ApiDocumentation documentation) {
        try {
            // Store OpenAPI specification
            String openApiPath = storageService.storeOpenApiSpec(documentation);
            documentation.setOpenApiPath(openApiPath);

            // Generate and store HTML documentation
            String htmlPath = storageService.generateAndStoreHtmlDocs(documentation);
            documentation.setHtmlPath(htmlPath);

            // Generate and store Markdown documentation
            String markdownPath = storageService.generateAndStoreMarkdownDocs(documentation);
            documentation.setMarkdownPath(markdownPath);

            // Update documentation with file paths
            documentationRepository.save(documentation);
        } catch (IOException e) {
            log.error("Error storing documentation files: {}", e.getMessage(), e);
        }
    }

    /**
     * Get documentation by ID.
     *
     * @param id the documentation ID
     * @return the API documentation
     */
    public ApiDocumentation getDocumentationById(UUID id) {
        return documentationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documentation not found with ID: " + id));
    }

    /**
     * Get documentation by service name and version.
     *
     * @param serviceName the service name
     * @param versionName the version name
     * @return the API documentation
     */
    public ApiDocumentation getDocumentationByServiceAndVersion(String serviceName, String versionName) {
        ServiceDefinition service = serviceRepository.findByName(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));

        ApiVersion version;
        if ("latest".equals(versionName)) {
            version = versioningService.getLatestVersion(service);
            if (version == null) {
                throw new IllegalArgumentException("No versions found for service: " + serviceName);
            }
        } else {
            version = versionRepository.findByServiceAndName(service, versionName)
                    .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionName));
        }

        return documentationRepository.findByServiceAndVersion(service, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Documentation not found for service " + serviceName + " and version " + versionName));
    }

    /**
     * Get all documentation for a service.
     *
     * @param serviceName the service name
     * @return the list of API documentation
     */
    public List<ApiDocumentation> getAllDocumentationForService(String serviceName) {
        ServiceDefinition service = serviceRepository.findByName(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceName));

        return documentationRepository.findByService(service);
    }

    /**
     * Get all documentation.
     *
     * @return the list of all API documentation
     */
    public List<ApiDocumentation> getAllDocumentation() {
        return documentationRepository.findAll();
    }

    /**
     * Delete documentation by ID.
     *
     * @param id the documentation ID
     */
    @Transactional
    public void deleteDocumentation(UUID id) {
        ApiDocumentation documentation = getDocumentationById(id);

        // Delete documentation files
        try {
            if (documentation.getOpenApiPath() != null) {
                Files.deleteIfExists(Paths.get(documentation.getOpenApiPath()));
            }
            if (documentation.getHtmlPath() != null) {
                Files.deleteIfExists(Paths.get(documentation.getHtmlPath()));
            }
            if (documentation.getMarkdownPath() != null) {
                Files.deleteIfExists(Paths.get(documentation.getMarkdownPath()));
            }
        } catch (IOException e) {
            log.error("Error deleting documentation files: {}", e.getMessage(), e);
        }

        // Delete documentation record
        documentationRepository.delete(documentation);
    }

    /**
     * Register a new service for documentation.
     *
     * @param name the service name
     * @param url the service URL
     * @param contextPath the service context path
     * @param scan whether to scan the service for documentation
     * @return the created service definition
     */
    @Transactional
    public ServiceDefinition registerService(String name, String url, String contextPath, boolean scan) {
        Optional<ServiceDefinition> existingService = serviceRepository.findByName(name);

        if (existingService.isPresent()) {
            ServiceDefinition service = existingService.get();
            service.setUrl(url);
            service.setContextPath(contextPath);
            service.setScan(scan);
            service.setEnabled(true);
            service.setUpdatedAt(Instant.now());
            return serviceRepository.save(service);
        } else {
            ServiceDefinition service = new ServiceDefinition();
            service.setName(name);
            service.setUrl(url);
            service.setContextPath(contextPath);
            service.setScan(scan);
            service.setEnabled(true);
            service.setCreatedAt(Instant.now());
            service.setUpdatedAt(Instant.now());
            return serviceRepository.save(service);
        }
    }

    /**
     * Get all registered services.
     *
     * @return the list of service definitions
     */
    public List<ServiceDefinition> getAllServices() {
        return serviceRepository.findAll();
    }

    /**
     * Get a service by name.
     *
     * @param name the service name
     * @return the service definition
     */
    public ServiceDefinition getServiceByName(String name) {
        return serviceRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + name));
    }

    /**
     * Enable or disable a service.
     *
     * @param name the service name
     * @param enabled the enabled status
     * @return the updated service definition
     */
    @Transactional
    public ServiceDefinition enableService(String name, boolean enabled) {
        ServiceDefinition service = getServiceByName(name);
        service.setEnabled(enabled);
        service.setUpdatedAt(Instant.now());
        return serviceRepository.save(service);
    }

    /**
     * Delete a service.
     *
     * @param name the service name
     */
    @Transactional
    public void deleteService(String name) {
        ServiceDefinition service = getServiceByName(name);

        // Delete all documentation for this service
        List<ApiDocumentation> docs = documentationRepository.findByService(service);
        for (ApiDocumentation doc : docs) {
            deleteDocumentation(doc.getId());
        }

        // Delete all versions for this service
        List<ApiVersion> versions = versionRepository.findByService(service);
        versionRepository.deleteAll(versions);

        // Delete service
        serviceRepository.delete(service);
    }
}

