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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;


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

    // ServerActivityLogService is not a direct dependency, removing mock
    // @Mock
    // private ServerActivityLogService serverActivityLogService;

    @Mock
    private CacheManager cacheManagerMock;
    @Mock
    private Cache serverByIdCacheMock;
    @Mock
    private Cache userServerAccessDetailsCacheMock; // For eviction from deleteServer

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

        // Setup CacheManager mock
        when(cacheManagerMock.getCache("serverById")).thenReturn(serverByIdCacheMock);
        when(cacheManagerMock.getCache("userServerAccessDetails")).thenReturn(userServerAccessDetailsCacheMock);
        // Removed serverActivityLogService mock as it's not a direct dependency
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

        // serverActivityLogService.createLog verification removed as it's not part of this service's direct responsibility for caching tests
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

        // serverActivityLogService.createLog verification removed
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
        // serverActivityLogService.createLog verification removed
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


    // --- Caching Tests for getServerById ---
    @Test
    void getServerById_whenNotCached_callsRepoAndCaches() {
        when(serverRepository.findByIdAndUserId(testServerId, testUserId)).thenReturn(Optional.of(server));
        when(serverByIdCacheMock.get(testServerId.toString())).thenReturn(null); // Cache miss
        doNothing().when(serverByIdCacheMock).put(eq(testServerId.toString()), any(ServerResponse.class));

        ServerResponse response = serverManagementService.getServerById(testServerId, testUserId);

        assertNotNull(response);
        assertEquals(server.getName(), response.getName());
        verify(serverRepository, times(1)).findByIdAndUserId(testServerId, testUserId);
        verify(serverByIdCacheMock, times(1)).get(testServerId.toString());
        verify(serverByIdCacheMock, times(1)).put(eq(testServerId.toString()), any(ServerResponse.class));
    }

    @Test
    void getServerById_whenCached_returnsCachedAndNotRepo() {
        ServerResponse cachedResponse = new ServerResponse();
        cachedResponse.setId(testServerId);
        cachedResponse.setName("Cached Server");

        Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);
        when(valueWrapper.get()).thenReturn(cachedResponse);
        when(serverByIdCacheMock.get(testServerId.toString())).thenReturn(valueWrapper);

        ServerResponse response = serverManagementService.getServerById(testServerId, testUserId);

        assertNotNull(response);
        assertEquals("Cached Server", response.getName());
        verify(serverRepository, never()).findByIdAndUserId(any(), any());
        verify(serverByIdCacheMock, times(1)).get(testServerId.toString());
        verify(serverByIdCacheMock, never()).put(anyString(), any());
    }

    @Test
    void getServerById_notFound_throwsResourceNotFound_AndNoCachePut() {
        when(serverRepository.findByIdAndUserId(testServerId, testUserId)).thenReturn(Optional.empty());
        when(serverByIdCacheMock.get(testServerId.toString())).thenReturn(null); // Cache miss

        assertThrows(ResourceNotFoundException.class, () -> {
            serverManagementService.getServerById(testServerId, testUserId);
        });
        verify(serverByIdCacheMock, never()).put(anyString(), any());
    }

    @Test
    void listServersForUser_returnsListOfServers() {
        when(serverRepository.findByUserId(testUserId)).thenReturn(Collections.singletonList(server));
        List<ServerResponse> responses = serverManagementService.listServersForUser(testUserId);
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(server.getName(), responses.get(0).getName());
    }

    // --- Caching Tests for updateServer ---
    @Test
    void updateServer_callsRepoSaveAndUpdatesCache() {
        serverRequest.setName("Updated Server Name For CachePut");
        // Assume other fields are set in serverRequest as needed for a valid update

        when(serverRepository.findByIdAndUserId(testServerId, testUserId)).thenReturn(Optional.of(server)); // server is the existing one
        // Mock the save operation to return the (conceptually) updated server object
        // In a real scenario, server object's state would be modified before save is called by the service method.
        when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> {
            Server s = invocation.getArgument(0);
            s.setName(serverRequest.getName()); // Simulate the update
            return s;
        });
        // Mock CachePut behavior - it will execute the method, then put the result.
        // We verify the 'put' operation.
        doAnswer(invocation -> {
            // invocation.getArgument(0) is key, invocation.getArgument(1) is value
            return null; // put method is void
        }).when(serverByIdCacheMock).put(eq(testServerId.toString()), any(ServerResponse.class));


        ServerResponse response = serverManagementService.updateServer(testServerId, serverRequest, testUserId);

        assertNotNull(response);
        assertEquals("Updated Server Name For CachePut", response.getName());

        verify(serverRepository, times(1)).findByIdAndUserId(testServerId, testUserId);
        verify(serverRepository, times(1)).save(any(Server.class)); // Method must be called
        verify(serverByIdCacheMock, times(1)).put(eq(testServerId.toString()), eq(response)); // Result is put into cache
    }

    // --- Caching Tests for deleteServer ---
    @Test
    void deleteServer_success_evictsCaches() {
        when(serverRepository.findByIdAndUserId(testServerId, testUserId)).thenReturn(Optional.of(server));
        doNothing().when(serverRepository).delete(server);
        doNothing().when(serverByIdCacheMock).evict(testServerId.toString());
        doNothing().when(userServerAccessDetailsCacheMock).clear();


        serverManagementService.deleteServer(testServerId, testUserId);

        verify(serverRepository).delete(server);
        verify(serverByIdCacheMock, times(1)).evict(testServerId.toString());
        verify(userServerAccessDetailsCacheMock, times(1)).clear(); // Verify userServerAccessDetails cache is cleared
        // serverActivityLogService.createLog verification removed
    }

    @Test
    void deleteServer_notFound_throwsResourceNotFoundException_andNoCacheEvict() {
        when(serverRepository.findByIdAndUserId(testServerId, testUserId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            serverManagementService.deleteServer(testServerId, testUserId);
        });
         verify(serverByIdCacheMock, never()).evict(any());
         verify(userServerAccessDetailsCacheMock, never()).clear();
         // serverActivityLogService.createLog verification removed
    }
}
