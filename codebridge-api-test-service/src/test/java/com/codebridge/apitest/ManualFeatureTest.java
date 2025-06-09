package com.codebridge.apitest;

import com.codebridge.apitest.dto.*;
import com.codebridge.apitest.model.*;
import com.codebridge.apitest.service.*;
import com.codebridge.apitest.repository.*;
import com.codebridge.apitest.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Manual test to verify key features of the API Test Service.
 * This test exercises the main functionality to ensure it works as expected.
 */
@SpringBootTest
@ActiveProfiles("test")
public class ManualFeatureTest {

    @Autowired
    private ApiTestService apiTestService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectTokenService projectTokenService;

    @Autowired
    private TestChainService testChainService;

    @Autowired
    private TestSnapshotService testSnapshotService;

    @MockBean
    private ApiTestRepository apiTestRepository;

    @MockBean
    private EnvironmentRepository environmentRepository;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private ProjectTokenRepository projectTokenRepository;

    @MockBean
    private TestChainRepository testChainRepository;

    @MockBean
    private TestSnapshotRepository testSnapshotRepository;

    @MockBean
    private TestResultRepository testResultRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID projectId;
    private UUID environmentId;
    private UUID testId;
    private UUID tokenId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        environmentId = UUID.randomUUID();
        testId = UUID.randomUUID();
        tokenId = UUID.randomUUID();

        // Mock project
        Project project = new Project();
        project.setId(projectId);
        project.setName("Test Project");
        project.setDescription("Test Project Description");
        project.setOwnerId(userId);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        // Mock environment
        Environment environment = new Environment();
        environment.setId(environmentId);
        environment.setName("Test Environment");
        environment.setDescription("Test Environment Description");
        environment.setOwnerId(userId);
        environment.setVariables("{\"baseUrl\":\"https://api.example.com\",\"apiKey\":\"test-api-key\"}");
        environment.setIsDefault(true);
        environment.setCreatedAt(LocalDateTime.now());
        environment.setUpdatedAt(LocalDateTime.now());

        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(environment));
        when(environmentRepository.save(any(Environment.class))).thenReturn(environment);
        when(environmentRepository.findByOwnerIdAndIsDefaultTrue(userId)).thenReturn(Optional.of(environment));

        // Mock API test
        ApiTest apiTest = new ApiTest();
        apiTest.setId(testId);
        apiTest.setName("Test API");
        apiTest.setDescription("Test API Description");
        apiTest.setProjectId(projectId);
        apiTest.setOwnerId(userId);
        apiTest.setMethod(HttpMethod.GET);
        apiTest.setProtocol(ProtocolType.HTTP);
        apiTest.setUrl("https://api.example.com/users");
        apiTest.setHeaders("{\"Content-Type\":\"application/json\"}");
        apiTest.setQueryParams("{\"page\":\"1\",\"limit\":\"10\"}");
        apiTest.setBody("{}");
        apiTest.setCreatedAt(LocalDateTime.now());
        apiTest.setUpdatedAt(LocalDateTime.now());

        when(apiTestRepository.findById(testId)).thenReturn(Optional.of(apiTest));
        when(apiTestRepository.save(any(ApiTest.class))).thenReturn(apiTest);

        // Mock project token
        ProjectToken token = new ProjectToken();
        token.setId(tokenId);
        token.setProjectId(projectId);
        token.setName("Test Token");
        token.setDescription("Test Token Description");
        token.setTokenType("Bearer");
        token.setTokenValue("test-token-value");
        token.setActive(true);
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusDays(30));

        when(projectTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(projectTokenRepository.save(any(ProjectToken.class))).thenReturn(token);
        when(projectTokenRepository.findByProjectIdAndActive(projectId, true)).thenReturn(List.of(token));

        // Mock test result
        TestResult testResult = new TestResult();
        testResult.setId(UUID.randomUUID());
        testResult.setTestId(testId);
        testResult.setUserId(userId);
        testResult.setStatus(TestStatus.SUCCESS);
        testResult.setStatusCode(200);
        testResult.setResponseBody("{\"data\":{\"users\":[{\"id\":1,\"name\":\"John Doe\"}]}}");
        testResult.setResponseHeaders("{\"Content-Type\":\"application/json\"}");
        testResult.setExecutionTime(150L);
        testResult.setCreatedAt(LocalDateTime.now());

        when(testResultRepository.save(any(TestResult.class))).thenReturn(testResult);
    }

    @Test
    @DisplayName("Test Core Features - Create and Execute API Test")
    void testCreateAndExecuteApiTest() {
        // 1. Create a project
        ProjectRequest projectRequest = new ProjectRequest();
        projectRequest.setName("Test Project");
        projectRequest.setDescription("Test Project Description");
        
        ProjectResponse projectResponse = projectService.createProject(projectRequest, userId);
        assertNotNull(projectResponse);
        assertEquals("Test Project", projectResponse.getName());
        
        // 2. Create an environment
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setName("Test Environment");
        environmentRequest.setDescription("Test Environment Description");
        environmentRequest.setIsDefault(true);
        Map<String, String> variables = new HashMap<>();
        variables.put("baseUrl", "https://api.example.com");
        variables.put("apiKey", "test-api-key");
        environmentRequest.setVariables(variables);
        
        EnvironmentResponse environmentResponse = environmentService.createEnvironment(environmentRequest, userId);
        assertNotNull(environmentResponse);
        assertEquals("Test Environment", environmentResponse.getName());
        assertTrue(environmentResponse.getIsDefault());
        
        // 3. Create a project token
        ProjectTokenRequest tokenRequest = new ProjectTokenRequest();
        tokenRequest.setName("Test Token");
        tokenRequest.setDescription("Test Token Description");
        tokenRequest.setTokenType("Bearer");
        tokenRequest.setTokenValue("test-token-value");
        tokenRequest.setActive(true);
        
        ProjectTokenResponse tokenResponse = projectTokenService.createToken(projectId, tokenRequest, userId);
        assertNotNull(tokenResponse);
        assertEquals("Test Token", tokenResponse.getName());
        assertEquals("Bearer", tokenResponse.getTokenType());
        
        // 4. Create an API test
        ApiTestRequest apiTestRequest = new ApiTestRequest();
        apiTestRequest.setName("Test API");
        apiTestRequest.setDescription("Test API Description");
        apiTestRequest.setProjectId(projectId);
        apiTestRequest.setMethod(HttpMethod.GET);
        apiTestRequest.setProtocol(ProtocolType.HTTP);
        apiTestRequest.setUrl("https://api.example.com/users");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        apiTestRequest.setHeaders(headers);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("page", "1");
        queryParams.put("limit", "10");
        apiTestRequest.setQueryParams(queryParams);
        apiTestRequest.setBody("{}");
        
        ApiTestResponse apiTestResponse = apiTestService.createTest(apiTestRequest, userId);
        assertNotNull(apiTestResponse);
        assertEquals("Test API", apiTestResponse.getName());
        assertEquals(HttpMethod.GET, apiTestResponse.getMethod());
        
        // 5. Execute the API test
        // Mock the HTTP client response
        TestResultResponse testResultResponse = apiTestService.executeTest(testId, userId);
        assertNotNull(testResultResponse);
        assertEquals(TestStatus.SUCCESS, testResultResponse.getStatus());
        assertEquals(200, testResultResponse.getStatusCode());
        
        // 6. Create a test chain
        String testSequence = "[{\"testId\":\"" + testId + "\",\"variableMappings\":{\"$.data.users[0].id\":\"userId\"}}]";
        TestChain testChain = testChainService.createTestChain(projectId, "Test Chain", "Test Chain Description", testSequence, userId);
        assertNotNull(testChain);
        assertEquals("Test Chain", testChain.getName());
        
        // 7. Create a test snapshot
        TestSnapshot snapshot = testSnapshotService.createSnapshot(testId, "Test Snapshot", "Test Snapshot Description", null, userId);
        assertNotNull(snapshot);
        assertEquals("Test Snapshot", snapshot.getName());
        
        System.out.println("All core features tested successfully!");
    }
}

