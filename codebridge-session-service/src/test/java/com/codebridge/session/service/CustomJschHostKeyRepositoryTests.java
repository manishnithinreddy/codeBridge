package com.codebridge.session.service;

import com.codebridge.session.model.SshHostKey;
import com.codebridge.session.repository.SshHostKeyRepository;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomJschHostKeyRepositoryTests {

    @Mock
    private SshHostKeyRepository sshHostKeyRepository;

    @Mock
    private JSch jsch;

    @Mock
    private UserInfo userInfo;

    private CustomJschHostKeyRepository repository;
    private String testHost;
    private int testPort;
    private String testKeyType;
    private byte[] testKeyBytes;
    private HostKey testJschHostKey;
    private UUID testUserId;

    @BeforeEach
    void setUp() throws JSchException {
        testHost = "localhost";
        testPort = 2222;
        testKeyType = "ssh-ed25519"; // JSch might derive this from key bytes
        // Example ed25519 public key bytes (simplified, actual key bytes are binary)
        // For a real test, you'd use actual key material.
        testKeyBytes = Base64.getDecoder().decode("AAAAC3NzaC1lZDI1NTE5AAAAIJLzgxvveRLMv9yvKMOjEGBWSFQg1y8+U5ShiIESQFG/");
        testJschHostKey = new HostKey(testHost, testPort, HostKey.GUESS, testKeyBytes);
        testUserId = UUID.randomUUID();

        repository = new CustomJschHostKeyRepository(sshHostKeyRepository, testUserId);
        repository.setJSch(jsch);
    }

    @Test
    void check_whenHostKeyExists_returnsOK() {
        // Arrange
        SshHostKey existingKey = new SshHostKey();
        existingKey.setId(UUID.randomUUID());
        existingKey.setUserId(testUserId);
        existingKey.setHost(testHost);
        existingKey.setPort(testPort);
        existingKey.setKeyType(testKeyType);
        existingKey.setKey(Base64.getEncoder().encodeToString(testKeyBytes));

        when(sshHostKeyRepository.findByUserIdAndHostAndPort(testUserId, testHost, testPort))
                .thenReturn(Collections.singletonList(existingKey));

        // Act
        int result = repository.check(testHost, testKeyBytes);

        // Assert
        assertEquals(CustomJschHostKeyRepository.OK, result);
    }

    @Test
    void check_whenHostKeyDoesNotExist_returnsNotIncluded() {
        // Arrange
        when(sshHostKeyRepository.findByUserIdAndHostAndPort(testUserId, testHost, testPort))
                .thenReturn(Collections.emptyList());

        // Act
        int result = repository.check(testHost, testKeyBytes);

        // Assert
        assertEquals(CustomJschHostKeyRepository.NOT_INCLUDED, result);
    }

    @Test
    void add_savesHostKeyToRepository() throws JSchException {
        // Arrange
        // HostKey testJschHostKeyForAdd = new HostKey(String.format("[%s]:%d", testHost, testPort), HostKey.GUESS, testKeyBytes);
        
        HostKey jschHostKeyForAdd = new HostKey(String.format("%s:%d", testHost, testPort), HostKey.GUESS, testKeyBytes);
        ArgumentCaptor<SshHostKey> keyCaptor = ArgumentCaptor.forClass(SshHostKey.class);

        // Act
        repository.add(jschHostKeyForAdd, userInfo);

        // Assert
        verify(sshHostKeyRepository).save(keyCaptor.capture());
        SshHostKey savedKey = keyCaptor.getValue();
        assertEquals(testUserId, savedKey.getUserId());
        assertEquals(testHost, savedKey.getHost());
        assertEquals(testPort, savedKey.getPort());
        assertNotNull(savedKey.getKey());
    }

    @Test
    void remove_deletesHostKeyFromRepository() {
        // Arrange
        SshHostKey existingKey = new SshHostKey();
        existingKey.setId(UUID.randomUUID());
        existingKey.setUserId(testUserId);
        existingKey.setHost(testHost);
        existingKey.setPort(testPort);
        existingKey.setKeyType(testKeyType);
        existingKey.setKey(Base64.getEncoder().encodeToString(testKeyBytes));

        when(sshHostKeyRepository.findByUserIdAndHostAndPortAndKeyType(testUserId, testHost, testPort, testKeyType))
                .thenReturn(Optional.of(existingKey));

        // Act
        repository.remove(testHost + ":" + testPort, testKeyType);

        // Assert
        verify(sshHostKeyRepository).delete(existingKey);
    }

    @Test
    void getHostKey_returnsMatchingKeys() throws JSchException {
        // Arrange
        SshHostKey existingKey = new SshHostKey();
        existingKey.setId(UUID.randomUUID());
        existingKey.setUserId(testUserId);
        existingKey.setHost(testHost);
        existingKey.setPort(testPort);
        existingKey.setKeyType(testKeyType);
        existingKey.setKey(Base64.getEncoder().encodeToString(testKeyBytes));

        when(sshHostKeyRepository.findByUserIdAndHostAndPort(testUserId, testHost, testPort))
                .thenReturn(Collections.singletonList(existingKey));

        // Act
        HostKey[] result = repository.getHostKey(testHost + ":" + testPort, null);

        // Assert
        assertEquals(1, result.length);
        assertEquals(testHost + ":" + testPort, result[0].getHost());
    }

    @Test
    void getHostKey_withNoArgs_returnsAllKeys() throws JSchException {
        // Arrange
        SshHostKey existingKey1 = new SshHostKey();
        existingKey1.setId(UUID.randomUUID());
        existingKey1.setUserId(testUserId);
        existingKey1.setHost(testHost);
        existingKey1.setPort(testPort);
        existingKey1.setKeyType(testKeyType);
        existingKey1.setKey(Base64.getEncoder().encodeToString(testKeyBytes));

        SshHostKey existingKey2 = new SshHostKey();
        existingKey2.setId(UUID.randomUUID());
        existingKey2.setUserId(testUserId);
        existingKey2.setHost("otherhost");
        existingKey2.setPort(22);
        existingKey2.setKeyType(testKeyType);
        existingKey2.setKey(Base64.getEncoder().encodeToString(testKeyBytes));

        when(sshHostKeyRepository.findByUserId(testUserId))
                .thenReturn(Arrays.asList(existingKey1, existingKey2));

        // Act
        HostKey[] result = repository.getHostKey();

        // Assert
        assertEquals(2, result.length);
    }
}

