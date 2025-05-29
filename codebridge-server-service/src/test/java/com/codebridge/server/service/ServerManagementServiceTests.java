package com.codebridge.server.service;

import com.codebridge.server.dto.ServerRequest;
import com.codebridge.server.dto.ServerResponse;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.model.enums.ServerCloudProvider;
import com.codebridge.server.model.enums.ServerStatus;
import com.codebridge.server.repository.ServerRepository;
import com.codebridge.server.repository.SshKeyRepository;
import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;


import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerManagementServiceTests {

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private SshKeyRepository sshKeyRepository;

    @Mock
    @Qualifier("jasyptStringEncryptor")
    private StringEncryptor stringEncryptor;

    @Mock
    private ServerActivityLogService serverActivityLogService;

    @InjectMocks
    private ServerManagementService serverManagementService;

    @Captor
    private ArgumentCaptor<Server> serverCaptor;

    private UUID testUserId;
    private UUID testServerId;
    private UUID testSshKeyId;
    private ServerRequest serverRequest;
    private Server server;
    private SshKey sshKey;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testServerId = UUID.randomUUID();
        testSshKeyId = UUID.randomUUID();

        serverRequest = new ServerRequest();
        serverRequest.setName("Test Server");
        serverRequest.setHostname("test.example.com");
        serverRequest.setPort(22);
        serverRequest.setRemoteUsername("testuser");
        serverRequest.setOperatingSystem("Linux");
        serverRequest.setCloudProvider(ServerCloudProvider.AWS.name());

        sshKey = new SshKey();
        sshKey.setId(testSshKeyId);
        sshKey.setName("Test SSH Key");
        sshKey.setUserId(testUserId);

        server = new Server();
        server.setId(testServerId);
        server.setUserId(testUserId);
        server.setName("Existing Server");
        server.setHostname("existing.example.com");
        server.setPort(22);
        server.setRemoteUsername("existuser");
        server.setAuthProvider(ServerAuthProvider.PASSWORD);
        server.setPassword("encryptedPassword");
        server.setStatus(ServerStatus.ACTIVE);
        server.setCreatedAt(LocalDateTime.now().minusDays(1));
        server.setUpdatedAt(LocalDateTime.now().minusDays(1));
        
        doNothing().when(serverActivityLogService).createLog(any(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void createServer_withPasswordAuth_success() {
        serverRequest.setAuthProvider(ServerAuthProvider.PASSWORD.name());
        serverRequest.setPassword("plainPassword");

        when(stringEncryptor.encrypt("plainPassword")).thenReturn("encryptedPassword");
        when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> {
            Server s = invocation.getArgument(0);
            s.setId(UUID.randomUUID()); // Simulate ID generation
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            return s;
        });

        ServerResponse response = serverManagementService.createServer(serverRequest, testUserId);

        assertNotNull(response);
        assertEquals(serverRequest.getName(), response.getName());
        assertEquals(serverRequest.getHostname(), response.getHostname());
        assertEquals(ServerStatus.UNKNOWN.name(), response.getStatus()); // Initial status

        verify(serverRepository).save(serverCaptor.capture());
        Server capturedServer = serverCaptor.getValue();
        assertEquals("encryptedPassword", capturedServer.getPassword());
        assertEquals(ServerAuthProvider.PASSWORD, capturedServer.getAuthProvider());
        assertNull(capturedServer.getSshKey());
        
        verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("CREATE_SERVER"), 
            any(UUID.class), // Server ID is generated within save, so any()
            contains("Server created: Test Server"), 
            eq("SUCCESS"), 
            isNull()
        );
    }

    @Test
    void createServer_withSshKeyAuth_success() {
        serverRequest.setAuthProvider(ServerAuthProvider.SSH_KEY.name());
        serverRequest.setSshKeyId(testSshKeyId);
        serverRequest.setPassword(null);


        when(sshKeyRepository.findByIdAndUserId(testSshKeyId, testUserId)).thenReturn(Optional.of(sshKey));
        when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> {
            Server s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            return s;
        });

        ServerResponse response = serverManagementService.createServer(serverRequest, testUserId);

        assertNotNull(response);
        assertEquals(serverRequest.getName(), response.getName());
        assertEquals(testSshKeyId, response.getSshKeyId());

        verify(serverRepository).save(serverCaptor.capture());
        Server capturedServer = serverCaptor.getValue();
        assertEquals(ServerAuthProvider.SSH_KEY, capturedServer.getAuthProvider());
        assertNotNull(capturedServer.getSshKey());
        assertEquals(testSshKeyId, capturedServer.getSshKey().getId());
        assertNull(capturedServer.getPassword());
        
        verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("CREATE_SERVER"), 
            any(UUID.class),
            contains("Server created: Test Server"), 
            eq("SUCCESS"), 
            isNull()
        );
    }

    @Test
    void createServer_withSshKeyAuth_sshKeyNotFound_throwsResourceNotFoundException() {
        serverRequest.setAuthProvider(ServerAuthProvider.SSH_KEY.name());
        serverRequest.setSshKeyId(testSshKeyId);
        when(sshKeyRepository.findByIdAndUserId(testSshKeyId, testUserId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            serverManagementService.createServer(serverRequest, testUserId);
        });
        assertTrue(exception.getMessage().contains("SshKey not found with id : '" + testSshKeyId));
        verify(serverActivityLogService, never()).createLog(any(), eq("CREATE_SERVER"), any(), anyString(), anyString(), anyString());
    }
    
    @Test
    void createServer_withPasswordAuth_passwordBlank_throwsIllegalArgumentException() {
        serverRequest.setAuthProvider(ServerAuthProvider.PASSWORD.name());
        serverRequest.setPassword(""); // Blank password

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            serverManagementService.createServer(serverRequest, testUserId);
        });
        assertTrue(exception.getMessage().contains("Password cannot be blank for PASSWORD auth provider."));
    }


    @Test
    void getServerById_found() {
        when(serverRepository.findByIdAndUserId(testServerId, testUserId)).thenReturn(Optional.of(server));
        ServerResponse response = serverManagementService.getServerById(testServerId, testUserId);
        assertNotNull(response);
        assertEquals(server.getName(), response.getName());
    }

    @Test
    void getServerById_notFound_throwsResourceNotFoundException() {
        when(serverRepository.findByIdAndUserId(testServerId, testUserId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            serverManagementService.getServerById(testServerId, testUserId);
        });
    }

    @Test
    void listServersForUser_returnsListOfServers() {
        when(serverRepository.findByUserId(testUserId)).thenReturn(Collections.singletonList(server));
        List<ServerResponse> responses = serverManagementService.listServersForUser(testUserId);
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(server.getName(), responses.get(0).getName());
    }
    
    @Test
    void updateServer_success_changeToSshKeyAuth() {
        serverRequest.setName("Updated Server Name");
        serverRequest.setAuthProvider(ServerAuthProvider.SSH_KEY.name());
        serverRequest.setSshKeyId(testSshKeyId);
        serverRequest.setPassword(null); // Explicitly nullify password for SSH key auth

        when(serverRepository.findByIdAndUserId(testServerId, testUserId)).thenReturn(Optional.of(server));
        when(sshKeyRepository.findByIdAndUserId(testSshKeyId, testUserId)).thenReturn(Optional.of(sshKey));
        when(serverRepository.save(any(Server.class))).thenReturn(server);

        ServerResponse response = serverManagementService.updateServer(testServerId, serverRequest, testUserId);

        assertNotNull(response);
        assertEquals("Updated Server Name", response.getName());
        assertEquals(ServerAuthProvider.SSH_KEY.name(), response.getAuthProvider());
        assertEquals(testSshKeyId, response.getSshKeyId());

        verify(serverRepository).save(serverCaptor.capture());
        Server captured = serverCaptor.getValue();
        assertEquals("Updated Server Name", captured.getName());
        assertEquals(ServerAuthProvider.SSH_KEY, captured.getAuthProvider());
        assertNotNull(captured.getSshKey());
        assertEquals(testSshKeyId, captured.getSshKey().getId());
        assertNull(captured.getPassword()); // Password should be nulled out
        
        verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("UPDATE_SERVER"), 
            eq(testServerId), 
            contains("Server updated: Updated Server Name"), 
            eq("SUCCESS"), 
            isNull()
        );
    }
    
    @Test
    void updateServer_success_changeToPasswordAuth() {
        server.setAuthProvider(ServerAuthProvider.SSH_KEY); // Start with SSH Key
        server.setSshKey(sshKey);

        serverRequest.setName("Updated Server Pwd");
        serverRequest.setAuthProvider(ServerAuthProvider.PASSWORD.name());
        serverRequest.setPassword("newPlainPassword");
        serverRequest.setSshKeyId(null); // Nullify SSH key for password auth

        when(serverRepository.findByIdAndUserId(testServerId, testUserId)).thenReturn(Optional.of(server));
        when(stringEncryptor.encrypt("newPlainPassword")).thenReturn("newEncryptedPassword");
        when(serverRepository.save(any(Server.class))).thenReturn(server);
        
        ServerResponse response = serverManagementService.updateServer(testServerId, serverRequest, testUserId);

        assertNotNull(response);
        assertEquals("Updated Server Pwd", response.getName());
        assertEquals(ServerAuthProvider.PASSWORD.name(), response.getAuthProvider());
        assertNull(response.getSshKeyId());

        verify(serverRepository).save(serverCaptor.capture());
        Server captured = serverCaptor.getValue();
        assertEquals("Updated Server Pwd", captured.getName());
        assertEquals(ServerAuthProvider.PASSWORD, captured.getAuthProvider());
        assertEquals("newEncryptedPassword", captured.getPassword());
        assertNull(captured.getSshKey()); // SSH Key should be nulled out
        
        verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("UPDATE_SERVER"), 
            eq(testServerId), 
            contains("Server updated: Updated Server Pwd"), 
            eq("SUCCESS"), 
            isNull()
        );
    }


    @Test
    void deleteServer_success() {
        when(serverRepository.findByIdAndUserId(testServerId, testUserId)).thenReturn(Optional.of(server));
        doNothing().when(serverRepository).delete(server);

        serverManagementService.deleteServer(testServerId, testUserId);

        verify(serverRepository).delete(server);
        verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("DELETE_SERVER"), 
            eq(testServerId), 
            contains("Server deleted: Existing Server"), 
            eq("SUCCESS"), 
            isNull()
        );
    }

    @Test
    void deleteServer_notFound_throwsResourceNotFoundException() {
        when(serverRepository.findByIdAndUserId(testServerId, testUserId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            serverManagementService.deleteServer(testServerId, testUserId);
        });
         verify(serverActivityLogService, never()).createLog(any(), eq("DELETE_SERVER"), any(), anyString(), anyString(), anyString());
    }
}
