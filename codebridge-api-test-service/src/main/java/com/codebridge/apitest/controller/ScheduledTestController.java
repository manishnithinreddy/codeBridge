package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ScheduledTestRequest;
import com.codebridge.apitest.dto.ScheduledTestResponse;
import com.codebridge.apitest.model.ScheduledTest;
import com.codebridge.apitest.service.TestSchedulerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
     * Creates a new scheduled test.
     *
     * @param request the scheduled test request
     * @param authentication the authentication object
     * @return the created scheduled test
     */
    @PostMapping
    public ResponseEntity<ScheduledTestResponse> createScheduledTest(@Valid @RequestBody ScheduledTestRequest request, Authentication authentication) {
        UUID userId = getUserId(authentication);
        ScheduledTest scheduledTest = testSchedulerService.createScheduledTest(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ScheduledTestResponse.fromEntity(scheduledTest));
    }
    
    /**
     * Gets all scheduled tests for the authenticated user.
     *
     * @param authentication the authentication object
     * @return the list of scheduled tests
     */
    @GetMapping
    public ResponseEntity<List<ScheduledTestResponse>> getAllScheduledTests(Authentication authentication) {
        UUID userId = getUserId(authentication);
        List<ScheduledTest> scheduledTests = testSchedulerService.getAllScheduledTests(userId);
        List<ScheduledTestResponse> responses = scheduledTests.stream()
            .map(ScheduledTestResponse::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Gets a scheduled test by ID.
     *
     * @param id the scheduled test ID
     * @param authentication the authentication object
     * @return the scheduled test
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduledTestResponse> getScheduledTestById(@PathVariable UUID id, Authentication authentication) {
        UUID userId = getUserId(authentication);
        ScheduledTest scheduledTest = testSchedulerService.getScheduledTestById(id, userId);
        return ResponseEntity.ok(ScheduledTestResponse.fromEntity(scheduledTest));
    }
    
    /**
     * Updates a scheduled test.
     *
     * @param id the scheduled test ID
     * @param request the scheduled test request
     * @param authentication the authentication object
     * @return the updated scheduled test
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduledTestResponse> updateScheduledTest(@PathVariable UUID id, @Valid @RequestBody ScheduledTestRequest request, Authentication authentication) {
        UUID userId = getUserId(authentication);
        ScheduledTest scheduledTest = testSchedulerService.updateScheduledTest(id, request, userId);
        return ResponseEntity.ok(ScheduledTestResponse.fromEntity(scheduledTest));
    }
    
    /**
     * Activates or deactivates a scheduled test.
     *
     * @param id the scheduled test ID
     * @param active whether the test should be active
     * @param authentication the authentication object
     * @return the updated scheduled test
     */
    @PatchMapping("/{id}/active")
    public ResponseEntity<ScheduledTestResponse> setScheduledTestActive(@PathVariable UUID id, @RequestParam boolean active, Authentication authentication) {
        UUID userId = getUserId(authentication);
        ScheduledTest scheduledTest = testSchedulerService.setScheduledTestActive(id, active, userId);
        return ResponseEntity.ok(ScheduledTestResponse.fromEntity(scheduledTest));
    }
    
    /**
     * Executes a scheduled test immediately.
     *
     * @param id the scheduled test ID
     * @param authentication the authentication object
     * @return the scheduled test
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<ScheduledTestResponse> executeScheduledTestNow(@PathVariable UUID id, Authentication authentication) {
        UUID userId = getUserId(authentication);
        ScheduledTest scheduledTest = testSchedulerService.executeScheduledTestNow(id, userId);
        return ResponseEntity.accepted().body(ScheduledTestResponse.fromEntity(scheduledTest));
    }
    
    /**
     * Deletes a scheduled test.
     *
     * @param id the scheduled test ID
     * @param authentication the authentication object
     * @return a response with no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheduledTest(@PathVariable UUID id, Authentication authentication) {
        UUID userId = getUserId(authentication);
        testSchedulerService.deleteScheduledTest(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Gets the user ID from the authentication object.
     *
     * @param authentication the authentication object
     * @return the user ID
     */
    private UUID getUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}

