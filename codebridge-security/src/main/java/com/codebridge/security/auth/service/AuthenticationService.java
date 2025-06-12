package com.codebridge.security.auth.service;

import com.codebridge.security.audit.AuditLogger;
import com.codebridge.security.auth.dto.AuthenticationRequest;
import com.codebridge.security.auth.dto.AuthenticationResponse;
import com.codebridge.security.auth.dto.MfaVerificationRequest;
import com.codebridge.security.auth.dto.RefreshTokenRequest;
import com.codebridge.security.auth.jwt.JwtTokenProvider;
import com.codebridge.security.auth.mfa.MfaService;
import com.codebridge.security.auth.model.User;
import com.codebridge.security.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final MfaService mfaService;
    private final AuditLogger auditLogger;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${security.account.max-failed-attempts}")
    private int maxFailedAttempts;
    
    @Value("${security.account.lockout-duration-minutes}")
    private int lockoutDurationMinutes;
    
    @Value("${security.jwt.refresh-token.expiration-time}")
    private long refreshTokenValidity;

    /**
     * Authenticates a user.
     *
     * @param request The authentication request
     * @return The authentication response
     */
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            // Attempt authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Get the authenticated user
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            // Reset failed attempts on successful login
            if (user.getFailedAttempts() > 0) {
                userRepository.resetFailedAttempts(user.getUsername());
            }
            
            // Check if MFA is required
            if (user.isMfaEnabled()) {
                // Generate a temporary token for MFA verification
                String mfaToken = tokenProvider.createAccessToken(user);
                
                // Store the MFA token in Redis with a short expiration
                String mfaKey = "mfa:" + user.getUsername();
                redisTemplate.opsForValue().set(mfaKey, mfaToken, 5, TimeUnit.MINUTES);
                
                // Log MFA required event
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("username", user.getUsername());
                metadata.put("userId", user.getId());
                
                auditLogger.logSecurityEvent(
                        "MFA_REQUIRED",
                        "Multi-factor authentication required for user",
                        metadata
                );
                
                return AuthenticationResponse.builder()
                        .mfaRequired(true)
                        .mfaToken(mfaToken)
                        .build();
            }
            
            // Generate tokens
            String accessToken = tokenProvider.createAccessToken(user);
            String refreshToken = tokenProvider.createRefreshToken(user);
            
            // Store refresh token in Redis
            String refreshKey = "refresh:" + user.getUsername();
            redisTemplate.opsForValue().set(refreshKey, refreshToken, refreshTokenValidity, TimeUnit.MILLISECONDS);
            
            // Log successful authentication
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("username", user.getUsername());
            metadata.put("userId", user.getId());
            
            auditLogger.logSecurityEvent(
                    "AUTHENTICATION_SUCCESS",
                    "User authenticated successfully",
                    metadata
            );
            
            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .mfaRequired(false)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .build();
            
        } catch (BadCredentialsException e) {
            // Handle failed login attempt
            handleFailedLogin(request.getUsername());
            
            // Log failed authentication
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("username", request.getUsername());
            metadata.put("reason", "Bad credentials");
            
            auditLogger.logSecurityEvent(
                    "AUTHENTICATION_FAILURE",
                    "Failed authentication attempt",
                    metadata
            );
            
            throw e;
        } catch (LockedException e) {
            // Log account locked event
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("username", request.getUsername());
            
            auditLogger.logSecurityEvent(
                    "ACCOUNT_LOCKED",
                    "Authentication attempt on locked account",
                    metadata
            );
            
            throw e;
        } catch (AuthenticationException e) {
            // Log other authentication failures
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("username", request.getUsername());
            metadata.put("reason", e.getMessage());
            
            auditLogger.logSecurityEvent(
                    "AUTHENTICATION_FAILURE",
                    "Failed authentication attempt",
                    metadata
            );
            
            throw e;
        }
    }

    /**
     * Verifies an MFA code.
     *
     * @param request The MFA verification request
     * @return The authentication response
     */
    @Transactional
    public AuthenticationResponse verifyMfa(MfaVerificationRequest request) {
        // Validate MFA token
        String mfaKey = "mfa:" + request.getUsername();
        String storedToken = redisTemplate.opsForValue().get(mfaKey);
        
        if (storedToken == null || !storedToken.equals(request.getMfaToken())) {
            // Log invalid MFA token
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("username", request.getUsername());
            
            auditLogger.logSecurityEvent(
                    "MFA_INVALID_TOKEN",
                    "Invalid MFA token provided",
                    metadata
            );
            
            throw new BadCredentialsException("Invalid MFA token");
        }
        
        // Get the user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        
        // Verify MFA code
        if (!mfaService.verifyCode(user, request.getMfaCode())) {
            // Log invalid MFA code
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("username", user.getUsername());
            metadata.put("userId", user.getId());
            
            auditLogger.logSecurityEvent(
                    "MFA_VERIFICATION_FAILURE",
                    "Invalid MFA code provided",
                    metadata
            );
            
            throw new BadCredentialsException("Invalid MFA code");
        }
        
        // Delete MFA token from Redis
        redisTemplate.delete(mfaKey);
        
        // Generate tokens
        String accessToken = tokenProvider.createAccessToken(user);
        String refreshToken = tokenProvider.createRefreshToken(user);
        
        // Store refresh token in Redis
        String refreshKey = "refresh:" + user.getUsername();
        redisTemplate.opsForValue().set(refreshKey, refreshToken, refreshTokenValidity, TimeUnit.MILLISECONDS);
        
        // Log successful MFA verification
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", user.getUsername());
        metadata.put("userId", user.getId());
        
        auditLogger.logSecurityEvent(
                "MFA_VERIFICATION_SUCCESS",
                "MFA verification successful",
                metadata
        );
        
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .mfaRequired(false)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    /**
     * Refreshes an access token.
     *
     * @param request The refresh token request
     * @return The authentication response
     */
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        // Validate refresh token
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            // Log invalid refresh token
            auditLogger.logSecurityEvent(
                    "REFRESH_TOKEN_INVALID",
                    "Invalid refresh token provided",
                    null
            );
            
            throw new BadCredentialsException("Invalid refresh token");
        }
        
        // Get username from token
        String username = tokenProvider.getUsername(request.getRefreshToken());
        
        // Check if token is in Redis
        String refreshKey = "refresh:" + username;
        String storedToken = redisTemplate.opsForValue().get(refreshKey);
        
        if (storedToken == null || !storedToken.equals(request.getRefreshToken())) {
            // Log invalid refresh token
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("username", username);
            
            auditLogger.logSecurityEvent(
                    "REFRESH_TOKEN_INVALID",
                    "Refresh token not found in store or does not match",
                    metadata
            );
            
            throw new BadCredentialsException("Invalid refresh token");
        }
        
        // Get the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        
        // Generate new tokens
        String accessToken = tokenProvider.createAccessToken(user);
        String refreshToken = tokenProvider.createRefreshToken(user);
        
        // Update refresh token in Redis
        redisTemplate.opsForValue().set(refreshKey, refreshToken, refreshTokenValidity, TimeUnit.MILLISECONDS);
        
        // Log token refresh
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", user.getUsername());
        metadata.put("userId", user.getId());
        
        auditLogger.logSecurityEvent(
                "TOKEN_REFRESH_SUCCESS",
                "Access token refreshed successfully",
                metadata
        );
        
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .mfaRequired(false)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    /**
     * Logs out a user.
     *
     * @param username The username
     */
    public void logout(String username) {
        // Remove refresh token from Redis
        String refreshKey = "refresh:" + username;
        redisTemplate.delete(refreshKey);
        
        // Log logout
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);
        
        auditLogger.logSecurityEvent(
                "USER_LOGOUT",
                "User logged out successfully",
                metadata
        );
    }

    /**
     * Handles a failed login attempt.
     *
     * @param username The username
     */
    @Transactional
    private void handleFailedLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            // Increment failed attempts
            userRepository.incrementFailedAttempts(username);
            
            // Check if account should be locked
            if (user.getFailedAttempts() + 1 >= maxFailedAttempts) {
                LocalDateTime lockoutTime = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
                userRepository.lockUser(username, lockoutTime);
                
                // Log account locked event
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("username", username);
                metadata.put("userId", user.getId());
                metadata.put("lockoutTime", lockoutTime);
                metadata.put("lockoutDurationMinutes", lockoutDurationMinutes);
                
                auditLogger.logSecurityEvent(
                        "ACCOUNT_LOCKED_MAX_ATTEMPTS",
                        "Account locked due to maximum failed login attempts",
                        metadata
                );
            }
        });
    }
}

