package com.codebridge.server.service;

import com.codebridge.server.dto.ServerUserRequest;
import com.codebridge.server.dto.ServerUserResponse;
import com.codebridge.server.dto.UserSpecificConnectionDetailsDto;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.ServerUser;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.repository.ServerRepository;
import com.codebridge.server.repository.ServerUserRepository;
import com.codebridge.server.repository.SshKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ServerAccessControlServiceCachingTests {

    @Mock private ServerUserRepository serverUserRepository;
    @Mock private ServerRepository serverRepository;
    @Mock private SshKeyRepository sshKeyRepository; // Dependency
    @Mock private SshKeyManagementService sshKeyManagementService; // Dependency
    @Mock private ServerManagementService serverManagementService; // Dependency
    @Mock private ServerActivityLogService activityLogService; // Dependency
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks
    private ServerAccessControlService serverAccessControlService;

    private UUID platformUserId;
    private UUID serverId;
    private UUID adminUserId;
    private Server serverEntity;
    private UserSpecificConnectionDetailsDto connectionDetailsDto;
    private String cacheKey;

    @BeforeEach
    void setUp() {
        platformUserId = UUID.randomUUID();
        serverId = UUID.randomUUID();
        adminUserId = UUID.randomUUID(); // Assuming admin is different from platformUserId for grant/revoke tests
        cacheKey = platformUserId.toString() + ":" + serverId.toString();

        serverEntity = new Server();
        serverEntity.setId(serverId);
        serverEntity.setUserId(adminUserId); // Admin owns the server
        serverEntity.setHostname("test-host");
        serverEntity.setPort(22);
        serverEntity.setRemoteUsername("default-user");
        serverEntity.setAuthProvider(ServerAuthProvider.SSH_KEY); // Example
        SshKey defaultKey = new SshKey(); defaultKey.setId(UUID.randomUUID());
        serverEntity.setSshKey(defaultKey);


        // This DTO is what would be returned by getValidatedConnectionDetails
        connectionDetailsDto = new UserSpecificConnectionDetailsDto(
            "test-host", 22, "default-user", ServerAuthProvider.SSH_KEY
        );
        // connectionDetailsDto.setDecryptedSshKey(...); // Populate if key is involved

        when(cacheManager.getCache("userServerAccessDetails")).thenReturn(cache);
    }

    @Test
    void getValidatedConnectionDetails_whenNotInCache_fetchesAndCaches() {
        when(cache.get(cacheKey)).thenReturn(null); // Not in cache
        when(serverRepository.findById(serverId)).thenReturn(Optional.of(serverEntity));
        // Assuming platformUserId is the owner for this specific path for simplicity
        // or mock ServerUser lookup if platformUserId is not owner
        if (serverEntity.getUserId().equals(platformUserId)) { // Test owner path
             when(sshKeyManagementService.getDecryptedSshKey(any(), eq(platformUserId))).thenReturn(new SshKey());
        } else { // Test grantee path
            ServerUser grant = new ServerUser();
            grant.setServer(serverEntity);
            grant.setRemoteUsernameForUser("grantee_user");
            // grant.setSshKeyForUser(...); // if specific key for grantee
            when(serverUserRepository.findByServerIdAndPlatformUserId(serverId, platformUserId)).thenReturn(Optional.of(grant));
            // if (grant.getSshKeyForUser() != null) when(sshKeyManagementService.getDecryptedSshKey(any(), eq(platformUserId))).thenReturn(new SshKey());
        }


        UserSpecificConnectionDetailsDto result = serverAccessControlService.getValidatedConnectionDetails(platformUserId, serverId);

        assertNotNull(result);
        verify(cache).put(eq(cacheKey), any(UserSpecificConnectionDetailsDto.class));
    }

    @Test
    void getValidatedConnectionDetails_whenInCache_returnsCached() {
        Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);
        when(valueWrapper.get()).thenReturn(connectionDetailsDto);
        when(cache.get(cacheKey)).thenReturn(valueWrapper); // In cache

        UserSpecificConnectionDetailsDto result = serverAccessControlService.getValidatedConnectionDetails(platformUserId, serverId);

        assertNotNull(result);
        verify(serverRepository, never()).findById(any()); // Should not hit DB for server
        verify(serverUserRepository, never()).findByServerIdAndPlatformUserId(any(), any()); // or for grant
    }

    @Test
    void grantServerAccess_evictsCache() {
        ServerUserRequest requestDto = new ServerUserRequest();
        requestDto.setPlatformUserId(platformUserId); // Grantee
        requestDto.setRemoteUsernameForUser("newuser");

        String grantCacheKey = requestDto.getPlatformUserId().toString() + ":" + serverId.toString();

        when(serverRepository.findById(serverId)).thenReturn(Optional.of(serverEntity));
        // Assume adminUserId owns serverEntity
        when(serverUserRepository.save(any(ServerUser.class))).thenReturn(new ServerUser()); // Mock save

        serverAccessControlService.grantServerAccess(adminUserId, serverId, requestDto);

        verify(cache).evict(grantCacheKey);
        verify(activityLogService).createLog(eq(adminUserId), eq("SERVER_ACCESS_GRANT"), eq(serverId), anyString(), eq("SUCCESS"), eq(null));
    }

    @Test
    void revokeServerAccess_evictsCache() {
        UUID targetPlatformUserId = platformUserId; // User whose access is revoked
        String revokeCacheKey = targetPlatformUserId.toString() + ":" + serverId.toString();

        ServerUser grantToRevoke = new ServerUser(); // Populate as needed
        when(serverRepository.findById(serverId)).thenReturn(Optional.of(serverEntity));
        when(serverUserRepository.findByServerIdAndPlatformUserId(serverId, targetPlatformUserId)).thenReturn(Optional.of(grantToRevoke));
        doNothing().when(serverUserRepository).delete(grantToRevoke);

        serverAccessControlService.revokeServerAccess(adminUserId, serverId, targetPlatformUserId);

        verify(cache).evict(revokeCacheKey);
        verify(activityLogService).createLog(eq(adminUserId), eq("SERVER_ACCESS_REVOKE"), eq(serverId), anyString(), eq("SUCCESS"), eq(null));
    }
}
