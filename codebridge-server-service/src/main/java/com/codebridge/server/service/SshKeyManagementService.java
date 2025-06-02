package com.codebridge.server.service;

import com.codebridge.server.dto.SshKeyRequest;
import com.codebridge.server.dto.SshKeyResponse;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.repository.SshKeyRepository;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SshKeyManagementService {

    private final SshKeyRepository sshKeyRepository;
    private final StringEncryptor stringEncryptor;

    public SshKeyManagementService(SshKeyRepository sshKeyRepository,
                                   @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor) {
        this.sshKeyRepository = sshKeyRepository;
        this.stringEncryptor = stringEncryptor;
    }

    @Transactional
    public SshKeyResponse createSshKey(SshKeyRequest requestDto, UUID userId) {
        SshKey sshKey = new SshKey();
        sshKey.setName(requestDto.getName());
        sshKey.setPublicKey(requestDto.getPublicKey());
        sshKey.setPrivateKey(stringEncryptor.encrypt(requestDto.getPrivateKey()));
        sshKey.setUserId(userId);
        // In a real application, fingerprint would be generated from the public key
        // sshKey.setFingerprint(generateFingerprint(requestDto.getPublicKey()));
        SshKey savedKey = sshKeyRepository.save(sshKey);
        return mapToSshKeyResponse(savedKey);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "sshKeyById", key = "#keyId.toString()")
    public SshKeyResponse getSshKeyById(UUID keyId, UUID userId) {
        SshKey sshKey = sshKeyRepository.findByIdAndUserId(keyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SshKey", "id", keyId));
        return mapToSshKeyResponse(sshKey);
    }

    @Transactional(readOnly = true)
    public List<SshKeyResponse> listSshKeysForUser(UUID userId) {
        return sshKeyRepository.findByUserId(userId).stream()
                .map(this::mapToSshKeyResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "sshKeyById", key = "#keyId.toString()")
    @CacheEvict(value="userServerAccessDetails", allEntries=true) // Added for broader eviction
    public void deleteSshKey(UUID keyId, UUID userId) {
        SshKey sshKey = sshKeyRepository.findByIdAndUserId(keyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SshKey", "id", keyId));
        // Add check here if the key is currently in use by any server configuration before deleting
        sshKeyRepository.delete(sshKey);
    }

    /**
     * Retrieves an SshKey entity by its ID and user ID, with its private key decrypted.
     * This method is intended for internal use by other services (e.g., ServerManagementService)
     * that need the decrypted private key for establishing SSH connections.
     * It should not be directly exposed via an API controller.
     *
     * @param keyId  The ID of the SSH key.
     * @param userId The ID of the user who owns the key.
     * @return The SshKey entity with the decrypted private key.
     * @throws ResourceNotFoundException if the key is not found.
     */
    @Transactional(readOnly = true)
    public SshKey getDecryptedSshKey(UUID keyId, UUID userId) {
        SshKey sshKey = sshKeyRepository.findByIdAndUserId(keyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SshKey", "id", keyId + " for user " + userId));

        SshKey decryptedKey = new SshKey();
        decryptedKey.setId(sshKey.getId());
        decryptedKey.setName(sshKey.getName());
        decryptedKey.setPublicKey(sshKey.getPublicKey());
        decryptedKey.setFingerprint(sshKey.getFingerprint());
        decryptedKey.setUserId(sshKey.getUserId());
        decryptedKey.setCreatedAt(sshKey.getCreatedAt());
        decryptedKey.setUpdatedAt(sshKey.getUpdatedAt());
        try {
            decryptedKey.setPrivateKey(stringEncryptor.decrypt(sshKey.getPrivateKey()));
        } catch (Exception e) {
            // Handle decryption failure, e.g., if the key is corrupted or encryption password changed
            // For simplicity, re-throwing as a runtime exception. Consider a specific unchecked exception.
            throw new RuntimeException("Failed to decrypt private key for key ID: " + keyId, e);
        }
        return decryptedKey;
    }


    private SshKeyResponse mapToSshKeyResponse(SshKey sshKey) {
        SshKeyResponse response = new SshKeyResponse();
        response.setId(sshKey.getId());
        response.setName(sshKey.getName());
        response.setPublicKey(sshKey.getPublicKey());
        response.setFingerprint(sshKey.getFingerprint());
        response.setUserId(sshKey.getUserId());
        response.setCreatedAt(sshKey.getCreatedAt());
        response.setUpdatedAt(sshKey.getUpdatedAt());
        return response;
    }

    // private String generateFingerprint(String publicKey) {
    //     // Implement fingerprint generation logic (e.g., using SSHJ or BouncyCastle)
    //     // This is a placeholder.
    //     if (publicKey == null || publicKey.trim().isEmpty()) {
    //         return null;
    //     }
    //     try {
    //         // A very basic "fingerprint" - replace with actual cryptographic fingerprint
    //         java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
    //         byte[] publicKeyBytes = publicKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    //         byte[] digest = md.digest(publicKeyBytes);
    //         StringBuilder sb = new StringBuilder();
    //         for (int i = 0; i < digest.length; i++) {
    //             sb.append(String.format("%02x", digest[i]));
    //             if (i < digest.length - 1) sb.append(":");
    //         }
    //         return sb.toString();
    //     } catch (java.security.NoSuchAlgorithmException e) {
    //         // Log error or handle
    //         return "ErrorGeneratingFingerprint";
    //     }
    // }
}
