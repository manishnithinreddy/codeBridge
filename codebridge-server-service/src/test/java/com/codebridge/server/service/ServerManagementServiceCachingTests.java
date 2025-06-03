package com.codebridge.server.service;

import com.codebridge.server.dto.ServerRequest;
import com.codebridge.server.dto.ServerResponse;
import com.codebridge.server.model.Server;
import com.codebridge.server.repository.ServerRepository;
import com.codebridge.server.repository.SshKeyRepository;
import org.jasypt.encryption.StringEncryptor;
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
class ServerManagementServiceCachingTests {

    @Mock private ServerRepository serverRepository;
    @Mock private SshKeyRepository sshKeyRepository; // Dependency of ServerManagementService
    @Mock private StringEncryptor stringEncryptor;   // Dependency
    @Mock private ServerActivityLogService activityLogService; // Dependency
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks
    private ServerManagementService serverManagementService;

    private UUID serverId;
    private UUID userId;
    private Server serverEntity;
    private ServerResponse serverResponseDto;
    private ServerRequest serverRequestDto;

    @BeforeEach
    void setUp() {
        serverId = UUID.randomUUID();
        userId = UUID.randomUUID();

        serverEntity = new Server();
        serverEntity.setId(serverId);
        serverEntity.setName("test-server");
        serverEntity.setUserId(userId);
        // ... other necessary fields

        // This mapping should match what mapToServerResponse does
        serverResponseDto = new ServerResponse(serverId, "test-server", "host", 22, "user", 
                                             null, null, null, null, null, null, userId, null, null);

        serverRequestDto = new ServerRequest(); // Populate if needed for update test
        serverRequestDto.setName("updated-server-name");


        when(cacheManager.getCache("serverById")).thenReturn(cache);
    }

    @Test
    void getServerById_whenNotInCache_fetchesFromDbAndCaches() {
        when(cache.get(serverId.toString())).thenReturn(null); // Not in cache
        when(serverRepository.findByIdAndUserId(serverId, userId)).thenReturn(Optional.of(serverEntity));
        
        ServerResponse result = serverManagementService.getServerById(serverId, userId);

        assertNotNull(result);
        assertEquals(serverEntity.getName(), result.getName());
        verify(serverRepository).findByIdAndUserId(serverId, userId);
        verify(cache).put(eq(serverId.toString()), any(ServerResponse.class));
    }

    @Test
    void getServerById_whenInCache_returnsCachedValue() {
        Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);
        when(valueWrapper.get()).thenReturn(serverResponseDto);
        when(cache.get(serverId.toString())).thenReturn(valueWrapper); // In cache

        ServerResponse result = serverManagementService.getServerById(serverId, userId);

        assertNotNull(result);
        assertEquals(serverResponseDto.getName(), result.getName());
        verify(serverRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void updateServer_updatesDbAndCache() {
        when(serverRepository.findByIdAndUserId(serverId, userId)).thenReturn(Optional.of(serverEntity));
        when(serverRepository.save(any(Server.class))).thenReturn(serverEntity); // Assume save returns updated entity
        
        // mapDtoToServer will be called internally, ensure its dependencies (like sshKeyRepo) are mocked if it makes calls.
        // For this test, we focus on the @CachePut behavior.

        ServerResponse result = serverManagementService.updateServer(serverId, serverRequestDto, userId);

        assertNotNull(result);
        // Name might not be updated in 'result' if mapToServerResponse uses original entity before changes are applied by save
        // This depends on the exact implementation of mapDtoToServer and when save happens.
        // The key is that @CachePut should be called with the result of the method.
        verify(serverRepository).save(any(Server.class));
        verify(cache).put(eq(serverId.toString()), eq(result)); // CachePut ensures the result is cached
        verify(activityLogService).createLog(any(), eq("SERVER_UPDATE"), eq(serverId), anyString(), eq("SUCCESS"), eq(null));
    }
    
    @Test
    void deleteServer_evictsFromCache() {
        when(serverRepository.findByIdAndUserId(serverId, userId)).thenReturn(Optional.of(serverEntity));
        doNothing().when(serverRepository).delete(serverEntity);

        serverManagementService.deleteServer(serverId, userId);

        verify(serverRepository).delete(serverEntity);
        verify(cache).evict(serverId.toString());
        verify(activityLogService).createLog(any(), eq("SERVER_DELETE"), eq(serverId), anyString(), eq("SUCCESS"), eq(null));
    }
}
