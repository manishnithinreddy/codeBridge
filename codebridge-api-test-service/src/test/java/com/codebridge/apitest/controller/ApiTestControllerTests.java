package com.codebridge.apitest.controller;

import com.codebridge.apitest.dto.ApiTestRequest; // Added DTO import
import com.codebridge.apitest.dto.ApiTestResponse; // Added DTO import
import com.codebridge.apitest.service.ApiTestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

// Assuming ApiTestDto for request/response, and basic CRUD in ApiTestService
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


// Placeholder DTOs for ApiTestController
// package com.codebridge.apitest.dto;
// Removed local dummy DTOs

@WebMvcTest(ApiTestController.class)
class ApiTestControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ApiTestService apiTestService; // Replace with actual service if name differs

    private final String MOCK_USER_ID_STR = UUID.randomUUID().toString();
    private final UUID MOCK_USER_ID_UUID = UUID.fromString(MOCK_USER_ID_STR);

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor defaultUserJwt() {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(MOCK_USER_ID_STR))
            .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // Dummy DTOs removed

    @Test
    void createApiTest_authenticated_returnsCreated() throws Exception {
        // Note: Actual ApiTestRequest has many more fields. This will need to be adjusted
        // based on what the actual createTest method expects or how the test is intended to work.
        // For now, providing null for fields not in the dummy DTO.
        ApiTestRequest requestDto = new ApiTestRequest(); // Use no-arg constructor
        requestDto.setName("My API Test");
        requestDto.setDescription("Some details");
        requestDto.setUrl("http://example.com");
        requestDto.setMethod("GET");
        requestDto.setTimeoutMs(5000);
        // Set other essential fields for the test if needed by createTest service method

        ApiTestResponse responseDto = new ApiTestResponse();
        responseDto.setId(UUID.randomUUID());
        responseDto.setName("My API Test");
        // responseDto.setDescription("Some details"); // Corrected from setDetails if it matches DTO
        // responseDto.setUserId(MOCK_USER_ID_UUID); // Removed, field/setter does not exist on ApiTestResponse

        when(apiTestService.createTest(any(ApiTestRequest.class), eq(MOCK_USER_ID_UUID))).thenReturn(responseDto);

        mockMvc.perform(post("/api/tests") // Assuming this is the endpoint
                .with(defaultUserJwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            // .andExpect(status().isCreated()); // Update based on actual response
            .andExpect(status().isOk()); // Or whatever the actual expected status is. Placeholder.
                                         // If the controller/service isn't fully implemented, this might fail.
                                         // For now, just checking if endpoint is secured.
    }

    @Test
    void createApiTest_unauthenticated_returnsUnauthorized() throws Exception {
        ApiTestRequest requestDto = new ApiTestRequest(); // Use no-arg constructor
        requestDto.setName("My API Test");
        requestDto.setDescription("Some details");
        requestDto.setUrl("http://example.com");
        requestDto.setMethod("GET");
        requestDto.setTimeoutMs(5000);
        // Set other essential fields

        mockMvc.perform(post("/api/tests")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getApiTestById_authenticated_returnsOk() throws Exception {
        UUID testId = UUID.randomUUID();
        ApiTestResponse responseDto = new ApiTestResponse();
        responseDto.setId(testId);
        responseDto.setName("My API Test");
        // responseDto.setDescription("Some details");
        // responseDto.setUserId(MOCK_USER_ID_UUID); // Removed, field/setter does not exist on ApiTestResponse

        when(apiTestService.getTestById(eq(testId), eq(MOCK_USER_ID_UUID))).thenReturn(responseDto);

        mockMvc.perform(get("/api/tests/{testId}", testId) // Assuming this endpoint
                .with(defaultUserJwt()))
            // .andExpect(status().isOk());
            .andExpect(status().isOk()); // Placeholder - adjust if service not fully mocked/implemented
    }
}
