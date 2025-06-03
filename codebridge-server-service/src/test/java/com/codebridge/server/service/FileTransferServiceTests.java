package com.codebridge.server.service;

import com.codebridge.server.dto.UserSpecificConnectionDetailsDto;
import com.codebridge.server.dto.file.RemoteFileEntry;
import com.codebridge.server.exception.FileTransferException;
import com.codebridge.server.util.JwtUtil; // Placeholder
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileTransferServiceTests {

    @Mock private RestTemplate restTemplate;
    @Mock private ServerAccessControlService serverAccessControlService;
    @Mock private ServerActivityLogService activityLogService;
    // JwtUtil is new'd up, similar challenge as in RemoteExecutionServiceTests

    @InjectMocks
    private FileTransferService fileTransferService;

    private final String sessionServiceBaseUrl = "http://dummy-session-service/api/sessions";
    private final UUID serverId = UUID.randomUUID();
    private final UUID platformUserId = UUID.randomUUID();
    private final String sessionToken = "test-sftp-session-token";
    private final String remotePath = "/test/path";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileTransferService, "sessionServiceBaseUrl", sessionServiceBaseUrl);

        // Mocking for validateTokenAndAuthorize helper method (implicitly)
        Claims mockClaims = Jwts.claims().setSubject(platformUserId.toString());
        mockClaims.put("resourceId", serverId.toString());
        mockClaims.put("type", "SSH"); // SFTP uses SSH sessions
        // We assume JwtUtil works for parsing; direct mocking is hard here.

        when(serverAccessControlService.getValidatedConnectionDetails(platformUserId, serverId))
            .thenReturn(mock(UserSpecificConnectionDetailsDto.class)); // Non-null to pass validation
    }

    @Test
    void listFiles_success() {
        RemoteFileEntry entry = new RemoteFileEntry("file.txt", false, 100L, "sometime", "rw-r--r--");
        List<RemoteFileEntry> expectedResponse = Collections.singletonList(entry);
        ResponseEntity<List<RemoteFileEntry>> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        when(restTemplate.exchange(
            contains("/ops/ssh/" + sessionToken + "/sftp/list"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class))) // Use ParameterizedTypeReference for List<T>
            .thenReturn(responseEntity);

        List<RemoteFileEntry> actualResponse = fileTransferService.listFiles(serverId, sessionToken, remotePath);

        assertNotNull(actualResponse);
        assertEquals(1, actualResponse.size());
        assertEquals("file.txt", actualResponse.get(0).getFilename());
        verify(activityLogService).createLog(eq(platformUserId), eq("FILE_LIST_PROXY"), eq(serverId), anyString(), eq("SUCCESS"), eq(null));
    }

    @Test
    void downloadFile_success() {
        byte[] fileContent = "dummy content".getBytes();
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(fileContent, HttpStatus.OK);

        when(restTemplate.exchange(
            contains("/ops/ssh/" + sessionToken + "/sftp/download"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(byte[].class)))
            .thenReturn(responseEntity);

        byte[] actualContent = fileTransferService.downloadFile(serverId, sessionToken, remotePath + "/file.txt");

        assertArrayEquals(fileContent, actualContent);
        verify(activityLogService).createLog(eq(platformUserId), eq("FILE_DOWNLOAD_PROXY"), eq(serverId), anyString(), eq("SUCCESS"), eq(null));
    }

    @Test
    void uploadFile_success() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("upload content".getBytes());
        String remoteFileName = "upload.txt";

        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);

        when(restTemplate.postForEntity(
            contains("/ops/ssh/" + sessionToken + "/sftp/upload"),
            any(HttpEntity.class), // Check for HttpEntity<MultiValueMap<String, Object>>
            eq(Void.class)))
            .thenReturn(responseEntity);

        fileTransferService.uploadFile(serverId, sessionToken, remotePath, inputStream, remoteFileName);

        verify(activityLogService).createLog(eq(platformUserId), eq("FILE_UPLOAD_PROXY"), eq(serverId), anyString(), eq("SUCCESS"), eq(null));
    }

    @Test
    void listFiles_sessionServiceReturnsError_throwsFileTransferException() {
        when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Session Service Error"));

        assertThrows(FileTransferException.class, () -> {
            fileTransferService.listFiles(serverId, sessionToken, remotePath);
        });
         verify(activityLogService).createLog(eq(platformUserId), eq("FILE_LIST_PROXY_FAILED"), eq(serverId), anyString(), eq("FAILED"), anyString());
    }
}
