package com.codebridge.server.sessions;

import com.codebridge.server.config.DbSessionConfigProperties;
import com.codebridge.server.config.JwtConfigProperties;
import com.codebridge.server.dto.sessions.DbSessionCredentials;
import com.codebridge.server.dto.sessions.DbSessionMetadata;
import com.codebridge.server.dto.sessions.KeepAliveResponse;
import com.codebridge.server.dto.sessions.SessionResponse;
import com.codebridge.server.model.enums.DbType;
import com.codebridge.server.security.jwt.JwtTokenProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisDbSessionManagerImplTests {

    @Mock
    private RedisTemplate<String, SessionKey> sessionKeyRedisTemplateMock;
    @Mock
    private ValueOperations<String, SessionKey> sessionKeyValueOpsMock;

    @Mock
    private RedisTemplate<String, DbSessionMetadata> dbSessionMetadataRedisTemplateMock;
    @Mock
    private ValueOperations<String, DbSessionMetadata> dbSessionMetadataValueOpsMock;

    @Mock
    private JwtTokenProvider jwtTokenProviderMock;
    @Mock
    private DbSessionConfigProperties dbSessionConfigPropertiesMock;
    @Mock
    private JwtConfigProperties jwtConfigPropertiesMock;

    // We will mock Connection and DriverManager for initDbSession's factory part
    @Mock
    private Connection connectionMock;


    private RedisDbSessionManagerImpl dbSessionManager;

    private UUID testUserId;
    private String testDbConnectionAlias;
    private SessionKey testSessionKey; // Will be derived from userId and alias
    private String testApplicationInstanceIdBase = "test-db-app:8080";
    private String testApplicationInstanceId;

    @Captor
    private ArgumentCaptor<DbSessionMetadata> metadataCaptor;
    @Captor
    private ArgumentCaptor<SessionKey> sessionKeyCaptor;


    // Static block to mock DriverManager before any instantiation if needed for more complex scenarios
    // For now, direct mocking in test or factory should suffice.
    // @BeforeAll
    // static void beforeAll() {
    //     mockStatic(DriverManager.class);
    // }


    @BeforeEach
    void setUp() {
        when(sessionKeyRedisTemplateMock.opsForValue()).thenReturn(sessionKeyValueOpsMock);
        when(dbSessionMetadataRedisTemplateMock.opsForValue()).thenReturn(dbSessionMetadataValueOpsMock);

        dbSessionManager = new RedisDbSessionManagerImpl(
                sessionKeyRedisTemplateMock,
                dbSessionMetadataRedisTemplateMock,
                jwtTokenProviderMock,
                dbSessionConfigPropertiesMock,
                jwtConfigPropertiesMock,
                testApplicationInstanceIdBase
        );
        testApplicationInstanceId = dbSessionManager.applicationInstanceId;

        testUserId = UUID.randomUUID();
        testDbConnectionAlias = "my_test_db";

        UUID resourceId = UUID.nameUUIDFromBytes((testUserId.toString() + ":" + testDbConnectionAlias).getBytes(StandardCharsets.UTF_8));
        // DbType will be part of credentials, so SessionKey's resourceType will be dynamic in tests
        // testSessionKey = new SessionKey(testUserId, resourceId, "DB:POSTGRESQL"); // Example

        when(dbSessionConfigPropertiesMock.getTimeoutMs()).thenReturn(300000L); // 5 minutes DB session idle
        when(jwtConfigPropertiesMock.getExpirationMs()).thenReturn(600000L);    // 10 minutes JWT validity
    }

    private DbSessionWrapper mockDbSessionWrapper(SessionKey key, boolean isValid, DbType dbType) throws SQLException {
        Connection connMock = mock(Connection.class);
        when(connMock.isValid(anyInt())).thenReturn(isValid);

        DbSessionWrapper wrapperMock = mock(DbSessionWrapper.class);
        when(wrapperMock.getSessionKey()).thenReturn(key);
        when(wrapperMock.getConnection()).thenReturn(connMock);
        when(wrapperMock.getDbType()).thenReturn(dbType);
        when(wrapperMock.isValid(anyInt())).thenReturn(isValid); // delegate to its own connection's mock
        when(wrapperMock.getLastAccessedTime()).thenReturn(System.currentTimeMillis());
        when(wrapperMock.getCreatedAt()).thenReturn(System.currentTimeMillis() - 10000); // created 10s ago
        return wrapperMock;
    }

    private DbSessionCredentials createSampleCredentials(DbType dbType) {
        DbSessionCredentials creds = new DbSessionCredentials();
        creds.setDbType(dbType);
        creds.setHost("localhost");
        creds.setPort(5432);
        creds.setUsername("testuser");
        creds.setPassword("testpass");
        creds.setDatabaseName("testdb");
        return creds;
    }


    // --- I. Tests for initDbSession ---
    @Test
    void initDbSession_success() throws Exception {
        DbSessionCredentials credentials = createSampleCredentials(DbType.POSTGRESQL);
        UUID resourceId = UUID.nameUUIDFromBytes((testUserId.toString() + ":" + testDbConnectionAlias).getBytes(StandardCharsets.UTF_8));
        SessionKey expectedSessionKey = new SessionKey(testUserId, resourceId, "DB:" + credentials.getDbType().name());

        String dummyJwt = "dummy.db.jwt.token";
        when(jwtTokenProviderMock.generateToken(eq(expectedSessionKey))).thenReturn(dummyJwt);

        // Mocking the DriverManager.getConnection part indirectly
        // The factory in initDbSession will be executed. We need to control its outcome.
        // This is tricky because the factory is defined inline.
        // A spy on dbSessionManager and mocking createSession can isolate initDbSession's Redis logic.

        DbSessionWrapper mockWrapper = mockDbSessionWrapper(expectedSessionKey, true, DbType.POSTGRESQL);
        RedisDbSessionManagerImpl spyDbSessionManager = spy(dbSessionManager);
        // Ensure the spy's createSession method returns our mockWrapper when the factory is called.
        doAnswer(invocation -> {
            Supplier<DbSessionWrapper> factory = invocation.getArgument(1);
            // To truly test the factory, we'd need to mock DriverManager.getConnection for the factory.
            // For this specific test of initDbSession, we assume the factory works IF createSession works.
            // So we make createSession itself return a known good wrapper.
            return mockWrapper;
        }).when(spyDbSessionManager).createSession(eq(expectedSessionKey), any(Supplier.class));


        SessionResponse response = spyDbSessionManager.initDbSession(testUserId, testDbConnectionAlias, credentials);

        assertNotNull(response);
        assertEquals(dummyJwt, response.getSessionToken());
        assertEquals(testUserId, response.getUserId());
        assertEquals(resourceId, response.getResourceId());
        assertEquals(expectedSessionKey.resourceType(), response.getResourceType());
        assertEquals(jwtConfigPropertiesMock.getExpirationMs(), response.getExpiresInMs());

        verify(spyDbSessionManager).createSession(eq(expectedSessionKey), any(Supplier.class));

        verify(sessionKeyValueOpsMock).set(
            eq("db:session:token:" + dummyJwt),
            eq(expectedSessionKey),
            eq(jwtConfigPropertiesMock.getExpirationMs()),
            eq(TimeUnit.MILLISECONDS)
        );
        verify(dbSessionMetadataValueOpsMock).set(
            eq("db:session:metadata:" + testUserId + ":" + resourceId + ":" + expectedSessionKey.resourceType()),
            metadataCaptor.capture(),
            eq(dbSessionConfigPropertiesMock.getTimeoutMs()),
            eq(TimeUnit.MILLISECONDS)
        );
        DbSessionMetadata capturedMetadata = metadataCaptor.getValue();
        assertEquals(expectedSessionKey, capturedMetadata.getSessionKey());
        assertEquals(dummyJwt, capturedMetadata.getJwtToken());
        assertEquals(testApplicationInstanceId, capturedMetadata.getApplicationInstanceId());
        assertEquals(credentials.getDbType().name(), capturedMetadata.getDbType());
        assertEquals(credentials.getHost(), capturedMetadata.getDbHost());
    }

    @Test
    void initDbSession_jdbcConnectionFactoryFails_throwsRuntimeException() throws Exception {
        DbSessionCredentials credentials = createSampleCredentials(DbType.POSTGRESQL);

        RedisDbSessionManagerImpl spyDbSessionManager = spy(dbSessionManager);
        doThrow(new RuntimeException("Simulated factory failure: DB connection error"))
            .when(spyDbSessionManager).createSession(any(SessionKey.class), any(Supplier.class));

        Exception e = assertThrows(RuntimeException.class, () -> {
            spyDbSessionManager.initDbSession(testUserId, testDbConnectionAlias, credentials);
        });
        assertTrue(e.getMessage().contains("DB Session initialization and storage failed"));

        verify(sessionKeyValueOpsMock, never()).set(anyString(), any(), anyLong(), any());
        verify(dbSessionMetadataValueOpsMock, never()).set(anyString(), any(), anyLong(), any());
    }


    // --- II. Tests for getSession(SessionKey key) ---
    @Test
    void getSession_localSessionFoundValid_updatesMetadataAndReturns() throws SQLException {
        SessionKey key = new SessionKey(testUserId, UUID.randomUUID(), "DB:MYSQL");
        DbSessionWrapper localWrapper = mockDbSessionWrapper(key, true, DbType.MYSQL);
        dbSessionManager.localActiveDbSessions.put(key, localWrapper);

        DbSessionMetadata existingMetadata = new DbSessionMetadata(key, 0,0,"jwt",testApplicationInstanceId, "MYSQL", "h","db","u");
        when(dbSessionMetadataValueOpsMock.get(dbSessionManager.dbSessionMetadataRedisKey(key))).thenReturn(existingMetadata);

        Optional<DbSessionWrapper> result = dbSessionManager.getSession(key);

        assertTrue(result.isPresent());
        assertSame(localWrapper, result.get());
        verify(localWrapper).updateLastAccessedTime();
        verify(dbSessionMetadataValueOpsMock).set(eq(dbSessionManager.dbSessionMetadataRedisKey(key)), any(DbSessionMetadata.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    void getSession_localSessionFoundInvalid_releasesAndReturnsEmpty() throws SQLException {
        SessionKey key = new SessionKey(testUserId, UUID.randomUUID(), "DB:ORACLE");
        DbSessionWrapper localWrapper = mockDbSessionWrapper(key, false, DbType.ORACLE); // isValid returns false
        dbSessionManager.localActiveDbSessions.put(key, localWrapper);

        DbSessionMetadata metadata = new DbSessionMetadata(key,0,0,"jwt",testApplicationInstanceId,"ORACLE","h","db","u");
        when(dbSessionMetadataValueOpsMock.get(dbSessionManager.dbSessionMetadataRedisKey(key))).thenReturn(metadata);


        Optional<DbSessionWrapper> result = dbSessionManager.getSession(key);

        assertTrue(result.isEmpty());
        verify(localWrapper).closeConnection(); // From releaseSessionInternal
        assertFalse(dbSessionManager.localActiveDbSessions.containsKey(key));
        verify(dbSessionMetadataValueOpsMock).delete(dbSessionManager.dbSessionMetadataRedisKey(key));
        verify(sessionKeyValueOpsMock).delete(dbSessionManager.dbTokenRedisKey("jwt"));
    }


    // --- III. Tests for keepAliveDbSession ---
    @Test
    void keepAliveDbSession_validJwt_metadataFound_updatesRedisReturnsNewJwt() throws SQLException {
        SessionKey key = new SessionKey(testUserId, UUID.randomUUID(), "DB:SQLSERVER");
        String oldJwt = "old.db.jwt";
        String newJwt = "new.db.jwt";
        DbSessionMetadata metadata = new DbSessionMetadata(key, 0,0,oldJwt,testApplicationInstanceId,"SQLSERVER","h","db","u");

        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(oldJwt)).thenReturn(Optional.of(key));
        when(dbSessionMetadataValueOpsMock.get(dbSessionManager.dbSessionMetadataRedisKey(key))).thenReturn(metadata);
        when(jwtTokenProviderMock.generateToken(key)).thenReturn(newJwt);

        // Simulate session is local and valid
        DbSessionWrapper localWrapper = mockDbSessionWrapper(key, true, DbType.SQLSERVER);
        dbSessionManager.localActiveDbSessions.put(key, localWrapper);


        Optional<KeepAliveResponse> response = dbSessionManager.keepAliveDbSession(oldJwt);

        assertTrue(response.isPresent());
        assertEquals(newJwt, response.get().getSessionToken());
        verify(dbSessionMetadataValueOpsMock).set(eq(dbSessionManager.dbSessionMetadataRedisKey(key)), any(DbSessionMetadata.class), anyLong(), any(TimeUnit.class));
        verify(sessionKeyValueOpsMock).delete(dbSessionManager.dbTokenRedisKey(oldJwt));
        verify(sessionKeyValueOpsMock).set(eq(dbSessionManager.dbTokenRedisKey(newJwt)), eq(key), anyLong(), any(TimeUnit.class));
        verify(localWrapper).updateLastAccessedTime();
    }

    // --- IV. Tests for releaseDbSession / releaseSession ---
    @Test
    void releaseDbSession_byToken_validJwt_localSessionExists_cleansAll() throws SQLException {
        SessionKey key = new SessionKey(testUserId, UUID.randomUUID(), "DB:POSTGRESQL");
        String jwtToRelease = "db.jwt.to.release";
        DbSessionWrapper localWrapper = mockDbSessionWrapper(key, true, DbType.POSTGRESQL);
        dbSessionManager.localActiveDbSessions.put(key, localWrapper);
        // Metadata not strictly needed for this path if token is known, but releaseInternal might fetch it
        // DbSessionMetadata metadata = new DbSessionMetadata(key,0,0,jwtToRelease,testApplicationInstanceId,"POSTGRESQL","h","db","u");
        // when(dbSessionMetadataValueOpsMock.get(dbSessionManager.dbSessionMetadataRedisKey(key))).thenReturn(metadata);


        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(jwtToRelease)).thenReturn(Optional.of(key));

        dbSessionManager.releaseDbSession(jwtToRelease);

        verify(sessionKeyValueOpsMock).delete(dbSessionManager.dbTokenRedisKey(jwtToRelease));
        verify(localWrapper).closeConnection();
        assertFalse(dbSessionManager.localActiveDbSessions.containsKey(key));
        verify(dbSessionMetadataValueOpsMock).delete(dbSessionManager.dbSessionMetadataRedisKey(key));
    }


    // --- V. Tests for cleanupExpiredSessions ---
    @Test
    void cleanupExpiredSessions_cleansExpiredLocalAndRedisEntries() throws SQLException {
        long currentTime = System.currentTimeMillis();
        long dbTimeout = dbSessionConfigPropertiesMock.getTimeoutMs();

        SessionKey expiredKey = new SessionKey(UUID.randomUUID(), UUID.randomUUID(), "DB:EXPIRED_TYPE");
        DbSessionWrapper expiredWrapper = mockDbSessionWrapper(expiredKey, true, DbType.OTHER);
        when(expiredWrapper.getLastAccessedTime()).thenReturn(currentTime - dbTimeout - 1000); // Expired
        String expiredJwt = "expired.db.jwt";
        DbSessionMetadata expiredMetadata = new DbSessionMetadata(expiredKey,0,0,expiredJwt,testApplicationInstanceId,"OTHER","h","db","u");

        dbSessionManager.localActiveDbSessions.put(expiredKey, expiredWrapper);
        when(dbSessionMetadataValueOpsMock.get(dbSessionManager.dbSessionMetadataRedisKey(expiredKey))).thenReturn(expiredMetadata);

        SessionKey activeKey = new SessionKey(UUID.randomUUID(), UUID.randomUUID(), "DB:ACTIVE_TYPE");
        DbSessionWrapper activeWrapper = mockDbSessionWrapper(activeKey, true, DbType.MYSQL);
        when(activeWrapper.getLastAccessedTime()).thenReturn(currentTime - 1000); // Active
        dbSessionManager.localActiveDbSessions.put(activeKey, activeWrapper);

        dbSessionManager.cleanupExpiredSessions();

        verify(expiredWrapper).closeConnection();
        assertFalse(dbSessionManager.localActiveDbSessions.containsKey(expiredKey));
        verify(dbSessionMetadataValueOpsMock).delete(dbSessionManager.dbSessionMetadataRedisKey(expiredKey));
        verify(sessionKeyValueOpsMock).delete(dbSessionManager.dbTokenRedisKey(expiredJwt));

        assertTrue(dbSessionManager.localActiveDbSessions.containsKey(activeKey));
        verify(activeWrapper, never()).closeConnection();
    }
}
