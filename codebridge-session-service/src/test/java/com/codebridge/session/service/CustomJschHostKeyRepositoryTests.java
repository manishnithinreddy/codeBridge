package com.codebridge.session.service;

import com.codebridge.session.model.KnownSshHostKey;
import com.codebridge.session.repository.KnownSshHostKeyRepository;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomJschHostKeyRepositoryTests {

    @Mock private KnownSshHostKeyRepository knownSshHostKeyRepository;
    @Mock private UserInfo mockUserInfo;

    @InjectMocks
    private CustomJschHostKeyRepository customJschHostKeyRepository;

    private String testHost;
    private int testPort;
    private String testKeyType;
    private byte[] testKeyBytes;
    private String testKeyBase64;
    private HostKey testJschHostKey;

    @BeforeEach
    void setUp() throws JSchException {
        testHost = "localhost";
        testPort = 2222;
        testKeyType = "ssh-ed25519"; // JSch might derive this from key bytes
        // Example ed25519 public key bytes (simplified, actual key bytes are binary)
        // For a real test, you'd use actual key material.
        testKeyBase64 = "AAAAC3NzaC1lZDI1NTE5AAAAIGgc9n3k+k2A+X2J+v0K+p7p+q8G+pWj+p7G+X2J+v0K";
        testKeyBytes = Base64.getDecoder().decode(testKeyBase64);
        
        // JSch HostKey constructor might need different params depending on how it parses host
        // For testing 'add', we often create it like JSch would after parsing a line or getting from agent
        testJschHostKey = new HostKey(testHost, testPort, HostKey.GUESS, testKeyBytes); 
        // JSch might internally convert testHost to "[localhost]:2222"
    }

    @Test
    void check_hostKeyExistsAndMatches_returnsOk() {
        KnownSshHostKey knownKey = new KnownSshHostKey();
        knownKey.setHostname(testHost);
        knownKey.setPort(testPort);
        knownKey.setKeyType(testJschHostKey.getType()); // Use type derived by JSch's HostKey
        knownKey.setHostKeyBase64(testKeyBase64);
        
        when(knownSshHostKeyRepository.findByHostnameAndPortAndKeyType(testHost, testPort, testJschHostKey.getType()))
            .thenReturn(Optional.of(knownKey));

        int result = customJschHostKeyRepository.check(String.format("%s:%d",testHost,testPort), testKeyBytes);
        assertEquals(HostKeyRepository.OK, result);
    }

    @Test
    void check_hostKeyExistsButDifferent_returnsChanged() {
        KnownSshHostKey knownKey = new KnownSshHostKey();
        knownKey.setHostname(testHost);
        knownKey.setPort(testPort);
        knownKey.setKeyType(testJschHostKey.getType());
        knownKey.setHostKeyBase64("DIFFERENT_KEY_BASE64"); // Different key
        
        when(knownSshHostKeyRepository.findByHostnameAndPortAndKeyType(testHost, testPort, testJschHostKey.getType()))
            .thenReturn(Optional.of(knownKey));

        int result = customJschHostKeyRepository.check(String.format("%s:%d",testHost,testPort), testKeyBytes);
        assertEquals(HostKeyRepository.CHANGED, result);
    }
    
    @Test
    void check_hostKeyNotExists_returnsNotIncluded() {
         when(knownSshHostKeyRepository.findByHostnameAndPortAndKeyType(anyString(), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        int result = customJschHostKeyRepository.check(String.format("%s:%d",testHost,testPort), testKeyBytes);
        assertEquals(HostKeyRepository.NOT_INCLUDED, result);
    }

    @Test
    void add_newHostKey_savesToDb() {
        // For 'add', JSch often provides HostKey with host string that might include port
        // HostKey testJschHostKeyForAdd = new HostKey(String.format("[%s]:%d", testHost, testPort), HostKey.GUESS, testKeyBytes);
        // For this test, ensure the parsing logic in 'add' correctly extracts hostname and port
        HostKey jschHostKeyForAdd = new HostKey(String.format("%s:%d", testHost, testPort), HostKey.GUESS, testKeyBytes);


        // UserInfo ui = null; // Simulate auto-accept for backend service
        customJschHostKeyRepository.add(jschHostKeyForAdd, mockUserInfo); // Using mockUserInfo

        ArgumentCaptor<KnownSshHostKey> captor = ArgumentCaptor.forClass(KnownSshHostKey.class);
        verify(knownSshHostKeyRepository).save(captor.capture());
        
        KnownSshHostKey savedKey = captor.getValue();
        assertEquals(testHost, savedKey.getHostname());
        assertEquals(testPort, savedKey.getPort());
        assertEquals(jschHostKeyForAdd.getType(), savedKey.getKeyType());
        assertEquals(jschHostKeyForAdd.getKey(), savedKey.getHostKeyBase64()); // JSch HostKey.getKey() returns base64
        assertNotNull(savedKey.getFingerprintSha256());
    }
}
