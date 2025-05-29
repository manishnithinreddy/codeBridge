package com.codebridge.server.service;

import com.codebridge.server.dto.ServerActivityLogResponse;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.ServerActivityLog;
import com.codebridge.server.repository.ServerActivityLogRepository;
import com.codebridge.server.repository.ServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerActivityLogServiceTests {

    @Mock
    private ServerActivityLogRepository serverActivityLogRepository;

    @Mock
    private ServerRepository serverRepository;

    @InjectMocks
    private ServerActivityLogService serverActivityLogService;

    @Captor
    private ArgumentCaptor<ServerActivityLog> activityLogCaptor;

    private UUID testUserId;
    private UUID testServerId;
    private Server server;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testServerId = UUID.randomUUID();

        server = new Server();
        server.setId(testServerId);
        server.setName("TestServer");
    }

    @Test
    void createLog_success() {
        String action = "TEST_ACTION";
        String details = "Test details";
        String status = "SUCCESS";
        String errorMessage = null;

        serverActivityLogService.createLog(testUserId, action, testServerId, details, status, errorMessage);

        verify(serverActivityLogRepository).save(activityLogCaptor.capture());
        ServerActivityLog capturedLog = activityLogCaptor.getValue();

        assertEquals(testUserId, capturedLog.getPlatformUserId());
        assertEquals(action, capturedLog.getAction());
        assertEquals(testServerId, capturedLog.getServer().getId());
        assertEquals(details, capturedLog.getDetails());
        assertEquals(status, capturedLog.getStatus());
        assertNull(capturedLog.getErrorMessage());
        assertNotNull(capturedLog.getTimestamp());
    }
    
    @Test
    void createLog_noServerId_success() {
        String action = "USER_ACTION";
        String details = "User logged in";
        String status = "SUCCESS";

        serverActivityLogService.createLog(testUserId, action, null, details, status, null);

        verify(serverActivityLogRepository).save(activityLogCaptor.capture());
        ServerActivityLog capturedLog = activityLogCaptor.getValue();

        assertEquals(testUserId, capturedLog.getPlatformUserId());
        assertEquals(action, capturedLog.getAction());
        assertNull(capturedLog.getServer());
        assertEquals(details, capturedLog.getDetails());
        assertEquals(status, capturedLog.getStatus());
        assertNull(capturedLog.getErrorMessage());
    }


    @Test
    void getLogsForServer_success() {
        ServerActivityLog log = new ServerActivityLog();
        log.setId(UUID.randomUUID());
        log.setPlatformUserId(testUserId);
        log.setAction("TEST_ACTION");
        log.setServer(server); 
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<ServerActivityLog> page = new PageImpl<>(Collections.singletonList(log), pageable, 1);

        when(serverActivityLogRepository.findByServerId(testServerId, pageable)).thenReturn(page);
        when(serverRepository.findById(testServerId)).thenReturn(Optional.of(server)); // For server name mapping

        Page<ServerActivityLogResponse> responsePage = serverActivityLogService.getLogsForServer(testServerId, pageable);

        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());
        ServerActivityLogResponse responseDto = responsePage.getContent().get(0);
        assertEquals(log.getAction(), responseDto.getAction());
        assertEquals(testServerId, responseDto.getServerId());
        assertEquals("TestServer", responseDto.getServerName());
    }
    
    @Test
    void getLogsForServer_serverNameMappingWhenServerMissing_handlesGracefully() {
        ServerActivityLog log = new ServerActivityLog();
        log.setId(UUID.randomUUID());
        log.setPlatformUserId(testUserId);
        log.setAction("TEST_ACTION_NO_SERVER_NAME");
        
        Server serverRef = new Server(); // Server reference with only ID
        serverRef.setId(testServerId);
        log.setServer(serverRef); 
        
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<ServerActivityLog> page = new PageImpl<>(Collections.singletonList(log), pageable, 1);

        when(serverActivityLogRepository.findByServerId(testServerId, pageable)).thenReturn(page);
        when(serverRepository.findById(testServerId)).thenReturn(Optional.empty()); // Simulate server deleted after log created

        Page<ServerActivityLogResponse> responsePage = serverActivityLogService.getLogsForServer(testServerId, pageable);

        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());
        ServerActivityLogResponse responseDto = responsePage.getContent().get(0);
        assertEquals(log.getAction(), responseDto.getAction());
        assertEquals(testServerId, responseDto.getServerId());
        assertNull(responseDto.getServerName(), "Server name should be null if server not found during mapping");
    }


    @Test
    void getLogsForUser_success() {
        ServerActivityLog log = new ServerActivityLog();
        log.setId(UUID.randomUUID());
        log.setPlatformUserId(testUserId);
        log.setAction("USER_ACTION");
        log.setServer(server); // Log can optionally have a server
        log.setStatus("SUCCESS");
        log.setTimestamp(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<ServerActivityLog> page = new PageImpl<>(Collections.singletonList(log), pageable, 1);

        when(serverActivityLogRepository.findByPlatformUserId(testUserId, pageable)).thenReturn(page);
        when(serverRepository.findById(testServerId)).thenReturn(Optional.of(server)); // For server name

        Page<ServerActivityLogResponse> responsePage = serverActivityLogService.getLogsForUser(testUserId, pageable);

        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());
        ServerActivityLogResponse responseDto = responsePage.getContent().get(0);
        assertEquals(log.getAction(), responseDto.getAction());
        assertEquals(testUserId, responseDto.getPlatformUserId());
        assertEquals(server.getName(), responseDto.getServerName());
    }
}
