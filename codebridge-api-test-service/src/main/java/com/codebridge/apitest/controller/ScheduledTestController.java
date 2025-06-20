package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ScheduledTestRequest;
import com.codebridge.apitest.dto.ScheduledTestResponse;
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
@RequestMapping("/api/scheduled-tests")
public class ScheduledTestController {
    
    private final TestSchedulerService testSchedulerService;
    
    @Autowired
    public ScheduledTestController(TestSchedulerService testSchedulerService) {
        this.testSchedulerService = testSchedulerService;
    }
    
    /**
     * Get all scheduled tests for the authenticated user.
     *
     * @param userDetails the authenticated user details
     * @return list of scheduled test responses
     */
    @GetMapping
    public ResponseEntity<List<ScheduledTestResponse>> getScheduledTests(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        List<ScheduledTestResponse> tests = testSchedulerService.getScheduledTests(userId);
        return ResponseEntity.ok(tests);
    }
    
    /**
     * Get a scheduled test by ID.
     *
     * @param id the scheduled test ID
     * @param userDetails the authenticated user details
     * @return the scheduled test response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduledTestResponse> getScheduledTest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        ScheduledTestResponse test = testSchedulerService.getScheduledTest(id, userId);
        return ResponseEntity.ok(test);
    }
    
    /**
     * Create a new scheduled test.
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
        ScheduledTestResponse test = testSchedulerService.createScheduledTest(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(test);
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
        ScheduledTestResponse test = testSchedulerService.updateScheduledTest(id, request, userId);
        return ResponseEntity.ok(test);
    }
    
    /**
     * Activate or deactivate a scheduled test.
     *
     * @param id the scheduled test ID
     * @param active whether to activate or deactivate
     * @param userDetails the authenticated user details
     * @return the updated scheduled test response
     */
    @PatchMapping("/{id}/active")
    public ResponseEntity<ScheduledTestResponse> setScheduledTestActive(
            @PathVariable Long id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        ScheduledTestResponse test = testSchedulerService.setScheduledTestActive(id, active, userId);
        return ResponseEntity.ok(test);
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
     * Run a scheduled test immediately.
     *
     * @param id the scheduled test ID
     * @param userDetails the authenticated user details
     * @return the updated scheduled test response
     */
    @PostMapping("/{id}/run")
    public ResponseEntity<ScheduledTestResponse> runScheduledTestNow(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        ScheduledTestResponse test = testSchedulerService.runScheduledTestNow(id, userId);
        return ResponseEntity.ok(test);
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

