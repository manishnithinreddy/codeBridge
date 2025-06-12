package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ApiTestRequest;
import com.codebridge.apitest.dto.ApiTestResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.service.ApiTestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for API test operations.
 */
@RestController
@RequestMapping("/api/tests")
public class ApiTestController {

    private final ApiTestService apiTestService;

    @Autowired
    public ApiTestController(ApiTestService apiTestService) {
        this.apiTestService = apiTestService;
    }

    private UUID getUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            // For testing purposes, return a fixed UUID
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        
        if (authentication.getPrincipal() instanceof UserDetails) {
            return UUID.fromString(((UserDetails) authentication.getPrincipal()).getUsername());
        } else if (authentication.getPrincipal() instanceof String) {
            return UUID.fromString((String) authentication.getPrincipal());
        }
        
        // Default test user
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    /**
     * Creates a new API test.
     *
     * @param request the API test request
     * @param authentication the authentication object
     * @return the created API test
     */
    @PostMapping
    public ResponseEntity<ApiTestResponse> createTest(
            @Valid @RequestBody ApiTestRequest request,
            Authentication authentication) {
        ApiTestResponse response = apiTestService.createTest(request, getUserId(authentication));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Gets all API tests for the authenticated user.
     *
     * @param authentication the authentication object
     * @return the list of API tests
     */
    @GetMapping
    public ResponseEntity<List<ApiTestResponse>> getAllTests(Authentication authentication) {
        List<ApiTestResponse> tests = apiTestService.getAllTests(getUserId(authentication));
        return ResponseEntity.ok(tests);
    }

    /**
     * Gets an API test by ID.
     *
     * @param id the API test ID
     * @param authentication the authentication object
     * @return the API test
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiTestResponse> getTestById(
            @PathVariable UUID id,
            Authentication authentication) {
        ApiTestResponse test = apiTestService.getTestById(id, getUserId(authentication));
        return ResponseEntity.ok(test);
    }

    /**
     * Updates an API test.
     *
     * @param id the API test ID
     * @param request the API test request
     * @param authentication the authentication object
     * @return the updated API test
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiTestResponse> updateTest(
            @PathVariable UUID id,
            @Valid @RequestBody ApiTestRequest request,
            Authentication authentication) {
        ApiTestResponse response = apiTestService.updateTest(id, request, getUserId(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes an API test.
     *
     * @param id the API test ID
     * @param authentication the authentication object
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTest(
            @PathVariable UUID id,
            Authentication authentication) {
        apiTestService.deleteTest(id, getUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    /**
     * Executes an API test.
     *
     * @param id the API test ID
     * @param authentication the authentication object
     * @return the test result
     */
    @PostMapping("/{id}/run")
    public ResponseEntity<TestResultResponse> executeTest(
            @PathVariable UUID id,
            Authentication authentication) {
        TestResultResponse result = apiTestService.executeTest(id, getUserId(authentication));
        return ResponseEntity.ok(result);
    }

    /**
     * Gets all test results for an API test.
     *
     * @param id the API test ID
     * @param authentication the authentication object
     * @return the list of test results
     */
    @GetMapping("/{id}/results")
    public ResponseEntity<List<TestResultResponse>> getTestResults(
            @PathVariable UUID id,
            Authentication authentication) {
        List<TestResultResponse> results = apiTestService.getTestResults(id, getUserId(authentication));
        return ResponseEntity.ok(results);
    }
}

