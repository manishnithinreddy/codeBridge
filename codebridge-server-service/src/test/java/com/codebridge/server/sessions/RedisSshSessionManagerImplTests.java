package com.codebridge.server.sessions;

import com.codebridge.server.config.JwtConfigProperties;
import com.codebridge.server.config.SshSessionConfigProperties;
import com.codebridge.server.dto.sessions.KeepAliveResponse;
import com.codebridge.server.dto.sessions.SessionResponse;
import com.codebridge.server.dto.sessions.SshSessionMetadata;
import com.codebridge.server.model.Server;
import com.codebridge.server.model.SshKey;
import com.codebridge.server.model.enums.ServerAuthProvider;
import com.codebridge.server.security.jwt.JwtTokenProvider;
import com.codebridge.server.service.ServerAccessControlService;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSshSessionManagerImplTests {

    @Mock
    private RedisTemplate<String, SessionKey> sessionKeyRedisTemplateMock;
    @Mock
    private ValueOperations<String, SessionKey> sessionKeyValueOpsMock;

    @Mock
    private RedisTemplate<String, SshSessionMetadata> sessionMetadataRedisTemplateMock;
    @Mock
    private ValueOperations<String, SshSessionMetadata> sessionMetadataValueOpsMock;

    @Mock
    private JwtTokenProvider jwtTokenProviderMock;
    @Mock
    private ServerAccessControlService serverAccessControlServiceMock;
    @Mock
    private SshSessionConfigProperties sshSessionConfigPropertiesMock;
    @Mock
    private JwtConfigProperties jwtConfigPropertiesMock;

    private RedisSshSessionManagerImpl sessionManager;

    private UUID testUserId;
    private UUID testServerId;
    private SessionKey testSessionKey;
    private String testApplicationInstanceIdBase = "test-app:8080";
    private String testApplicationInstanceId; // Will be base + random UUID part

    @Captor
    private ArgumentCaptor<SshSessionMetadata> metadataCaptor;
    @Captor
    private ArgumentCaptor<SessionKey> sessionKeyCaptor;
    @Captor
    private ArgumentCaptor<String> stringCaptor;


    @BeforeEach
    void setUp() {
        when(sessionKeyRedisTemplateMock.opsForValue()).thenReturn(sessionKeyValueOpsMock);
        when(sessionMetadataRedisTemplateMock.opsForValue()).thenReturn(sessionMetadataValueOpsMock);

        sessionManager = new RedisSshSessionManagerImpl(
                sessionKeyRedisTemplateMock,
                sessionMetadataRedisTemplateMock,
                jwtTokenProviderMock,
                serverAccessControlServiceMock,
                sshSessionConfigPropertiesMock,
                jwtConfigPropertiesMock,
                testApplicationInstanceIdBase
        );
        // Capture the generated instance ID for assertions
        testApplicationInstanceId = sessionManager.applicationInstanceId;


        testUserId = UUID.randomUUID();
        testServerId = UUID.randomUUID();
        testSessionKey = new SessionKey(testUserId, testServerId, "SSH");

        // Default config properties
        when(sshSessionConfigPropertiesMock.getTimeoutMs()).thenReturn(600000L); // 10 minutes SSH session idle
        when(jwtConfigPropertiesMock.getExpirationMs()).thenReturn(900000L);    // 15 minutes JWT validity
    }

    private SshSessionWrapper mockSshSessionWrapper(SessionKey key, boolean connected, long createdAt, long lastAccessedTime) {
        Session jschSessionMock = mock(Session.class);
        when(jschSessionMock.isConnected()).thenReturn(connected);

        SshSessionWrapper wrapperMock = mock(SshSessionWrapper.class);
        when(wrapperMock.getSessionKey()).thenReturn(key);
        when(wrapperMock.getJschSession()).thenReturn(jschSessionMock);
        when(wrapperMock.isConnected()).thenReturn(connected);
        when(wrapperMock.getCreatedAt()).thenReturn(createdAt);
        when(wrapperMock.getLastAccessedTime()).thenReturn(lastAccessedTime);
        return wrapperMock;
    }

    private ServerAccessControlService.UserSpecificConnectionDetails mockConnectionDetails() {
        Server serverMock = new Server();
        serverMock.setAuthProvider(ServerAuthProvider.SSH_KEY);
        serverMock.setHostname("localhost");
        serverMock.setPort(22);
        serverMock.setId(testServerId);


        SshKey sshKeyMock = mock(SshKey.class);
        when(sshKeyMock.getId()).thenReturn(UUID.randomUUID());
        when(sshKeyMock.getPrivateKeyBytes()).thenReturn("fakeprivatekey".getBytes());
        // when(sshKeyMock.getPublicKeyBytes()).thenReturn("fakepublickey".getBytes()); // If needed

        return new ServerAccessControlService.UserSpecificConnectionDetails(serverMock, "testuser", sshKeyMock);
    }

    // --- I. Tests for initSshSession ---
    @Test
    void initSshSession_success() throws Exception {
        ServerAccessControlService.UserSpecificConnectionDetails details = mockConnectionDetails();
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId)).thenReturn(details);

        String dummyJwt = "dummy.jwt.token";
        when(jwtTokenProviderMock.generateToken(eq(testSessionKey))).thenReturn(dummyJwt);

        // Mock the behavior of the sessionFactory within createSession
        // For this test, we assume the factory successfully creates a connected session.
        // The actual JSch connection logic isn't tested here, but that the factory is called.
        SshSessionWrapper mockWrapper = mockSshSessionWrapper(testSessionKey, true, System.currentTimeMillis(), System.currentTimeMillis());
        // We need to use a more flexible way to mock the supplier if createSession is called internally.
        // For now, let's assume createSession works and focus on initSshSession's Redis logic.
        // This part is tricky as createSession takes a Supplier.
        // A direct call to createSession is easier to test for its own logic.

        // To test initSshSession fully, we need to ensure its call to 'this.createSession'
        // returns a mock wrapper. This typically requires spy or refactoring.
        // For now, let's assume createSession will successfully put a mock into localActiveSessions
        // by also mocking the supplier it would create.
        // This test will be more of an integration test of initSshSession with a mocked createSession behavior.

        // To simplify, we'll mock the supplier that initSshSession builds and passes to createSession
        // This means we won't directly test the JSch logic here but the orchestration by initSshSession

        RedisSshSessionManagerImpl spySessionManager = spy(sessionManager);
        doReturn(mockWrapper).when(spySessionManager).createSession(eq(testSessionKey), any(Supplier.class));


        SessionResponse response = spySessionManager.initSshSession(testUserId, testServerId);

        assertNotNull(response);
        assertEquals(dummyJwt, response.getSessionToken());
        assertEquals(testUserId, response.getUserId());
        assertEquals(testServerId, response.getResourceId());
        assertEquals(jwtConfigPropertiesMock.getExpirationMs(), response.getExpiresInMs());

        // Verify createSession was called (which populates localActiveSessions)
        verify(spySessionManager, times(1)).createSession(eq(testSessionKey), any(Supplier.class));

        // Verify Redis interactions
        verify(sessionKeyValueOpsMock, times(1)).set(
            eq("ssh:session:token:" + dummyJwt),
            eq(testSessionKey),
            eq(jwtConfigPropertiesMock.getExpirationMs()),
            eq(TimeUnit.MILLISECONDS)
        );
        verify(sessionMetadataValueOpsMock, times(1)).set(
            eq("ssh:session:metadata:" + testUserId + ":" + testServerId + ":SSH"),
            metadataCaptor.capture(),
            eq(sshSessionConfigPropertiesMock.getTimeoutMs()),
            eq(TimeUnit.MILLISECONDS)
        );
        SshSessionMetadata capturedMetadata = metadataCaptor.getValue();
        assertEquals(testSessionKey, capturedMetadata.getSessionKey());
        assertEquals(dummyJwt, capturedMetadata.getJwtToken());
        assertEquals(testApplicationInstanceId, capturedMetadata.getApplicationInstanceId());
        assertTrue(capturedMetadata.getCreatedAt() > 0);
        assertEquals(capturedMetadata.getCreatedAt(), capturedMetadata.getLastAccessedTime());
    }

    @Test
    void initSshSession_serverAccessControlThrowsError_noSessionCreatedOrStored() throws Exception {
        when(serverAccessControlServiceMock.checkUserAccessAndGetConnectionDetails(testUserId, testServerId))
            .thenThrow(new com.codebridge.server.exception.AccessDeniedException("Access Denied Test"));

        assertThrows(RuntimeException.class, () -> {
            sessionManager.initSshSession(testUserId, testServerId);
        });

        assertTrue(sessionManager.localActiveSessions.isEmpty());
        verify(sessionKeyValueOpsMock, never()).set(anyString(), any(), anyLong(), any());
        verify(sessionMetadataValueOpsMock, never()).set(anyString(), any(), anyLong(), any());
        verify(jwtTokenProviderMock, never()).generateToken(any());
    }


    // --- II. Tests for getSession(SessionKey key) ---
    @Test
    void getSession_localSessionFoundConnected_updatesMetadataAndReturnsWrapper() {
        long createTime = System.currentTimeMillis() - 10000; // 10s ago
        long lastAccessTime = createTime;
        SshSessionWrapper localWrapper = mockSshSessionWrapper(testSessionKey, true, createTime, lastAccessTime);
        sessionManager.localActiveSessions.put(testSessionKey, localWrapper);

        SshSessionMetadata existingMetadata = new SshSessionMetadata(testSessionKey, createTime, lastAccessTime, "old.jwt", testApplicationInstanceId);
        when(sessionMetadataValueOpsMock.get(sessionManager.sessionMetadataRedisKey(testSessionKey))).thenReturn(existingMetadata);

        Optional<SshSessionWrapper> resultOpt = sessionManager.getSession(testSessionKey);

        assertTrue(resultOpt.isPresent());
        assertSame(localWrapper, resultOpt.get());
        verify(localWrapper, times(1)).updateLastAccessedTime(); // important internal call

        // Verify metadata update in Redis
        verify(sessionMetadataValueOpsMock, times(1)).set(
            eq(sessionManager.sessionMetadataRedisKey(testSessionKey)),
            metadataCaptor.capture(),
            eq(sshSessionConfigPropertiesMock.getTimeoutMs()),
            eq(TimeUnit.MILLISECONDS)
        );
        assertTrue(metadataCaptor.getValue().getLastAccessedTime() > lastAccessTime || metadataCaptor.getValue().getLastAccessedTime() == System.currentTimeMillis());
    }

    @Test
    void getSession_localSessionFoundDisconnected_releasesSessionAndReturnsEmpty() {
        SshSessionWrapper localWrapper = mockSshSessionWrapper(testSessionKey, false, System.currentTimeMillis(), System.currentTimeMillis());
        sessionManager.localActiveSessions.put(testSessionKey, localWrapper);

        // For releaseSessionInternal to clean up token, it needs metadata
        SshSessionMetadata metadata = new SshSessionMetadata(testSessionKey, 0,0, "jwt-for-disconnected", testApplicationInstanceId);
        when(sessionMetadataValueOpsMock.get(sessionManager.sessionMetadataRedisKey(testSessionKey))).thenReturn(metadata);


        Optional<SshSessionWrapper> resultOpt = sessionManager.getSession(testSessionKey);

        assertTrue(resultOpt.isEmpty());
        verify(localWrapper, times(1)).disconnect();
        assertFalse(sessionManager.localActiveSessions.containsKey(testSessionKey));
        verify(sessionMetadataValueOpsMock, times(1)).delete(eq(sessionManager.sessionMetadataRedisKey(testSessionKey)));
        verify(sessionKeyValueOpsMock, times(1)).delete(eq(sessionManager.tokenRedisKey("jwt-for-disconnected")));
    }

    @Test
    void getSession_notLocal_metadataInRedis_returnsEmpty() {
        // localActiveSessions is empty for testSessionKey
        SshSessionMetadata metadataFromOtherInstance = new SshSessionMetadata(testSessionKey, System.currentTimeMillis(), System.currentTimeMillis(), "some.jwt", "other-instance-id");
        when(sessionMetadataValueOpsMock.get(sessionManager.sessionMetadataRedisKey(testSessionKey))).thenReturn(metadataFromOtherInstance);

        Optional<SshSessionWrapper> resultOpt = sessionManager.getSession(testSessionKey);
        assertTrue(resultOpt.isEmpty());
    }

    @Test
    void getSession_notLocal_noMetadataInRedis_returnsEmpty() {
        // localActiveSessions is empty
        when(sessionMetadataValueOpsMock.get(sessionManager.sessionMetadataRedisKey(testSessionKey))).thenReturn(null);
        Optional<SshSessionWrapper> resultOpt = sessionManager.getSession(testSessionKey);
        assertTrue(resultOpt.isEmpty());
    }


    // --- III. Tests for keepAliveSshSession(String sessionToken) ---
    @Test
    void keepAliveSshSession_validJwt_metadataFound_updatesRedisAndReturnsNewJwt() {
        String oldJwt = "old.jwt.token";
        String newJwt = "new.jwt.token";
        long creationTime = System.currentTimeMillis() - 10000;
        SshSessionMetadata existingMetadata = new SshSessionMetadata(testSessionKey, creationTime, creationTime, oldJwt, testApplicationInstanceId);

        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(oldJwt)).thenReturn(Optional.of(testSessionKey));
        when(sessionMetadataValueOpsMock.get(sessionManager.sessionMetadataRedisKey(testSessionKey))).thenReturn(existingMetadata);
        when(jwtTokenProviderMock.generateToken(testSessionKey)).thenReturn(newJwt);

        // Simulate session is local to this instance for full test coverage
        SshSessionWrapper localWrapper = mockSshSessionWrapper(testSessionKey, true, creationTime, creationTime);
        sessionManager.localActiveSessions.put(testSessionKey, localWrapper);


        Optional<KeepAliveResponse> responseOpt = sessionManager.keepAliveSshSession(oldJwt);

        assertTrue(responseOpt.isPresent());
        assertEquals(newJwt, responseOpt.get().getSessionToken());
        assertEquals(jwtConfigPropertiesMock.getExpirationMs(), responseOpt.get().getExpiresInMs());

        verify(sessionMetadataValueOpsMock).set(
            eq(sessionManager.sessionMetadataRedisKey(testSessionKey)),
            metadataCaptor.capture(),
            eq(sshSessionConfigPropertiesMock.getTimeoutMs()),
            eq(TimeUnit.MILLISECONDS)
        );
        SshSessionMetadata updatedMetadata = metadataCaptor.getValue();
        assertEquals(newJwt, updatedMetadata.getJwtToken());
        assertTrue(updatedMetadata.getLastAccessedTime() > creationTime);

        verify(sessionKeyValueOpsMock).delete(sessionManager.tokenRedisKey(oldJwt));
        verify(sessionKeyValueOpsMock).set(
            eq(sessionManager.tokenRedisKey(newJwt)),
            eq(testSessionKey),
            eq(jwtConfigPropertiesMock.getExpirationMs()),
            eq(TimeUnit.MILLISECONDS)
        );
        verify(localWrapper, times(1)).updateLastAccessedTime(); // Local wrapper also updated
    }

    @Test
    void keepAliveSshSession_invalidJwt_returnsEmpty() {
        String invalidJwt = "invalid.jwt";
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(invalidJwt)).thenReturn(Optional.empty());

        Optional<KeepAliveResponse> responseOpt = sessionManager.keepAliveSshSession(invalidJwt);

        assertTrue(responseOpt.isEmpty());
        verify(sessionKeyValueOpsMock).delete(sessionManager.tokenRedisKey(invalidJwt)); // Ensures cleanup attempt
    }

    @Test
    void keepAliveSshSession_jwtValid_noMetadata_returnsEmptyAndCleansTokenMap() {
        String validJwtButNoMeta = "valid.jwt.no.meta";
        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(validJwtButNoMeta)).thenReturn(Optional.of(testSessionKey));
        when(sessionMetadataValueOpsMock.get(sessionManager.sessionMetadataRedisKey(testSessionKey))).thenReturn(null); // No metadata

        Optional<KeepAliveResponse> responseOpt = sessionManager.keepAliveSshSession(validJwtButNoMeta);

        assertTrue(responseOpt.isEmpty());
        verify(sessionKeyValueOpsMock).delete(sessionManager.tokenRedisKey(validJwtButNoMeta)); // Token mapping cleaned
    }


    // --- IV. Tests for releaseSshSession(String sessionToken) and releaseSession(SessionKey key) ---
    @Test
    void releaseSshSession_byToken_validJwt_localSessionExists() {
        String jwtToRelease = "jwt.to.release";
        SshSessionWrapper localWrapper = mockSshSessionWrapper(testSessionKey, true, System.currentTimeMillis(), System.currentTimeMillis());
        sessionManager.localActiveSessions.put(testSessionKey, localWrapper);
        SshSessionMetadata metadata = new SshSessionMetadata(testSessionKey, 0,0, jwtToRelease, testApplicationInstanceId);

        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(jwtToRelease)).thenReturn(Optional.of(testSessionKey));
        // No need to mock get for metadata for release by token, as internalRelease will be called
        // But if internalRelease needs to find token via metadata, then yes.
        // Current internalRelease does not need metadata to find token if called from releaseSshSession(token)
        // because token is already known.

        sessionManager.releaseSshSession(jwtToRelease);

        verify(sessionKeyValueOpsMock).delete(sessionManager.tokenRedisKey(jwtToRelease));
        verify(localWrapper).disconnect();
        assertFalse(sessionManager.localActiveSessions.containsKey(testSessionKey));
        verify(sessionMetadataValueOpsMock).delete(sessionManager.sessionMetadataRedisKey(testSessionKey));
    }

    @Test
    void releaseSession_byKey_localSessionExists_metadataAndTokenCleaned() {
        String jwtInMetadata = "jwt.in.metadata.for.key.release";
        SshSessionWrapper localWrapper = mockSshSessionWrapper(testSessionKey, true, System.currentTimeMillis(), System.currentTimeMillis());
        sessionManager.localActiveSessions.put(testSessionKey, localWrapper);
        SshSessionMetadata metadata = new SshSessionMetadata(testSessionKey, 0,0, jwtInMetadata, testApplicationInstanceId);
        when(sessionMetadataValueOpsMock.get(sessionManager.sessionMetadataRedisKey(testSessionKey))).thenReturn(metadata);

        boolean released = sessionManager.releaseSession(testSessionKey); // Release by KEY

        assertTrue(released);
        verify(localWrapper).disconnect();
        assertFalse(sessionManager.localActiveSessions.containsKey(testSessionKey));
        verify(sessionMetadataValueOpsMock).delete(sessionManager.sessionMetadataRedisKey(testSessionKey));
        verify(sessionKeyValueOpsMock).delete(sessionManager.tokenRedisKey(jwtInMetadata)); // Token from metadata deleted
    }


    // --- V. Tests for cleanupExpiredSessions ---
    @Test
    void cleanupExpiredSessions_cleansExpiredLocalSessionsAndRedisEntries() {
        long currentTime = System.currentTimeMillis();
        long sshTimeout = sshSessionConfigPropertiesMock.getTimeoutMs();

        // Expired session
        SessionKey expiredKey = new SessionKey(UUID.randomUUID(), UUID.randomUUID(), "SSH_EXPIRED");
        SshSessionWrapper expiredWrapper = mockSshSessionWrapper(expiredKey, true,
                                                                currentTime - sshTimeout - 20000, // created long ago
                                                                currentTime - sshTimeout - 1000);  // accessed just outside timeout
        String expiredJwt = "expired.jwt";
        SshSessionMetadata expiredMetadata = new SshSessionMetadata(expiredKey, 0,0, expiredJwt, testApplicationInstanceId);
        sessionManager.localActiveSessions.put(expiredKey, expiredWrapper);
        when(sessionMetadataValueOpsMock.get(sessionManager.sessionMetadataRedisKey(expiredKey))).thenReturn(expiredMetadata);


        // Active session
        SessionKey activeKey = new SessionKey(UUID.randomUUID(), UUID.randomUUID(), "SSH_ACTIVE");
        SshSessionWrapper activeWrapper = mockSshSessionWrapper(activeKey, true,
                                                              currentTime - 10000, // created recently
                                                              currentTime - 5000);   // accessed recently
        sessionManager.localActiveSessions.put(activeKey, activeWrapper);
        // No need to mock metadata for active session as it shouldn't be touched by delete operations

        sessionManager.cleanupExpiredSessions();

        verify(expiredWrapper).disconnect();
        assertFalse(sessionManager.localActiveSessions.containsKey(expiredKey));
        verify(sessionMetadataValueOpsMock).delete(sessionManager.sessionMetadataRedisKey(expiredKey));
        verify(sessionKeyValueOpsMock).delete(sessionManager.tokenRedisKey(expiredJwt));

        assertTrue(sessionManager.localActiveSessions.containsKey(activeKey));
        verify(activeWrapper, never()).disconnect();
    }

    // --- VI. Tests for storeSession ---
    @Test
    void storeSession_updatesLocalAndRedisMetadata() {
        SshSessionWrapper localWrapper = mockSshSessionWrapper(testSessionKey, true, System.currentTimeMillis(), System.currentTimeMillis());
        String jwtForStore = "jwt.for.store";

        SshSessionMetadata existingMetadata = new SshSessionMetadata(testSessionKey, 0,0, jwtForStore, testApplicationInstanceId);
        when(sessionMetadataValueOpsMock.get(sessionManager.sessionMetadataRedisKey(testSessionKey))).thenReturn(existingMetadata);
        // Simulate finding the token for this session key (this part is complex as per implementation notes)
        // For simplicity, assume metadata already has the token or it remains null if not found by scan.
        // We are mainly testing that lastAccessedTime is updated and set is called.

        sessionManager.storeSession(testSessionKey, localWrapper);

        assertTrue(sessionManager.localActiveSessions.containsKey(testSessionKey));
        assertSame(localWrapper, sessionManager.localActiveSessions.get(testSessionKey));

        verify(sessionMetadataValueOpsMock).set(
            eq(sessionManager.sessionMetadataRedisKey(testSessionKey)),
            metadataCaptor.capture(),
            eq(sshSessionConfigPropertiesMock.getTimeoutMs()),
            eq(TimeUnit.MILLISECONDS)
        );
        assertEquals(localWrapper.getLastAccessedTime(), metadataCaptor.getValue().getLastAccessedTime());
        assertEquals(testApplicationInstanceId, metadataCaptor.getValue().getApplicationInstanceId());
    }
}
