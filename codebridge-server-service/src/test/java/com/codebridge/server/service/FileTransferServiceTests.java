package com.codebridge.server.service;

import com.codebridge.server.dto.remote.RemoteFileEntry;
import com.codebridge.server.exception.AccessDeniedException;
import com.codebridge.server.exception.FileTransferException;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server;
import com.codebridge.server.dto.remote.RemoteFileEntry;
import com.codebridge.server.exception.AccessDeniedException;
import com.codebridge.server.exception.FileTransferException;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server; // Keep for UserSpecificConnectionDetails
import com.codebridge.server.model.SshKey;   // Keep for UserSpecificConnectionDetails
import com.codebridge.server.sessions.SessionKey; // For mocking jwtTokenProvider
import com.codebridge.server.security.jwt.JwtTokenProvider;
import com.codebridge.server.service.ServerAccessControlService.UserSpecificConnectionDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
// import org.mockito.MockedStatic; // Not used currently
// import org.mockito.Mockito; // Already static imported
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
// import java.util.Vector; // No longer used

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyInt; // No longer used
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileTransferServiceTests {

    @Mock
    private ServerAccessControlService serverAccessControlServiceMock;
    @Mock
    private JwtTokenProvider jwtTokenProviderMock;
    @Mock
    private RestTemplate restTemplateMock;
    @Mock
    private ServerActivityLogService activityLogServiceMock;

    @InjectMocks
    private FileTransferService fileTransferService;

    private UUID testUserId;
    private UUID testServerId;
    private String testSessionToken;
    private SessionKey testSessionKey;
    private String sessionServiceBaseUrl = "http://fake-session-service/api";
    // UserSpecificConnectionDetails connectionDetails; // Not directly used in this manner anymore
    // Server server; // Not directly used
    // SshKey decryptedSshKey; // Not directly used

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testServerId = UUID.randomUUID();
        testSessionToken = "test.sftp.jwt.token";
        testSessionKey = new SessionKey(testUserId, testServerId, "SSH");

        ReflectionTestUtils.setField(fileTransferService, "sessionServiceBaseUrl", sessionServiceBaseUrl);
        doNothing().when(activityLogServiceMock).createLog(any(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void listFiles_success() {
        String remotePath = "/home/user";
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testSessionToken)).thenReturn(Optional.of(testSessionKey));
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenReturn(mock(UserSpecificConnectionDetails.class)); // Authorization passes

        List<RemoteFileEntry> expectedResponse = Collections.singletonList(new RemoteFileEntry("file.txt", false, 100, "","", ""));
        ResponseEntity<List<RemoteFileEntry>> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        when(restTemplateMock.exchange(
            eq(sessionServiceBaseUrl + "/ops/ssh/" + testSessionToken + "/sftp/list?remotePath=" + remotePath),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class) // For List<RemoteFileEntry>
        )).thenReturn(responseEntity);

        List<RemoteFileEntry> actualResponse = fileTransferService.listFiles(testServerId, testSessionToken, remotePath);

        assertNotNull(actualResponse);
        assertEquals(1, actualResponse.size());
        assertEquals("file.txt", actualResponse.get(0).getFilename());
        verify(activityLogServiceMock).createLog(eq(testUserId), eq("SFTP_LIST_FILES"), eq(testServerId), anyString(), eq("SUCCESS"), isNull());
    }

    @Test
    void listFiles_sessionServiceReturnsError_throwsFileTransferException() {
        String remotePath = "/home/user";
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testSessionToken)).thenReturn(Optional.of(testSessionKey));
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenReturn(mock(UserSpecificConnectionDetails.class));

        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Session Service SFTP Error"));

        FileTransferException thrown = assertThrows(FileTransferException.class, () -> {
            fileTransferService.listFiles(testServerId, testSessionToken, remotePath);
        });
        assertTrue(thrown.getMessage().contains("Failed to list files via SessionService: 500 Session Service SFTP Error"));
        verify(activityLogServiceMock).createLog(eq(testUserId), eq("SFTP_LIST_FILES"), eq(testServerId), anyString(), eq("FAILURE"), anyString());
    }

    @Test
    void listFiles_authorizationFailed_throwsFileTransferException() {
        String remotePath = "/home/user";
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testSessionToken)).thenReturn(Optional.of(testSessionKey));
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenThrow(new AccessDeniedException("User not authorized for SFTP on this server."));

        FileTransferException thrown = assertThrows(FileTransferException.class, () -> {
            fileTransferService.listFiles(testServerId, testSessionToken, remotePath);
        });
        assertEquals("Authorization failed for SFTP operation: User not authorized for SFTP on this server.", thrown.getMessage());
        verify(activityLogServiceMock).createLog(eq(testUserId), eq("SFTP_LIST_FILES"), eq(testServerId), anyString(), eq("FAILURE"), anyString());
    }


    @Test
    void downloadFile_success() {
        String remotePath = "/file.txt";
        byte[] expectedBytes = "file content".getBytes(StandardCharsets.UTF_8);
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testSessionToken)).thenReturn(Optional.of(testSessionKey));
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenReturn(mock(UserSpecificConnectionDetails.class));

        ResponseEntity<ByteArrayResource> responseEntity = new ResponseEntity<>(new ByteArrayResource(expectedBytes), HttpStatus.OK);
        when(restTemplateMock.exchange(
            eq(sessionServiceBaseUrl + "/ops/ssh/" + testSessionToken + "/sftp/download?remotePath=" + remotePath),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ByteArrayResource.class)
        )).thenReturn(responseEntity);

        byte[] actualBytes = fileTransferService.downloadFile(testServerId, testSessionToken, remotePath);

        assertArrayEquals(expectedBytes, actualBytes);
        verify(activityLogServiceMock).createLog(eq(testUserId), eq("SFTP_DOWNLOAD_FILE"), eq(testServerId), anyString(), eq("SUCCESS"), isNull());
    }

    @Test
    @Disabled("Upload test needs more specific setup for RestTemplate with Multipart")
    void uploadFile_success() throws IOException {
        String remotePath = "/uploads/";
        String remoteFileName = "uploaded.txt";
        byte[] fileContent = "upload this".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(fileContent);

        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(testSessionToken)).thenReturn(Optional.of(testSessionKey));
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenReturn(mock(UserSpecificConnectionDetails.class));

        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
        // This mocking for exchange with Multipart is tricky and might need ArgumentCaptor for body.
        when(restTemplateMock.exchange(
            anyString(), // Contains URL with remotePath query param
            eq(HttpMethod.POST),
            any(HttpEntity.class), // This entity should contain the multipart data
            eq(Void.class)
        )).thenReturn(responseEntity);

        fileTransferService.uploadFile(testServerId, testSessionToken, remotePath, inputStream, remoteFileName);

        verify(activityLogServiceMock).createLog(eq(testUserId), eq("SFTP_UPLOAD_FILE"), eq(testServerId), anyString(), eq("SUCCESS"), isNull());
        // Add ArgumentCaptor for HttpEntity<MultiValueMap<String, Object>> to verify file content and name
    }

    // Add more tests for error handling in download/upload (token, authz, SessionService errors)
    // similar to listFiles_sessionServiceReturnsError_throwsFileTransferException and listFiles_authorizationFailed_throwsFileTransferException
    }

}
