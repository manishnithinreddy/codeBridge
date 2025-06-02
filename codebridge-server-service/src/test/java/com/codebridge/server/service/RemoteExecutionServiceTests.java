package com.codebridge.server.service;

import com.codebridge.server.dto.remote.CommandRequest;
import com.codebridge.server.dto.remote.CommandResponse;
import com.codebridge.server.dto.remote.CommandRequest;
import com.codebridge.server.dto.remote.CommandResponse;
import com.codebridge.server.exception.AccessDeniedException;
import com.codebridge.server.exception.RemoteCommandException;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server; // Keep for UserSpecificConnectionDetails
import com.codebridge.server.model.SshKey;   // Keep for UserSpecificConnectionDetails
import com.codebridge.server.sessions.SessionKey; // For mocking jwtTokenProvider
import com.codebridge.server.security.jwt.JwtTokenProvider;
import com.codebridge.server.service.ServerAccessControlService.UserSpecificConnectionDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoteExecutionServiceTests {

    @Mock
    private ServerAccessControlService serverAccessControlServiceMock;
    @Mock
    private JwtTokenProvider jwtTokenProviderMock;
    @Mock
    private RestTemplate restTemplateMock;
    @Mock
    private ServerActivityLogService activityLogServiceMock;

    @InjectMocks
    private RemoteExecutionService remoteExecutionService;

    private UUID testUserId;
    private UUID testServerId;
    private String testSessionToken;
    private SessionKey testSessionKey;
    private CommandRequest commandRequest;
    private String sessionServiceBaseUrl = "http://fake-session-service/api";


    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testServerId = UUID.randomUUID();
        testSessionToken = "test.jwt.token";
        testSessionKey = new SessionKey(testUserId, testServerId, "SSH");

        commandRequest = new CommandRequest();
        commandRequest.setCommand("ls -la");

        ReflectionTestUtils.setField(remoteExecutionService, "sessionServiceBaseUrl", sessionServiceBaseUrl);

        // Default behavior for successful log creation
        doNothing().when(activityLogServiceMock).createLog(any(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void executeCommand_success() {
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testSessionToken)).thenReturn(Optional.of(testSessionKey));
        // Assume checkUserAccessAndGetConnectionDetails does not throw for success path
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenReturn(mock(UserSpecificConnectionDetails.class)); // Content doesn't matter, just that it doesn't throw

        CommandResponse expectedResponseFromSessionService = new CommandResponse("output", "", 0, 100L);
        ResponseEntity<CommandResponse> responseEntity = new ResponseEntity<>(expectedResponseFromSessionService, HttpStatus.OK);

        when(restTemplateMock.exchange(
            eq(sessionServiceBaseUrl + "/ops/ssh/" + testSessionToken + "/execute-command"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(CommandResponse.class)
        )).thenReturn(responseEntity);

        CommandResponse actualResponse = remoteExecutionService.executeCommand(testServerId, testSessionToken, commandRequest);

        assertNotNull(actualResponse);
        assertEquals("output", actualResponse.getStdout());
        assertEquals(0, actualResponse.getExitStatus());

        verify(jwtTokenProviderMock).validateTokenAndExtractSessionKey(testSessionToken);
        verify(serverAccessControlServiceMock).checkUserAccessAndGetConnectionDetails(testUserId, testServerId);
        verify(activityLogServiceMock).createLog(
            eq(testUserId), eq("EXECUTE_COMMAND_VIA_SESSION_SERVICE"), eq(testServerId),
            anyString(), eq("SUCCESS"), anyString(), anyString(), eq(0)
        );
    }

    @Test
    void executeCommand_invalidToken_throwsAccessDenied() {
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testSessionToken)).thenReturn(Optional.empty());

        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            remoteExecutionService.executeCommand(testServerId, testSessionToken, commandRequest);
        });
        assertEquals("Invalid or expired session token.", thrown.getMessage());
        verify(activityLogServiceMock).createLog(eq(testServerId),isNull(),eq(commandRequest.getCommand()), contains("Invalid or expired session token"), eq("FAILURE"),isNull(), isNull(), isNull());
    }

    @Test
    void executeCommand_tokenServerMismatch_throwsAccessDenied() {
        SessionKey mismatchedKey = new SessionKey(testUserId, UUID.randomUUID(), "SSH"); // Different serverId
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testSessionToken)).thenReturn(Optional.of(mismatchedKey));

        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            remoteExecutionService.executeCommand(testServerId, testSessionToken, commandRequest);
        });
        assertEquals("Session token mismatch: Token is not valid for the requested server or resource type.", thrown.getMessage());
         verify(activityLogServiceMock).createLog(eq(testServerId),eq(testUserId),eq(commandRequest.getCommand()), contains("Session token mismatch"), eq("FAILURE"),isNull(), isNull(), isNull());
    }

    @Test
    void executeCommand_authorizationFailed_throwsAccessDenied() {
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testSessionToken)).thenReturn(Optional.of(testSessionKey));
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenThrow(new AccessDeniedException("User not authorized for this server."));

        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            remoteExecutionService.executeCommand(testServerId, testSessionToken, commandRequest);
        });
        assertEquals("User not authorized for this server.", thrown.getMessage());
        verify(activityLogServiceMock).createLog(eq(testServerId),eq(testUserId),eq(commandRequest.getCommand()), contains("Authorization failed: User not authorized for this server."), eq("FAILURE"),isNull(), isNull(), isNull());
    }

    @Test
    void executeCommand_sessionServiceReturnsError_throwsRemoteCommandException() {
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testSessionToken)).thenReturn(Optional.of(testSessionKey));
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenReturn(mock(UserSpecificConnectionDetails.class));

        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CommandResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Session Service Error"));

        RemoteCommandException thrown = assertThrows(RemoteCommandException.class, () -> {
            remoteExecutionService.executeCommand(testServerId, testSessionToken, commandRequest);
        });
        assertTrue(thrown.getMessage().contains("Failed to execute command via SessionService: 500 Session Service Error"));
        verify(activityLogServiceMock).createLog(eq(testServerId),eq(testUserId),eq(commandRequest.getCommand()), contains("SessionService error: 500"), eq("FAILURE"),isNull(), anyString(), isNull());
    }

    @Test
    void executeCommand_sessionServiceReturnsNullBody_logsAndReturnsNull() {
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testSessionToken)).thenReturn(Optional.of(testSessionKey));
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenReturn(mock(UserSpecificConnectionDetails.class));

        ResponseEntity<CommandResponse> nullBodyResponse = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CommandResponse.class)))
            .thenReturn(nullBodyResponse);

        CommandResponse actualResponse = remoteExecutionService.executeCommand(testServerId, testSessionToken, commandRequest);

        assertNull(actualResponse); // Or throw an exception based on desired behavior
        verify(activityLogServiceMock).createLog(eq(testServerId),eq(testUserId),eq(commandRequest.getCommand()), contains("Command execution via SessionService returned null body."), eq("FAILURE"),isNull(), isNull(), isNull());
    }
}
