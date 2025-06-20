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
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for test chain operations.
 */
@Service
public class TestChainService {

    private static final Logger logger = LoggerFactory.getLogger(TestChainService.class);

    private final TestChainRepository testChainRepository;
    private final ApiTestRepository apiTestRepository;
    private final ApiTestService apiTestService;
    private final ProjectSharingService projectSharingService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TestChainService(
            TestChainRepository testChainRepository,
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
     * @return list of test chains
     */
    public List<TestChain> getTestChains(Long projectId, Long userId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId);
        
        return testChainRepository.findByProjectId(projectId);
    }

    /**
     * Get a test chain by ID.
     *
     * @param chainId the chain ID
     * @param projectId the project ID
     * @param userId the user ID
     * @return the test chain
     * @throws ResourceNotFoundException if the chain is not found
     */
    public TestChain getTestChain(Long chainId, Long projectId, Long userId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId);
        
        TestChain chain = testChainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("Test chain not found"));
        
        if (!chain.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("Test chain does not belong to this project");
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
        projectSharingService.verifyProjectAccess(chain.getProjectId(), userId, SharePermissionLevel.WRITE);
        
        return testChainRepository.save(chain);
    }

    /**
     * Update a test chain.
     *
     * @param chainId the chain ID
     * @param updatedChain the updated chain data
     * @param projectId the project ID
     * @param userId the user ID
     * @return the updated test chain
     * @throws ResourceNotFoundException if the chain is not found
     */
    @Transactional
    public TestChain updateTestChain(Long chainId, TestChain updatedChain, Long projectId, Long userId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId, SharePermissionLevel.WRITE);
        
        TestChain chain = testChainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("Test chain not found"));
        
        if (!chain.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("Test chain does not belong to this project");
        }
        
        chain.setName(updatedChain.getName());
        chain.setDescription(updatedChain.getDescription());
        chain.setActive(updatedChain.getActive());
        chain.setConfiguration(updatedChain.getConfiguration());
        
        return testChainRepository.save(chain);
    }

    /**
     * Delete a test chain.
     *
     * @param chainId the chain ID
     * @param projectId the project ID
     * @param userId the user ID
     * @throws ResourceNotFoundException if the chain is not found
     */
    @Transactional
    public void deleteTestChain(Long chainId, Long projectId, Long userId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId, SharePermissionLevel.WRITE);
        
        TestChain chain = testChainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("Test chain not found"));
        
        if (!chain.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("Test chain does not belong to this project");
        }
        
        testChainRepository.delete(chain);
    }

    /**
     * Execute a test chain.
     *
     * @param chainId the chain ID
     * @param projectId the project ID
     * @param userId the user ID
     * @param environmentId the environment ID to use for execution
     * @return list of test results
     * @throws ResourceNotFoundException if the chain is not found
     * @throws TestExecutionException if there's an error during execution
     */
    @Transactional
    public List<TestResultResponse> executeTestChain(Long chainId, Long projectId, Long userId, Long environmentId) {
        // Verify user has access to the project
        projectSharingService.verifyProjectAccess(projectId, userId, SharePermissionLevel.EXECUTE);
        
        TestChain chain = testChainRepository.findById(chainId)
                .orElseThrow(() -> new ResourceNotFoundException("Test chain not found"));
        
        if (!chain.getProjectId().equals(projectId)) {
            throw new AccessDeniedException("Test chain does not belong to this project");
        }
        
        if (!chain.getActive()) {
            throw new TestExecutionException("Cannot execute inactive test chain");
        }
        
        try {
            // Parse the chain configuration
            Map<String, Object> config = parseChainConfiguration(chain);
            List<Map<String, Object>> steps = getStepsFromConfig(config);
            
            // Execute the chain steps
            return executeChainSteps(steps, projectId, userId, environmentId);
            
        } catch (JsonProcessingException e) {
            throw new TestExecutionException("Error parsing test chain configuration", e);
        }
    }

    /**
     * Parse the chain configuration from JSON.
     *
     * @param chain the test chain
     * @return the parsed configuration
     * @throws JsonProcessingException if there's an error parsing the JSON
     */
    private Map<String, Object> parseChainConfiguration(TestChain chain) throws JsonProcessingException {
        if (chain.getConfiguration() == null || chain.getConfiguration().isEmpty()) {
            throw new TestExecutionException("Test chain has no configuration");
        }
        
        return objectMapper.readValue(chain.getConfiguration(), new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Get the steps from the chain configuration.
     *
     * @param config the chain configuration
     * @return list of step configurations
     */
    private List<Map<String, Object>> getStepsFromConfig(Map<String, Object> config) {
        Object stepsObj = config.get("steps");
        if (stepsObj == null || !(stepsObj instanceof List)) {
            throw new TestExecutionException("Invalid chain configuration: missing or invalid 'steps' array");
        }
        
        List<?> stepsList = (List<?>) stepsObj;
        return stepsList.stream()
                .map(step -> {
                    if (!(step instanceof Map)) {
                        throw new TestExecutionException("Invalid step configuration: step must be an object");
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stepMap = (Map<String, Object>) step;
                    return stepMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Execute the chain steps.
     *
     * @param steps the chain steps
     * @param projectId the project ID
     * @param userId the user ID
     * @param environmentId the environment ID
     * @return list of test results
     */
    private List<TestResultResponse> executeChainSteps(
            List<Map<String, Object>> steps,
            Long projectId,
            Long userId,
            Long environmentId) {
        
        List<TestResultResponse> results = new ArrayList<>();
        Map<String, Object> context = new ConcurrentHashMap<>();
        
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            
            // Get the test ID for this step
            Long testId = getTestIdFromStep(step);
            
            // Get the test
            ApiTest test = apiTestRepository.findById(testId)
                    .orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
            
            // Verify the test belongs to the same project
            if (!test.getProjectId().equals(projectId)) {
                throw new AccessDeniedException("Test does not belong to this project: " + testId);
            }
            
            // Process variables for this step
            Map<String, String> variables = processStepVariables(step, context);
            
            // Execute the test
            TestResultResponse result;
            try {
                result = apiTestService.executeTest(testId, projectId, userId, environmentId, variables);
                results.add(result);
                
                // If the test failed and failFast is enabled, stop execution
                if (result.getStatus() == TestStatus.FAILED && isFailFastEnabled(steps.get(0))) {
                    logger.info("Test failed and failFast is enabled, stopping chain execution");
                    break;
                }
                
                // Extract data from the response if needed for subsequent steps
                extractDataFromResponse(step, result, context);
                
            } catch (Exception e) {
                logger.error("Error executing test in chain", e);
                throw new TestExecutionException("Error executing test in chain: " + e.getMessage(), e);
            }
        }
        
        return results;
    }

    /**
     * Get the test ID from a step configuration.
     *
     * @param step the step configuration
     * @return the test ID
     */
    private Long getTestIdFromStep(Map<String, Object> step) {
        Object testIdObj = step.get("testId");
        if (testIdObj == null) {
            throw new TestExecutionException("Invalid step configuration: missing 'testId'");
        }
        
        if (testIdObj instanceof Integer) {
            return ((Integer) testIdObj).longValue();
        } else if (testIdObj instanceof Long) {
            return (Long) testIdObj;
        } else if (testIdObj instanceof String) {
            try {
                return Long.parseLong((String) testIdObj);
            } catch (NumberFormatException e) {
                throw new TestExecutionException("Invalid testId: " + testIdObj);
            }
        }
        
        throw new TestExecutionException("Invalid testId type: " + testIdObj.getClass().getName());
    }

    /**
     * Process variables for a step.
     *
     * @param step the step configuration
     * @param context the execution context
     * @return map of variables
     */
    private Map<String, String> processStepVariables(Map<String, Object> step, Map<String, Object> context) {
        Map<String, String> variables = new HashMap<>();
        
        // Get variables from the step configuration
        Object varsObj = step.get("variables");
        if (varsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> vars = (Map<String, Object>) varsObj;
            
            // Process each variable
            for (Map.Entry<String, Object> entry : vars.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof String) {
                    String strValue = (String) value;
                    
                    // Check if this is a reference to a context variable
                    if (strValue.startsWith("${") && strValue.endsWith("}")) {
                        String varName = strValue.substring(2, strValue.length() - 1);
                        Object contextValue = evaluateExpression(varName, context);
                        if (contextValue != null) {
                            variables.put(key, contextValue.toString());
                        }
                    } else {
                        variables.put(key, strValue);
                    }
                } else if (value != null) {
                    variables.put(key, value.toString());
                }
            }
        }
        
        return variables;
    }

    /**
     * Check if failFast is enabled in the chain configuration.
     *
     * @param firstStep the first step in the chain (contains chain-level settings)
     * @return true if failFast is enabled
     */
    private boolean isFailFastEnabled(Map<String, Object> firstStep) {
        Object chainConfigObj = firstStep.get("chainConfig");
        if (chainConfigObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> chainConfig = (Map<String, Object>) chainConfigObj;
            Object failFastObj = chainConfig.get("failFast");
            return failFastObj instanceof Boolean && (Boolean) failFastObj;
        }
        return false;
    }

    /**
     * Extract data from a test response for use in subsequent steps.
     *
     * @param step the step configuration
     * @param result the test result
     * @param context the execution context
     */
    private void extractDataFromResponse(Map<String, Object> step, TestResultResponse result, Map<String, Object> context) {
        Object extractObj = step.get("extract");
        if (extractObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> extractions = (Map<String, Object>) extractObj;
            
            String responseBody = result.getResponseBody();
            if (responseBody == null || responseBody.isEmpty()) {
                logger.warn("Cannot extract data from empty response body");
                return;
            }
            
            // Process each extraction
            for (Map.Entry<String, Object> entry : extractions.entrySet()) {
                String contextKey = entry.getKey();
                Object extractionDef = entry.getValue();
                
                if (extractionDef instanceof String) {
                    String jsonPath = (String) extractionDef;
                    try {
                        Object extracted = JsonPath.read(responseBody, jsonPath);
                        context.put(contextKey, extracted);
                        logger.debug("Extracted {} = {} using path: {}", contextKey, extracted, jsonPath);
                    } catch (PathNotFoundException e) {
                        logger.warn("JsonPath not found: {}", jsonPath);
                    }
                } else if (extractionDef instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> extractionConfig = (Map<String, Object>) extractionDef;
                    
                    String source = (String) extractionConfig.getOrDefault("source", "body");
                    String path = (String) extractionConfig.get("path");
                    
                    if (path != null) {
                        try {
                            Object sourceData;
                            if ("body".equals(source)) {
                                sourceData = responseBody;
                            } else if ("headers".equals(source)) {
                                sourceData = objectMapper.writeValueAsString(result.getResponseHeaders());
                            } else {
                                logger.warn("Unknown extraction source: {}", source);
                                continue;
                            }
                            
                            Object extracted = JsonPath.read(sourceData.toString(), path);
                            context.put(contextKey, extracted);
                            logger.debug("Extracted {} = {} from {} using path: {}", 
                                    contextKey, extracted, source, path);
                        } catch (PathNotFoundException | JsonProcessingException e) {
                            logger.warn("Error extracting data: {}", e.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * Evaluate an expression against the context.
     *
     * @param expression the expression to evaluate
     * @param context the context
     * @return the evaluated result
     */
    private Object evaluateExpression(String expression, Map<String, Object> context) {
        try {
            // Simple direct variable reference
            if (context.containsKey(expression)) {
                return context.get(expression);
            }
            
            // SpEL expression
            ExpressionParser parser = new SpelExpressionParser();
            Expression exp = parser.parseExpression(expression);
            
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            context.forEach(evalContext::setVariable);
            
            return exp.getValue(evalContext);
        } catch (Exception e) {
            logger.warn("Error evaluating expression: {}", expression, e);
            return null;
        }
    }
}

