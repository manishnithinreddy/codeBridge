package com.codebridge.core.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Service for securely storing and retrieving sensitive information.
 * Uses AES-GCM encryption for secure storage.
 */
@Service
public class SecretStorageService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;
    private static final int KEY_LENGTH_BIT = 256;
    private static final int ITERATION_COUNT = 65536;

    @Value("${secret.encryption.key}")
    private String encryptionKey;

    /**
     * Encrypts a secret value.
     *
     * @param secretValue the secret value to encrypt
     * @return the encrypted value
     */
    public String encryptSecret(String secretValue) {
        try {
            // Generate a random salt
            byte[] salt = new byte[SALT_LENGTH_BYTE];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(salt);

            // Derive the key
            SecretKey key = getAESKeyFromPassword(encryptionKey.toCharArray(), salt);

            // Generate a random IV
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            // Initialize the cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            // Encrypt the secret value
            byte[] encryptedBytes = cipher.doFinal(secretValue.getBytes(StandardCharsets.UTF_8));

            // Combine salt, IV, and encrypted bytes
            ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + iv.length + encryptedBytes.length);
            byteBuffer.put(salt);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            // Encode as Base64
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting secret", e);
        }
    }

    /**
     * Decrypts an encrypted secret value.
     *
     * @param encryptedValue the encrypted value
     * @return the decrypted secret value
     */
    public String decryptSecret(String encryptedValue) {
        try {
            // Decode from Base64
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedValue);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);

            // Extract salt, IV, and encrypted bytes
            byte[] salt = new byte[SALT_LENGTH_BYTE];
            byteBuffer.get(salt);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            // Derive the key
            SecretKey key = getAESKeyFromPassword(encryptionKey.toCharArray(), salt);

            // Initialize the cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            // Decrypt the secret value
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting secret", e);
        }
    }

    /**
     * Derives an AES key from a password and salt.
     *
     * @param password the password
     * @param salt the salt
     * @return the derived key
     * @throws Exception if an error occurs
     */
    private SecretKey getAESKeyFromPassword(char[] password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH_BIT);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}

