package com.codebridge.docker.service.impl;

import com.codebridge.docker.model.AuthRequest;
import com.codebridge.docker.model.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DockerAuthServiceImplTest {

    @InjectMocks
    private DockerAuthServiceImpl dockerAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dockerAuthService, "tokenExpirationMs", 86400000L);
    }

    @Test
    void authenticate_ValidCredentials_ReturnsAuthResponse() {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("testpassword");
        authRequest.setRegistry("registry.example.com");

        // Act
        AuthResponse response = dockerAuthService.authenticate(authRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400, response.getExpiresIn());
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void authenticate_NullRegistry_ReturnsAuthResponse() {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("testpassword");
        authRequest.setRegistry(null);

        // Act
        AuthResponse response = dockerAuthService.authenticate(authRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400, response.getExpiresIn());
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void authenticate_EmptyRegistry_ReturnsAuthResponse() {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("testpassword");
        authRequest.setRegistry("");

        // Act
        AuthResponse response = dockerAuthService.authenticate(authRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400, response.getExpiresIn());
        assertEquals("testuser", response.getUsername());
    }
}

