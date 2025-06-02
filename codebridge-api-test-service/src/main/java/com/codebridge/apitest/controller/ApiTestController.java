package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ApiTestRequest;
import com.codebridge.apitest.dto.ApiTestResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.service.ApiTestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // New import
// import org.springframework.security.core.annotation.AuthenticationPrincipal; // Replaced
// import org.springframework.security.core.userdetails.UserDetails; // Replaced
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

    /**
     * Creates a new API test.
     *
     * @param request the API test request
     * @param userDetails the authenticated user
     * @return the created API test
     */
    @PostMapping
    public ResponseEntity<ApiTestResponse> createTest(
            @Valid @RequestBody ApiTestRequest request,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        ApiTestResponse response = apiTestService.createTest(request, platformUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Gets all API tests for the authenticated user.
     *
     * @param authentication the authenticated user principal
     * @return the list of API tests
     */
    @GetMapping
    public ResponseEntity<List<ApiTestResponse>> getAllTests(Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        List<ApiTestResponse> tests = apiTestService.getAllTests(platformUserId);
        return ResponseEntity.ok(tests);
    }

    /**
     * Gets an API test by ID.
     *
     * @param id the API test ID
     * @param authentication the authenticated user principal
     * @return the API test
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiTestResponse> getTestById(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        ApiTestResponse test = apiTestService.getTestById(id, platformUserId);
        return ResponseEntity.ok(test);
    }

    /**
     * Updates an API test.
     *
     * @param id the API test ID
     * @param request the API test request
     * @param authentication the authenticated user principal
     * @return the updated API test
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiTestResponse> updateTest(
            @PathVariable UUID id,
            @Valid @RequestBody ApiTestRequest request,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        ApiTestResponse response = apiTestService.updateTest(id, request, platformUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes an API test.
     *
     * @param id the API test ID
     * @param authentication the authenticated user principal
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTest(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        apiTestService.deleteTest(id, platformUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Executes an API test.
     *
     * @param id the API test ID
     * @param authentication the authenticated user principal
     * @return the test result
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<TestResultResponse> executeTest(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        TestResultResponse result = apiTestService.executeTest(id, platformUserId);
        return ResponseEntity.ok(result);
    }

    /**
     * Gets all test results for an API test.
     *
     * @param id the API test ID
     * @param authentication the authenticated user principal
     * @return the list of test results
     */
    @GetMapping("/{id}/results")
    public ResponseEntity<List<TestResultResponse>> getTestResults(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID platformUserId = UUID.fromString(authentication.getName());
        List<TestResultResponse> results = apiTestService.getTestResults(id, platformUserId);
        return ResponseEntity.ok(results);
    }
}

