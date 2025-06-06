package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.TestResultResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for test chain operations.
 */
@Service
public class TestChainService {
    
    private final TestChainRepository testChainRepository;
    private final ApiTestRepository apiTestRepository;
    private final ApiTestService apiTestService;
    private final ProjectSharingService projectSharingService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TestChainService(TestChainRepository testChainRepository,
                           ApiTestRepository apiTestRepository,
                           ApiTestService apiTestService,
                           ProjectSharingService projectSharingService,
                           AuditLogService auditLogService,
                           ObjectMapper objectMapper) {
        this.testChainRepository = testChainRepository;
        this.apiTestRepository = apiTestRepository;
        this.apiTestService = apiTestService;
        this.projectSharingService = projectSharingService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create a new test chain.
     *
     * @param projectId the project ID
     * @param name the chain name
     * @param description the chain description
     * @param testSequence the test sequence
     * @param userId the user ID
     * @return the created test chain
     */
    @Transactional
    public TestChain createTestChain(UUID projectId, String name, String description, String testSequence, UUID userId) {
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(projectId, userId);
        if (permission == null || permission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to create test chains for project " + projectId);
        }
        
        // Create test chain
        TestChain testChain = new TestChain();
        testChain.setId(UUID.randomUUID());
        testChain.setName(name);
        testChain.setDescription(description);
        testChain.setProjectId(projectId);
        testChain.setTestSequence(testSequence);
        testChain.setActive(true);
        testChain.setCreatedBy(userId);
        
        TestChain savedTestChain = testChainRepository.save(testChain);
        
        // Log action
        auditLogService.logAction(
            userId,
            "TEST_CHAIN_CREATION",
            savedTestChain.getId(),
            "TestChain",
            Map.of("projectId", projectId, "name", name)
        );
        
        return savedTestChain;
    }
    
    /**
     * Get all test chains for a project.
     *
     * @param projectId the project ID
     * @param userId the user ID
     * @return the list of test chains
     */
    @Transactional(readOnly = true)
    public List<TestChain> getTestChains(UUID projectId, UUID userId) {
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(projectId, userId);
        if (permission == null) {
            throw new AccessDeniedException("User does not have permission to view test chains for project " + projectId);
        }
        
        return testChainRepository.findByProjectId(projectId);
    }
    
    /**
     * Get a test chain by ID.
     *
     * @param chainId the chain ID
     * @param userId the user ID
     * @return the test chain
     */
    @Transactional(readOnly = true)
    public TestChain getTestChain(UUID chainId, UUID userId) {
        TestChain testChain = testChainRepository.findById(chainId)
            .orElseThrow(() -> new ResourceNotFoundException("TestChain", "id", chainId.toString()));
        
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(testChain.getProjectId(), userId);
        if (permission == null) {
            throw new AccessDeniedException("User does not have permission to view test chain " + chainId);
        }
        
        return testChain;
    }
    
    /**
     * Update a test chain.
     *
     * @param chainId the chain ID
     * @param name the chain name
     * @param description the chain description
     * @param testSequence the test sequence
     * @param active the active status
     * @param userId the user ID
     * @return the updated test chain
     */
    @Transactional
    public TestChain updateTestChain(UUID chainId, String name, String description, String testSequence, Boolean active, UUID userId) {
        TestChain testChain = testChainRepository.findById(chainId)
            .orElseThrow(() -> new ResourceNotFoundException("TestChain", "id", chainId.toString()));
        
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(testChain.getProjectId(), userId);
        if (permission == null || permission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to update test chain " + chainId);
        }
        
        // Update test chain
        if (name != null) {
            testChain.setName(name);
        }
        
        if (description != null) {
            testChain.setDescription(description);
        }
        
        if (testSequence != null) {
            testChain.setTestSequence(testSequence);
        }
        
        if (active != null) {
            testChain.setActive(active);
        }
        
        testChain.setUpdatedAt(LocalDateTime.now());
        
        TestChain savedTestChain = testChainRepository.save(testChain);
        
        // Log action
        auditLogService.logAction(
            userId,
            "TEST_CHAIN_UPDATE",
            savedTestChain.getId(),
            "TestChain",
            Map.of("projectId", testChain.getProjectId(), "name", testChain.getName())
        );
        
        return savedTestChain;
    }
    
    /**
     * Delete a test chain.
     *
     * @param chainId the chain ID
     * @param userId the user ID
     */
    @Transactional
    public void deleteTestChain(UUID chainId, UUID userId) {
        TestChain testChain = testChainRepository.findById(chainId)
            .orElseThrow(() -> new ResourceNotFoundException("TestChain", "id", chainId.toString()));
        
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(testChain.getProjectId(), userId);
        if (permission == null || permission.ordinal() < SharePermissionLevel.CAN_EDIT.ordinal()) {
            throw new AccessDeniedException("User does not have permission to delete test chain " + chainId);
        }
        
        // Log action before deletion
        auditLogService.logAction(
            userId,
            "TEST_CHAIN_DELETION",
            chainId,
            "TestChain",
            Map.of("projectId", testChain.getProjectId(), "name", testChain.getName())
        );
        
        // Delete test chain
        testChainRepository.delete(testChain);
    }
    
    /**
     * Execute a test chain.
     *
     * @param chainId the chain ID
     * @param environmentId the environment ID
     * @param userId the user ID
     * @return the list of test results
     */
    @Transactional
    public List<TestResultResponse> executeTestChain(UUID chainId, UUID environmentId, UUID userId) {
        TestChain testChain = testChainRepository.findById(chainId)
            .orElseThrow(() -> new ResourceNotFoundException("TestChain", "id", chainId.toString()));
        
        // Check permissions
        SharePermissionLevel permission = projectSharingService.getEffectivePermission(testChain.getProjectId(), userId);
        if (permission == null) {
            throw new AccessDeniedException("User does not have permission to execute test chain " + chainId);
        }
        
        // Parse test sequence
        List<ChainStep> steps;
        try {
            steps = objectMapper.readValue(testChain.getTestSequence(), new TypeReference<List<ChainStep>>() {});
        } catch (JsonProcessingException e) {
            throw new TestExecutionException("Failed to parse test sequence: " + e.getMessage());
        }
        
        // Execute tests in sequence
        List<TestResultResponse> results = new ArrayList<>();
        Map<String, Object> chainContext = new HashMap<>();
        
        for (ChainStep step : steps) {
            // Get test
            ApiTest test = apiTestRepository.findById(step.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", step.getTestId().toString()));
            
            // Apply variable mappings from previous test results
            String processedUrl = applyVariableMappings(test.getUrl(), chainContext);
            test.setUrl(processedUrl);
            
            if (test.getRequestBody() != null) {
                String processedBody = applyVariableMappings(test.getRequestBody(), chainContext);
                test.setRequestBody(processedBody);
            }
            
            if (test.getHeaders() != null) {
                String processedHeaders = applyVariableMappings(test.getHeaders(), chainContext);
                test.setHeaders(processedHeaders);
            }
            
            // Execute test
            TestResultResponse result = apiTestService.executeTest(test.getId(), environmentId, userId);
            results.add(result);
            
            // Stop chain execution if test failed
            if (result.getStatus() == TestStatus.ERROR || result.getStatus() == TestStatus.FAILURE) {
                break;
            }
            
            // Extract variables from response
            if (step.getVariableMappings() != null && !step.getVariableMappings().isEmpty()) {
                extractVariables(result, step.getVariableMappings(), chainContext);
            }
        }
        
        // Log action
        auditLogService.logAction(
            userId,
            "TEST_CHAIN_EXECUTION",
            chainId,
            "TestChain",
            Map.of(
                "projectId", testChain.getProjectId(),
                "name", testChain.getName(),
                "environmentId", environmentId,
                "resultCount", results.size()
            )
        );
        
        return results;
    }
    
    /**
     * Apply variable mappings to a string.
     *
     * @param input the input string
     * @param context the chain context
     * @return the processed string
     */
    private String applyVariableMappings(String input, Map<String, Object> context) {
        if (input == null || context.isEmpty()) {
            return input;
        }
        
        String result = input;
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(input);
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object variableValue = context.get(variableName);
            
            if (variableValue != null) {
                result = result.replace("{{" + variableName + "}}", variableValue.toString());
            }
        }
        
        return result;
    }
    
    /**
     * Extract variables from a test result.
     *
     * @param result the test result
     * @param variableMappings the variable mappings
     * @param context the chain context
     */
    private void extractVariables(TestResultResponse result, Map<String, String> variableMappings, Map<String, Object> context) {
        String responseBody = result.getResponseBody();
        
        if (responseBody == null || responseBody.isEmpty()) {
            return;
        }
        
        // Parse response body as JSON
        try {
            Map<String, Object> responseJson = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            
            // Extract variables using JSONPath-like expressions
            for (Map.Entry<String, String> mapping : variableMappings.entrySet()) {
                String variableName = mapping.getKey();
                String jsonPath = mapping.getValue();
                
                Object value = extractValueFromJson(responseJson, jsonPath);
                if (value != null) {
                    context.put(variableName, value);
                }
            }
        } catch (JsonProcessingException e) {
            // If response is not JSON, try to extract using regex
            for (Map.Entry<String, String> mapping : variableMappings.entrySet()) {
                String variableName = mapping.getKey();
                String regex = mapping.getValue();
                
                if (regex.startsWith("regex:")) {
                    regex = regex.substring(6);
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(responseBody);
                    
                    if (matcher.find() && matcher.groupCount() > 0) {
                        context.put(variableName, matcher.group(1));
                    }
                }
            }
        }
    }
    
    /**
     * Extract a value from a JSON object using a JSONPath-like expression.
     *
     * @param json the JSON object
     * @param path the JSONPath-like expression
     * @return the extracted value
     */
    private Object extractValueFromJson(Map<String, Object> json, String path) {
        String[] parts = path.split("\\.");
        Object current = json;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Chain step.
     */
    private static class ChainStep {
        private UUID testId;
        private Map<String, String> variableMappings;
        
        public UUID getTestId() {
            return testId;
        }
        
        public void setTestId(UUID testId) {
            this.testId = testId;
        }
        
        public Map<String, String> getVariableMappings() {
            return variableMappings;
        }
        
        public void setVariableMappings(Map<String, String> variableMappings) {
            this.variableMappings = variableMappings;
        }
    }
}

