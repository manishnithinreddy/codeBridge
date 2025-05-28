package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.CollectionRequest;
import com.codebridge.apitest.dto.CollectionResponse;
import com.codebridge.apitest.dto.CollectionTestRequest;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.service.CollectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
            @AuthenticationPrincipal UserDetails userDetails) {
        CollectionResponse response = collectionService.createCollection(request, UUID.fromString(userDetails.getUsername()));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all collections for the authenticated user.
     *
     * @param userDetails the authenticated user details
     * @return list of collections
     */
    @GetMapping
    public ResponseEntity<List<CollectionResponse>> getAllCollections(@AuthenticationPrincipal UserDetails userDetails) {
        List<CollectionResponse> collections = collectionService.getAllCollections(UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(collections);
    }

    /**
     * Get collection by ID.
     *
     * @param id the collection ID
     * @param userDetails the authenticated user details
     * @return the collection
     */
    @GetMapping("/{id}")
    public ResponseEntity<CollectionResponse> getCollectionById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        CollectionResponse collection = collectionService.getCollectionById(id, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(collection);
    }

    /**
     * Update a collection.
     *
     * @param id the collection ID
     * @param request the collection request
     * @param userDetails the authenticated user details
     * @return the updated collection
     */
    @PutMapping("/{id}")
    public ResponseEntity<CollectionResponse> updateCollection(
            @PathVariable UUID id,
            @Valid @RequestBody CollectionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CollectionResponse response = collectionService.updateCollection(id, request, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a collection.
     *
     * @param id the collection ID
     * @param userDetails the authenticated user details
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        collectionService.deleteCollection(id, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Add a test to a collection.
     *
     * @param collectionId the collection ID
     * @param request the collection test request
     * @param userDetails the authenticated user details
     * @return the updated collection
     */
    @PostMapping("/{collectionId}/tests")
    public ResponseEntity<CollectionResponse> addTestToCollection(
            @PathVariable UUID collectionId,
            @Valid @RequestBody CollectionTestRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CollectionResponse response = collectionService.addTestToCollection(
                collectionId, request, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a test from a collection.
     *
     * @param collectionId the collection ID
     * @param testId the test ID
     * @param userDetails the authenticated user details
     * @return the updated collection
     */
    @DeleteMapping("/{collectionId}/tests/{testId}")
    public ResponseEntity<CollectionResponse> removeTestFromCollection(
            @PathVariable UUID collectionId,
            @PathVariable UUID testId,
            @AuthenticationPrincipal UserDetails userDetails) {
        CollectionResponse response = collectionService.removeTestFromCollection(
                collectionId, testId, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(response);
    }

    /**
     * Execute all tests in a collection.
     *
     * @param id the collection ID
     * @param userDetails the authenticated user details
     * @return list of test results
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<List<TestResultResponse>> executeCollection(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<TestResultResponse> results = collectionService.executeCollection(id, UUID.fromString(userDetails.getUsername()));
        return ResponseEntity.ok(results);
    }
}

