package com.codebridge.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provider for JWT token generation and validation.
 */
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "roles";
    private static final String USER_ID_KEY = "userId";
    private static final String TEAM_ID_KEY = "teamId";
    private static final String TOKEN_TYPE_KEY = "tokenType";
    private static final String EMAIL_KEY = "email";
    private static final String NAME_KEY = "name";
    private static final String PERMISSIONS_KEY = "permissions";
    private static final String TOKEN_ID_KEY = "jti";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Generates a JWT token for the given authentication.
     *
     * @param authentication the authentication object
     * @return the generated JWT token
     */
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_KEY, userPrincipal.getId());
        claims.put(AUTHORITIES_KEY, authorities);
        claims.put(TOKEN_TYPE_KEY, "access");
        claims.put(TOKEN_ID_KEY, java.util.UUID.randomUUID().toString());
        
        if (userPrincipal.getTeamId() != null) {
            claims.put(TEAM_ID_KEY, userPrincipal.getTeamId());
        }
        
        // Add additional claims if available
        if (userPrincipal.getEmail() != null) {
            claims.put(EMAIL_KEY, userPrincipal.getEmail());
        }
        
        if (userPrincipal.getName() != null) {
            claims.put(NAME_KEY, userPrincipal.getName());
        }
        
        if (userPrincipal.getPermissions() != null && !userPrincipal.getPermissions().isEmpty()) {
            claims.put(PERMISSIONS_KEY, String.join(",", userPrincipal.getPermissions()));
        }
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generates a refresh token for the given authentication.
     *
     * @param authentication the authentication object
     * @return the generated refresh token
     */
    public String generateRefreshToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_KEY, userPrincipal.getId());
        claims.put(TOKEN_TYPE_KEY, "refresh");
        claims.put(TOKEN_ID_KEY, java.util.UUID.randomUUID().toString());
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validates a JWT token.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            // Invalid JWT signature
        } catch (MalformedJwtException ex) {
            // Invalid JWT token
        } catch (ExpiredJwtException ex) {
            // Expired JWT token
        } catch (UnsupportedJwtException ex) {
            // Unsupported JWT token
        } catch (IllegalArgumentException ex) {
            // JWT claims string is empty
        }
        return false;
    }

    /**
     * Gets the authentication from a JWT token.
     *
     * @param token the JWT token
     * @return the authentication object
     */
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        String username = claims.getSubject();
        String userId = claims.get(USER_ID_KEY, String.class);
        
        Collection<? extends GrantedAuthority> authorities = null;
        if (claims.containsKey(AUTHORITIES_KEY)) {
            authorities = Arrays
                    .stream(claims.get(AUTHORITIES_KEY, String.class).split(","))
                    .filter(auth -> !auth.trim().isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            authorities = List.of();
        }
        
        UserPrincipal principal = new UserPrincipal(userId, username, "", authorities);
        
        if (claims.containsKey(TEAM_ID_KEY)) {
            principal.setTeamId(claims.get(TEAM_ID_KEY, String.class));
        }
        
        if (claims.containsKey(EMAIL_KEY)) {
            principal.setEmail(claims.get(EMAIL_KEY, String.class));
        }
        
        if (claims.containsKey(NAME_KEY)) {
            principal.setName(claims.get(NAME_KEY, String.class));
        }
        
        if (claims.containsKey(PERMISSIONS_KEY)) {
            String permissionsStr = claims.get(PERMISSIONS_KEY, String.class);
            List<String> permissions = Arrays.stream(permissionsStr.split(","))
                    .filter(perm -> !perm.trim().isEmpty())
                    .collect(Collectors.toList());
            principal.setPermissions(permissions);
        }
        
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * Gets the signing key for JWT token generation and validation.
     *
     * @return the signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
