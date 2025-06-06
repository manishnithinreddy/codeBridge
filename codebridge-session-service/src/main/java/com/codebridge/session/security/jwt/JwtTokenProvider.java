package com.codebridge.session.security.jwt;

import com.codebridge.session.config.JwtConfigProperties;
import com.codebridge.session.model.SessionKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey jwtSecretKey;
    private final long jwtExpirationMs;
    private final String issuer;

    public JwtTokenProvider(JwtConfigProperties jwtConfigProperties) {
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtConfigProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtConfigProperties.getExpirationMs();
        this.issuer = jwtConfigProperties.getIssuer();
    }

    public String generateToken(SessionKey sessionKey, UUID platformUserId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        Claims claims = Jwts.claims().setSubject(platformUserId.toString());
        claims.put("sessionId", sessionKey.toString()); // Or individual fields of SessionKey
        claims.put("resourceId", sessionKey.resourceId().toString());
        claims.put("type", sessionKey.sessionType());
        // Add other claims as needed, e.g., "instanceId" if relevant for token

        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Error parsing JWT token: {}", e.getMessage());
            // Depending on strictness, could throw custom exception or return null/empty claims
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(jwtSecretKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // Log different exceptions (ExpiredJwtException, UnsupportedJwtException, MalformedJwtException, SignatureException, IllegalArgumentException)
            logger.warn("JWT token validation failed: {}", e.getMessage());
        }
        return false;
    }

    public Date getExpiryDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }
}
