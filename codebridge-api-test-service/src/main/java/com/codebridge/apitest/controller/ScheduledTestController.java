package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ScheduledTestRequest;
import com.codebridge.apitest.dto.ScheduledTestResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.service.TestSchedulerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for scheduled test operations.
 */
@RestController
@RequestMapping("/api/v1/scheduled-tests")
public class ScheduledTestController {

    private final TestSchedulerService testSchedulerService;

    @Autowired
    public ScheduledTestController(TestSchedulerService testSchedulerService) {
        this.testSchedulerService = testSchedulerService;
    }

    /**
     * Create a scheduled test.
     *
     * @param request the scheduled test request
     * @param userDetails the authenticated user details
     * @return the created scheduled test response
     */
    @PostMapping
    public ResponseEntity<ScheduledTestResponse> createScheduledTest(
            @Valid @RequestBody ScheduledTestRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        ScheduledTestResponse response = testSchedulerService.createScheduledTest(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a scheduled test by ID.
     *
     * @param id the scheduled test ID
     * @param userDetails the authenticated user details
     * @return the scheduled test response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduledTestResponse> getScheduledTestById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        ScheduledTestResponse response = testSchedulerService.getScheduledTestById(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all scheduled tests for the authenticated user.
     *
     * @param userDetails the authenticated user details
     * @return the list of scheduled test responses
     */
    @GetMapping
    public ResponseEntity<List<ScheduledTestResponse>> getAllScheduledTests(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        List<ScheduledTestResponse> responses = testSchedulerService.getAllScheduledTestsForUser(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Update a scheduled test.
     *
     * @param id the scheduled test ID
     * @param request the scheduled test request
     * @param userDetails the authenticated user details
     * @return the updated scheduled test response
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduledTestResponse> updateScheduledTest(
            @PathVariable Long id,
            @Valid @RequestBody ScheduledTestRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        ScheduledTestResponse response = testSchedulerService.updateScheduledTest(id, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a scheduled test.
     *
     * @param id the scheduled test ID
     * @param userDetails the authenticated user details
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheduledTest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        testSchedulerService.deleteScheduledTest(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Execute a scheduled test manually.
     *
     * @param id the scheduled test ID
     * @param userDetails the authenticated user details
     * @return the test result response
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<TestResultResponse> executeScheduledTest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        TestResultResponse result = testSchedulerService.executeScheduledTest(id, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Run a scheduled test now.
     *
     * @param id the scheduled test ID
     * @param userDetails the authenticated user details
     * @return the updated scheduled test response
     */
    @PostMapping("/{id}/run")
    public ResponseEntity<TestResultResponse> runScheduledTestNow(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        TestResultResponse result = testSchedulerService.executeScheduledTest(id, userId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Extract user ID from user details.
     *
     * @param userDetails the user details
     * @return the user ID
     */
    private Long getUserIdFromUserDetails(UserDetails userDetails) {
        // In a real application, this would extract the user ID from the UserDetails
        // For now, we'll just return a placeholder value
        return 1L;
    }
}

