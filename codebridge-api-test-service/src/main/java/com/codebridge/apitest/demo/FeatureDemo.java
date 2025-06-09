package com.codebridge.apitest.demo;

import com.codebridge.apitest.dto.*;
import com.codebridge.apitest.model.*;
import com.codebridge.apitest.service.*;
import com.codebridge.apitest.repository.*;
import com.codebridge.apitest.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;

/**
 * Demo application that showcases the key features of the API Test Service.
 * This is for demonstration purposes only and is not meant to be run in production.
 */
@Component
@Profile("demo")
public class FeatureDemo implements CommandLineRunner {

    private final ApiTestService apiTestService;
    private final EnvironmentService environmentService;
    private final ProjectService projectService;
    private final ProjectTokenService projectTokenService;
    private final TestChainService testChainService;
    private final TestSnapshotService testSnapshotService;
    private final AuditLogService auditLogService;
    private final ProjectSharingService projectSharingService;
    private final ObjectMapper objectMapper;

    public FeatureDemo(
            ApiTestService apiTestService,
            EnvironmentService environmentService,
            ProjectService projectService,
            ProjectTokenService projectTokenService,
            TestChainService testChainService,
            TestSnapshotService testSnapshotService,
            AuditLogService auditLogService,
            ProjectSharingService projectSharingService,
            ObjectMapper objectMapper) {
        this.apiTestService = apiTestService;
        this.environmentService = environmentService;
        this.projectService = projectService;
        this.projectTokenService = projectTokenService;
        this.testChainService = testChainService;
        this.testSnapshotService = testSnapshotService;
        this.auditLogService = auditLogService;
        this.projectSharingService = projectSharingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        System.out.println("Starting API Test Service Feature Demo");
        
        try {
            // Generate a demo user ID
            UUID userId = UUID.randomUUID();
            System.out.println("Demo User ID: " + userId);
            
            // 1. Create a project
            System.out.println("\n=== Creating Project ===");
            ProjectRequest projectRequest = new ProjectRequest();
            projectRequest.setName("Demo Project");
            projectRequest.setDescription("A demo project for showcasing API Test Service features");
            
            ProjectResponse projectResponse = projectService.createProject(projectRequest, userId);
            System.out.println("Project created: " + projectResponse.getName() + " (ID: " + projectResponse.getId() + ")");
            
            UUID projectId = projectResponse.getId();
            
            // 2. Create an environment
            System.out.println("\n=== Creating Environment ===");
            EnvironmentRequest environmentRequest = new EnvironmentRequest();
            environmentRequest.setName("Demo Environment");
            environmentRequest.setDescription("A demo environment for testing");
            environmentRequest.setIsDefault(true);
            Map<String, String> variables = new HashMap<>();
            variables.put("baseUrl", "https://jsonplaceholder.typicode.com");
            variables.put("apiKey", "demo-api-key");
            environmentRequest.setVariables(variables);
            
            EnvironmentResponse environmentResponse = environmentService.createEnvironment(environmentRequest, userId);
            System.out.println("Environment created: " + environmentResponse.getName() + " (ID: " + environmentResponse.getId() + ")");
            System.out.println("Variables: " + environmentResponse.getVariables());
            
            UUID environmentId = environmentResponse.getId();
            
            // 3. Create a project token
            System.out.println("\n=== Creating Project Token ===");
            ProjectTokenRequest tokenRequest = new ProjectTokenRequest();
            tokenRequest.setName("Demo Token");
            tokenRequest.setDescription("A demo token for API authentication");
            tokenRequest.setTokenType("Bearer");
            tokenRequest.setTokenValue("demo-token-value");
            tokenRequest.setActive(true);
            
            ProjectTokenResponse tokenResponse = projectTokenService.createToken(projectId, tokenRequest, userId);
            System.out.println("Token created: " + tokenResponse.getName() + " (ID: " + tokenResponse.getId() + ")");
            System.out.println("Token Type: " + tokenResponse.getTokenType());
            
            // 4. Create an API test
            System.out.println("\n=== Creating API Test ===");
            ApiTestRequest apiTestRequest = new ApiTestRequest();
            apiTestRequest.setName("Get Users Test");
            apiTestRequest.setDescription("Test to fetch users from JSONPlaceholder API");
            apiTestRequest.setProjectId(projectId);
            apiTestRequest.setMethod(HttpMethod.GET);
            apiTestRequest.setProtocol(ProtocolType.HTTP);
            apiTestRequest.setUrl("{{baseUrl}}/users");
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            apiTestRequest.setHeaders(headers);
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("_limit", "3");
            apiTestRequest.setQueryParams(queryParams);
            
            ApiTestResponse apiTestResponse = apiTestService.createTest(apiTestRequest, userId);
            System.out.println("API Test created: " + apiTestResponse.getName() + " (ID: " + apiTestResponse.getId() + ")");
            System.out.println("Method: " + apiTestResponse.getMethod() + ", URL: " + apiTestResponse.getUrl());
            
            UUID testId = apiTestResponse.getId();
            
            // 5. Create a POST test
            System.out.println("\n=== Creating POST API Test ===");
            ApiTestRequest postTestRequest = new ApiTestRequest();
            postTestRequest.setName("Create User Test");
            postTestRequest.setDescription("Test to create a user via JSONPlaceholder API");
            postTestRequest.setProjectId(projectId);
            postTestRequest.setMethod(HttpMethod.POST);
            postTestRequest.setProtocol(ProtocolType.HTTP);
            postTestRequest.setUrl("{{baseUrl}}/users");
            postTestRequest.setHeaders(headers);
            postTestRequest.setBody("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}");
            
            ApiTestResponse postTestResponse = apiTestService.createTest(postTestRequest, userId);
            System.out.println("POST API Test created: " + postTestResponse.getName() + " (ID: " + postTestResponse.getId() + ")");
            
            UUID postTestId = postTestResponse.getId();
            
            // 6. Create a test chain
            System.out.println("\n=== Creating Test Chain ===");
            String testSequence = "[{\"testId\":\"" + testId + "\",\"variableMappings\":{\"$[0].id\":\"userId\"}},{\"testId\":\"" + postTestId + "\",\"variableMappings\":{\"$.id\":\"newUserId\"}}]";
            TestChain testChain = testChainService.createTestChain(projectId, "Demo Chain", "A demo test chain", testSequence, userId);
            System.out.println("Test Chain created: " + testChain.getName() + " (ID: " + testChain.getId() + ")");
            System.out.println("Test Sequence: " + testChain.getTestSequence());
            
            UUID chainId = testChain.getId();
            
            // 7. Share the project with another user
            System.out.println("\n=== Sharing Project ===");
            UUID anotherUserId = UUID.randomUUID();
            System.out.println("Another User ID: " + anotherUserId);
            
            ShareGrantRequest shareRequest = new ShareGrantRequest();
            shareRequest.setGranteeUserId(anotherUserId);
            shareRequest.setPermissionLevel(SharePermissionLevel.CAN_EDIT);
            
            ShareGrantResponse shareResponse = projectSharingService.grantProjectAccess(projectId, shareRequest, userId);
            System.out.println("Project shared with user: " + shareResponse.getGranteeUserId());
            System.out.println("Permission Level: " + shareResponse.getPermissionLevel());
            
            // 8. Log an audit event
            System.out.println("\n=== Creating Audit Log ===");
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("action", "demo_run");
            auditDetails.put("timestamp", LocalDateTime.now().toString());
            
            auditLogService.logAction(userId, "DEMO_EXECUTION", projectId, "Project", auditDetails);
            System.out.println("Audit log created for user: " + userId);
            
            System.out.println("\n=== Feature Demo Completed Successfully ===");
            System.out.println("All key features have been demonstrated and are working correctly.");
            
        } catch (Exception e) {
            System.err.println("Error during feature demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

