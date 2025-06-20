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
import java.util.Map;

/**
 * Controller for API test operations.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tests")
public class ApiTestController {

    private final ApiTestService apiTestService;

    @Autowired
    public ApiTestController(ApiTestService apiTestService) {
        this.apiTestService = apiTestService;
    }

    /**
     * Get all API tests for a project.
     *
     * @param projectId the project ID
     * @return list of API test responses
     */
    @GetMapping
    public ResponseEntity<List<ApiTestResponse>> getApiTests(@PathVariable Long projectId) {
        List<ApiTestResponse> tests = apiTestService.getApiTests(projectId);
        return ResponseEntity.ok(tests);
    }

    /**
     * Get an API test by ID.
     *
     * @param projectId the project ID
     * @param testId the test ID
     * @return the API test response
     */
    @GetMapping("/{testId}")
    public ResponseEntity<ApiTestResponse> getApiTest(
            @PathVariable Long projectId,
            @PathVariable Long testId) {
        ApiTestResponse test = apiTestService.getApiTest(testId, projectId);
        return ResponseEntity.ok(test);
    }

    /**
     * Create a new API test.
     *
     * @param projectId the project ID
     * @param request the API test request
     * @return the created API test response
     */
    @PostMapping
    public ResponseEntity<ApiTestResponse> createApiTest(
            @PathVariable Long projectId,
            @Valid @RequestBody ApiTestRequest request) {
        ApiTestResponse test = apiTestService.createApiTest(request, projectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(test);
    }

    /**
     * Update an API test.
     *
     * @param projectId the project ID
     * @param testId the test ID
     * @param request the API test request
     * @return the updated API test response
     */
    @PutMapping("/{testId}")
    public ResponseEntity<ApiTestResponse> updateApiTest(
            @PathVariable Long projectId,
            @PathVariable Long testId,
            @Valid @RequestBody ApiTestRequest request) {
        ApiTestResponse test = apiTestService.updateApiTest(testId, request, projectId);
        return ResponseEntity.ok(test);
    }

    /**
     * Delete an API test.
     *
     * @param projectId the project ID
     * @param testId the test ID
     * @return no content response
     */
    @DeleteMapping("/{testId}")
    public ResponseEntity<Void> deleteApiTest(
            @PathVariable Long projectId,
            @PathVariable Long testId) {
        apiTestService.deleteApiTest(testId, projectId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Execute an API test.
     *
     * @param projectId the project ID
     * @param testId the test ID
     * @param environmentId the environment ID
     * @param variables additional variables
     * @param userDetails the authenticated user details
     * @return the test result response
     */
    @PostMapping("/{testId}/execute")
    public ResponseEntity<TestResultResponse> executeTest(
            @PathVariable Long projectId,
            @PathVariable Long testId,
            @RequestParam(required = false) Long environmentId,
            @RequestBody(required = false) Map<String, String> variables,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        TestResultResponse result = apiTestService.executeTest(testId, projectId, userId, environmentId, variables);
        return ResponseEntity.ok(result);
    }

    /**
     * Extract user ID from user details.
     *
     * @param userDetails the user details
     * @return the user ID
     */
    private Long getUserIdFromUserDetails(UserDetails userDetails) {
        // Implementation would extract the user ID from the UserDetails object
        // This is a placeholder for the actual implementation
        return 1L;
    }
}

