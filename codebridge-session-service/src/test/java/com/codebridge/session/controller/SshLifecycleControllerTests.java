package com.codebridge.session.controller;

import com.codebridge.session.config.JwtConfigProperties;
import com.codebridge.session.config.SshSessionConfigProperties;
import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.dto.SshSessionServiceApiInitRequest;
import com.codebridge.session.dto.UserProvidedConnectionDetails;
import com.codebridge.session.exception.GlobalExceptionHandler;
import com.codebridge.session.service.SshSessionLifecycleManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SshLifecycleController.class)
@Import({GlobalExceptionHandler.class, JwtConfigProperties.class, SshSessionConfigProperties.class})
class SshLifecycleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SshSessionLifecycleManager sessionLifecycleManagerMock;

    private UUID testPlatformUserId;
    private UUID testServerId;
    private UserProvidedConnectionDetails testConnectionDetails;
    private SshSessionServiceApiInitRequest testInitRequest;

    @BeforeEach
    void setUp() {
        testPlatformUserId = UUID.randomUUID();
        testServerId = UUID.randomUUID();
        testConnectionDetails = new UserProvidedConnectionDetails(
            "localhost", 22, "user", "key".getBytes(), null
        );
        testInitRequest = new SshSessionServiceApiInitRequest(testPlatformUserId, testServerId, testConnectionDetails);
    }

    @Test
    void initializeSession_success() throws Exception {
        SessionResponse mockSessionResponse = new SessionResponse("dummy-jwt", 3600000L, testPlatformUserId, testServerId, "SSH");
        when(sessionLifecycleManagerMock.initSshSession(eq(testPlatformUserId), eq(testServerId), any(UserProvidedConnectionDetails.class)))
            .thenReturn(mockSessionResponse);

        mockMvc.perform(post("/api/session/lifecycle/ssh/init") // Path from SshLifecycleController
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInitRequest)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.sessionToken").value("dummy-jwt"))
            .andExpect(jsonPath("$.userId").value(testPlatformUserId.toString()))
            .andExpect(jsonPath("$.resourceId").value(testServerId.toString()));
    }

    @Test
    void initializeSession_invalidRequest_nullPlatformUserId() throws Exception {
        SshSessionServiceApiInitRequest invalidRequest = new SshSessionServiceApiInitRequest(null, testServerId, testConnectionDetails);
        mockMvc.perform(post("/api/session/lifecycle/ssh/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest()); // From @NotNull on DTO field
    }

    @Test
    void initializeSession_invalidRequest_nullConnectionDetails() throws Exception {
        SshSessionServiceApiInitRequest invalidRequest = new SshSessionServiceApiInitRequest(testPlatformUserId, testServerId, null);
        mockMvc.perform(post("/api/session/lifecycle/ssh/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void initializeSession_invalidRequest_invalidInnerConnectionDetails() throws Exception {
        UserProvidedConnectionDetails invalidConnDetails = new UserProvidedConnectionDetails(null, 22, "user", "key".getBytes(), null);
        SshSessionServiceApiInitRequest invalidRequest = new SshSessionServiceApiInitRequest(testPlatformUserId, testServerId, invalidConnDetails);

        mockMvc.perform(post("/api/session/lifecycle/ssh/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }


    @Test
    void initializeSession_serviceThrowsRuntimeException() throws Exception {
        when(sessionLifecycleManagerMock.initSshSession(any(UUID.class), any(UUID.class), any(UserProvidedConnectionDetails.class)))
            .thenThrow(new RuntimeException("Internal connection setup failed"));

        mockMvc.perform(post("/api/session/lifecycle/ssh/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testInitRequest)))
            .andExpect(status().isInternalServerError()); // Handled by GlobalExceptionHandler
    }

    @Test
    void keepAliveSession_success() throws Exception {
        String validToken = "valid-token";
        KeepAliveResponse mockKeepAliveResponse = new KeepAliveResponse("new-jwt-token", 600000L);
        when(sessionLifecycleManagerMock.keepAliveSshSession(validToken)).thenReturn(Optional.of(mockKeepAliveResponse));

        mockMvc.perform(post("/api/session/lifecycle/ssh/{sessionToken}/keepalive", validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value("new-jwt-token"))
            .andExpect(jsonPath("$.expiresInMs").value(600000L));
    }

    @Test
    void keepAliveSession_notFoundOrInvalidToken() throws Exception {
        String invalidToken = "invalid-token";
        when(sessionLifecycleManagerMock.keepAliveSshSession(invalidToken)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/session/lifecycle/ssh/{sessionToken}/keepalive", invalidToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void releaseSession_success() throws Exception {
        String validToken = "valid-token-to-release";
        doNothing().when(sessionLifecycleManagerMock).releaseSshSession(validToken);

        mockMvc.perform(post("/api/session/lifecycle/ssh/{sessionToken}/release", validToken))
            .andExpect(status().isNoContent());
    }
}
