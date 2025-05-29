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
    
    @Mock
    private ServerActivityLogService serverActivityLogService;


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
        
        // Mock successful log creation
        doNothing().when(serverActivityLogService).createLog(any(), anyString(), any(), anyString(), anyString(), anyString());

    }

    @Test
    void createSshKey_success() {
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
        assertEquals(sshKeyRequest.getName(), response.getName());
        assertEquals(sshKeyRequest.getPublicKey(), response.getPublicKey());
        assertEquals(testUserId, response.getUserId());

        verify(sshKeyRepository).save(sshKeyCaptor.capture());
        SshKey capturedKey = sshKeyCaptor.getValue();
        assertEquals("encryptedPrivateKey", capturedKey.getPrivateKey());
        assertEquals(testUserId, capturedKey.getUserId());
        
        verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("CREATE_SSH_KEY"), 
            isNull(), // No serverId for SSH key creation itself
            contains("SSH Key created: Test Key"), 
            eq("SUCCESS"), 
            isNull()
        );
    }

    @Test
    void getSshKeyById_found() {
        when(sshKeyRepository.findByIdAndUserId(testKeyId, testUserId)).thenReturn(Optional.of(sshKey));

        SshKeyResponse response = sshKeyManagementService.getSshKeyById(testKeyId, testUserId);

        assertNotNull(response);
        assertEquals(sshKey.getId(), response.getId());
        assertEquals(sshKey.getName(), response.getName());
    }

    @Test
    void getSshKeyById_notFound_throwsResourceNotFoundException() {
        when(sshKeyRepository.findByIdAndUserId(testKeyId, testUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            sshKeyManagementService.getSshKeyById(testKeyId, testUserId);
        });
    }

    @Test
    void listSshKeysForUser_returnsListOfKeys() {
        when(sshKeyRepository.findByUserId(testUserId)).thenReturn(Collections.singletonList(sshKey));

        List<SshKeyResponse> responses = sshKeyManagementService.listSshKeysForUser(testUserId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(sshKey.getName(), responses.get(0).getName());
    }
    
    @Test
    void listSshKeysForUser_returnsEmptyListWhenNoKeys() {
        when(sshKeyRepository.findByUserId(testUserId)).thenReturn(Collections.emptyList());

        List<SshKeyResponse> responses = sshKeyManagementService.listSshKeysForUser(testUserId);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void deleteSshKey_success() {
        when(sshKeyRepository.findByIdAndUserId(testKeyId, testUserId)).thenReturn(Optional.of(sshKey));
        doNothing().when(sshKeyRepository).delete(sshKey);

        sshKeyManagementService.deleteSshKey(testKeyId, testUserId);

        verify(sshKeyRepository).delete(sshKey);
        verify(serverActivityLogService).createLog(
            eq(testUserId), 
            eq("DELETE_SSH_KEY"), 
            isNull(), 
            contains("SSH Key deleted: " + testKeyId), 
            eq("SUCCESS"), 
            isNull()
        );
    }

    @Test
    void deleteSshKey_notFound_throwsResourceNotFoundException() {
        when(sshKeyRepository.findByIdAndUserId(testKeyId, testUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            sshKeyManagementService.deleteSshKey(testKeyId, testUserId);
        });
         verify(serverActivityLogService, never()).createLog(any(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void getDecryptedSshKey_success() {
        when(sshKeyRepository.findById(testKeyId)).thenReturn(Optional.of(sshKey));
        when(stringEncryptor.decrypt("encryptedPrivateKey")).thenReturn("decryptedPrivateKey");

        SshKey decryptedKey = sshKeyManagementService.getDecryptedSshKey(testKeyId);

        assertNotNull(decryptedKey);
        assertEquals("decryptedPrivateKey", decryptedKey.getPrivateKey());
        assertEquals(sshKey.getName(), decryptedKey.getName());
    }
    
    @Test
    void getDecryptedSshKey_notFound_throwsResourceNotFoundException() {
        when(sshKeyRepository.findById(testKeyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            sshKeyManagementService.getDecryptedSshKey(testKeyId);
        });
    }

    @Test
    void getDecryptedSshKey_decryptionError_throwsRuntimeException() {
        when(sshKeyRepository.findById(testKeyId)).thenReturn(Optional.of(sshKey));
        when(stringEncryptor.decrypt("encryptedPrivateKey")).thenThrow(new EncryptionOperationNotPossibleException("Decryption failure"));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            sshKeyManagementService.getDecryptedSshKey(testKeyId);
        });
        assertTrue(exception.getMessage().contains("Failed to decrypt private key for key ID: " + testKeyId));
    }
}
