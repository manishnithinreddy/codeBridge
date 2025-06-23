package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.exception.AccessDeniedException;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.exception.TestExecutionException;
import com.codebridge.apitest.model.ApiTest;
import com.codebridge.apitest.model.TestChain;
import com.codebridge.apitest.model.TestResult;
import com.codebridge.apitest.model.TestStatus;
import com.codebridge.apitest.model.enums.SharePermissionLevel;
import com.codebridge.apitest.repository.ApiTestRepository;
import com.codebridge.apitest.repository.TestChainRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for test chain operations.
 */
@Service
public class TestChainService {

    private static final Logger logger = LoggerFactory.getLogger(TestChainService.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private final TestChainRepository testChainRepository;
    private final ApiTestRepository apiTestRepository;
    private final ApiTestService apiTestService;
    private final ProjectSharingService projectSharingService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TestChainService(TestChainRepository testChainRepository,
                           ApiTestRepository apiTestRepository,
                           ApiTestService apiTestService,
                           ProjectSharingService projectSharingService,
                           ObjectMapper objectMapper) {
        this.testChainRepository = testChainRepository;
        this.apiTestRepository = apiTestRepository;
        this.apiTestService = apiTestService;
        this.projectSharingService = projectSharingService;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all test chains for a project.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @return the list of test chains
     */
    public List<TestChain> getAllTestChains(Long projectId, Long userId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId, SharePermissionLevel.CAN_VIEW);
        
        return testChainRepository.findByProjectId(projectId);
    }

    /**
     * Get a test chain by ID.
     *
     * @param chainId the chain ID
     * @param projectId the project ID
     * @param userId the user ID
     * @return the test chain
     */
    public TestChain getTestChainById(Long chainId, Long projectId, Long userId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId, SharePermissionLevel.CAN_VIEW);
        
        TestChain chain = testChainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("TestChain", "id", chainId.toString()));
        
        if (!chain.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("You do not have access to this test chain");
        }
        
        return chain;
    }

    /**
     * Create a new test chain.
     *
     * @param chain the test chain to create
     * @param userId the user ID
     * @return the created test chain
     */
    @Transactional
    public TestChain createTestChain(TestChain chain, Long userId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(chain.getProjectId(), userId, SharePermissionLevel.CAN_EDIT);
        
        // Validate test steps
        validateTestSteps(chain);
        
        return testChainRepository.save(chain);
    }

    /**
     * Update a test chain.
     *
     * @param chainId the chain ID
     * @param updatedChain the updated test chain
     * @param projectId the project ID
     * @param userId the user ID
     * @return the updated test chain
     */
    @Transactional
    public TestChain updateTestChain(Long chainId, TestChain updatedChain, Long projectId, Long userId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId, SharePermissionLevel.CAN_EDIT);
        
        TestChain chain = testChainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("TestChain", "id", chainId.toString()));
        
        if (!chain.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("You do not have access to this test chain");
        }
        
        // Update fields
        chain.setName(updatedChain.getName());
        chain.setDescription(updatedChain.getDescription());
        chain.setSteps(updatedChain.getSteps());
        chain.setVariables(updatedChain.getVariables());
        
        // Validate test steps
        validateTestSteps(chain);
        
        return testChainRepository.save(chain);
    }

    /**
     * Delete a test chain.
     *
     * @param chainId the chain ID
     * @param projectId the project ID
     * @param userId the user ID
     */
    @Transactional
    public void deleteTestChain(Long chainId, Long projectId, Long userId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId, SharePermissionLevel.CAN_EDIT);
        
        TestChain chain = testChainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("TestChain", "id", chainId.toString()));
        
        if (!chain.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("You do not have access to this test chain");
        }
        
        testChainRepository.delete(chain);
    }

    /**
     * Validate test steps in a chain.
     *
     * @param chain the test chain to validate
     */
    private void validateTestSteps(TestChain chain) {
        try {
            List<Map<String, Object>> steps = objectMapper.readValue(
                    chain.getSteps(), new TypeReference<List<Map<String, Object>>>() {});
            
            for (Map<String, Object> step : steps) {
                // Validate required fields
                if (!step.containsKey("testId")) {
                    throw new IllegalArgumentException("Each step must have a testId");
                }
                
                // Validate test exists
                Long testId = Long.valueOf(step.get("testId").toString());
                apiTestRepository.findById(testId)
                        .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", testId.toString()));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid test steps format: " + e.getMessage());
        }
    }

    /**
     * Execute a test chain.
     *
     * @param chainId the chain ID
     * @param environmentId the environment ID
     * @param userId the user ID
     * @return the list of test results
     */
    public List<TestResultResponse> executeTestChain(Long chainId, Long environmentId, Long userId) {
        TestChain chain = testChainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("Test chain not found"));
        
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(chain.getProjectId(), userId, SharePermissionLevel.CAN_EXECUTE);
        
        try {
            // Parse steps and variables
            List<Map<String, Object>> steps = objectMapper.readValue(
                    chain.getSteps(), new TypeReference<List<Map<String, Object>>>() {});
            
            Map<String, String> variables = new HashMap<>();
            if (chain.getVariables() != null && !chain.getVariables().isEmpty()) {
                variables = objectMapper.readValue(
                        chain.getVariables(), new TypeReference<Map<String, String>>() {});
            }
            
            // Execute each step
            List<TestResultResponse> results = new ArrayList<>();
            for (Map<String, Object> step : steps) {
                Long testId = Long.valueOf(step.get("testId").toString());
                
                // Get additional variables for this step
                Map<String, String> stepVariables = new HashMap<>(variables);
                if (step.containsKey("variables")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> stepVars = (Map<String, String>) step.get("variables");
                    stepVariables.putAll(stepVars);
                }
                
                // Replace variables in step variables
                replaceVariablesInMap(stepVariables, variables);
                
                // Execute the test
                TestResultResponse result = apiTestService.executeTest(testId, chain.getProjectId(), userId, environmentId, stepVariables);
                results.add(result);
                
                // Check if test passed
                if (!result.getPassed() && step.containsKey("stopOnFailure") && Boolean.TRUE.equals(step.get("stopOnFailure"))) {
                    logger.warn("Test chain execution stopped due to test failure: {}", testId);
                    break;
                }
                
                // Extract variables from response
                if (step.containsKey("extractVariables") && result.getResponseBody() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> extractVars = (Map<String, String>) step.get("extractVariables");
                    extractVariablesFromResponse(extractVars, result.getResponseBody(), variables);
                }
            }
            
            return results;
        } catch (JsonProcessingException e) {
            throw new TestExecutionException("Error parsing test chain configuration: " + e.getMessage());
        } catch (Exception e) {
            throw new TestExecutionException("Error executing test chain: " + e.getMessage());
        }
    }
    
    /**
     * Execute a test chain.
     *
     * @param chainId the chain ID
     * @param projectId the project ID
     * @param userId the user ID
     * @param environmentId the environment ID
     * @return the list of test results
     */
    public List<TestResultResponse> executeTestChain(Long chainId, Long projectId, Long userId, Long environmentId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId, SharePermissionLevel.CAN_EXECUTE);
        
        TestChain chain = testChainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("Test chain not found"));
        
        if (!chain.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("You do not have access to this test chain");
        }
        
        return executeTestChain(chainId, environmentId, userId);
    }

    /**
     * Replace variables in a map.
     *
     * @param map the map to process
     * @param variables the variables
     */
    private void replaceVariablesInMap(Map<String, String> map, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                Matcher matcher = VARIABLE_PATTERN.matcher(value);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    String varName = matcher.group(1);
                    String replacement = variables.getOrDefault(varName, matcher.group(0));
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                }
                matcher.appendTail(sb);
                entry.setValue(sb.toString());
            }
        }
    }

    /**
     * Extract variables from a response.
     *
     * @param extractVars the variables to extract
     * @param responseBody the response body
     * @param variables the variables map to update
     */
    private void extractVariablesFromResponse(Map<String, String> extractVars, String responseBody, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : extractVars.entrySet()) {
            String varName = entry.getKey();
            String jsonPath = entry.getValue();
            
            try {
                Object value = JsonPath.read(responseBody, jsonPath);
                variables.put(varName, value.toString());
            } catch (PathNotFoundException e) {
                logger.warn("Could not extract variable {} using path {}: {}", varName, jsonPath, e.getMessage());
            }
        }
    }
}

