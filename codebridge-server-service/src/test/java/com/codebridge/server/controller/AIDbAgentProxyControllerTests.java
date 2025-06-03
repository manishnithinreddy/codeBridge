package com.codebridge.server.controller;

import com.codebridge.server.config.AIDbAgentServiceConfigProperties;
// Assuming GlobalExceptionHandler from server.exception is picked up or imported
import com.codebridge.server.dto.ai.ClientNaturalLanguageQueryResponse;
import com.codebridge.server.dto.ai.PluginNaturalLanguageQueryRequest;
import com.codebridge.server.service.ServerAccessControlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(AIDbAgentProxyController.class)
// If AIDbAgentServiceConfigProperties is not found, need to ensure it's scanned
// or provide it via @TestConfiguration or ensure it's on classpath correctly.
// @Import(AIDbAgentServiceConfigProperties.class) // May not be needed if component scan picks it up
class AIDbAgentProxyControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RestTemplate restTemplate;
    @MockBean private ServerAccessControlService serverAccessControlService; // For future db alias auth
    @MockBean private AIDbAgentServiceConfigProperties aiDbAgentServiceConfigProperties;

    private final String MOCK_USER_ID_STR = UUID.randomUUID().toString();
    // private final UUID MOCK_USER_ID_UUID = UUID.fromString(MOCK_USER_ID_STR); // If needed for service calls
    private final String aiDbAgentServiceUrl = "http://dummy-ai-agent/api/ai-db-agent/query";

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultUserJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(MOCK_USER_ID_STR))
            .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @BeforeEach
    void setUp() {
        when(aiDbAgentServiceConfigProperties.getAiDbAgentService()).thenReturn("http://dummy-ai-agent/api/ai-db-agent");
    }

    @Test
    void query_validRequest_proxiesAndReturnsSuccess() throws Exception {
        PluginNaturalLanguageQueryRequest pluginRequest = new PluginNaturalLanguageQueryRequest();
        pluginRequest.setDbConnectionAlias("myDB");
        pluginRequest.setNaturalLanguageQuery("Show all customers");
        pluginRequest.setDbSessionToken("db-token-123");

        ClientNaturalLanguageQueryResponse agentResponse = new ClientNaturalLanguageQueryResponse();
        agentResponse.setGeneratedSql("SELECT * FROM customers");
        // Populate agentResponse.sqlExecutionResult if needed for assertions

        ResponseEntity<ClientNaturalLanguageQueryResponse> responseEntity =
            new ResponseEntity<>(agentResponse, HttpStatus.OK);

        // Mock conceptual authorization for db alias (simplified for now)
        // when(serverAccessControlService.checkUserAccessToDbAlias(MOCK_USER_ID_UUID, "myDB")).thenReturn(true);

        when(restTemplate.exchange(
            eq(aiDbAgentServiceUrl), // Ensure this matches the full URL including /query
            eq(HttpMethod.POST),
            any(HttpEntity.class), // Can inspect this HttpEntity for propagated User JWT
            eq(ClientNaturalLanguageQueryResponse.class)))
            .thenReturn(responseEntity);

        mockMvc.perform(post("/api/ai-db-proxy/query")
                .with(defaultUserJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pluginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.generatedSql").value("SELECT * FROM customers"));
    }

    @Test
    void query_unauthenticated_returnsUnauthorized() throws Exception {
        PluginNaturalLanguageQueryRequest pluginRequest = new PluginNaturalLanguageQueryRequest();
        pluginRequest.setDbConnectionAlias("myDB");
        pluginRequest.setNaturalLanguageQuery("Show all customers");
        pluginRequest.setDbSessionToken("db-token-123");

        mockMvc.perform(post("/api/ai-db-proxy/query")
                // No JWT
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pluginRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void query_aiAgentServiceReturnsError_propagatesError() throws Exception {
        PluginNaturalLanguageQueryRequest pluginRequest = new PluginNaturalLanguageQueryRequest();
        pluginRequest.setDbConnectionAlias("myDB");
        pluginRequest.setNaturalLanguageQuery("Error query");
        pluginRequest.setDbSessionToken("db-token-error");

        when(restTemplate.exchange(
            eq(aiDbAgentServiceUrl),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(ClientNaturalLanguageQueryResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "AI Service Error",
                                                    "{\"aiError\":\"Bad prompt\"}".getBytes(), null));

        mockMvc.perform(post("/api/ai-db-proxy/query")
                .with(defaultUserJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pluginRequest)))
            .andExpect(status().isBadRequest()) // Or whatever status controller maps HttpStatusCodeException to
            .andExpect(jsonPath("$.processingError").exists());
            // .andExpect(jsonPath("$.processingError").value("Failed to process query via AI DB Agent: {\"aiError\":\"Bad prompt\"}"));
    }
}
