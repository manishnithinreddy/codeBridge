package com.codebridge.documentation.service;

import com.codebridge.documentation.model.ApiVersion;
import com.codebridge.documentation.model.ServiceDefinition;
import com.codebridge.documentation.model.VersionStatus;
import com.codebridge.documentation.repository.ApiVersionRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for managing API versioning.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VersioningService {

    private final ApiVersionRepository versionRepository;
    private final OpenApiService openApiService;

    @Value("${documentation.versioning.strategy:semantic}")
    private String versioningStrategy;

    @Value("${documentation.versioning.default-version:latest}")
    private String defaultVersion;

    private static final Pattern SEMANTIC_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([\\w.-]+))?$");

    /**
     * Create or update an API version.
     *
     * @param service the service definition
     * @param openApiSpec the OpenAPI specification
     * @return the created or updated API version
     */
    @Transactional
    public ApiVersion createOrUpdateVersion(ServiceDefinition service, String openApiSpec) {
        String versionName = extractVersionFromSpec(openApiSpec);
        if (versionName == null || versionName.isEmpty()) {
            versionName = generateVersionName();
        }

        Optional<ApiVersion> existingVersion = versionRepository.findByServiceAndName(service, versionName);

        if (existingVersion.isPresent()) {
            ApiVersion version = existingVersion.get();
            version.setUpdatedAt(Instant.now());
            return versionRepository.save(version);
        } else {
            ApiVersion version = new ApiVersion();
            version.setService(service);
            version.setName(versionName);
            version.setStatus(VersionStatus.CURRENT);
            version.setCreatedAt(Instant.now());
            version.setUpdatedAt(Instant.now());
            
            // Set previous version to deprecated if it exists
            ApiVersion latestVersion = getLatestVersion(service);
            if (latestVersion != null && !latestVersion.getName().equals(versionName)) {
                if (isNewerVersion(versionName, latestVersion.getName())) {
                    latestVersion.setStatus(VersionStatus.DEPRECATED);
                    versionRepository.save(latestVersion);
                }
            }
            
            return versionRepository.save(version);
        }
    }

    /**
     * Extract version from OpenAPI specification.
     *
     * @param openApiSpec the OpenAPI specification
     * @return the extracted version
     */
    private String extractVersionFromSpec(String openApiSpec) {
        OpenAPI openAPI = openApiService.parseOpenApiSpec(openApiSpec);
        if (openAPI != null && openAPI.getInfo() != null) {
            Info info = openAPI.getInfo();
            return info.getVersion();
        }
        return null;
    }

    /**
     * Generate a version name based on the configured strategy.
     *
     * @return the generated version name
     */
    private String generateVersionName() {
        switch (versioningStrategy.toLowerCase()) {
            case "semantic":
                return "1.0.0";
            case "date":
                return LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            case "custom":
            default:
                return defaultVersion;
        }
    }

    /**
     * Check if a version is newer than another version.
     *
     * @param version1 the first version
     * @param version2 the second version
     * @return true if version1 is newer than version2
     */
    public boolean isNewerVersion(String version1, String version2) {
        if (versioningStrategy.equalsIgnoreCase("semantic")) {
            return compareSemanticVersions(version1, version2) > 0;
        } else if (versioningStrategy.equalsIgnoreCase("date")) {
            return version1.compareTo(version2) > 0;
        } else {
            return version1.compareTo(version2) > 0;
        }
    }

    /**
     * Compare two semantic versions.
     *
     * @param version1 the first version
     * @param version2 the second version
     * @return a negative integer, zero, or a positive integer as version1 is less than, equal to, or greater than version2
     */
    private int compareSemanticVersions(String version1, String version2) {
        Matcher matcher1 = SEMANTIC_VERSION_PATTERN.matcher(version1);
        Matcher matcher2 = SEMANTIC_VERSION_PATTERN.matcher(version2);
        
        if (!matcher1.matches() || !matcher2.matches()) {
            return version1.compareTo(version2);
        }
        
        int major1 = Integer.parseInt(matcher1.group(1));
        int minor1 = Integer.parseInt(matcher1.group(2));
        int patch1 = Integer.parseInt(matcher1.group(3));
        
        int major2 = Integer.parseInt(matcher2.group(1));
        int minor2 = Integer.parseInt(matcher2.group(2));
        int patch2 = Integer.parseInt(matcher2.group(3));
        
        if (major1 != major2) {
            return major1 - major2;
        }
        
        if (minor1 != minor2) {
            return minor1 - minor2;
        }
        
        return patch1 - patch2;
    }

    /**
     * Get the latest version for a service.
     *
     * @param service the service definition
     * @return the latest API version
     */
    public ApiVersion getLatestVersion(ServiceDefinition service) {
        List<ApiVersion> versions = versionRepository.findByService(service);
        
        if (versions.isEmpty()) {
            return null;
        }
        
        if (versioningStrategy.equalsIgnoreCase("semantic")) {
            return versions.stream()
                    .max(Comparator.comparing(ApiVersion::getName, (v1, v2) -> compareSemanticVersions(v1, v2)))
                    .orElse(null);
        } else if (versioningStrategy.equalsIgnoreCase("date")) {
            return versions.stream()
                    .max(Comparator.comparing(ApiVersion::getName))
                    .orElse(null);
        } else {
            return versions.stream()
                    .max(Comparator.comparing(ApiVersion::getCreatedAt))
                    .orElse(null);
        }
    }

    /**
     * Create a default version for a service.
     *
     * @param service the service definition
     * @return the created API version
     */
    @Transactional
    public ApiVersion createDefaultVersion(ServiceDefinition service) {
        ApiVersion version = new ApiVersion();
        version.setService(service);
        version.setName(defaultVersion);
        version.setStatus(VersionStatus.CURRENT);
        version.setCreatedAt(Instant.now());
        version.setUpdatedAt(Instant.now());
        return versionRepository.save(version);
    }

    /**
     * Get all versions for a service.
     *
     * @param service the service definition
     * @return the list of API versions
     */
    public List<ApiVersion> getAllVersions(ServiceDefinition service) {
        return versionRepository.findByService(service);
    }

    /**
     * Get a version by ID.
     *
     * @param id the version ID
     * @return the API version
     */
    public ApiVersion getVersionById(UUID id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Version not found with ID: " + id));
    }

    /**
     * Get a version by service and name.
     *
     * @param service the service definition
     * @param name the version name
     * @return the API version
     */
    public ApiVersion getVersionByName(ServiceDefinition service, String name) {
        return versionRepository.findByServiceAndName(service, name)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version not found for service " + service.getName() + " and name " + name));
    }

    /**
     * Update version status.
     *
     * @param id the version ID
     * @param status the new status
     * @return the updated API version
     */
    @Transactional
    public ApiVersion updateVersionStatus(UUID id, VersionStatus status) {
        ApiVersion version = getVersionById(id);
        version.setStatus(status);
        version.setUpdatedAt(Instant.now());
        return versionRepository.save(version);
    }

    /**
     * Delete a version.
     *
     * @param id the version ID
     */
    @Transactional
    public void deleteVersion(UUID id) {
        versionRepository.deleteById(id);
    }
}

