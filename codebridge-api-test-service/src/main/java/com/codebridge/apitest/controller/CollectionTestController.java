package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ApiTestRequest;
import com.codebridge.apitest.dto.ApiTestResponse;
import com.codebridge.apitest.service.ApiTestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for collection-specific test operations.
 */
@RestController
@RequestMapping("/api/collections/{collectionId}/tests")
public class CollectionTestController {

    private final ApiTestService apiTestService;

    public CollectionTestController(ApiTestService apiTestService) {
        this.apiTestService = apiTestService;
    }

    private UUID getUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            // For testing purposes, return a fixed UUID
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        return UUID.fromString(authentication.getName());
    }

    /**
     * Creates a new API test in a collection.
     *
     * @param collectionId the collection ID
     * @param request the API test request
     * @param authentication the authentication object
     * @return the created API test
     */
    @PostMapping
    public ResponseEntity<ApiTestResponse> createTest(
            @PathVariable UUID collectionId,
            @Valid @RequestBody ApiTestRequest request,
            Authentication authentication) {
        request.setCollectionId(collectionId);
        ApiTestResponse response = apiTestService.createTest(request, getUserId(authentication));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Gets all API tests in a collection.
     *
     * @param collectionId the collection ID
     * @param authentication the authentication object
     * @return the list of API tests
     */
    @GetMapping
    public ResponseEntity<List<ApiTestResponse>> getTestsInCollection(
            @PathVariable UUID collectionId,
            Authentication authentication) {
        List<ApiTestResponse> tests = apiTestService.getTestsInCollection(collectionId, getUserId(authentication));
        return ResponseEntity.ok(tests);
    }
}

