package com.codebridge.apitest.service;

import com.codebridge.apitester.model.Collection; // Corrected model import
import com.codebridge.apitester.model.Project;   // Corrected model import
import com.codebridge.apitest.dto.CollectionRequest;
import com.codebridge.apitest.dto.CollectionResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.repository.CollectionRepository; // Repository from apitest
import com.codebridge.apitest.repository.ProjectRepository;   // Repository from apitest
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final ProjectRepository projectRepository; // Added ProjectRepository

    public CollectionService(CollectionRepository collectionRepository, ProjectRepository projectRepository) {
        this.collectionRepository = collectionRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public CollectionResponse createCollection(CollectionRequest collectionRequest, UUID platformUserId) {
        // Validate project existence and ownership
        Project project = projectRepository.findByIdAndPlatformUserId(
                collectionRequest.getProjectId(), platformUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id " + collectionRequest.getProjectId() + " for this user. Cannot create collection."
                ));

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
        // This method currently assumes direct ownership for standalone collections or collections where userId matches.
        // For project-based collections, access might be via project ownership/sharing (handled in later phase).
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, platformUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id " + collectionId + " for this user."));
        return mapToCollectionResponse(collection);
    }

    @Transactional(readOnly = true)
    public List<CollectionResponse> getAllCollectionsForUser(UUID platformUserId) {
        // This lists collections directly owned by the user, not yet considering project sharing.
        return collectionRepository.findByUserId(platformUserId).stream()
                .map(this::mapToCollectionResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<CollectionResponse> getAllCollectionsForProject(UUID projectId, UUID platformUserId) {
        // Ensure user owns the project before listing its collections
        projectRepository.findByIdAndPlatformUserId(projectId, platformUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id " + projectId + " for this user. Cannot list collections."
                ));

        return collectionRepository.findByProjectId(projectId).stream()
                .map(this::mapToCollectionResponse)
                .collect(Collectors.toList());
    }


    @Transactional
    public CollectionResponse updateCollection(UUID collectionId, CollectionRequest collectionRequest, UUID platformUserId) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, platformUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id " + collectionId + " for this user, cannot update."));

        // If collection is part of a project, ensure the project ID in request matches (or handle project change)
        if (collection.getProject() != null) {
            if (!collection.getProject().getId().equals(collectionRequest.getProjectId())) {
                // This logic might need refinement: are we allowing moving collection between projects?
                // For now, let's assume projectId in request should match existing project if collection is already associated.
                // Or, if it's for associating an unassigned collection:
                Project project = projectRepository.findByIdAndPlatformUserId(
                        collectionRequest.getProjectId(), platformUserId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Project not found with id " + collectionRequest.getProjectId() + " for this user."
                        ));
                collection.setProject(project);
            }
        } else if (collectionRequest.getProjectId() != null) { // If it was a standalone collection and now gets a project
             Project project = projectRepository.findByIdAndPlatformUserId(
                        collectionRequest.getProjectId(), platformUserId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Project not found with id " + collectionRequest.getProjectId() + " for this user."
                        ));
             collection.setProject(project);
        }


        collection.setName(collectionRequest.getName());
        // collection.setDescription(collectionRequest.getDescription()); // if description is in CollectionRequest & Collection model

        Collection updatedCollection = collectionRepository.save(collection);
        return mapToCollectionResponse(updatedCollection);
    }

    @Transactional
    public void deleteCollection(UUID collectionId, UUID platformUserId) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, platformUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id " + collectionId + " for this user, cannot delete."));
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
