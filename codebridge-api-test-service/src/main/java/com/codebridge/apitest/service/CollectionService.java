package com.codebridge.apitest.service;

import com.codebridge.apitester.model.Collection; // Corrected model import
import com.codebridge.apitester.model.Project;   // Corrected model import
import com.codebridge.apitester.model.enums.SharePermissionLevel; // Added
import com.codebridge.apitest.dto.CollectionRequest;
import com.codebridge.apitest.dto.CollectionResponse;
import com.codebridge.apitest.dto.ProjectResponse; // Added for listSharedProjectsForUser
import com.codebridge.apitest.exception.AccessDeniedException; // Added
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.repository.CollectionRepository; // Repository from apitest
import com.codebridge.apitest.repository.ProjectRepository;   // Repository from apitest
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set; // Added
import java.util.HashSet; // Added
import java.util.ArrayList; // Added
import java.util.Comparator; // Added for sorting

@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectSharingService projectSharingService; // Added

    public CollectionService(CollectionRepository collectionRepository,
                             ProjectRepository projectRepository,
                             ProjectSharingService projectSharingService) { // Added
        this.collectionRepository = collectionRepository;
        this.projectRepository = projectRepository;
        this.projectSharingService = projectSharingService; // Added
    }

    @Transactional
    public CollectionResponse createCollection(CollectionRequest collectionRequest, UUID platformUserId) {
        UUID projectId = collectionRequest.getProjectId();
        SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(projectId, platformUserId);

        if (effectivePermission == null || effectivePermission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to add collections to project " + projectId);
        }

        // Fetch project after permission check
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));

        Collection collection = new Collection();
        collection.setName(collectionRequest.getName());
        // collection.setDescription(collectionRequest.getDescription()); // Assuming description is still wanted
        collection.setUserId(platformUserId); // platformUserId is the owner of this collection instance
        collection.setProject(project); // Associate with the project

        // Other fields from CollectionRequest like variables, scripts, shared can be set here if they are kept
        // For this phase, focusing on project association.

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
            if (effectivePermission == null) { // Needs at least VIEW_ONLY
                throw new ResourceNotFoundException("Collection not found or access denied to its project.");
            }
        } else { // Standalone collection
            if (!collection.getUserId().equals(platformUserId)) {
                throw new ResourceNotFoundException("Collection not found or access denied.");
            }
        }
        return mapToCollectionResponse(collection);
    }

    @Transactional(readOnly = true)
    public List<CollectionResponse> getAllCollectionsForUser(UUID platformUserId) {
        Set<CollectionResponse> resultSet = new HashSet<>();

        // Directly owned standalone collections (project is null)
        collectionRepository.findByUserId(platformUserId).stream()
            .filter(c -> c.getProject() == null)
            .map(this::mapToCollectionResponse)
            .forEach(resultSet::add);

        // Collections from owned projects
        projectRepository.findByPlatformUserId(platformUserId).forEach(project ->
            collectionRepository.findByProjectId(project.getId()).stream()
                .map(this::mapToCollectionResponse)
                .forEach(resultSet::add)
        );

        // Collections from shared projects
        List<ProjectResponse> sharedProjects = projectSharingService.listSharedProjectsForUser(platformUserId);
        for (ProjectResponse sharedProject : sharedProjects) {
            SharePermissionLevel effectivePermission = projectSharingService.getEffectivePermission(sharedProject.getId(), platformUserId);
            if (effectivePermission != null) { // Any permission level allows viewing collections
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
        if (effectivePermission == null) { // Needs at least VIEW_ONLY
            throw new AccessDeniedException("User does not have permission to view collections in project " + projectId);
        }
        // User has some permission, allow fetching collections for the project
        projectRepository.findById(projectId) // Ensure project exists, though permission check implies it
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
            // If projectId in request differs, it implies moving the collection.
            // This logic needs careful consideration: should user have CAN_EDIT on both old and new project?
            // For now, assume if collectionRequest.projectId is set and different, it's an attempt to move.
            // The current logic in controller sets collectionRequest.projectId from path, so it should match collection.getProject().getId().
            // If we want to support moving, the controller and this service method need more advanced logic.
            // Sticking to updating within the same project for now.
            if (collectionRequest.getProjectId() != null && !collectionRequest.getProjectId().equals(projectId)) {
                 throw new IllegalArgumentException("Moving collections between projects is not supported via this method.");
            }

        } else { // Standalone collection
            if (!collection.getUserId().equals(platformUserId)) {
                throw new AccessDeniedException("User does not have permission to update this standalone collection.");
            }
            // Potentially associating a standalone collection with a project
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
        // collection.setDescription(collectionRequest.getDescription()); // if description is in CollectionRequest & Collection model

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
            if (effectivePermission == null || effectivePermission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) { // Or a more specific CAN_DELETE_COLLECTION if exists
                throw new AccessDeniedException("User does not have permission to delete collections in project " + projectId);
            }
        } else { // Standalone collection
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
        // response.setDescription(collection.getDescription()); // If description is present
        response.setCreatedAt(collection.getCreatedAt());
        response.setUpdatedAt(collection.getUpdatedAt());
        // response.setUserId(collection.getUserId()); // Typically not exposed directly if platformUserId is implicit

        if (collection.getProject() != null) {
            response.setProjectId(collection.getProject().getId());
            response.setProjectName(collection.getProject().getName());
        }
        // Other fields from the more complex CollectionResponse can be mapped here if kept (variables, scripts, tests etc.)
        return response;
    }
}
