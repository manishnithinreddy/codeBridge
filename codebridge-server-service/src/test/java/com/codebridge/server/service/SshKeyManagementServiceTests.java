package com.codebridge.server.service;

import com.codebridge.server.dto.SshKeyRequest;
import com.codebridge.server.dto.SshKeyResponse;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.repository.SshKeyRepository;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
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
class SshKeyManagementServiceTests {

    @Mock
    private SshKeyRepository sshKeyRepository;

    @Mock
    @Qualifier("jasyptStringEncryptor") // Ensure the correct StringEncryptor is mocked if multiple exist
    private StringEncryptor stringEncryptor;

    // ServerActivityLogService is not directly related to caching tests for this service,
    // but if it's a required dependency for SshKeyManagementService constructor, it needs to be mocked.
    // Based on previous files, it's not a direct dependency of SshKeyManagementService.
    // Let's remove it if not needed by constructor, or keep if it is.
    // Re-checking SshKeyManagementService constructor: it does not take ServerActivityLogService.

    @Mock
    private CacheManager cacheManagerMock;
    @Mock
    private Cache sshKeyByIdCacheMock;
    @Mock
    private Cache userServerAccessDetailsCacheMock; // For eviction from deleteSshKey

    @InjectMocks
    private SshKeyManagementService sshKeyManagementService;

    @Captor
    private ArgumentCaptor<SshKey> sshKeyCaptor;

    private UUID testUserId;
    private UUID testKeyId;
    private SshKeyRequest sshKeyRequest;
    private SshKey sshKey;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testKeyId = UUID.randomUUID();

        sshKeyRequest = new SshKeyRequest();
        sshKeyRequest.setName("Test Key");
        sshKeyRequest.setPublicKey("ssh-rsa AAA...");
        sshKeyRequest.setPrivateKey("-----BEGIN RSA PRIVATE KEY-----...");

        sshKey = new SshKey();
        sshKey.setId(testKeyId);
        sshKey.setUserId(testUserId);
        sshKey.setName("Test Key");
        sshKey.setPublicKey("ssh-rsa AAA...");
        sshKey.setPrivateKey("encryptedPrivateKey"); // Assume it's stored encrypted
        sshKey.setCreatedAt(LocalDateTime.now().minusDays(1));
        sshKey.setUpdatedAt(LocalDateTime.now().minusDays(1));

        // Setup for CacheManager mock
        when(cacheManagerMock.getCache("sshKeyById")).thenReturn(sshKeyByIdCacheMock);
        when(cacheManagerMock.getCache("userServerAccessDetails")).thenReturn(userServerAccessDetailsCacheMock);
    }

    @Test
    void createSshKey_success() { // No caching annotations on create, just a standard test
        when(stringEncryptor.encrypt(sshKeyRequest.getPrivateKey())).thenReturn("encryptedPrivateKey");
        when(sshKeyRepository.save(any(SshKey.class))).thenAnswer(invocation -> {
            SshKey key = invocation.getArgument(0);
            key.setId(testKeyId); // Simulate ID generation on save
            key.setCreatedAt(LocalDateTime.now());
            key.setUpdatedAt(LocalDateTime.now());
            return key;
        });

        SshKeyResponse response = sshKeyManagementService.createSshKey(sshKeyRequest, testUserId);

        assertNotNull(response);
        assertEquals(testKeyId, response.getId());
        // ... other assertions ...
        verify(sshKeyRepository).save(any(SshKey.class));
    }

    @Test
    void getSshKeyById_whenNotCached_callsRepoAndCaches() {
        when(sshKeyRepository.findByIdAndUserId(testKeyId, testUserId)).thenReturn(Optional.of(sshKey));
        // Simulate cache miss: cache.get(key) returns null
        when(sshKeyByIdCacheMock.get(testKeyId.toString())).thenReturn(null);
        // Mock put operation
        doNothing().when(sshKeyByIdCacheMock).put(eq(testKeyId.toString()), any(SshKeyResponse.class));


        SshKeyResponse response = sshKeyManagementService.getSshKeyById(testKeyId, testUserId);

        assertNotNull(response);
        assertEquals(sshKey.getId(), response.getId());
        verify(sshKeyRepository, times(1)).findByIdAndUserId(testKeyId, testUserId);
        verify(sshKeyByIdCacheMock, times(1)).get(testKeyId.toString());
        verify(sshKeyByIdCacheMock, times(1)).put(eq(testKeyId.toString()), any(SshKeyResponse.class));
    }

    @Test
    void getSshKeyById_whenCached_returnsCachedAndNotRepo() {
        SshKeyResponse cachedResponse = new SshKeyResponse(); // Populate as needed
        cachedResponse.setId(testKeyId);
        cachedResponse.setName("Cached Test Key");

        Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);
        when(valueWrapper.get()).thenReturn(cachedResponse);
        when(sshKeyByIdCacheMock.get(testKeyId.toString())).thenReturn(valueWrapper);

        SshKeyResponse response = sshKeyManagementService.getSshKeyById(testKeyId, testUserId);

        assertNotNull(response);
        assertEquals("Cached Test Key", response.getName());
        verify(sshKeyRepository, never()).findByIdAndUserId(any(), any());
        verify(sshKeyByIdCacheMock, times(1)).get(testKeyId.toString());
        verify(sshKeyByIdCacheMock, never()).put(anyString(), any());
    }

    @Test
    void getSshKeyById_notFound_throwsResourceNotFound_AndNoCachePut() {
        when(sshKeyRepository.findByIdAndUserId(testKeyId, testUserId)).thenReturn(Optional.empty());
        when(sshKeyByIdCacheMock.get(testKeyId.toString())).thenReturn(null); // Cache miss

        assertThrows(ResourceNotFoundException.class, () -> {
            sshKeyManagementService.getSshKeyById(testKeyId, testUserId);
        });
        verify(sshKeyByIdCacheMock, never()).put(anyString(), any());
    }


    @Test
    void deleteSshKey_evictsFromCaches() {
        when(sshKeyRepository.findByIdAndUserId(testKeyId, testUserId)).thenReturn(Optional.of(sshKey));
        doNothing().when(sshKeyRepository).delete(sshKey);
        // Mock evict and clear operations
        doNothing().when(sshKeyByIdCacheMock).evict(testKeyId.toString());
        doNothing().when(userServerAccessDetailsCacheMock).clear();


        sshKeyManagementService.deleteSshKey(testKeyId, testUserId);

        verify(sshKeyRepository).delete(sshKey);
        verify(sshKeyByIdCacheMock, times(1)).evict(testKeyId.toString());
        verify(userServerAccessDetailsCacheMock, times(1)).clear(); // Due to @CacheEvict(allEntries=true)
    }

    // --- Test for getDecryptedSshKey (ensure it's NOT cached as per plan) ---
    @Test
    void getDecryptedSshKey_isNotCached() {
        // This test primarily ensures no @Cacheable annotation was accidentally added.
        // We call it twice; repo should be hit twice if not cached.
        when(sshKeyRepository.findByIdAndUserId(testKeyId, testUserId)).thenReturn(Optional.of(sshKey));
        when(stringEncryptor.decrypt("encryptedPrivateKey")).thenReturn("decryptedPrivateKey");

        sshKeyManagementService.getDecryptedSshKey(testKeyId, testUserId); // First call
        sshKeyManagementService.getDecryptedSshKey(testKeyId, testUserId); // Second call

        verify(sshKeyRepository, times(2)).findByIdAndUserId(testKeyId, testUserId);
        verify(stringEncryptor, times(2)).decrypt("encryptedPrivateKey");
        // Verify no interaction with any cache for this method specifically
        verify(sshKeyByIdCacheMock, never()).get(anyString());
        verify(sshKeyByIdCacheMock, never()).put(anyString(), any());
    }


    // --- Existing non-caching related tests can remain, e.g., listSshKeysForUser ---
    @Test
    void listSshKeysForUser_returnsListOfKeys() {
        when(sshKeyRepository.findByUserId(testUserId)).thenReturn(Collections.singletonList(sshKey));

        List<SshKeyResponse> responses = sshKeyManagementService.listSshKeysForUser(testUserId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(sshKey.getName(), responses.get(0).getName());
    }

    // Corrected signature for getDecryptedSshKey based on its actual implementation
    @Test
    void getDecryptedSshKey_withUserId_success() {
        when(sshKeyRepository.findByIdAndUserId(testKeyId, testUserId)).thenReturn(Optional.of(sshKey));
        when(stringEncryptor.decrypt("encryptedPrivateKey")).thenReturn("decryptedPrivateKey");

        SshKey decryptedKey = sshKeyManagementService.getDecryptedSshKey(testKeyId, testUserId);

        assertNotNull(decryptedKey);
        assertEquals("decryptedPrivateKey", decryptedKey.getPrivateKey());
        assertEquals(sshKey.getName(), decryptedKey.getName());
    }

    @Test
    void getDecryptedSshKey_withUserId_notFound_throwsResourceNotFoundException() {
        when(sshKeyRepository.findByIdAndUserId(testKeyId, testUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            sshKeyManagementService.getDecryptedSshKey(testKeyId, testUserId);
        });
    }

    @Test
    void getDecryptedSshKey_withUserId_decryptionError_throwsRuntimeException() {
        when(sshKeyRepository.findByIdAndUserId(testKeyId, testUserId)).thenReturn(Optional.of(sshKey));
        when(stringEncryptor.decrypt("encryptedPrivateKey")).thenThrow(new EncryptionOperationNotPossibleException("Decryption failure"));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            sshKeyManagementService.getDecryptedSshKey(testKeyId, testUserId);
        });
        assertTrue(exception.getMessage().contains("Failed to decrypt private key for key ID: " + testKeyId));
    }
}
