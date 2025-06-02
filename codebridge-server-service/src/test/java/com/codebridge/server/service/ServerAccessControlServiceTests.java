package com.codebridge.server.service;

import com.codebridge.server.dto.ServerUserRequest;
import com.codebridge.server.dto.ServerUserResponse;
import com.codebridge.server.exception.AccessDeniedException;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.ServerUser;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.repository.ServerRepository;
import com.codebridge.server.repository.ServerUserRepository;
import com.codebridge.server.repository.SshKeyRepository;
import com.codebridge.server.service.ServerAccessControlService.UserSpecificConnectionDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerAccessControlServiceTests {

    @Mock
    private ServerUserRepository serverUserRepository;

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private SshKeyRepository sshKeyRepository;

    @Mock
    private SshKeyManagementService sshKeyManagementService;

    // ServerActivityLogService is not a direct dependency, removing mock
    // @Mock
    // private ServerActivityLogService serverActivityLogService;

    @Mock
    private CacheManager cacheManagerMock;
    @Mock
    private Cache userServerAccessDetailsCacheMock;

    @InjectMocks
    private ServerAccessControlService serverAccessControlService;

    @Captor
    private ArgumentCaptor<ServerUser> serverUserCaptor;

    private UUID adminUserId;
    private UUID targetUserId;
    private UUID serverId;
    private UUID sshKeyId;

    private Server server;
    private SshKey sshKey;
    private ServerUserRequest serverUserRequest;
    private ServerUser serverUser;

    @BeforeEach
    void setUp() {
        adminUserId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
        serverId = UUID.randomUUID();
        sshKeyId = UUID.randomUUID();

        server = new Server();
        server.setId(serverId);
        server.setUserId(adminUserId); // Admin owns this server
        server.setName("TestServer");
        server.setAuthProvider(ServerAuthProvider.SSH_KEY);

        sshKey = new SshKey();
        sshKey.setId(sshKeyId);
        sshKey.setName("TestKey");
        sshKey.setUserId(adminUserId); // Admin owns this key

        serverUserRequest = new ServerUserRequest();
        serverUserRequest.setServerId(serverId);
        serverUserRequest.setPlatformUserId(targetUserId);
        serverUserRequest.setRemoteUsernameForUser("targetRemoteUser");
        serverUserRequest.setSshKeyIdForUser(sshKeyId);

        serverUser = new ServerUser();
        serverUser.setId(UUID.randomUUID());
        serverUser.setServer(server);
        serverUser.setPlatformUserId(targetUserId);
        serverUser.setRemoteUsernameForUser("targetRemoteUser");
        serverUser.setSshKeyForUser(sshKey);
        serverUser.setAccessGrantedBy(adminUserId);
        serverUser.setCreatedAt(LocalDateTime.now());
        serverUser.setUpdatedAt(LocalDateTime.now());

        // Setup CacheManager mock
        when(cacheManagerMock.getCache("userServerAccessDetails")).thenReturn(userServerAccessDetailsCacheMock);
        // serverActivityLogService mock removed
    }

    @Test
    void grantServerAccess_success_newGrant() {
        when(serverRepository.findById(serverId)).thenReturn(Optional.of(server));
        when(sshKeyRepository.findById(sshKeyId)).thenReturn(Optional.of(sshKey));
        when(serverUserRepository.findByServerIdAndPlatformUserId(serverId, targetUserId)).thenReturn(Optional.empty());
        when(serverUserRepository.save(any(ServerUser.class))).thenReturn(serverUser);

        ServerUserResponse response = serverAccessControlService.grantServerAccess(adminUserId, serverUserRequest);

        assertNotNull(response);
        assertEquals(targetUserId, response.getPlatformUserId());
        assertEquals(serverId, response.getServerId());
        assertEquals("targetRemoteUser", response.getRemoteUsernameForUser());
        assertEquals(sshKeyId, response.getSshKeyIdForUser());

        verify(serverUserRepository).save(serverUserCaptor.capture());
        ServerUser captured = serverUserCaptor.getValue();
        assertEquals(server, captured.getServer());
        assertEquals(targetUserId, captured.getPlatformUserId());
        assertEquals("targetRemoteUser", captured.getRemoteUsernameForUser());
        assertEquals(sshKey, captured.getSshKeyForUser());
        assertEquals(adminUserId, captured.getAccessGrantedBy());

        // Verify cache eviction
        String expectedCacheKey = targetUserId.toString() + ":" + serverId.toString();
        verify(userServerAccessDetailsCacheMock, times(1)).evict(expectedCacheKey);
        // serverActivityLogService.createLog verification removed
    }

    @Test
    void grantServerAccess_adminNotOwner_throwsAccessDeniedException() {
        server.setUserId(UUID.randomUUID()); // Different owner
        when(serverRepository.findById(serverId)).thenReturn(Optional.of(server));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            serverAccessControlService.grantServerAccess(adminUserId, serverUserRequest);
        });
        assertTrue(exception.getMessage().contains("does not have rights to grant access"));
        // serverActivityLogService.createLog verification removed
        verify(userServerAccessDetailsCacheMock, never()).evict(any()); // Eviction should not happen on failure before main logic
    }

    @Test
    void grantServerAccess_serverRequiresSshKey_butNoKeyProvided_throwsIllegalArgumentException() {
        server.setAuthProvider(ServerAuthProvider.SSH_KEY);
        serverUserRequest.setSshKeyIdForUser(null); // No key provided
        when(serverRepository.findById(serverId)).thenReturn(Optional.of(server));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            serverAccessControlService.grantServerAccess(adminUserId, serverUserRequest);
        });
        assertTrue(exception.getMessage().contains("SSH Key ID must be provided"));
    }


    @Test
    void revokeServerAccess_success() {
        when(serverRepository.findById(serverId)).thenReturn(Optional.of(server));
        when(serverUserRepository.findByServerIdAndPlatformUserId(serverId, targetUserId)).thenReturn(Optional.of(serverUser));
        doNothing().when(serverUserRepository).delete(serverUser);

        serverAccessControlService.revokeServerAccess(adminUserId, serverId, targetUserId);

        verify(serverUserRepository).delete(serverUser);
        // Verify cache eviction
        String expectedCacheKey = targetUserId.toString() + ":" + serverId.toString();
        verify(userServerAccessDetailsCacheMock, times(1)).evict(expectedCacheKey);
        // serverActivityLogService.createLog verification removed
    }

    @Test
    void revokeServerAccess_grantNotFound_throwsResourceNotFoundException() {
        when(serverRepository.findById(serverId)).thenReturn(Optional.of(server));
        when(serverUserRepository.findByServerIdAndPlatformUserId(serverId, targetUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            serverAccessControlService.revokeServerAccess(adminUserId, serverId, targetUserId);
        });
    }


    // --- Caching tests for checkUserAccessAndGetConnectionDetails ---
    @Test
    void checkUserAccessAndGetConnectionDetails_whenNotCached_callsServiceAndCaches() {
        when(serverUserRepository.findByServerIdAndPlatformUserId(serverId, targetUserId)).thenReturn(Optional.of(serverUser));
        // Note: sshKeyManagementService.getDecryptedSshKey is NOT called by checkUserAccessAndGetConnectionDetails anymore

        String expectedCacheKey = targetUserId.toString() + ":" + serverId.toString();
        when(userServerAccessDetailsCacheMock.get(expectedCacheKey)).thenReturn(null); // Cache miss
        doNothing().when(userServerAccessDetailsCacheMock).put(eq(expectedCacheKey), any(UserSpecificConnectionDetails.class));

        UserSpecificConnectionDetails details = serverAccessControlService.checkUserAccessAndGetConnectionDetails(targetUserId, serverId);

        assertNotNull(details);
        assertEquals(server, details.server());
        assertEquals("targetRemoteUser", details.remoteUsername());
        assertEquals(sshKeyId, details.sshKeyIdToUse()); // Verify sshKeyIdToUse

        verify(serverUserRepository, times(1)).findByServerIdAndPlatformUserId(serverId, targetUserId);
        verify(userServerAccessDetailsCacheMock, times(1)).get(expectedCacheKey);
        verify(userServerAccessDetailsCacheMock, times(1)).put(eq(expectedCacheKey), eq(details));
    }

    @Test
    void checkUserAccessAndGetConnectionDetails_whenCached_returnsCached() {
        String expectedCacheKey = targetUserId.toString() + ":" + serverId.toString();
        UserSpecificConnectionDetails cachedDetails = new UserSpecificConnectionDetails(server, "cachedUser", sshKeyId);

        Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);
        when(valueWrapper.get()).thenReturn(cachedDetails);
        when(userServerAccessDetailsCacheMock.get(expectedCacheKey)).thenReturn(valueWrapper);

        UserSpecificConnectionDetails details = serverAccessControlService.checkUserAccessAndGetConnectionDetails(targetUserId, serverId);

        assertNotNull(details);
        assertSame(cachedDetails, details);
        verify(serverUserRepository, never()).findByServerIdAndPlatformUserId(any(), any());
        verify(userServerAccessDetailsCacheMock, times(1)).get(expectedCacheKey);
        verify(userServerAccessDetailsCacheMock, never()).put(anyString(), any());
    }

    @Test
    void checkUserAccessAndGetConnectionDetails_serverIsPasswordAuth_userHasNoKey_success() {
        server.setAuthProvider(ServerAuthProvider.PASSWORD); // Server itself is password based
        serverUser.setSshKeyForUser(null); // User access grant does not specify a key

        when(serverUserRepository.findByServerIdAndPlatformUserId(serverId, targetUserId)).thenReturn(Optional.of(serverUser));

        UserSpecificConnectionDetails details = serverAccessControlService.checkUserAccessAndGetConnectionDetails(targetUserId, serverId);

        assertNotNull(details);
        assertEquals(server, details.server());
        assertEquals("targetRemoteUser", details.remoteUsername());
        assertNull(details.decryptedSshKey()); // No key expected
    }


    @Test
    void checkUserAccessAndGetConnectionDetails_accessDenied_noGrant() {
        when(serverUserRepository.findByServerIdAndPlatformUserId(serverId, targetUserId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> {
            serverAccessControlService.checkUserAccessAndGetConnectionDetails(targetUserId, serverId);
        });
    }

    @Test
    void checkUserAccessAndGetConnectionDetails_serverRequiresSshKey_userGrantHasNoKey_throwsAccessDenied() {
        server.setAuthProvider(ServerAuthProvider.SSH_KEY); // Server needs SSH
        serverUser.setSshKeyForUser(null); // User grant doesn't have one

        when(serverUserRepository.findByServerIdAndPlatformUserId(serverId, targetUserId)).thenReturn(Optional.of(serverUser));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            serverAccessControlService.checkUserAccessAndGetConnectionDetails(targetUserId, serverId);
        });
        assertTrue(exception.getMessage().contains("has no SSH key assigned for this SSH-key authenticated server"));
    }
}
