package com.codebridge.server.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Utility class for JWT operations.
 */
@Component
public class JwtUtil {

    @Value("${codebridge.security.jwt.shared-secret:codebridgeSecretKeyForDevelopmentOnly}")
    private String secret;

    /**
     * Extracts all claims from a token.
     *
     * @param token the JWT token
     * @return the claims
     */
    public Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts a specific claim from a token.
     *
     * @param token the JWT token
     * @param claimsResolver the function to extract the claim
     * @param <T> the type of the claim
     * @return the claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts the subject from a token.
     *
     * @param token the JWT token
     * @return the subject
     */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a token.
     *
     * @param token the JWT token
     * @return the expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Checks if a token is expired.
     *
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates a token.
     *
     * @param token the JWT token
     * @return true if the token is valid, false otherwise
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}

