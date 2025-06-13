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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final ExpressionParser expressionParser;
    
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
        this.expressionParser = new SpelExpressionParser();
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
            // Check if this step should be executed based on conditions
            if (!evaluateStepCondition(step, chainContext)) {
                logger.info("Skipping step with test ID {} due to condition evaluation", step.getTestId());
                continue;
            }
            
            // Get test
            ApiTest test = apiTestRepository.findById(step.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", step.getTestId().toString()));
            
            // Apply variable mappings and templates to test request
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
            TestResultResponse result = apiTestService.executeTest(test.getId(), userId);
            results.add(result);
            
            // Store the result in the context for potential use in conditions
            chainContext.put("lastResult", result);
            chainContext.put("lastStatusCode", result.getResponseStatusCode());
            
            // Stop chain execution if test failed and failFast is enabled
            if ((TestStatus.ERROR.name().equals(result.getStatus()) || 
                 TestStatus.FAILURE.name().equals(result.getStatus())) && 
                (step.isFailFast() != null && step.isFailFast())) {
                logger.info("Stopping chain execution due to test failure and failFast=true");
                break;
            }
            
            // Extract variables from response
            if (step.getVariableMappings() != null && !step.getVariableMappings().isEmpty()) {
                extractVariables(result, step.getVariableMappings(), chainContext);
            }
            
            // Execute post-step actions
            if (step.getPostStepActions() != null && !step.getPostStepActions().isEmpty()) {
                executePostStepActions(step.getPostStepActions(), chainContext, result);
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
     * Evaluates whether a step should be executed based on its condition.
     *
     * @param step the chain step
     * @param context the chain context
     * @return true if the step should be executed, false otherwise
     */
    private boolean evaluateStepCondition(ChainStep step, Map<String, Object> context) {
        if (step.getCondition() == null || step.getCondition().isEmpty()) {
            return true; // No condition means always execute
        }
        
        try {
            // Create evaluation context with variables from chain context
            StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                evaluationContext.setVariable(entry.getKey(), entry.getValue());
            }
            
            // Parse and evaluate the condition
            Expression expression = expressionParser.parseExpression(step.getCondition());
            Boolean result = expression.getValue(evaluationContext, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            logger.warn("Error evaluating condition '{}': {}", step.getCondition(), e.getMessage());
            return false; // If condition evaluation fails, don't execute the step
        }
    }
    
    /**
     * Executes post-step actions.
     *
     * @param actions the post-step actions
     * @param context the chain context
     * @param result the test result
     */
    private void executePostStepActions(List<PostStepAction> actions, Map<String, Object> context, TestResultResponse result) {
        for (PostStepAction action : actions) {
            try {
                switch (action.getType()) {
                    case "SET_VARIABLE":
                        if (action.getParameters() != null && action.getParameters().containsKey("name") && 
                            action.getParameters().containsKey("value")) {
                            String name = action.getParameters().get("name");
                            String value = action.getParameters().get("value");
                            // Apply variable mappings to the value
                            String processedValue = applyVariableMappings(value, context);
                            context.put(name, processedValue);
                            logger.debug("Set variable '{}' to '{}'", name, processedValue);
                        }
                        break;
                    case "TRANSFORM_VARIABLE":
                        if (action.getParameters() != null && action.getParameters().containsKey("name") && 
                            action.getParameters().containsKey("transformation")) {
                            String name = action.getParameters().get("name");
                            String transformation = action.getParameters().get("transformation");
                            if (context.containsKey(name)) {
                                Object value = context.get(name);
                                Object transformedValue = applyTransformation(value, transformation);
                                context.put(name, transformedValue);
                                logger.debug("Transformed variable '{}' using '{}'", name, transformation);
                            }
                        }
                        break;
                    case "ASSERT":
                        if (action.getParameters() != null && action.getParameters().containsKey("condition")) {
                            String condition = action.getParameters().get("condition");
                            boolean assertResult = evaluateAssertion(condition, context);
                            logger.debug("Assertion '{}' result: {}", condition, assertResult);
                            if (!assertResult && action.getParameters().containsKey("failOnError") && 
                                Boolean.parseBoolean(action.getParameters().get("failOnError"))) {
                                throw new TestExecutionException("Assertion failed: " + condition);
                            }
                        }
                        break;
                    default:
                        logger.warn("Unknown post-step action type: {}", action.getType());
                }
            } catch (Exception e) {
                logger.error("Error executing post-step action: {}", e.getMessage(), e);
                if (action.getParameters() != null && action.getParameters().containsKey("failOnError") && 
                    Boolean.parseBoolean(action.getParameters().get("failOnError"))) {
                    throw new TestExecutionException("Post-step action failed: " + e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Applies a transformation to a value.
     *
     * @param value the value to transform
     * @param transformation the transformation to apply
     * @return the transformed value
     */
    private Object applyTransformation(Object value, String transformation) {
        if (value == null) {
            return null;
        }
        
        String stringValue = value.toString();
        
        if (transformation.startsWith("toUpperCase")) {
            return stringValue.toUpperCase();
        } else if (transformation.startsWith("toLowerCase")) {
            return stringValue.toLowerCase();
        } else if (transformation.startsWith("trim")) {
            return stringValue.trim();
        } else if (transformation.startsWith("substring")) {
            // Parse parameters: substring(start,end)
            Pattern pattern = Pattern.compile("substring\\((\\d+),(\\d+)\\)");
            Matcher matcher = pattern.matcher(transformation);
            if (matcher.matches()) {
                int start = Integer.parseInt(matcher.group(1));
                int end = Integer.parseInt(matcher.group(2));
                if (start < stringValue.length()) {
                    end = Math.min(end, stringValue.length());
                    return stringValue.substring(start, end);
                }
            }
            return stringValue;
        } else if (transformation.startsWith("replace")) {
            // Parse parameters: replace(target,replacement)
            Pattern pattern = Pattern.compile("replace\\(([^,]+),([^)]+)\\)");
            Matcher matcher = pattern.matcher(transformation);
            if (matcher.matches()) {
                String target = matcher.group(1);
                String replacement = matcher.group(2);
                return stringValue.replace(target, replacement);
            }
            return stringValue;
        } else if (transformation.startsWith("toInteger")) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
                logger.warn("Failed to convert '{}' to integer", stringValue);
                return 0;
            }
        } else if (transformation.startsWith("toDouble")) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException e) {
                logger.warn("Failed to convert '{}' to double", stringValue);
                return 0.0;
            }
        } else if (transformation.startsWith("toBoolean")) {
            return Boolean.parseBoolean(stringValue);
        }
        
        // If no transformation matched, return the original value
        return value;
    }
    
    /**
     * Evaluates an assertion.
     *
     * @param assertion the assertion to evaluate
     * @param context the chain context
     * @return true if the assertion is true, false otherwise
     */
    private boolean evaluateAssertion(String assertion, Map<String, Object> context) {
        try {
            // Create evaluation context with variables from chain context
            StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                evaluationContext.setVariable(entry.getKey(), entry.getValue());
            }
            
            // Parse and evaluate the assertion
            Expression expression = expressionParser.parseExpression(assertion);
            Boolean result = expression.getValue(evaluationContext, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            logger.warn("Error evaluating assertion '{}': {}", assertion, e.getMessage());
            return false;
        }
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
        
        // Handle Mustache-style variable references: {{variableName}}
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(input);
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object variableValue = resolveVariableReference(variableName, context);
            
            if (variableValue != null) {
                result = result.replace("{{" + variableName + "}}", variableValue.toString());
            }
        }
        
        // Handle expression language references: ${expression}
        pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        matcher = pattern.matcher(result);
        
        while (matcher.find()) {
            String expression = matcher.group(1);
            try {
                StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
                for (Map.Entry<String, Object> entry : context.entrySet()) {
                    evaluationContext.setVariable(entry.getKey(), entry.getValue());
                }
                
                Expression parsedExpression = expressionParser.parseExpression(expression);
                Object expressionValue = parsedExpression.getValue(evaluationContext);
                
                if (expressionValue != null) {
                    result = result.replace("${" + expression + "}", expressionValue.toString());
                }
            } catch (Exception e) {
                logger.warn("Error evaluating expression '{}': {}", expression, e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Resolves a variable reference, which may include nested properties.
     *
     * @param reference the variable reference
     * @param context the chain context
     * @return the resolved value
     */
    private Object resolveVariableReference(String reference, Map<String, Object> context) {
        if (reference.contains(".")) {
            String[] parts = reference.split("\\.", 2);
            String variableName = parts[0];
            String propertyPath = parts[1];
            
            Object variable = context.get(variableName);
            if (variable == null) {
                return null;
            }
            
            try {
                // Try to resolve using JsonPath for JSON objects
                if (variable instanceof String && ((String) variable).trim().startsWith("{")) {
                    try {
                        return JsonPath.read((String) variable, "$." + propertyPath);
                    } catch (PathNotFoundException e) {
                        logger.debug("Path not found in JSON: {}", propertyPath);
                        return null;
                    }
                }
                
                // For other objects, use SpEL
                StandardEvaluationContext evaluationContext = new StandardEvaluationContext(variable);
                Expression expression = expressionParser.parseExpression(propertyPath);
                return expression.getValue(evaluationContext);
            } catch (Exception e) {
                logger.warn("Error resolving property '{}' of variable '{}': {}", propertyPath, variableName, e.getMessage());
                return null;
            }
        } else {
            return context.get(reference);
        }
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
        
        // Store the raw response body in the context
        context.put("responseBody", responseBody);
        
        // Store response headers in the context
        if (result.getResponseHeaders() != null) {
            context.put("responseHeaders", result.getResponseHeaders());
        }
        
        // Store response status code in the context
        context.put("statusCode", result.getResponseStatusCode());
        
        // Try to parse response body as JSON
        boolean isJson = false;
        try {
            Map<String, Object> responseJson = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            context.put("responseJson", responseJson);
            isJson = true;
            
            // Extract variables using JSONPath expressions
            for (Map.Entry<String, String> mapping : variableMappings.entrySet()) {
                String variableName = mapping.getKey();
                String extractorExpression = mapping.getValue();
                
                if (extractorExpression.startsWith("jsonpath:")) {
                    String jsonPath = extractorExpression.substring(9);
                    try {
                        Object value = JsonPath.read(responseBody, jsonPath);
                        context.put(variableName, value);
                        logger.debug("Extracted variable '{}' using JSONPath '{}': {}", variableName, jsonPath, value);
                    } catch (PathNotFoundException e) {
                        logger.debug("JSONPath '{}' not found in response", jsonPath);
                    }
                } else if (extractorExpression.startsWith("header:")) {
                    String headerName = extractorExpression.substring(7);
                    if (result.getResponseHeaders() != null && result.getResponseHeaders().containsKey(headerName)) {
                        String headerValue = result.getResponseHeaders().get(headerName);
                        context.put(variableName, headerValue);
                        logger.debug("Extracted variable '{}' from header '{}': {}", variableName, headerName, headerValue);
                    }
                } else if (extractorExpression.startsWith("xpath:")) {
                    // XPath extraction would be implemented here for XML responses
                    logger.warn("XPath extraction not yet implemented");
                } else if (extractorExpression.startsWith("regex:")) {
                    String regex = extractorExpression.substring(6);
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(responseBody);
                    
                    if (matcher.find() && matcher.groupCount() > 0) {
                        context.put(variableName, matcher.group(1));
                        logger.debug("Extracted variable '{}' using regex '{}': {}", variableName, regex, matcher.group(1));
                    }
                } else {
                    // Default to JSONPath if the response is JSON
                    if (isJson) {
                        try {
                            Object value = JsonPath.read(responseBody, extractorExpression);
                            context.put(variableName, value);
                            logger.debug("Extracted variable '{}' using default JSONPath '{}': {}", variableName, extractorExpression, value);
                        } catch (PathNotFoundException e) {
                            logger.debug("Default JSONPath '{}' not found in response", extractorExpression);
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            // If response is not JSON, try to extract using regex
            for (Map.Entry<String, String> mapping : variableMappings.entrySet()) {
                String variableName = mapping.getKey();
                String extractorExpression = mapping.getValue();
                
                if (extractorExpression.startsWith("regex:")) {
                    String regex = extractorExpression.substring(6);
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(responseBody);
                    
                    if (matcher.find() && matcher.groupCount() > 0) {
                        context.put(variableName, matcher.group(1));
                        logger.debug("Extracted variable '{}' using regex '{}': {}", variableName, regex, matcher.group(1));
                    }
                } else if (extractorExpression.startsWith("header:")) {
                    String headerName = extractorExpression.substring(7);
                    if (result.getResponseHeaders() != null && result.getResponseHeaders().containsKey(headerName)) {
                        String headerValue = result.getResponseHeaders().get(headerName);
                        context.put(variableName, headerValue);
                        logger.debug("Extracted variable '{}' from header '{}': {}", variableName, headerName, headerValue);
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
        private String condition;
        private Boolean failFast;
        private List<PostStepAction> postStepActions;
        
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
        
        public String getCondition() {
            return condition;
        }
        
        public void setCondition(String condition) {
            this.condition = condition;
        }
        
        public Boolean isFailFast() {
            return failFast;
        }
        
        public void setFailFast(Boolean failFast) {
            this.failFast = failFast;
        }
        
        public List<PostStepAction> getPostStepActions() {
            return postStepActions;
        }
        
        public void setPostStepActions(List<PostStepAction> postStepActions) {
            this.postStepActions = postStepActions;
        }
    }
    
    /**
     * Post-step action.
     */
    private static class PostStepAction {
        private String type;
        private Map<String, String> parameters;
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Map<String, String> getParameters() {
            return parameters;
        }
        
        public void setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
        }
    }
}
