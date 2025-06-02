package com.codebridge.server.service;

import com.codebridge.server.dto.SshKeyRequest;
import com.codebridge.server.dto.SshKeyResponse;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.repository.SshKeyRepository;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict; // Added
import org.springframework.cache.annotation.Cacheable; // Added
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SshKeyManagementService {

    private final SshKeyRepository sshKeyRepository;
    private final StringEncryptor stringEncryptor;
    private final ServerActivityLogService activityLogService; // Added

    // Assuming Jasypt default bean name is "jasyptStringEncryptor"
    // If a custom bean is defined, its name should be used in @Qualifier
    public SshKeyManagementService(SshKeyRepository sshKeyRepository,
                                   @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor,
                                   ServerActivityLogService activityLogService) { // Added
        this.sshKeyRepository = sshKeyRepository;
        this.stringEncryptor = stringEncryptor;
        this.activityLogService = activityLogService; // Added
    }

    @Transactional
    public SshKeyResponse createSshKey(SshKeyRequest dto, UUID userId) {
        SshKey sshKey = new SshKey();
        String details;
        try {
            sshKey.setName(dto.getName());
        sshKey.setPublicKey(dto.getPublicKey());
        sshKey.setUserId(userId);

        if (StringUtils.hasText(dto.getPrivateKey())) {
            sshKey.setPrivateKey(stringEncryptor.encrypt(dto.getPrivateKey()));
        }
        // Fingerprint might be generated from public key here or by a utility method
        // For now, assuming it's either provided or set separately if auto-generated.

        SshKey savedKey = sshKeyRepository.save(sshKey);
        details = String.format("SSH Key created: Name='%s', ID='%s'", savedKey.getName(), savedKey.getId());
        activityLogService.createLog(userId, "SSH_KEY_CREATE", null, details, "SUCCESS", null);
        return mapToSshKeyResponse(savedKey);
        } catch (Exception e) {
            details = String.format("Failed to create SSH Key: Name='%s'", dto.getName());
            activityLogService.createLog(userId, "SSH_KEY_CREATE_FAILED", null, details, "FAILED", e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "sshKeyById", key = "#keyId.toString()") // Added
    public SshKeyResponse getSshKeyById(UUID keyId, UUID userId) {
        SshKey sshKey = sshKeyRepository.findById(keyId)
                .filter(key -> key.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("SshKey", "id", keyId + " for user " + userId));
        return mapToSshKeyResponse(sshKey);
    }

    @Transactional(readOnly = true)
    public List<SshKeyResponse> listSshKeysForUser(UUID userId) {
        return sshKeyRepository.findByUserId(userId).stream()
                .map(this::mapToSshKeyResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "sshKeyById", key = "#keyId.toString()") // Added
    public void deleteSshKey(UUID keyId, UUID userId) {
        String details;
        try {
            SshKey sshKey = sshKeyRepository.findById(keyId)
                    .filter(key -> key.getUserId().equals(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("SshKey", "id", keyId + " for user " + userId));
            // Check if key is in use by any server before deleting (important consideration for future)
            sshKeyRepository.delete(sshKey);
            details = String.format("SSH Key deleted: ID='%s', Name='%s'", keyId, sshKey.getName());
            activityLogService.createLog(userId, "SSH_KEY_DELETE", null, details, "SUCCESS", null);
        } catch (Exception e) {
            details = String.format("Failed to delete SSH Key: ID='%s'", keyId);
            activityLogService.createLog(userId, "SSH_KEY_DELETE_FAILED", null, details, "FAILED", e.getMessage());
            throw e;
        }
    }

    // Internal method for other services, e.g., ServerManagementService or connection services
    @Transactional(readOnly = true)
    public SshKey getDecryptedSshKey(UUID keyId, UUID userId) {
        SshKey sshKey = sshKeyRepository.findById(keyId)
                .filter(key -> key.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("SshKey", "id", keyId + " for user " + userId));

        if (StringUtils.hasText(sshKey.getPrivateKey())) {
            try {
                SshKey decryptedKey = new SshKey(); // Create a new instance to avoid changing the managed entity's state
                decryptedKey.setId(sshKey.getId());
                decryptedKey.setName(sshKey.getName());
                decryptedKey.setPublicKey(sshKey.getPublicKey());
                decryptedKey.setFingerprint(sshKey.getFingerprint());
                decryptedKey.setUserId(sshKey.getUserId());
                decryptedKey.setCreatedAt(sshKey.getCreatedAt());
                decryptedKey.setUpdatedAt(sshKey.getUpdatedAt());
                decryptedKey.setPrivateKey(stringEncryptor.decrypt(sshKey.getPrivateKey()));
                return decryptedKey;
            } catch (Exception e) {
                // Handle decryption failure, e.g., log and throw a specific internal server error
                // Or if EncryptionOperationNotPossibleException is specific enough, let it propagate for GlobalExceptionHandler
                throw new RuntimeException("Failed to decrypt private key for key ID: " + keyId, e);
            }
        }
        return sshKey; // Return original if no private key to decrypt
    }


    private SshKeyResponse mapToSshKeyResponse(SshKey sshKey) {
        if (sshKey == null) return null;
        return new SshKeyResponse(
                sshKey.getId(),
                sshKey.getName(),
                sshKey.getPublicKey(),
                sshKey.getFingerprint(),
                sshKey.getUserId(),
                sshKey.getCreatedAt(),
                sshKey.getUpdatedAt()
        );
    }
}
