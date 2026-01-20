package com.codebridge.docker.controller;

import com.codebridge.docker.model.AuthRequest;
import com.codebridge.docker.model.AuthResponse;
import com.codebridge.docker.service.DockerAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DockerAuthController.class)
class DockerAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DockerAuthService dockerAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_ValidRequest_ReturnsAuthResponse() throws Exception {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("testpassword");
        
        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken("jwt_token");
        authResponse.setTokenType("Bearer");
        authResponse.setExpiresIn(86400);
        authResponse.setUsername("testuser");
        
        when(dockerAuthService.authenticate(any(AuthRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void login_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        // Username and password are missing
        
        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ServiceThrowsException_ReturnsInternalServerError() throws Exception {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("testpassword");
        
        when(dockerAuthService.authenticate(any(AuthRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isInternalServerError());
    }
}

