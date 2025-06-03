package com.codebridge.server.service;

import com.codebridge.server.dto.SshKeyResponse;
import com.codebridge.server.model.SshKey;
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
class SshKeyManagementServiceCachingTests {

    @Mock private SshKeyRepository sshKeyRepository;
    @Mock private StringEncryptor stringEncryptor; // Not directly involved in cache logic but part of service
    @Mock private ServerActivityLogService activityLogService; // Also part of service
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache; // Mock the specific cache

    @InjectMocks
    private SshKeyManagementService sshKeyManagementService;

    private UUID keyId;
    private UUID userId;
    private SshKey sshKeyEntity;
    private SshKeyResponse sshKeyResponseDto;

    @BeforeEach
    void setUp() {
        keyId = UUID.randomUUID();
        userId = UUID.randomUUID();

        sshKeyEntity = new SshKey();
        sshKeyEntity.setId(keyId);
        sshKeyEntity.setName("test-key");
        sshKeyEntity.setUserId(userId);
        // ... other fields

        sshKeyResponseDto = new SshKeyResponse(keyId, "test-key", "pub-key", "fingerprint", userId, null, null);

        when(cacheManager.getCache("sshKeyById")).thenReturn(cache);
    }

    @Test
    void getSshKeyById_whenNotInCache_fetchesFromDbAndCaches() {
        when(cache.get(keyId.toString())).thenReturn(null); // Not in cache
        when(sshKeyRepository.findById(keyId)).thenReturn(Optional.of(sshKeyEntity));

        SshKeyResponse result = sshKeyManagementService.getSshKeyById(keyId, userId);

        assertNotNull(result);
        assertEquals(sshKeyResponseDto.getName(), result.getName()); // Basic check
        verify(sshKeyRepository).findById(keyId);
        verify(cache).put(eq(keyId.toString()), any(SshKeyResponse.class)); // DTO is cached
    }

    @Test
    void getSshKeyById_whenInCache_returnsCachedValue() {
        Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);
        when(valueWrapper.get()).thenReturn(sshKeyResponseDto);
        when(cache.get(keyId.toString())).thenReturn(valueWrapper); // In cache

        SshKeyResponse result = sshKeyManagementService.getSshKeyById(keyId, userId);

        assertNotNull(result);
        assertEquals(sshKeyResponseDto.getName(), result.getName());
        verify(sshKeyRepository, never()).findById(any()); // Should not hit DB
    }

    @Test
    void deleteSshKey_evictsFromCache() {
        when(sshKeyRepository.findById(keyId)).thenReturn(Optional.of(sshKeyEntity));
        // Ensure delete does not throw
        doNothing().when(sshKeyRepository).delete(sshKeyEntity);

        sshKeyManagementService.deleteSshKey(keyId, userId);

        verify(sshKeyRepository).delete(sshKeyEntity);
        verify(cache).evict(keyId.toString());
        verify(activityLogService).createLog(any(), eq("SSH_KEY_DELETE"), eq(null), anyString(), eq("SUCCESS"), eq(null));
    }
}
