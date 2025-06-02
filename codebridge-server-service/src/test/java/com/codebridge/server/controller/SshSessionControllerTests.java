package com.codebridge.server.controller;

import com.codebridge.server.config.JwtConfigProperties;
// import com.codebridge.server.config.SshSessionConfigProperties; // Removed
import com.codebridge.server.dto.client.ClientUserProvidedConnectionDetails;
import com.codebridge.server.dto.client.SshSessionServiceInitRequestDto;
import com.codebridge.server.dto.sessions.KeepAliveResponse;
import com.codebridge.server.dto.sessions.SessionResponse;
import com.codebridge.server.dto.sessions.SshSessionInitRequest;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.service.ServerAccessControlService;
import com.codebridge.server.service.SshKeyManagementService;
import com.codebridge.server.web.rest.errors.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SshSessionController.class)
@Import({GlobalExceptionHandler.class, JwtConfigProperties.class}) // SshSessionConfigProperties removed
class SshSessionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestTemplate restTemplateMock;
    @MockBean
    private ServerAccessControlService serverAccessControlServiceMock;
    @MockBean
    private SshKeyManagementService sshKeyManagementServiceMock;
    // @MockBean JwtTokenProvider jwtTokenProviderMock; // Controller doesn't use it directly

    private String sessionServiceBaseUrl = "http://codebridge-session-service/api"; // Default, can be overridden

    private UUID testServerId;
    private UUID fixedPlatformUserId;
    private SshSessionInitRequest controllerRequest;

    @BeforeEach
    void setUp() {
        testServerId = UUID.randomUUID();
        fixedPlatformUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        controllerRequest = new SshSessionInitRequest();
        controllerRequest.setServerId(testServerId);

        // Inject the base URL using ReflectionTestUtils as @Value might not work easily in @WebMvcTest slice
        ReflectionTestUtils.setField(SshSessionController.class, "sessionServiceBaseUrl", sessionServiceBaseUrl, mockMvc.getDispatcherServlet().getWebApplicationContext().getBean(SshSessionController.class));

    }

    @Test
    void initializeSession_success() throws Exception {
        Server serverMock = new Server();
        serverMock.setHostname("test.host");
        serverMock.setPort(22);
        serverMock.setAuthProvider(ServerAuthProvider.SSH_KEY);

        ServerAccessControlService.UserSpecificConnectionDetails accessDetails =
            new ServerAccessControlService.UserSpecificConnectionDetails(serverMock, "remoteuser", UUID.randomUUID());
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(fixedPlatformUserId, testServerId))
            .thenReturn(accessDetails);

        SshKey decryptedKey = new SshKey();
        decryptedKey.setPrivateKey("decrypted-private-key");
        when(sshKeyManagementServiceMock.getDecryptedSshKey(accessDetails.sshKeyIdToUse(), fixedPlatformUserId))
            .thenReturn(decryptedKey);

        SessionResponse expectedSessionResponse = new SessionResponse("session-jwt-from-service", 3600L, fixedPlatformUserId, testServerId, "SSH");
        when(restTemplateMock.postForEntity(eq(sessionServiceBaseUrl + "/lifecycle/ssh/init"), any(SshSessionServiceInitRequestDto.class), eq(SessionResponse.class)))
            .thenReturn(new ResponseEntity<>(expectedSessionResponse, HttpStatus.CREATED));

        mockMvc.perform(post("/api/ssh/sessions/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(controllerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionToken").value("session-jwt-from-service"))
            .andExpect(jsonPath("$.userId").value(fixedPlatformUserId.toString()));
    }

    @Test
    void initializeSession_invalidRequest_nullServerId_returnsBadRequest() throws Exception {
        SshSessionInitRequest request = new SshSessionInitRequest(); // serverId is null

        mockMvc.perform(post("/api/ssh/sessions/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
            // Further assertions on error response structure can be added if GlobalExceptionHandler defines them
    }

    @Test
    void initializeSession_managerThrowsRuntimeException_globalExceptionHandlerHandles() throws Exception {
        SshSessionInitRequest request = new SshSessionInitRequest();
        request.setServerId(testServerId);

        when(sshSessionManagerMock.initSshSession(eq(fixedPlatformUserId), eq(testServerId)))
            .thenThrow(new RuntimeException("Session creation failed internally"));

        mockMvc.perform(post("/api/ssh/sessions/init")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError()); // Assuming GlobalExceptionHandler maps RuntimeException to 500
    }


    @Test
    void keepAliveSession_validToken_returnsOk() throws Exception {
        String validToken = "valid-jwt-token";
        KeepAliveResponse keepAliveDto = new KeepAliveResponse(validToken, 3600000L);

        when(sshSessionManagerMock.keepAliveSshSession(eq(validToken)))
            .thenReturn(Optional.of(keepAliveDto));

        mockMvc.perform(post("/api/ssh/sessions/{sessionToken}/keepalive", validToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value(validToken))
            .andExpect(jsonPath("$.expiresInMs").value(3600000L));
    }

    @Test
    void keepAliveSession_invalidOrExpiredToken_returnsNotFound() throws Exception {
        String invalidToken = "invalid-jwt-token";
        when(sshSessionManagerMock.keepAliveSshSession(eq(invalidToken)))
            .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/ssh/sessions/{sessionToken}/keepalive", invalidToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void releaseSession_validToken_returnsNoContent() throws Exception {
        String validToken = "valid-jwt-token-to-release";
        doNothing().when(sshSessionManagerMock).releaseSshSession(eq(validToken));

        mockMvc.perform(post("/api/ssh/sessions/{sessionToken}/release", validToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void releaseSession_invalidToken_stillReturnsNoContent() throws Exception {
        // Release operation is typically idempotent from client's perspective.
        // If token doesn't exist, manager handles it gracefully.
        String invalidToken = "invalid-token-for-release";
        doNothing().when(sshSessionManagerMock).releaseSshSession(eq(invalidToken));

        mockMvc.perform(post("/api/ssh/sessions/{sessionToken}/release", invalidToken))
            .andExpect(status().isNoContent());
    }
}
