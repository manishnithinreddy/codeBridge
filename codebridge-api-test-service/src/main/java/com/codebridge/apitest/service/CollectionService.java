package com.codebridge.apitest.service;

import com.codebridge.apitest.model.Collection;
import com.codebridge.apitest.model.Project;
import com.codebridge.apitest.dto.CollectionRequest;
import com.codebridge.apitest.dto.CollectionResponse;
import com.codebridge.apitest.exception.AccessDeniedException;
import com.codebridge.apitest.exception.DuplicateResourceException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.enums.SharePermissionLevel;
import com.codebridge.apitest.repository.CollectionRepository;
import com.codebridge.apitest.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

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
    public CollectionResponse createCollection(CollectionRequest request, Long userId) {
        // Validate project if provided
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));
            
            // Check if user has edit access to the project
            SharePermissionLevel permission = projectSharingService.getEffectivePermission(project.getId(), userId);
            if (permission == null || permission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
                throw new AccessDeniedException("You don't have permission to create collections in this project");
            }
            
            // Check for duplicate name within project
            if (collectionRepository.existsByNameAndProjectId(request.getName(), project.getId())) {
                throw new DuplicateResourceException("Collection with name '" + request.getName() + "' already exists in this project");
            }
        } else {
            // Check for duplicate name for user's personal collections
            if (collectionRepository.existsByNameAndUserIdAndProjectIsNull(request.getName(), userId)) {
                throw new DuplicateResourceException("Collection with name '" + request.getName() + "' already exists in your personal collections");
            }
        }

        Collection collection = new Collection();
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        collection.setUserId(userId);
        collection.setProject(project);
        
        Collection savedCollection = collectionRepository.save(collection);
        return mapToCollectionResponse(savedCollection);
    }

    @Transactional(readOnly = true)
    public CollectionResponse getCollectionById(Long collectionId, Long userId) {
        Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection", "id", collectionId));
        
        // Check access
        verifyCollectionAccess(collection, userId);
        
        return mapToCollectionResponse(collection);
    }

    @Transactional(readOnly = true)
    public List<CollectionResponse> getAllCollections(Long userId) {
        Set<CollectionResponse> resultSet = new HashSet<>();
        
        // Add user's personal collections
        collectionRepository.findByUserId(userId).stream()
            .filter(c -> c.getProject() == null)
            .map(this::mapToCollectionResponse)
            .forEach(resultSet::add);

        projectRepository.findByUserId(userId).forEach(project ->
            collectionRepository.findByProjectId(project.getId()).stream()
                .map(this::mapToCollectionResponse)
                .forEach(resultSet::add)
        );
        
        // Add collections from projects shared with the user
        projectSharingService.listSharedProjectsForUser(userId).forEach(projectResponse -> {
            collectionRepository.findByProjectId(projectResponse.getId()).stream()
                .map(this::mapToCollectionResponse)
                .forEach(resultSet::add);
        });
        
        return resultSet.stream()
            .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
            .collect(Collectors.toList());
    }

    @Transactional
    public CollectionResponse updateCollection(Long collectionId, CollectionRequest request, Long userId) {
        Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection", "id", collectionId));
        
        // Check access
        verifyCollectionAccess(collection, userId, SharePermissionLevel.CAN_EDIT);
        
        // Check for duplicate name
        if (!collection.getName().equals(request.getName())) {
            if (collection.getProject() != null) {
                if (collectionRepository.existsByNameAndProjectIdAndIdNot(
                        request.getName(), collection.getProject().getId(), collectionId)) {
                    throw new DuplicateResourceException("Collection with name '" + request.getName() + 
                            "' already exists in this project");
                }
            } else {
                if (collectionRepository.existsByNameAndUserIdAndProjectIsNullAndIdNot(
                        request.getName(), userId, collectionId)) {
                    throw new DuplicateResourceException("Collection with name '" + request.getName() + 
                            "' already exists in your personal collections");
                }
            }
        }
        
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        
        Collection updatedCollection = collectionRepository.save(collection);
        return mapToCollectionResponse(updatedCollection);
    }

    @Transactional
    public void deleteCollection(Long collectionId, Long userId) {
        Collection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Collection", "id", collectionId));
        
        // Check access
        verifyCollectionAccess(collection, userId, SharePermissionLevel.CAN_EDIT);
        
        collectionRepository.delete(collection);
    }
    
    /**
     * Verify if a user has access to a collection with the specified permission level.
     */
    private void verifyCollectionAccess(Collection collection, Long userId, SharePermissionLevel requiredPermission) {
        // User is the owner of the collection
        if (collection.getUserId().equals(userId)) {
            return;
        }
        
        // Collection belongs to a project
        if (collection.getProject() != null) {
            SharePermissionLevel permission = projectSharingService.getEffectivePermission(
                    collection.getProject().getId(), userId);
            
            if (permission == null || permission.ordinal() < requiredPermission.ordinal()) {
                throw new AccessDeniedException("You don't have sufficient permissions for this collection");
            }
        } else {
            // Personal collection of another user
            throw new AccessDeniedException("You don't have access to this collection");
        }
    }
    
    /**
     * Verify if a user has any access to a collection.
     */
    private void verifyCollectionAccess(Collection collection, Long userId) {
        verifyCollectionAccess(collection, userId, SharePermissionLevel.CAN_VIEW);
    }
    
    private CollectionResponse mapToCollectionResponse(Collection collection) {
        CollectionResponse response = new CollectionResponse();
        response.setId(collection.getId());
        response.setName(collection.getName());
        response.setDescription(collection.getDescription());
        response.setUserId(collection.getUserId());
        
        if (collection.getProject() != null) {
            response.setProjectId(collection.getProject().getId());
            response.setProjectName(collection.getProject().getName());
        }
        
        response.setCreatedAt(collection.getCreatedAt());
        response.setUpdatedAt(collection.getUpdatedAt());
        return response;
    }
}

