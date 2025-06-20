package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.CollectionRequest;
import com.codebridge.apitest.dto.CollectionResponse;
import com.codebridge.apitest.service.CollectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            // For testing purposes, return a fixed ID
            return 1L;
        }
        return Long.parseLong(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<CollectionResponse> createCollection(@PathVariable Long projectId,
                                                               @Valid @RequestBody CollectionRequest collectionRequest,
                                                               Authentication authentication) {
        Long userId = getUserId(authentication);
        collectionRequest.setProjectId(projectId); // Set projectId from path
        CollectionResponse createdCollection = collectionService.createCollection(collectionRequest, userId);
        return new ResponseEntity<>(createdCollection, HttpStatus.CREATED);
    }

    @GetMapping("/{collectionId}")
    public ResponseEntity<CollectionResponse> getCollectionById(@PathVariable Long projectId,
                                                                @PathVariable Long collectionId,
                                                                Authentication authentication) {
        Long userId = getUserId(authentication);
        // The service 'getCollectionByIdForUser' currently checks direct ownership.
        // It will need enhancement in Phase E for shared project access.
        // For now, we also pass projectId, but the service might not use it yet for auth.
        // A check in controller:
        CollectionResponse collection = collectionService.getCollectionByIdForUser(collectionId, userId);
        if (!collection.getProjectId().equals(projectId)) {
             // Or throw ResourceNotFoundException / AccessDeniedException
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(collection);
    }

    @GetMapping
    public ResponseEntity<List<CollectionResponse>> getAllCollectionsForProject(@PathVariable Long projectId,
                                                                                Authentication authentication) {
        Long userId = getUserId(authentication);
        List<CollectionResponse> collections = collectionService.getAllCollectionsForProject(projectId, userId);
        return ResponseEntity.ok(collections);
    }

    @PutMapping("/{collectionId}")
    public ResponseEntity<CollectionResponse> updateCollection(@PathVariable Long projectId,
                                                               @PathVariable Long collectionId,
                                                               @Valid @RequestBody CollectionRequest collectionRequest,
                                                               Authentication authentication) {
        Long userId = getUserId(authentication);
        collectionRequest.setProjectId(projectId); // Ensure projectId is set for the service
        CollectionResponse updatedCollection = collectionService.updateCollection(collectionId, collectionRequest, userId);
        return ResponseEntity.ok(updatedCollection);
    }

    @DeleteMapping("/{collectionId}")
    public ResponseEntity<Void> deleteCollection(@PathVariable Long projectId,
                                                 @PathVariable Long collectionId,
                                                 Authentication authentication) {
        Long userId = getUserId(authentication);
        // The service 'deleteCollection' should ideally also verify that the collection belongs to the project,
        // or that the user has direct rights.
        // For now, basic ownership check is in service.
        collectionService.deleteCollection(collectionId, userId);
        return ResponseEntity.noContent().build();
    }
}
