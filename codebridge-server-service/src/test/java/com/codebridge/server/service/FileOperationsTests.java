package com.codebridge.server.service;

import com.codebridge.server.exception.FileTransferException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileOperationsTests {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ServerActivityLogService activityLogService;

    @InjectMocks
    private FileTransferService fileTransferService;

    private final UUID serverId = UUID.randomUUID();
    private final UUID platformUserId = UUID.randomUUID();
    private final String sessionToken = "test-session-token";
    private final String sessionServiceBaseUrl = "http://localhost:8082/api/sessions";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileTransferService, "sessionServiceBaseUrl", sessionServiceBaseUrl);
        
        // Mock the validateTokenAndAuthorize method to return platformUserId
        doReturn(platformUserId).when(fileTransferService).validateTokenAndAuthorize(eq(serverId), eq(sessionToken));
    }

    @Test
    void deleteFile_Success() {
        // Arrange
        String remotePath = "/path/to/file.txt";
        boolean recursive = false;

        // Act
        fileTransferService.deleteFile(serverId, sessionToken, remotePath, recursive);

        // Assert
        verify(restTemplate).exchange(
                contains("/ops/ssh/" + sessionToken + "/sftp/delete"),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Void.class)
        );
        verify(activityLogService).createLog(
                eq(platformUserId),
                eq("FILE_DELETE_PROXY"),
                eq(serverId),
                contains(remotePath),
                eq("SUCCESS"),
                isNull()
        );
    }

    @Test
    void deleteFile_HttpError() {
        // Arrange
        String remotePath = "/path/to/file.txt";
        boolean recursive = false;
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND, "File not found");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenThrow(exception);

        // Act & Assert
        assertThrows(FileTransferException.class, () ->
                fileTransferService.deleteFile(serverId, sessionToken, remotePath, recursive));

        verify(activityLogService).createLog(
                eq(platformUserId),
                eq("FILE_DELETE_PROXY_FAILED"),
                eq(serverId),
                contains(remotePath),
                eq("FAILED"),
                anyString()
        );
    }

    @Test
    void changeFilePermissions_Success() {
        // Arrange
        String remotePath = "/path/to/file.txt";
        String permissions = "755";
        boolean recursive = false;

        // Act
        fileTransferService.changeFilePermissions(serverId, sessionToken, remotePath, permissions, recursive);

        // Assert
        verify(restTemplate).exchange(
                contains("/ops/ssh/" + sessionToken + "/sftp/chmod"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        );
        verify(activityLogService).createLog(
                eq(platformUserId),
                eq("FILE_CHMOD_PROXY"),
                eq(serverId),
                contains(remotePath),
                eq("SUCCESS"),
                isNull()
        );
    }

    @Test
    void changeFilePermissions_HttpError() {
        // Arrange
        String remotePath = "/path/to/file.txt";
        String permissions = "755";
        boolean recursive = false;
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.FORBIDDEN, "Permission denied");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenThrow(exception);

        // Act & Assert
        assertThrows(FileTransferException.class, () ->
                fileTransferService.changeFilePermissions(serverId, sessionToken, remotePath, permissions, recursive));

        verify(activityLogService).createLog(
                eq(platformUserId),
                eq("FILE_CHMOD_PROXY_FAILED"),
                eq(serverId),
                contains(remotePath),
                eq("FAILED"),
                anyString()
        );
    }

    @Test
    void renameFile_Success() {
        // Arrange
        String sourcePath = "/path/to/source.txt";
        String targetPath = "/path/to/target.txt";

        // Act
        fileTransferService.renameFile(serverId, sessionToken, sourcePath, targetPath);

        // Assert
        verify(restTemplate).exchange(
                contains("/ops/ssh/" + sessionToken + "/sftp/rename"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        );
        verify(activityLogService).createLog(
                eq(platformUserId),
                eq("FILE_RENAME_PROXY"),
                eq(serverId),
                contains(sourcePath),
                eq("SUCCESS"),
                isNull()
        );
    }

    @Test
    void renameFile_HttpError() {
        // Arrange
        String sourcePath = "/path/to/source.txt";
        String targetPath = "/path/to/target.txt";
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.CONFLICT, "Target file already exists");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenThrow(exception);

        // Act & Assert
        assertThrows(FileTransferException.class, () ->
                fileTransferService.renameFile(serverId, sessionToken, sourcePath, targetPath));

        verify(activityLogService).createLog(
                eq(platformUserId),
                eq("FILE_RENAME_PROXY_FAILED"),
                eq(serverId),
                contains(sourcePath),
                eq("FAILED"),
                anyString()
        );
    }
}

