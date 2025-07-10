package com.codebridge.gitlab.service.impl;

import com.codebridge.gitlab.model.AuthRequest;
import com.codebridge.gitlab.model.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitLabAuthServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GitLabAuthServiceImpl gitLabAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gitLabAuthService, "tokenExpirationMs", 86400000L);
    }

    @Test
    void authenticate_ValidToken_ReturnsAuthResponse() {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPersonalAccessToken("valid_token");
        
        // Mock the GitLab API response
        when(restTemplate.getForObject(anyString(), any())).thenReturn(new Object());

        // Act
        AuthResponse response = gitLabAuthService.authenticate(authRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400, response.getExpiresIn());
    }

    @Test
    void authenticate_InvalidToken_ThrowsException() {
        // Arrange
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPersonalAccessToken("invalid_token");
        
        // Mock the GitLab API response to throw an exception
        when(restTemplate.getForObject(anyString(), any())).thenThrow(new RuntimeException("Invalid token"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            gitLabAuthService.authenticate(authRequest);
        });
        
        assertTrue(exception.getMessage().contains("Invalid token"));
    }
}
