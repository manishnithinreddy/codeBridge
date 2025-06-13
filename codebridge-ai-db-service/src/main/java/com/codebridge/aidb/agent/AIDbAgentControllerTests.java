package com.codebridge.aidb.controller;

import com.codebridge.aidb.agent.NaturalLanguageQueryRequest;
import com.codebridge.aidb.agent.NaturalLanguageQueryResponse;
import com.codebridge.aidb.service.AIDbQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import; // If GlobalExceptionHandler is separate
// For this service, assuming no Spring Security setup yet, so no JWT post-processor needed for its own endpoints.
// If its endpoints were secured, we'd add:
// import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// Add csrf() if Spring Security is on classpath and CSRF enabled by default in WebMvcTest
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


// Assuming GlobalExceptionHandler is picked up automatically or imported if in a different package
// and that this service does NOT have its own Spring Security for its endpoints yet.
@WebMvcTest(AIDbAgentController.class)
// @Import(GlobalExceptionHandler.class) // If needed
class AIDbAgentControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AIDbQueryService aiDbQueryService;

    // private final String MOCK_USER_ID_STR = UUID.randomUUID().toString(); // Needed if controller uses Authentication

    @Test
    void processNaturalLanguageQuery_validRequest_returnsOk() throws Exception {
        NaturalLanguageQueryRequest requestDto = new NaturalLanguageQueryRequest();
        requestDto.setDbSessionToken("test-db-token");
        requestDto.setNaturalLanguageQuery("Show me all users");

        NaturalLanguageQueryResponse mockResponse = new NaturalLanguageQueryResponse();
        mockResponse.setGeneratedSql("SELECT * FROM users");
        // Populate mockResponse.sqlExecutionResult if needed for assertion

        // Controller extracts platformUserId from Authentication, which will be null if no security context
        // For now, allow null platformUserId to be passed to service.
        when(aiDbQueryService.processQuery(eq("test-db-token"), eq("Show me all users"), any()))
            .thenReturn(Mono.just(mockResponse));

        mockMvc.perform(post("/api/ai-db-agent/query")
                // .with(csrf()) // Add if CSRF is active
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.generatedSql").value("SELECT * FROM users"));
    }

    @Test
    void processNaturalLanguageQuery_serviceReturnsError_propagatesError() throws Exception {
        NaturalLanguageQueryRequest requestDto = new NaturalLanguageQueryRequest();
        requestDto.setDbSessionToken("test-db-token-fail");
        requestDto.setNaturalLanguageQuery("This will cause an error");

        NaturalLanguageQueryResponse mockErrorResponse = new NaturalLanguageQueryResponse();
        mockErrorResponse.setProcessingError("Service layer error simulation");

        when(aiDbQueryService.processQuery(anyString(), anyString(), any())).thenReturn(Mono.just(mockErrorResponse));

        mockMvc.perform(post("/api/ai-db-agent/query")
                // .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk()) // Controller maps internal errors to 200 OK with error in body
            .andExpect(jsonPath("$.processingError").value("Service layer error simulation"));
    }

    @Test
    void processNaturalLanguageQuery_invalidRequest_returnsBadRequest() throws Exception {
        NaturalLanguageQueryRequest requestDto = new NaturalLanguageQueryRequest();
        // Missing dbSessionToken and naturalLanguageQuery which are @NotBlank

        mockMvc.perform(post("/api/ai-db-agent/query")
                // .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isBadRequest()); // Due to @Valid and @NotBlank violations
    }
}
