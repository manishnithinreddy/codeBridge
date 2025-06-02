package com.codebridge.session.security.jwt; // Adapted package

import com.codebridge.session.config.JwtConfigProperties; // Adapted import
import com.codebridge.session.model.SessionKey; // Adapted import
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtConfigProperties jwtConfigProperties;
    private Key key;

    public JwtTokenProvider(JwtConfigProperties jwtConfigProperties) {
        this.jwtConfigProperties = jwtConfigProperties;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtConfigProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT signing key initialized.");
    }

    public String generateToken(SessionKey sessionKey) {
        if (sessionKey == null) {
            throw new IllegalArgumentException("SessionKey cannot be null for JWT generation");
        }

        Claims claims = Jwts.claims().setSubject(sessionKey.userId().toString());
        claims.put("resourceId", sessionKey.resourceId().toString());
        claims.put("resourceType", sessionKey.resourceType());
        // Add any other claims relevant to the session if needed

        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtConfigProperties.getExpirationMs());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS512) // Using HS512 for stronger signature
                .compact();
    }

    public Optional<SessionKey> validateTokenAndExtractSessionKey(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                                        .setSigningKey(key)
                                        .build()
                                        .parseClaimsJws(token);

            Claims claims = claimsJws.getBody();
            UUID userId = UUID.fromString(claims.getSubject());
            UUID resourceId = UUID.fromString(claims.get("resourceId", String.class));
            String resourceType = claims.get("resourceType", String.class);

            if (resourceType == null || resourceId == null ) {
                 log.warn("Token {} is missing resourceId or resourceType claims.", token);
                 return Optional.empty();
            }

            return Optional.of(new SessionKey(userId, resourceId, resourceType));

        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}. Message: {}", token, e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}. Message: {}", token, e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}. Message: {}", token, e.getMessage());
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}. Message: {}", token, e.getMessage());
        } catch (IllegalArgumentException e) { // Handles null/empty tokens if not caught earlier, or other issues
            log.warn("JWT claims string is empty or token is invalid: {}. Message: {}", token, e.getMessage());
        } catch (Exception e) { // Catch-all for other potential parsing errors
            log.error("Unexpected error validating JWT token: {}. Message: {}", token, e.getMessage(), e);
        }
        return Optional.empty();
    }
}
