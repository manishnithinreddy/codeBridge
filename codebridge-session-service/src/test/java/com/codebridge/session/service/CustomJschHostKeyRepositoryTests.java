package com.codebridge.session.service;

import com.codebridge.session.model.KnownSshHostKey;
import com.codebridge.session.repository.KnownSshHostKeyRepository;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomJschHostKeyRepositoryTests {

    @Mock
    private KnownSshHostKeyRepository knownHostKeyRepositoryMock;

    @Mock
    private UserInfo userInfoMock; // For testing the 'add' method's prompt path

    @InjectMocks
    private CustomJschHostKeyRepository customHostKeyRepository;

    @Captor
    private ArgumentCaptor<KnownSshHostKey> knownSshHostKeyCaptor;

    // Sample RSA key (dummy, replace with actual key structure if needed for type parsing)
    // For testing, we often just need a consistent byte array.
    private static final String RSA_KEY_TYPE = "ssh-rsa";
    private static final byte[] rsaKeyBytes = Base64.getDecoder().decode("AAAAB3NzaC1yc2EAAAADAQABAAABAQC3gq7iL7G2YdxR2z6DyPY3JRk2bX9z7b+sL+N6UeZ3Z+A8L9Ld9W5g0wN7AmcE/7YHcPJj0cGLf6fTEellRsL3kQWZrA6xO6LgLhAEvmNBbB9iAgO8jBQNer8P0aGZ2sLCcRj+DRoffQU2mtgWxKeUCNtG7tA2RAyYtffDEsV74IOBOg茍yllsFH8D9Q3332GgYVLx2gA8N0yDxFUuQAUV8qTez4AUuVGy0B0aKZOJbT+vST3rL7+y9GkYSyAWXgNDs0P4z6jVpT7dD8XgC0Pqj5xN2mYVzP5Lqj0oR7Q2bXoN6A8L9M3Z3W5g0wN7AmcE");

    private static final String ED25519_KEY_TYPE = "ssh-ed25519";
    private static final byte[] ed25519KeyBytes = Base64.getDecoder().decode("AAAAC3NzaC1lZDI1NTE5AAAAIEvITnJ/q4PeNBlsb2PBDbKxTb3P0nZ0yegWkXDOh7Q3");

    private String testHost = "example.com";
    private int testPort = 22;


    // Helper to get key type from bytes using JSch's HostKey
    private String getKeyType(byte[] keyBytes) throws JSchException {
        // Hostname here is just a placeholder for HostKey constructor.
        HostKey tempHostKey = new HostKey("dummyHostForType", HostKey.GUESS, keyBytes);
        return tempHostKey.getType();
    }

     private String calculateSha256FingerprintForTest(byte[] key) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(key);
        return Base64.getEncoder().encodeToString(digest);
    }


    @BeforeEach
    void setUp() {
        // Reset mocks if needed, but @ExtendWith(MockitoExtension.class) does this.
    }

    // --- Tests for check(String host, byte[] key) ---

    @Test
    void check_hostAndKeyTypeUnknown_returnsNotInCLUDED() throws JSchException {
        // JSch passes host string which might be just hostname or [hostname]:port
        // The 'check' method in CustomJschHostKeyRepository currently has a simplified host matching.
        // For this test, we'll assume the internal parsing/matching logic is being tested.
        // Let's assume getHostKey(host, type) is called by JSch first, or host string is simple.

        // The current implementation of check() iterates all keys if host string doesn't give port.
        // This is inefficient and the test reflects this.
        when(knownHostKeyRepositoryMock.findAll()).thenReturn(Collections.emptyList());


        int result = customHostKeyRepository.check(testHost, rsaKeyBytes);
        assertEquals(HostKeyRepository.NOT_INCLUDED, result);
    }

    @Test
    void check_hostKnown_keyMatches_returnsOk() throws JSchException {
        String rsaKeyBase64 = Base64.getEncoder().encodeToString(rsaKeyBytes);
        KnownSshHostKey knownKey = new KnownSshHostKey();
        knownKey.setHostname(testHost);
        knownKey.setPort(testPort); // Assuming port is matched or host string is specific
        knownKey.setKeyType(getKeyType(rsaKeyBytes));
        knownKey.setHostKeyBase64(rsaKeyBase64);
        knownKey.setLastVerified(LocalDateTime.now().minusHours(1));

        // Reflecting current check() implementation detail for this test
        when(knownHostKeyRepositoryMock.findAll()).thenReturn(List.of(knownKey));
        when(knownHostKeyRepositoryMock.save(any(KnownSshHostKey.class))).thenReturn(knownKey);

        int result = customHostKeyRepository.check(testHost, rsaKeyBytes);

        assertEquals(HostKeyRepository.OK, result);
        verify(knownHostKeyRepositoryMock).save(knownSshHostKeyCaptor.capture());
        assertTrue(knownSshHostKeyCaptor.getValue().getLastVerified().isAfter(knownKey.getLastVerified()));
    }

    @Test
    void check_hostKnown_keyDifferentForSameType_returnsChanged() throws JSchException {
        String originalRsaKeyBase64 = Base64.getEncoder().encodeToString(rsaKeyBytes);
        KnownSshHostKey knownKey = new KnownSshHostKey();
        knownKey.setHostname(testHost);
        knownKey.setPort(testPort);
        knownKey.setKeyType(getKeyType(rsaKeyBytes));
        knownKey.setHostKeyBase64(originalRsaKeyBase64);

        byte[] differentRsaKeyBytes = Base64.getDecoder().decode("AAAAB3NzaC1yc2EAAAADAQABAAABAQC7differentKeyBytes..."); // Different key

        when(knownHostKeyRepositoryMock.findAll()).thenReturn(List.of(knownKey));

        int result = customHostKeyRepository.check(testHost, differentRsaKeyBytes);
        assertEquals(HostKeyRepository.CHANGED, result);
    }

    @Test
    void check_hostKnown_differentKeyTypePresented_returnsNotInCLUDED() throws JSchException {
        // Existing key is RSA
        String rsaKeyBase64 = Base64.getEncoder().encodeToString(rsaKeyBytes);
        KnownSshHostKey knownRsaKey = new KnownSshHostKey();
        knownRsaKey.setHostname(testHost);
        knownRsaKey.setPort(testPort);
        knownRsaKey.setKeyType(getKeyType(rsaKeyBytes));
        knownRsaKey.setHostKeyBase64(rsaKeyBase64);

        // New key presented is ED25519
        when(knownHostKeyRepositoryMock.findAll()).thenReturn(List.of(knownRsaKey));

        int result = customHostKeyRepository.check(testHost, ed25519KeyBytes);
        assertEquals(HostKeyRepository.NOT_INCLUDED, result);
    }


    // --- Tests for add(HostKey hostkey, UserInfo ui) ---
    @Test
    void add_newHostKey_tofuAcceptsAndSaves() throws Exception {
        String newHost = "newhost.example.com";
        int newPort = 2222;
        HostKey newJschHostKey = new HostKey(newHost, newPort, rsaKeyBytes); // JSch parses type internally

        when(knownHostKeyRepositoryMock.findByHostnameAndPort(newHost, newPort)).thenReturn(Collections.emptyList());
        when(knownHostKeyRepositoryMock.save(any(KnownSshHostKey.class))).thenAnswer(inv -> inv.getArgument(0));

        customHostKeyRepository.add(newJschHostKey, null); // UserInfo is null for TOFU backend

        verify(knownHostKeyRepositoryMock).save(knownSshHostKeyCaptor.capture());
        KnownSshHostKey savedKey = knownSshHostKeyCaptor.getValue();

        assertEquals(newHost, savedKey.getHostname());
        assertEquals(newPort, savedKey.getPort());
        assertEquals(newJschHostKey.getType(), savedKey.getKeyType());
        assertEquals(Base64.getEncoder().encodeToString(rsaKeyBytes), savedKey.getHostKeyBase64());
        assertNotNull(savedKey.getFingerprintSha256());
        assertEquals(calculateSha256FingerprintForTest(rsaKeyBytes), savedKey.getFingerprintSha256());
        assertNotNull(savedKey.getFirstSeen());
        assertNotNull(savedKey.getLastVerified());
    }

    @Test
    void add_newHostKey_userInfoDeclines_doesNotSave() throws JSchException {
        HostKey newJschHostKey = new HostKey("newhost.example.com", rsaKeyBytes);
        when(userInfoMock.promptYesNo(anyString())).thenReturn(false); // User says NO

        customHostKeyRepository.add(newJschHostKey, userInfoMock);

        verify(knownHostKeyRepositoryMock, never()).save(any());
    }

    @Test
    void add_duplicateKey_updatesLastVerified() throws Exception {
        String host = "existing.com";
        int port = 22;
        HostKey jschHostKey = new HostKey(host, port, rsaKeyBytes);

        KnownSshHostKey existingDbKey = new KnownSshHostKey();
        existingDbKey.setHostname(host);
        existingDbKey.setPort(port);
        existingDbKey.setKeyType(jschHostKey.getType());
        existingDbKey.setHostKeyBase64(Base64.getEncoder().encodeToString(rsaKeyBytes)); // Same key
        LocalDateTime oldLastVerified = LocalDateTime.now().minusDays(1);
        existingDbKey.setLastVerified(oldLastVerified);

        when(knownHostKeyRepositoryMock.findByHostnameAndPort(host,port)).thenReturn(List.of(existingDbKey));
        when(knownHostKeyRepositoryMock.save(any(KnownSshHostKey.class))).thenReturn(existingDbKey);

        customHostKeyRepository.add(jschHostKey, null);

        verify(knownHostKeyRepositoryMock).save(knownSshHostKeyCaptor.capture());
        assertTrue(knownSshHostKeyCaptor.getValue().getLastVerified().isAfter(oldLastVerified));
        verify(knownHostKeyRepositoryMock, times(1)).save(any(KnownSshHostKey.class)); // Only one save (update)
    }


    // --- Tests for other HostKeyRepository methods ---
    @Test
    void getHostKey_byHostAndType_returnsMatchingKeys() throws Exception {
        KnownSshHostKey key1 = new KnownSshHostKey();
        key1.setHostname(testHost); key1.setPort(testPort); key1.setKeyType(RSA_KEY_TYPE);
        key1.setHostKeyBase64(Base64.getEncoder().encodeToString(rsaKeyBytes));

        when(knownHostKeyRepositoryMock.findByHostnameAndPortAndKeyType(testHost, testPort, RSA_KEY_TYPE))
            .thenReturn(Optional.of(key1));

        // Test with port in host string
        HostKey[] result = customHostKeyRepository.getHostKey("[" + testHost + "]:" + testPort, RSA_KEY_TYPE);
        assertEquals(1, result.length);
        assertEquals(testHost, result[0].getHost()); // JSch HostKey might normalize host string
        assertTrue(result[0].getHost().contains(testHost));
        assertEquals(RSA_KEY_TYPE, result[0].getType());
        assertTrue(Arrays.equals(rsaKeyBytes, Base64.getDecoder().decode(result[0].getKey())));
    }

    // TODO: Add more tests for remove methods, getHostKey() (all keys), and edge cases in parsing/matching.
}
