package com.codebridge.documentation.service;

import com.codebridge.documentation.model.ApiDocumentation;
import com.codebridge.documentation.model.PublishStatus;
import com.codebridge.documentation.model.PublishTarget;
import com.codebridge.documentation.repository.ApiDocumentationRepository;
import com.codebridge.documentation.repository.PublishTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for publishing API documentation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PublishingService {

    private final ApiDocumentationRepository documentationRepository;
    private final PublishTargetRepository targetRepository;
    private final StorageService storageService;

    @Value("${documentation.publishing.auto-publish:true}")
    private boolean autoPublish;

    /**
     * Scheduled task to publish documentation.
     */
    @Scheduled(fixedDelayString = "${documentation.publishing.publish-interval:86400000}")
    @Transactional
    public void publishAllDocumentation() {
        if (!autoPublish) {
            log.info("Auto-publish is disabled. Skipping scheduled publishing.");
            return;
        }

        log.info("Starting scheduled publishing of all documentation");
        List<ApiDocumentation> allDocs = documentationRepository.findAll();

        for (ApiDocumentation doc : allDocs) {
            try {
                publishDocumentation(doc);
            } catch (Exception e) {
                log.error("Error publishing documentation {}: {}", doc.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Publish documentation to all targets.
     *
     * @param documentation the API documentation to publish
     */
    @Transactional
    public void publishDocumentation(ApiDocumentation documentation) {
        log.info("Publishing documentation for service: {} version: {}", 
                documentation.getService().getName(), documentation.getVersion().getName());

        List<PublishTarget> targets = targetRepository.findByEnabled(true);
        
        for (PublishTarget target : targets) {
            try {
                publishToTarget(documentation, target);
                
                // Update documentation status
                documentation.setPublishStatus(PublishStatus.PUBLISHED);
                documentation.setLastPublishedAt(Instant.now());
                documentationRepository.save(documentation);
                
                log.info("Successfully published documentation to target: {}", target.getName());
            } catch (Exception e) {
                log.error("Error publishing documentation to target {}: {}", target.getName(), e.getMessage(), e);
                
                // Update documentation status
                documentation.setPublishStatus(PublishStatus.FAILED);
                documentation.setPublishError(e.getMessage());
                documentationRepository.save(documentation);
            }
        }
    }

    /**
     * Publish documentation to a specific target.
     *
     * @param documentation the API documentation
     * @param target the publish target
     * @throws IOException if an I/O error occurs
     */
    private void publishToTarget(ApiDocumentation documentation, PublishTarget target) throws IOException {
        switch (target.getType()) {
            case FILE_SYSTEM:
                publishToFileSystem(documentation, target);
                break;
            case S3:
                publishToS3(documentation, target);
                break;
            case GIT:
                publishToGit(documentation, target);
                break;
            case FTP:
                publishToFtp(documentation, target);
                break;
            default:
                throw new IllegalArgumentException("Unsupported publish target type: " + target.getType());
        }
    }

    /**
     * Publish documentation to the file system.
     *
     * @param documentation the API documentation
     * @param target the publish target
     * @throws IOException if an I/O error occurs
     */
    private void publishToFileSystem(ApiDocumentation documentation, PublishTarget target) throws IOException {
        String targetPath = target.getUrl();
        if (targetPath == null || targetPath.isEmpty()) {
            throw new IllegalArgumentException("Target URL (path) is required for file system publishing");
        }
        
        String serviceName = documentation.getService().getName();
        String versionName = documentation.getVersion().getName();
        
        // Create target directory
        Path serviceDir = Paths.get(targetPath, serviceName);
        Path versionDir = serviceDir.resolve(versionName);
        Files.createDirectories(versionDir);
        
        // Copy OpenAPI spec
        if (documentation.getOpenApiPath() != null) {
            Path source = Paths.get(documentation.getOpenApiPath());
            Path dest = versionDir.resolve("openapi.json");
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Copy HTML docs
        if (documentation.getHtmlPath() != null) {
            Path source = Paths.get(documentation.getHtmlPath());
            Path dest = versionDir.resolve("index.html");
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Copy Markdown docs
        if (documentation.getMarkdownPath() != null) {
            Path source = Paths.get(documentation.getMarkdownPath());
            Path dest = versionDir.resolve("README.md");
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Publish documentation to S3.
     *
     * @param documentation the API documentation
     * @param target the publish target
     */
    private void publishToS3(ApiDocumentation documentation, PublishTarget target) {
        // In a real implementation, this would use AWS SDK to upload files to S3
        log.info("S3 publishing not implemented in this version");
    }

    /**
     * Publish documentation to Git.
     *
     * @param documentation the API documentation
     * @param target the publish target
     */
    private void publishToGit(ApiDocumentation documentation, PublishTarget target) {
        // In a real implementation, this would use JGit to commit and push files to a Git repository
        log.info("Git publishing not implemented in this version");
    }

    /**
     * Publish documentation to FTP.
     *
     * @param documentation the API documentation
     * @param target the publish target
     */
    private void publishToFtp(ApiDocumentation documentation, PublishTarget target) {
        // In a real implementation, this would use Apache Commons Net to upload files via FTP
        log.info("FTP publishing not implemented in this version");
    }

    /**
     * Create a new publish target.
     *
     * @param name the target name
     * @param type the target type
     * @param url the target URL
     * @param username the username (optional)
     * @param password the password (optional)
     * @param enabled whether the target is enabled
     * @return the created publish target
     */
    @Transactional
    public PublishTarget createTarget(String name, PublishTarget.TargetType type, String url, 
                                     String username, String password, boolean enabled) {
        PublishTarget target = new PublishTarget();
        target.setName(name);
        target.setType(type);
        target.setUrl(url);
        target.setUsername(username);
        target.setPassword(password);
        target.setEnabled(enabled);
        target.setCreatedAt(Instant.now());
        target.setUpdatedAt(Instant.now());
        
        return targetRepository.save(target);
    }

    /**
     * Get all publish targets.
     *
     * @return the list of publish targets
     */
    public List<PublishTarget> getAllTargets() {
        return targetRepository.findAll();
    }

    /**
     * Get a publish target by ID.
     *
     * @param id the target ID
     * @return the publish target
     */
    public PublishTarget getTargetById(UUID id) {
        return targetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Publish target not found with ID: " + id));
    }

    /**
     * Update a publish target.
     *
     * @param id the target ID
     * @param target the updated target
     * @return the updated publish target
     */
    @Transactional
    public PublishTarget updateTarget(UUID id, PublishTarget target) {
        PublishTarget existingTarget = getTargetById(id);
        
        existingTarget.setName(target.getName());
        existingTarget.setType(target.getType());
        existingTarget.setUrl(target.getUrl());
        existingTarget.setUsername(target.getUsername());
        
        // Only update password if provided
        if (target.getPassword() != null && !target.getPassword().isEmpty()) {
            existingTarget.setPassword(target.getPassword());
        }
        
        existingTarget.setEnabled(target.isEnabled());
        existingTarget.setUpdatedAt(Instant.now());
        
        return targetRepository.save(existingTarget);
    }

    /**
     * Enable or disable a publish target.
     *
     * @param id the target ID
     * @param enabled the enabled status
     * @return the updated publish target
     */
    @Transactional
    public PublishTarget enableTarget(UUID id, boolean enabled) {
        PublishTarget target = getTargetById(id);
        target.setEnabled(enabled);
        target.setUpdatedAt(Instant.now());
        return targetRepository.save(target);
    }

    /**
     * Delete a publish target.
     *
     * @param id the target ID
     */
    @Transactional
    public void deleteTarget(UUID id) {
        targetRepository.deleteById(id);
    }
}

