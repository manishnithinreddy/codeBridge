package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.LoadTestRequest;
import com.codebridge.apitest.dto.LoadTestResponse;
import com.codebridge.apitest.model.LoadTest;
import com.codebridge.apitest.service.LoadTestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for load test operations.
 */
@RestController
@RequestMapping("/api/v1/load-tests")
public class LoadTestController {
    
    private final LoadTestService loadTestService;
    
    @Autowired
    public LoadTestController(LoadTestService loadTestService) {
        this.loadTestService = loadTestService;
    }
    
    /**
     * Creates a new load test.
     *
     * @param request the load test request
     * @param authentication the authentication object
     * @return the created load test
     */
    @PostMapping
    public ResponseEntity<LoadTestResponse> createLoadTest(@Valid @RequestBody LoadTestRequest request, Authentication authentication) {
        Long userId = getUserId(authentication);
        LoadTest loadTest = loadTestService.createLoadTest(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(LoadTestResponse.fromEntity(loadTest));
    }
    
    /**
     * Gets all load tests for the authenticated user.
     *
     * @param authentication the authentication object
     * @return the list of load tests
     */
    @GetMapping
    public ResponseEntity<List<LoadTestResponse>> getAllLoadTests(Authentication authentication) {
        Long userId = getUserId(authentication);
        List<LoadTest> loadTests = loadTestService.getAllLoadTests(userId);
        List<LoadTestResponse> responses = loadTests.stream()
            .map(LoadTestResponse::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Gets a load test by ID.
     *
     * @param id the load test ID
     * @param authentication the authentication object
     * @return the load test
     */
    @GetMapping("/{id}")
    public ResponseEntity<LoadTestResponse> getLoadTestById(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        LoadTest loadTest = loadTestService.getLoadTestById(id, userId);
        return ResponseEntity.ok(LoadTestResponse.fromEntity(loadTest));
    }
    
    /**
     * Executes a load test.
     *
     * @param id the load test ID
     * @param authentication the authentication object
     * @return a response indicating that the load test execution has started
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<LoadTestResponse> executeLoadTest(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        LoadTest loadTest = loadTestService.getLoadTestById(id, userId);
        loadTestService.executeLoadTest(id, userId);
        return ResponseEntity.accepted().body(LoadTestResponse.fromEntity(loadTest));
    }
    
    /**
     * Cancels a running load test.
     *
     * @param id the load test ID
     * @param authentication the authentication object
     * @return the cancelled load test
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<LoadTestResponse> cancelLoadTest(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        LoadTest loadTest = loadTestService.cancelLoadTest(id, userId);
        return ResponseEntity.ok(LoadTestResponse.fromEntity(loadTest));
    }
    
    /**
     * Deletes a load test.
     *
     * @param id the load test ID
     * @param authentication the authentication object
     * @return a response with no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoadTest(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        loadTestService.deleteLoadTest(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Gets the user ID from the authentication object.
     *
     * @param authentication the authentication object
     * @return the user ID
     */
    private Long getUserId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}

