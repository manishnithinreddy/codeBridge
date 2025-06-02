package com.codebridge.server.sessions;

import com.codebridge.server.config.JwtConfigProperties;
import com.codebridge.server.config.SshSessionConfigProperties;
import com.codebridge.server.dto.sessions.KeepAliveResponse;
import com.codebridge.server.dto.sessions.SessionResponse;
import com.codebridge.server.security.jwt.JwtTokenProvider;
import com.codebridge.server.exception.AccessDeniedException;
import com.codebridge.server.exception.ResourceNotFoundException;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.service.ServerAccessControlService;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InMemorySessionManagerImplTests {

    @Mock
    private ServerAccessControlService serverAccessControlServiceMock;

    @Mock
    private SshSessionConfigProperties sshConfigPropertiesMock;

    @Mock
    private JwtTokenProvider jwtTokenProviderMock; // New dependency

    @Mock
    private JwtConfigProperties jwtConfigPropertiesMock; // For SessionResponse.expiresInMs alignment


    // No longer using @Spy for internal maps, will verify through behavior and direct access for assertion.
    private InMemorySessionManagerImpl sessionManager;

    private UUID testUserId;
    private UUID testServerId;
    private SessionKey testSessionKey;

    @BeforeEach
    void setUp() {
        sessionManager = new InMemorySessionManagerImpl(
            sshConfigPropertiesMock,
            serverAccessControlServiceMock,
            jwtTokenProviderMock // new dependency
        );

        testUserId = UUID.randomUUID();
        testServerId = UUID.randomUUID();
        testSessionKey = new SessionKey(testUserId, testServerId, "SSH");

        // Default config properties
        when(sshConfigPropertiesMock.getTimeoutMs()).thenReturn(600000L); // 10 minutes for session expiry
        when(jwtConfigPropertiesMock.getExpirationMs()).thenReturn(900000L); // 15 minutes for JWT expiry (example)
    }

    private SshSessionWrapper mockSshSessionWrapper(SessionKey key, boolean connected) {
        Session jschSessionMock = mock(Session.class);
        when(jschSessionMock.isConnected()).thenReturn(connected);
        SshSessionWrapper wrapperMock = mock(SshSessionWrapper.class);
        when(wrapperMock.getSessionKey()).thenReturn(key);
        when(wrapperMock.getJschSession()).thenReturn(jschSessionMock);
        when(wrapperMock.isConnected()).thenReturn(connected);
        when(wrapperMock.getLastAccessedTime()).thenReturn(System.currentTimeMillis());
        return wrapperMock;
    }

    // --- Tests for initSshSession ---
    @Test
    void initSshSession_success() throws Exception {
        Server serverMock = new Server();
        serverMock.setAuthProvider(ServerAuthProvider.SSH_KEY);
        serverMock.setHostname("localhost");
        serverMock.setPort(22);

        SshKey sshKeyMock = mock(SshKey.class);
        when(sshKeyMock.getId()).thenReturn(UUID.randomUUID());
        when(sshKeyMock.getPrivateKeyBytes()).thenReturn("fakeprivatekey".getBytes());

        ServerAccessControlService.UserSpecificConnectionDetails details =
            new ServerAccessControlService.UserSpecificConnectionDetails(serverMock, "testuser", sshKeyMock);
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenReturn(details);

        // This test will indirectly test the JSch connection logic within the factory.
        // We are not mocking the factory itself, but the components it uses.
        // For a more isolated unit test of initSshSession *excluding* factory logic,
        // we would have to refactor createSession to accept a pre-built SshSessionWrapper,
        // or make the factory itself mockable/injectable.

        String dummyJwt = "dummy.jwt.token";
        when(jwtTokenProviderMock.generateToken(eq(testSessionKey))).thenReturn(dummyJwt);
        // Assume SessionResponse.expiresInMs now comes from jwtConfigProperties
        when(sessionManager.jwtConfigProperties.getExpirationMs()).thenReturn(900000L);


        SessionResponse response = sessionManager.initSshSession(testUserId, testServerId);

        assertNotNull(response);
        assertEquals(dummyJwt, response.getSessionToken()); // Token is now JWT
        assertEquals(testUserId, response.getUserId());
        assertEquals(testServerId, response.getResourceId());
        assertEquals("SSH", response.getResourceType());
        assertEquals(jwtConfigPropertiesMock.getExpirationMs(), response.getExpiresInMs()); // Aligned with JWT

        verify(jwtTokenProviderMock, times(1)).generateToken(eq(testSessionKey));
        assertTrue(sessionManager.getSession(testSessionKey).isPresent());
        assertEquals(testSessionKey, sessionManager.sessionTokenToKeyMap.get(dummyJwt));
    }

    @Test
    void initSshSession_accessDenied_throwsRuntimeException() throws Exception {
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenThrow(new AccessDeniedException("Access Denied"));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            sessionManager.initSshSession(testUserId, testServerId);
        });
        assertTrue(exception.getMessage().contains("Access denied or server not found"));
        assertTrue(sessionManager.activeSessions.isEmpty());
        assertTrue(sessionManager.sessionTokenToKeyMap.isEmpty());
    }

    @Test
    void initSshSession_factoryThrowsJSchException_throwsRuntimeException() throws Exception {
        Server serverMock = new Server();
        serverMock.setAuthProvider(ServerAuthProvider.SSH_KEY);
        // Setup to make JSch fail (e.g. invalid key format if JSch was real, or mock JSch object if possible)
        // For this test, we assume the factory logic itself (which uses real JSch) will throw.
        // This is hard to mock perfectly without refactoring createSession to accept a Supplier<Session>
        // rather than Supplier<SshSessionWrapper> and mocking that internal Session.
        // Here, we'll simulate by having checkUserAccessAndGetConnectionDetails return data
        // that would cause JSch constructor or connect to fail.
        // A simpler way for this level of test is to make the factory itself throw an exception.
        // However, the current design has the factory defined inside initSshSession.

        SshKey sshKeyMock = mock(SshKey.class); // Bad key that might cause JSchException
        when(sshKeyMock.getPrivateKeyBytes()).thenReturn("bad-key-format".getBytes());


        ServerAccessControlService.UserSpecificConnectionDetails details =
             new ServerAccessControlService.UserSpecificConnectionDetails(serverMock, "testuser", sshKeyMock);
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenReturn(details);

        // To truly test the JSchException from the factory, we'd need to ensure new JSch().getSession() fails.
        // This is an integration aspect within the unit test.
        // The provided code for initSshSession catches Exception from createSession, which wraps factory exception.

        Exception exception = assertThrows(RuntimeException.class, () -> {
             sessionManager.initSshSession(testUserId, testServerId);
        });
        // Depending on how JSch is mocked or if it's real, the message might vary.
        // If JSch is real and fails due to bad key, it would be a JSchException wrapped.
        assertTrue(exception.getMessage().contains("Failed to create SSH session") || exception.getMessage().contains("Session initialization failed"));
        assertTrue(sessionManager.activeSessions.isEmpty());
        assertTrue(sessionManager.sessionTokenToKeyMap.isEmpty());
    }


    // --- Tests for keepAliveSshSession ---
    @Test
    void keepAliveSshSession_validToken_returnsResponseAndUpdatesAccessTime() {
        String oldJwt = "old.jwt.token";
        String newJwt = "new.jwt.token";
        SshSessionWrapper wrapperMock = mockSshSessionWrapper(testSessionKey, true);

        sessionManager.activeSessions.put(testSessionKey, wrapperMock); // Active session
        sessionManager.sessionTokenToKeyMap.put(oldJwt, testSessionKey); // Old token mapped

        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(eq(oldJwt))).thenReturn(Optional.of(testSessionKey));
        when(jwtTokenProviderMock.generateToken(eq(testSessionKey))).thenReturn(newJwt);
        // SessionResponse.expiresInMs from jwtConfigProperties
        when(sessionManager.jwtConfigProperties.getExpirationMs()).thenReturn(900000L);


        long initialAccessTime = wrapperMock.getLastAccessedTime();
         // Mock behavior of updateLastAccessedTime if needed, or ensure it's called via getSession
        doAnswer(invocation -> {
            when(wrapperMock.getLastAccessedTime()).thenReturn(System.currentTimeMillis());
            return null;
        }).when(wrapperMock).updateLastAccessedTime();


        Optional<KeepAliveResponse> responseOpt = sessionManager.keepAliveSshSession(oldJwt);

        assertTrue(responseOpt.isPresent());
        assertEquals(newJwt, responseOpt.get().getSessionToken()); // New JWT returned
        assertEquals(jwtConfigPropertiesMock.getExpirationMs(), responseOpt.get().getExpiresInMs());

        verify(jwtTokenProviderMock, times(1)).validateTokenAndExtractSessionKey(eq(oldJwt));
        verify(jwtTokenProviderMock, times(1)).generateToken(eq(testSessionKey));
        verify(wrapperMock, times(1)).updateLastAccessedTime(); // getSession calls this
        assertTrue(wrapperMock.getLastAccessedTime() > initialAccessTime || wrapperMock.getLastAccessedTime() == System.currentTimeMillis());

        assertFalse(sessionManager.sessionTokenToKeyMap.containsKey(oldJwt)); // Old token removed
        assertTrue(sessionManager.sessionTokenToKeyMap.containsKey(newJwt)); // New token added
        assertEquals(testSessionKey, sessionManager.sessionTokenToKeyMap.get(newJwt));
    }

    @Test
    void keepAliveSshSession_invalidJwt_returnsEmpty() {
        String invalidJwt = "invalid.jwt.token";
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(eq(invalidJwt))).thenReturn(Optional.empty());

        Optional<KeepAliveResponse> responseOpt = sessionManager.keepAliveSshSession(invalidJwt);

        assertTrue(responseOpt.isEmpty());
        verify(jwtTokenProviderMock, times(1)).validateTokenAndExtractSessionKey(eq(invalidJwt));
        verify(jwtTokenProviderMock, never()).generateToken(any());
        assertTrue(sessionManager.sessionTokenToKeyMap.isEmpty()); // Assuming remove is called for invalid token
    }

    @Test
    void keepAliveSshSession_sessionExpiredBetweenTokenLookupAndGet_returnsEmptyAndCleansToken() {
        String validJwt = "valid.jwt.token";
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(eq(validJwt))).thenReturn(Optional.of(testSessionKey));
        sessionManager.sessionTokenToKeyMap.put(validJwt, testSessionKey); // Token initially maps to key
        // activeSessions does NOT contain testSessionKey, simulating it expired

        Optional<KeepAliveResponse> responseOpt = sessionManager.keepAliveSshSession(validJwt);

        assertTrue(responseOpt.isEmpty());
        verify(jwtTokenProviderMock, times(1)).validateTokenAndExtractSessionKey(eq(validJwt));
        assertFalse(sessionManager.sessionTokenToKeyMap.containsKey(validJwt)); // Token should be cleaned up
    }

    // --- Tests for releaseSshSession ---
    @Test
    void releaseSshSession_validToken_releasesSessionAndToken() {
        SshSessionWrapper wrapperMock = mockSshSessionWrapper(testSessionKey, true);
        sessionManager.activeSessions.put(testSessionKey, wrapperMock);
        String validJwt = "valid.jwt.token.to.release";
        sessionManager.sessionTokenToKeyMap.put(validJwt, testSessionKey);

        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(eq(validJwt))).thenReturn(Optional.of(testSessionKey));

        sessionManager.releaseSshSession(validJwt);

        verify(wrapperMock, times(1)).disconnect();
        verify(jwtTokenProviderMock, times(1)).validateTokenAndExtractSessionKey(eq(validJwt));
        assertFalse(sessionManager.activeSessions.containsKey(testSessionKey));
        assertFalse(sessionManager.sessionTokenToKeyMap.containsKey(validJwt));
    }

    @Test
    void releaseSshSession_invalidToken_doesNothingSignificant() {
        String invalidJwt = "invalid.jwt.token.for.release";
        SshSessionWrapper wrapperMock = mockSshSessionWrapper(testSessionKey, true);
        sessionManager.activeSessions.put(testSessionKey, wrapperMock); // A session exists

        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(eq(invalidJwt))).thenReturn(Optional.empty());

        sessionManager.releaseSshSession(invalidJwt); // But released with wrong token

        verify(wrapperMock, never()).disconnect(); // Original session not disconnected
        verify(jwtTokenProviderMock, times(1)).validateTokenAndExtractSessionKey(eq(invalidJwt));
        assertTrue(sessionManager.activeSessions.containsKey(testSessionKey)); // Still there
        // Check if token map was cleaned for the specific invalid token
        // The implementation calls sessionTokenToKeyMap.remove(sessionToken) even if invalid
        assertFalse(sessionManager.sessionTokenToKeyMap.containsKey(invalidJwt));
    }

    // --- Tests for cleanupExpiredSessions (Interaction with token map) ---
    @Test
    @SuppressWarnings("unchecked") // For the spy interaction if needed, though direct check is better
    void cleanupExpiredSessions_removesExpiredSessionsAndTheirTokens() throws InterruptedException {
        SshSessionWrapper expiredWrapper = mock(SshSessionWrapper.class);
        SessionKey expiredKey = new SessionKey(UUID.randomUUID(), UUID.randomUUID(), "SSH_EXPIRED");
        when(expiredWrapper.getLastAccessedTime()).thenReturn(System.currentTimeMillis() - 700000L); // 700s ago, timeout is 600s
        when(expiredWrapper.isConnected()).thenReturn(true); // Still connected but expired by time
        when(expiredWrapper.getSessionKey()).thenReturn(expiredKey);


        SshSessionWrapper activeWrapper = mock(SshSessionWrapper.class);
        SessionKey activeKey = new SessionKey(UUID.randomUUID(), UUID.randomUUID(), "SSH_ACTIVE");
        when(activeWrapper.getLastAccessedTime()).thenReturn(System.currentTimeMillis() - 10000L); // 10s ago
        when(activeWrapper.getSessionKey()).thenReturn(activeKey);


        String expiredJwt = "expired.jwt.token";
        String activeJwt = "active.jwt.token";

        sessionManager.activeSessions.put(expiredKey, expiredWrapper);
        sessionManager.sessionTokenToKeyMap.put(expiredJwt, expiredKey);

        sessionManager.activeSessions.put(activeKey, activeWrapper);
        sessionManager.sessionTokenToKeyMap.put(activeJwt, activeKey);


        sessionManager.cleanupExpiredSessions();

        verify(expiredWrapper, times(1)).disconnect();
        assertFalse(sessionManager.activeSessions.containsKey(expiredKey));
        assertFalse(sessionManager.sessionTokenToKeyMap.containsKey(expiredJwt));

        assertTrue(sessionManager.activeSessions.containsKey(activeKey)); // Active session remains
        assertTrue(sessionManager.sessionTokenToKeyMap.containsKey(activeJwt)); // Active token remains
        verify(activeWrapper, never()).disconnect();
    }

    // Test for releaseSession(SessionKey key) interaction with token map
    @Test
    void releaseSessionBySessionKey_alsoRemovesToken() {
        SshSessionWrapper wrapperMock = mockSshSessionWrapper(testSessionKey, true);
        sessionManager.activeSessions.put(testSessionKey, wrapperMock);
        String jwt = "some.jwt.token";
        sessionManager.sessionTokenToKeyMap.put(jwt, testSessionKey); // Token linked to session key

        sessionManager.releaseSession(testSessionKey); // Release by key

        verify(wrapperMock, times(1)).disconnect();
        assertFalse(sessionManager.activeSessions.containsKey(testSessionKey));
        assertFalse(sessionManager.sessionTokenToKeyMap.containsKey(jwt), "Token should be removed when session is released by key");
    }


    // --- General tests for existing methods to ensure no regressions ---
    @Test
    void getSession_forConnectedSession_updatesLastAccessedTime() {
        SshSessionWrapper wrapperMock = mockSshSessionWrapper(testSessionKey, true);
        sessionManager.activeSessions.put(testSessionKey, wrapperMock);
        long initialAccessTime = wrapperMock.getLastAccessedTime();
         doAnswer(invocation -> {
            when(wrapperMock.getLastAccessedTime()).thenReturn(System.currentTimeMillis());
            return null;
        }).when(wrapperMock).updateLastAccessedTime();


        Optional<SshSessionWrapper> result = sessionManager.getSession(testSessionKey);

        assertTrue(result.isPresent());
        verify(wrapperMock, times(1)).updateLastAccessedTime();
        assertTrue(wrapperMock.getLastAccessedTime() > initialAccessTime || wrapperMock.getLastAccessedTime() == System.currentTimeMillis());

    }

    @Test
    void getSession_forDisconnectedSession_releasesItAndReturnsEmpty() {
        SshSessionWrapper wrapperMock = mockSshSessionWrapper(testSessionKey, false); // Not connected
        sessionManager.activeSessions.put(testSessionKey, wrapperMock);
        String jwt = "some.jwt.token.for.disconnected.session";
        sessionManager.sessionTokenToKeyMap.put(jwt, testSessionKey);


        Optional<SshSessionWrapper> result = sessionManager.getSession(testSessionKey);

        assertTrue(result.isEmpty());
        verify(wrapperMock, times(1)).disconnect(); // Should be called by releaseSession
        assertFalse(sessionManager.activeSessions.containsKey(testSessionKey));
        // Token cleanup for disconnected session found by getSession is handled by cleanupExpiredSessions
        // or if keepAlive/release explicitly find the token and then an empty session.
        // getSession itself doesn't know about tokens.
        // However, the test for cleanupExpiredSessions covers token removal for expired (which could be disconnected)
        // The releaseSessionBySessionKey_alsoRemovesToken test covers explicit key-based release.
        // For getSession finding a disconnected one, the token remains until cleanup or explicit release by token.
        // This is acceptable.
         assertTrue(sessionManager.sessionTokenToKeyMap.containsKey(jwt), "Token should still be there until explicit release by token or cleanup");
    }

    @Test
    void createSession_whenKeyAlreadyExists_replacesOldSession() throws Exception {
        SshSessionWrapper oldWrapperMock = mockSshSessionWrapper(testSessionKey, true);
        sessionManager.activeSessions.put(testSessionKey, oldWrapperMock);
        String oldJwt = "old.jwt.token.for.replaced.session";
        sessionManager.sessionTokenToKeyMap.put(oldJwt, testSessionKey);


        Supplier<SshSessionWrapper> newSessionFactory = () -> mockSshSessionWrapper(testSessionKey, true);
        SshSessionWrapper newWrapper = sessionManager.createSession(testSessionKey, newSessionFactory);

        assertNotNull(newWrapper);
        assertNotSame(oldWrapperMock, newWrapper);
        verify(oldWrapperMock, times(1)).disconnect(); // Old session disconnected
        assertEquals(newWrapper, sessionManager.activeSessions.get(testSessionKey));

        // Check token map. createSession doesn't handle tokens. initSshSession does.
        // If an old token was associated with testSessionKey, it's still there.
        // This is complex: if createSession is called directly, it doesn't update tokens.
        // If initSshSession calls createSession (which it does), createSession evicts the old *session*.
        // Then initSshSession puts a *new* token. The old token for the same key might become dangling
        // if not managed carefully.
        // The current implementation of createSession calls releaseSession, which now also cleans the token.
        assertTrue(sessionManager.sessionTokenToKeyMap.get(oldJwt) == null, "Old token should be removed if releaseSession cleans tokens");
    }
}
