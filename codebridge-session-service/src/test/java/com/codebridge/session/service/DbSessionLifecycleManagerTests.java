package com.codebridge.session.service;

import com.codebridge.session.config.DbSessionConfigProperties;
import com.codebridge.session.config.JwtConfigProperties;
import com.codebridge.session.dto.DbSessionCredentials;
import com.codebridge.session.dto.DbSessionMetadata;
import com.codebridge.session.dto.KeepAliveResponse;
import com.codebridge.session.dto.SessionResponse;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.model.enums.DbType;
import com.codebridge.session.security.jwt.JwtTokenProvider;
import com.codebridge.session.sessions.DbSessionWrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;


import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
class DbSessionLifecycleManagerTests {

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
    @Mock
    private ApplicationInstanceIdProvider applicationInstanceIdProviderMock;
    @Mock
    private Connection connectionMock; // For mocking DriverManager.getConnection

    private DbSessionLifecycleManager dbSessionManager;

    @Captor
    private ArgumentCaptor<DbSessionMetadata> metadataCaptor;

    private UUID testUserId;
    private String testDbConnectionAlias;
    private String testInstanceId;
    private DbSessionCredentials credentials;
    private SessionKey testSessionKey;


    @BeforeEach
    void setUp() {
        when(sessionKeyRedisTemplateMock.opsForValue()).thenReturn(sessionKeyValueOpsMock);
        when(dbSessionMetadataRedisTemplateMock.opsForValue()).thenReturn(dbSessionMetadataValueOpsMock);

        testInstanceId = "test-instance-db-123";
        when(applicationInstanceIdProviderMock.getInstanceId()).thenReturn(testInstanceId);

        dbSessionManager = new DbSessionLifecycleManager(
                sessionKeyRedisTemplateMock,
                dbSessionMetadataRedisTemplateMock,
                jwtTokenProviderMock,
                dbSessionConfigPropertiesMock,
                jwtConfigPropertiesMock,
                applicationInstanceIdProviderMock
        );
        ReflectionTestUtils.setField(dbSessionManager, "localActiveDbSessions", new ConcurrentHashMap<>());


        testUserId = UUID.randomUUID();
        testDbConnectionAlias = "my_pg_db";
        credentials = new DbSessionCredentials();
        credentials.setDbType(DbType.POSTGRESQL);
        credentials.setHost("localhost");
        credentials.setPort(5432);
        credentials.setUsername("pguser");
        credentials.setPassword("pgpass");
        credentials.setDatabaseName("postgresdb");

        UUID resourceId = UUID.nameUUIDFromBytes((testUserId.toString() + ":" + testDbConnectionAlias).getBytes(StandardCharsets.UTF_8));
        testSessionKey = new SessionKey(testUserId, resourceId, "DB:" + credentials.getDbType().name());


        when(dbSessionConfigPropertiesMock.getTimeoutMs()).thenReturn(300000L); // 5 minutes
        when(jwtConfigPropertiesMock.getExpirationMs()).thenReturn(600000L);    // 10 minutes
    }

    private DbSessionWrapper mockDbSessionWrapper(SessionKey key, boolean isValid, DbType dbType) throws SQLException {
        Connection connMock = mock(Connection.class);
        when(connMock.isValid(anyInt())).thenReturn(isValid);

        DbSessionWrapper wrapperMock = mock(DbSessionWrapper.class);
        when(wrapperMock.getSessionKey()).thenReturn(key);
        when(wrapperMock.getConnection()).thenReturn(connMock);
        when(wrapperMock.getDbType()).thenReturn(dbType);
        when(wrapperMock.isValid(anyInt())).thenReturn(isValid);
        when(wrapperMock.getLastAccessedTime()).thenReturn(System.currentTimeMillis());
        when(wrapperMock.getCreatedAt()).thenReturn(System.currentTimeMillis() - 1000);
        return wrapperMock;
    }

    @Test
    void initDbSession_success() {
        String expectedJwt = "new.db.jwt.token";
        when(jwtTokenProviderMock.generateToken(eq(testSessionKey))).thenReturn(expectedJwt);

        // Mock DriverManager.getConnection for the factory
        try (MockedStatic<DriverManager> mockedDriverManager = Mockito.mockStatic(DriverManager.class)) {
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                .thenReturn(connectionMock);
            when(connectionMock.isValid(anyInt())).thenReturn(true); // Ensure connection is valid

            SessionResponse response = dbSessionManager.initDbSession(testUserId, testDbConnectionAlias, credentials);

            assertNotNull(response);
            assertEquals(expectedJwt, response.getSessionToken());
            assertEquals(jwtConfigPropertiesMock.getExpirationMs(), response.getExpiresInMs());
            assertEquals(testUserId, response.getUserId());
            assertEquals(testSessionKey.resourceId(), response.getResourceId());
            assertEquals(testSessionKey.resourceType(), response.getResourceType());

            // Verify local cache
            assertTrue(dbSessionManager.getLocalSession(testSessionKey).isPresent());

            // Verify Redis interactions
            verify(sessionKeyValueOpsMock).set(
                eq("db:token:" + expectedJwt), // Check for db-specific prefix if used
                eq(testSessionKey),
                eq(jwtConfigPropertiesMock.getExpirationMs()),
                eq(TimeUnit.MILLISECONDS)
            );
            verify(dbSessionMetadataValueOpsMock).set(
                eq(dbSessionManager.dbSessionMetadataRedisKey(testSessionKey)),
                metadataCaptor.capture(),
                eq(dbSessionConfigPropertiesMock.getTimeoutMs()),
                eq(TimeUnit.MILLISECONDS)
            );
            DbSessionMetadata metadata = metadataCaptor.getValue();
            assertEquals(testSessionKey, metadata.getSessionKey());
            assertEquals(expectedJwt, metadata.getJwtToken());
            assertEquals(dbSessionManager.applicationInstanceId, metadata.getApplicationInstanceId());
            assertEquals(credentials.getDbType().name(), metadata.getDbType());
            assertEquals(credentials.getHost(), metadata.getDbHost());
        } catch (SQLException e) {
            fail("SQLException during mock setup: " + e.getMessage());
        }
    }

    @Test
    void initDbSession_jdbcConnectionFactoryThrowsSQLException_throwsRuntimeException() throws SQLException {
         try (MockedStatic<DriverManager> mockedDriverManager = Mockito.mockStatic(DriverManager.class)) {
            mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                .thenThrow(new SQLException("DB Connection Failed"));

            Exception e = assertThrows(RuntimeException.class, () -> {
                dbSessionManager.initDbSession(testUserId, testDbConnectionAlias, credentials);
            });
            assertTrue(e.getMessage().contains("Failed to create DB session via JDBC"));
        }
        assertTrue(dbSessionManager.localActiveDbSessions.isEmpty());
        verify(sessionKeyValueOpsMock, never()).set(anyString(), any(), anyLong(), any());
        verify(dbSessionMetadataValueOpsMock, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void keepAliveDbSession_validToken_metadataFound_localSessionPresentAndValid() throws SQLException {
        String oldJwt = "old.db.jwt";
        String newJwt = "new.db.jwt";
        DbSessionMetadata metadata = new DbSessionMetadata(testSessionKey, 0,0,oldJwt,dbSessionManager.applicationInstanceId,"POSTGRESQL","h","db","u");
        DbSessionWrapper localWrapper = mockDbSessionWrapper(testSessionKey, true, DbType.POSTGRESQL);
        ReflectionTestUtils.setField(dbSessionManager, "localActiveDbSessions", new ConcurrentHashMap<>(Map.of(testSessionKey, localWrapper)));

        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(oldJwt)).thenReturn(Optional.of(testSessionKey));
        when(dbSessionMetadataValueOpsMock.get(dbSessionManager.dbSessionMetadataRedisKey(testSessionKey))).thenReturn(metadata);
        when(jwtTokenProviderMock.generateToken(testSessionKey)).thenReturn(newJwt);

        Optional<KeepAliveResponse> response = dbSessionManager.keepAliveDbSession(oldJwt);

        assertTrue(response.isPresent());
        assertEquals(newJwt, response.get().getSessionToken());
        verify(localWrapper).updateLastAccessedTime();
        verify(dbSessionMetadataValueOpsMock).set(eq(dbSessionManager.dbSessionMetadataRedisKey(testSessionKey)), any(DbSessionMetadata.class), anyLong(), any(TimeUnit.class));
        verify(sessionKeyValueOpsMock).delete(dbSessionManager.dbTokenRedisKey(oldJwt));
        verify(sessionKeyValueOpsMock).set(eq(dbSessionManager.dbTokenRedisKey(newJwt)), eq(testSessionKey), anyLong(), any(TimeUnit.class));
    }

    @Test
    void keepAliveDbSession_validToken_localSessionInvalid_releasesAndReturnsEmpty() throws SQLException {
        String oldJwt = "old.db.jwt.invalid.local";
        DbSessionMetadata metadata = new DbSessionMetadata(testSessionKey, 0,0,oldJwt,dbSessionManager.applicationInstanceId,"POSTGRESQL","h","db","u");
        DbSessionWrapper localWrapper = mockDbSessionWrapper(testSessionKey, false, DbType.POSTGRESQL); // isValid returns false
        ReflectionTestUtils.setField(dbSessionManager, "localActiveDbSessions", new ConcurrentHashMap<>(Map.of(testSessionKey, localWrapper)));

        when(jwtTokenProviderMock.validateTokenAndExtractSessionKey(oldJwt)).thenReturn(Optional.of(testSessionKey));
        when(dbSessionMetadataValueOpsMock.get(dbSessionManager.dbSessionMetadataRedisKey(testSessionKey))).thenReturn(metadata);
        // generateToken will be called before session is found invalid by keepAlive
        when(jwtTokenProviderMock.generateToken(testSessionKey)).thenReturn("new.jwt.for.invalid.local");


        Optional<KeepAliveResponse> response = dbSessionManager.keepAliveDbSession(oldJwt);

        assertTrue(response.isEmpty());
        verify(localWrapper).closeConnection(); // From forceRelease
        assertFalse(dbSessionManager.localActiveDbSessions.containsKey(testSessionKey));
        // Verify redis cleanup due to forceRelease
        verify(dbSessionMetadataValueOpsMock).delete(dbSessionManager.dbSessionMetadataRedisKey(testSessionKey));
        // Token from metadata (which was 'oldJwt' before being updated to 'new.jwt.for.invalid.local' in memory for metadata object)
        // forceReleaseDbSessionByKey(key, true) -> true means it tries to get token from metadata to delete
        // The metadata object passed to forceRelease would have the *new* token if it reached that point.
        // However, keepAlive's logic for invalid local session:
        // it calls forceReleaseDbSessionByKey(sessionKey, true);
        // forceRelease will fetch metadata (which has oldJwt), then delete metadata, then delete oldJwt from token map.
        // The new token generated ('new.jwt.for.invalid.local') was never stored in token map if keepAlive returns empty early.
        // The old token ('oldJwt') is deleted by keepAlive itself *before* local check.
        verify(sessionKeyValueOpsMock).delete(dbSessionManager.dbTokenRedisKey(oldJwt)); // Deleted by keepAlive
        // If the new token was generated and metadata updated before local check failed, then new token mapping would be there
        // and forceRelease would clean it.
        // Current logic: metadata is updated, new token generated, then local check. If local check fails, forceRelease called.
        // forceRelease(key, true) -> gets metadata (now with new token), deletes metadata, deletes new token from token map.
        // So old token deleted by keepAlive, new token from (updated) metadata deleted by forceRelease.
        verify(sessionKeyValueOpsMock).delete(dbSessionManager.dbTokenRedisKey("new.jwt.for.invalid.local"));
    }


    @Test
    void cleanupExpiredDbSessions_cleansExpiredLocalSessionAndRedisEntries() throws SQLException {
        long currentTime = System.currentTimeMillis();
        long dbTimeout = dbSessionConfigPropertiesMock.getTimeoutMs();

        DbSessionWrapper expiredWrapper = mockDbSessionWrapper(testSessionKey, true, DbType.POSTGRESQL);
        when(expiredWrapper.getLastAccessedTime()).thenReturn(currentTime - dbTimeout - 1000); // Expired
        String expiredJwt = "expired.db.jwt";
        DbSessionMetadata expiredMetadata = new DbSessionMetadata(testSessionKey, 0,0,expiredJwt,testApplicationInstanceId,"POSTGRESQL","h","db","u");

        ReflectionTestUtils.setField(dbSessionManager, "localActiveDbSessions", new ConcurrentHashMap<>(Map.of(testSessionKey, expiredWrapper)));
        when(dbSessionMetadataValueOpsMock.get(dbSessionManager.dbSessionMetadataRedisKey(testSessionKey))).thenReturn(expiredMetadata);

        dbSessionManager.cleanupExpiredDbSessions();

        verify(expiredWrapper).closeConnection();
        assertTrue(dbSessionManager.localActiveDbSessions.isEmpty());
        verify(dbSessionMetadataValueOpsMock).delete(dbSessionManager.dbSessionMetadataRedisKey(testSessionKey));
        verify(sessionKeyValueOpsMock).delete(dbSessionManager.dbTokenRedisKey(expiredJwt));
    }
}
