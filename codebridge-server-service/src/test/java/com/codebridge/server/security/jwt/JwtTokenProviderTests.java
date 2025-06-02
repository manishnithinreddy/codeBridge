package com.codebridge.server.security.jwt;

import com.codebridge.server.config.JwtConfigProperties;
import com.codebridge.server.sessions.SessionKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTests {

    @Mock
    private JwtConfigProperties jwtConfigPropertiesMock;

    private JwtTokenProvider jwtTokenProvider;

    private final String testSecret = "test-secret-key-longer-than-256-bits-for-hs512-testing-test-secret";
    private final long testExpirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        when(jwtConfigPropertiesMock.getSecret()).thenReturn(testSecret);
        // jwtTokenProvider needs to be manually constructed to call @PostConstruct or init()
        jwtTokenProvider = new JwtTokenProvider(jwtConfigPropertiesMock);
        jwtTokenProvider.init(); // Manually call @PostConstruct method
    }

    @Test
    void testGenerateToken_success() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        String resourceType = "SSH";
        SessionKey sessionKey = new SessionKey(userId, resourceId, resourceType);

        when(jwtConfigPropertiesMock.getExpirationMs()).thenReturn(testExpirationMs);
        String token = jwtTokenProvider.generateToken(sessionKey);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Parse without signature validation for claim inspection
        String[] parts = token.split("\\.");
        assertTrue(parts.length == 3, "JWT should have 3 parts");

        // Decode claims (not verifying signature here, just checking content)
        Claims claims = Jwts.parserBuilder()
                            .setSigningKey(jwtTokenProvider.key) // Use the actual key for parsing
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals(resourceId.toString(), claims.get("resourceId", String.class));
        assertEquals(resourceType, claims.get("resourceType", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().getTime() > claims.getIssuedAt().getTime());
        assertEquals(claims.getIssuedAt().getTime() + testExpirationMs, claims.getExpiration().getTime());
    }

    @Test
    void testValidateTokenAndExtractSessionKey_success() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        String resourceType = "TEST_VM";
        SessionKey originalSessionKey = new SessionKey(userId, resourceId, resourceType);

        when(jwtConfigPropertiesMock.getExpirationMs()).thenReturn(testExpirationMs);
        String token = jwtTokenProvider.generateToken(originalSessionKey);

        Optional<SessionKey> extractedKeyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(token);

        assertTrue(extractedKeyOpt.isPresent());
        SessionKey extractedKey = extractedKeyOpt.get();
        assertEquals(originalSessionKey.userId(), extractedKey.userId());
        assertEquals(originalSessionKey.resourceId(), extractedKey.resourceId());
        assertEquals(originalSessionKey.resourceType(), extractedKey.resourceType());
    }

    @Test
    void testValidateTokenAndExtractSessionKey_expiredToken() throws InterruptedException {
        SessionKey sessionKey = new SessionKey(UUID.randomUUID(), UUID.randomUUID(), "SSH");
        when(jwtConfigPropertiesMock.getExpirationMs()).thenReturn(1L); // 1 millisecond expiration

        String token = jwtTokenProvider.generateToken(sessionKey);
        Thread.sleep(50); // Wait for token to expire

        Optional<SessionKey> extractedKeyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(token);
        assertTrue(extractedKeyOpt.isEmpty(), "Expected empty Optional for expired token");
    }

    @Test
    void testValidateTokenAndExtractSessionKey_invalidSignature() {
        SessionKey sessionKey = new SessionKey(UUID.randomUUID(), UUID.randomUUID(), "SSH");
        when(jwtConfigPropertiesMock.getExpirationMs()).thenReturn(testExpirationMs);
        String token = jwtTokenProvider.generateToken(sessionKey);

        // Create another provider with a different secret
        JwtConfigProperties otherConfig = mock(JwtConfigProperties.class);
        when(otherConfig.getSecret()).thenReturn("another-different-secret-key-for-testing-purposes-blah-blah");
        JwtTokenProvider otherTokenProvider = new JwtTokenProvider(otherConfig);
        otherTokenProvider.init();

        Optional<SessionKey> extractedKeyOpt = otherTokenProvider.validateTokenAndExtractSessionKey(token);
        assertTrue(extractedKeyOpt.isEmpty(), "Expected empty Optional for token with invalid signature");
    }

    @Test
    void testValidateTokenAndExtractSessionKey_malformedToken() {
        String malformedToken = "this.is.not.a.jwt";
        Optional<SessionKey> extractedKeyOpt = jwtTokenProvider.validateTokenAndExtractSessionKey(malformedToken);
        assertTrue(extractedKeyOpt.isEmpty(), "Expected empty Optional for malformed token");
    }

    @Test
    void testValidateTokenAndExtractSessionKey_nullToken_returnsEmpty() {
        Optional<SessionKey> result = jwtTokenProvider.validateTokenAndExtractSessionKey(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testValidateTokenAndExtractSessionKey_emptyToken_returnsEmpty() {
        Optional<SessionKey> result = jwtTokenProvider.validateTokenAndExtractSessionKey("");
        assertTrue(result.isEmpty());
    }

    @Test
    void testValidateTokenAndExtractSessionKey_tokenWithMissingCustomClaims_returnsEmpty() {
        // Generate a token with only subject, iat, exp
        Key signingKey = Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));
        String tokenWithMissingClaims = Jwts.builder()
            .setSubject(UUID.randomUUID().toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + testExpirationMs))
            .signWith(signingKey, SignatureAlgorithm.HS512)
            .compact();

        Optional<SessionKey> result = jwtTokenProvider.validateTokenAndExtractSessionKey(tokenWithMissingClaims);
        assertTrue(result.isEmpty(), "Token missing custom claims should be considered invalid for SessionKey extraction");
    }
}
