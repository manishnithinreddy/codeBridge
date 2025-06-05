package com.codebridge.apitest.service;

import com.codebridge.apitest.model.Collection;
import com.codebridge.apitest.model.Project;
import com.codebridge.apitest.model.enums.SharePermissionLevel;
import com.codebridge.apitest.dto.CollectionRequest;
import com.codebridge.apitest.dto.CollectionResponse;
import com.codebridge.apitest.dto.ProjectResponse;
import com.codebridge.apitest.exception.AccessDeniedException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.repository.CollectionRepository;
import com.codebridge.apitest.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;

@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectSharingService projectSharingService;

    public CollectionService(CollectionRepository collectionRepository,
                             ProjectRepository projectRepository,
                             ProjectSharingService projectSharingService) {
        this.collectionRepository = collectionRepository;
        this.projectRepository = projectRepository;
        this.projectSharingService = projectSharingService;
    }

    @Transactional
    public CollectionResponse createCollection(CollectionRequest collectionRequest, UUID platformUserId) {
        UUID projectId = collectionRequest.getProjectId();
        SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(projectId, platformUserId);

        if (effectivePermission == null || effectivePermission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to add collections to project " + projectId);
        }

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));

        Collection collection = new Collection();
        collection.setName(collectionRequest.getName());
        collection.setUserId(platformUserId);
        collection.setProject(project);

        Collection savedCollection = collectionRepository.save(collection);
        return mapToCollectionResponse(savedCollection);
    }

    @Transactional(readOnly = true)
    public CollectionResponse getCollectionByIdForUser(UUID collectionId, UUID platformUserId) {
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id " + collectionId));

        if (collection.getProject() != null) {
            UUID projectId = collection.getProject().getId();
            SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(projectId, platformUserId);
            if (effectivePermission == null) {
                throw new ResourceNotFoundException("Collection not found or access denied to its project.");
            }
        } else {
            if (!collection.getUserId().equals(platformUserId)) {
                throw new ResourceNotFoundException("Collection not found or access denied.");
            }
        }
        return mapToCollectionResponse(collection);
    }

    @Transactional(readOnly = true)
    public List<CollectionResponse> getAllCollectionsForUser(UUID platformUserId) {
        Set<CollectionResponse> resultSet = new HashSet<>();

        collectionRepository.findByUserId(platformUserId).stream()
            .filter(c -> c.getProject() == null)
            .map(this::mapToCollectionResponse)
            .forEach(resultSet::add);

        projectRepository.findByPlatformUserId(platformUserId).forEach(project ->
            collectionRepository.findByProjectId(project.getId()).stream()
                .map(this::mapToCollectionResponse)
                .forEach(resultSet::add)
        );

        List<ProjectResponse> sharedProjects = projectSharingService.listSharedProjectsForUser(platformUserId);
        for (ProjectResponse sharedProject : sharedProjects) {
            SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(sharedProject.getId(), platformUserId);
            if (effectivePermission != null) {
                collectionRepository.findByProjectId(sharedProject.getId()).stream()
                    .map(this::mapToCollectionResponse)
                    .forEach(resultSet::add);
            }
        }

        List<CollectionResponse> finalResult = new ArrayList<>(resultSet);
        finalResult.sort(Comparator.comparing(CollectionResponse::getName, String.CASE_INSENSITIVE_ORDER));
        return finalResult;
    }

    @Transactional(readOnly = true)
    public List<CollectionResponse> getAllCollectionsForProject(UUID projectId, UUID platformUserId) {
        SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(projectId, platformUserId);
        if (effectivePermission == null) {
            throw new AccessDeniedException("User does not have permission to view collections in project " + projectId);
        }
        projectRepository.findById(projectId)
             .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));

        return collectionRepository.findByProjectId(projectId).stream()
                .map(this::mapToCollectionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CollectionResponse updateCollection(UUID collectionId, CollectionRequest collectionRequest, UUID platformUserId) {
        Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id " + collectionId));

        if (collection.getProject() != null) {
            UUID projectId = collection.getProject().getId();
            SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(projectId, platformUserId);
            if (effectivePermission == null || effectivePermission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
                throw new AccessDeniedException("User does not have permission to update collections in project " + projectId);
            }
            if (collectionRequest.getProjectId() != null && !collectionRequest.getProjectId().equals(projectId)) {
                throw new IllegalArgumentException("Moving collections between projects is not supported via this method.");
            }
        } else {
            if (!collection.getUserId().equals(platformUserId)) {
                throw new AccessDeniedException("User does not have permission to update this standalone collection.");
            }
            if (collectionRequest.getProjectId() != null) {
                SharePermissionLevel projectAccess = projectSharingService.getEffectivePermission(collectionRequest.getProjectId(), platformUserId);
                if (projectAccess == null || projectAccess.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
                    throw new AccessDeniedException("User does not have permission to move this collection to project " + collectionRequest.getProjectId());
                }
                Project project = projectRepository.findById(collectionRequest.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target project not found with id " + collectionRequest.getProjectId()));
                collection.setProject(project);
            }
        }
        collection.setName(collectionRequest.getName());

        Collection updatedCollection = collectionRepository.save(collection);
        return mapToCollectionResponse(updatedCollection);
    }

    @Transactional
    public void deleteCollection(UUID collectionId, UUID platformUserId) {
        Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id " + collectionId));

        if (collection.getProject() != null) {
            UUID projectId = collection.getProject().getId();
            SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(projectId, platformUserId);
            if (effectivePermission == null || effectivePermission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
                throw new AccessDeniedException("User does not have permission to delete collections in project " + projectId);
            }
        } else {
            if (!collection.getUserId().equals(platformUserId)) {
                throw new AccessDeniedException("User does not have permission to delete this standalone collection.");
            }
        }
        collectionRepository.delete(collection);
    }

    private CollectionResponse mapToCollectionResponse(Collection collection) {
        if (collection == null) {
            return null;
        }
        CollectionResponse response = new CollectionResponse();
        response.setId(collection.getId());
        response.setName(collection.getName());
        response.setCreatedAt(collection.getCreatedAt());
        response.setUpdatedAt(collection.getUpdatedAt());
        
        if (collection.getProject() != null) {
            response.setProjectId(collection.getProject().getId());
            response.setProjectName(collection.getProject().getName());
        }
        
        return response;
    }
}
