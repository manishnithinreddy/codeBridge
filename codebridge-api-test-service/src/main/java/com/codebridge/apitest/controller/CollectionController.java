package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.CollectionRequest;
import com.codebridge.apitest.dto.CollectionResponse;
import com.codebridge.apitest.dto.CollectionTestRequest;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.service.CollectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // New import
// import org.springframework.security.core.annotation.AuthenticationPrincipal; // Replaced
// import org.springframework.security.core.userdetails.UserDetails; // Replaced
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller for collection operations.
 */
@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    /**
     * Create a new collection.
     *
     * @param request the collection request
     * @param userDetails the authenticated user details
     * @return the created collection
     */
    @PostMapping
    public ResponseEntity<CollectionResponse> createCollection(
            @Valid @RequestBody CollectionRequest request,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        CollectionResponse response = collectionService.createCollection(request, platformUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all collections for the authenticated user.
     *
     * @param authentication the authenticated user principal
     * @return list of collections
     */
    @GetMapping
    public ResponseEntity<List<CollectionResponse>> getAllCollections(Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        List<CollectionResponse> collections = collectionService.getAllCollections(platformUserId);
        return ResponseEntity.ok(collections);
    }

    /**
     * Get collection by ID.
     *
     * @param id the collection ID
     * @param authentication the authenticated user principal
     * @return the collection
     */
    @GetMapping("/{id}")
    public ResponseEntity<CollectionResponse> getCollectionById(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        CollectionResponse collection = collectionService.getCollectionById(id, platformUserId);
        return ResponseEntity.ok(collection);
    }

    /**
     * Update a collection.
     *
     * @param id the collection ID
     * @param request the collection request
     * @param authentication the authenticated user principal
     * @return the updated collection
     */
    @PutMapping("/{id}")
    public ResponseEntity<CollectionResponse> updateCollection(
            @PathVariable UUID id,
            @Valid @RequestBody CollectionRequest request,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        CollectionResponse response = collectionService.updateCollection(id, request, platformUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a collection.
     *
     * @param id the collection ID
     * @param authentication the authenticated user principal
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        collectionService.deleteCollection(id, platformUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Add a test to a collection.
     *
     * @param collectionId the collection ID
     * @param request the collection test request
     * @param authentication the authenticated user principal
     * @return the updated collection
     */
    @PostMapping("/{collectionId}/tests")
    public ResponseEntity<CollectionResponse> addTestToCollection(
            @PathVariable UUID collectionId,
            @Valid @RequestBody CollectionTestRequest request,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        CollectionResponse response = collectionService.addTestToCollection(
                collectionId, request, platformUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a test from a collection.
     *
     * @param collectionId the collection ID
     * @param testId the test ID
     * @param authentication the authenticated user principal
     * @return the updated collection
     */
    @DeleteMapping("/{collectionId}/tests/{testId}")
    public ResponseEntity<CollectionResponse> removeTestFromCollection(
            @PathVariable UUID collectionId,
            @PathVariable UUID testId,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        CollectionResponse response = collectionService.removeTestFromCollection(
                collectionId, testId, platformUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Execute all tests in a collection.
     *
     * @param id the collection ID
     * @param authentication the authenticated user principal
     * @return list of test results
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<List<TestResultResponse>> executeCollection(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        List<TestResultResponse> results = collectionService.executeCollection(id, platformUserId);
        return ResponseEntity.ok(results);
    }
}

