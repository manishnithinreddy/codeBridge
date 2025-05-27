package com.codebridge.security.service;

import com.codebridge.core.audit.AuditEventPublisher;
import com.codebridge.security.dto.SignUpRequest;
import com.codebridge.security.exception.AuthenticationException;
import com.codebridge.security.exception.ResourceNotFoundException;
import com.codebridge.security.model.RefreshToken;
import com.codebridge.security.model.Role;
import com.codebridge.security.model.User;
import com.codebridge.security.model.UserRole;
import com.codebridge.security.model.UserSession;
import com.codebridge.security.repository.RefreshTokenRepository;
import com.codebridge.security.repository.RoleRepository;
import com.codebridge.security.repository.UserRepository;
import com.codebridge.security.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for authentication operations.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final AuthenticationManager authenticationManager;
    private final AuditEventPublisher auditEventPublisher;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       RefreshTokenService refreshTokenService,
                       SessionService sessionService,
                       AuthenticationManager authenticationManager,
                       AuditEventPublisher auditEventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.sessionService = sessionService;
        this.authenticationManager = authenticationManager;
        this.auditEventPublisher = auditEventPublisher;
    }

    /**
     * Authenticates a user.
     *
     * @param username the username
     * @param password the password
     * @return the authentication object
     */
    public Mono<Authentication> authenticate(String username, String password) {
        return Mono.fromCallable(() -> {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            
            // Audit the login
            User user = userRepository.findByUsernameAndActiveTrue(username)
                    .orElseThrow(() -> new AuthenticationException("User not found with username: " + username));
            
            Map<String, Object> metadata = Map.of(
                    "userId", user.getId().toString(),
                    "username", user.getUsername()
            );
            
            auditEventPublisher.publishAuditEvent(
                    "USER_LOGIN",
                    "/api/auth/login",
                    "POST",
                    user.getId(),
                    user.getTeamId(),
                    "SUCCESS",
                    null,
                    null,
                    metadata
            );
            
            return authentication;
        });
    }

    /**
     * Registers a new user.
     *
     * @param signUpRequest the sign-up request
     * @return the created user
     */
    @Transactional
    public Mono<User> registerUser(SignUpRequest signUpRequest) {
        return Mono.fromCallable(() -> {
            // Check if username is already taken
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                throw new AuthenticationException("Username is already taken");
            }
            
            // Check if email is already taken
            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                throw new AuthenticationException("Email is already taken");
            }
            
            // Create new user
            User user = new User();
            user.setId(UUID.randomUUID());
            user.setName(signUpRequest.getName());
            user.setUsername(signUpRequest.getUsername());
            user.setEmail(signUpRequest.getEmail());
            user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
            user.setActive(true);
            
            User savedUser = userRepository.save(user);
            
            // Assign default role
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "USER"));
            
            UserRole userRoleEntity = new UserRole();
            userRoleEntity.setId(UUID.randomUUID());
            userRoleEntity.setUserId(savedUser.getId());
            userRoleEntity.setRoleId(userRole.getId());
            
            userRoleRepository.save(userRoleEntity);
            
            // Audit the registration
            Map<String, Object> metadata = Map.of(
                    "userId", savedUser.getId().toString(),
                    "username", savedUser.getUsername(),
                    "email", savedUser.getEmail()
            );
            
            auditEventPublisher.publishAuditEvent(
                    "USER_REGISTERED",
                    "/api/auth/signup",
                    "POST",
                    savedUser.getId(),
                    null,
                    "SUCCESS",
                    null,
                    null,
                    metadata
            );
            
            logger.info("Registered new user: {}", savedUser.getUsername());
            
            return savedUser;
        });
    }

    /**
     * Refreshes an authentication token.
     *
     * @param refreshToken the refresh token
     * @return the authentication object
     */
    public Mono<Authentication> refreshToken(String refreshToken) {
        return Mono.fromCallable(() -> {
            if (!tokenProvider.validateToken(refreshToken)) {
                throw new AuthenticationException("Invalid refresh token");
            }
            
            Authentication authentication = tokenProvider.getAuthentication(refreshToken);
            
            // Verify the refresh token in the database
            RefreshToken token = refreshTokenService.findByToken(refreshToken)
                    .orElseThrow(() -> new AuthenticationException("Refresh token not found"));
            
            if (token.isRevoked()) {
                throw new AuthenticationException("Refresh token has been revoked");
            }
            
            if (token.getExpiryDate().isBefore(Instant.now())) {
                throw new AuthenticationException("Refresh token has expired");
            }
            
            // Update the session's last activity
            UserSession session = sessionService.getSessionByRefreshToken(refreshToken).block();
            if (session != null) {
                sessionService.updateLastActivity(session.getSessionToken()).block();
            }
            
            return authentication;
        });
    }

    /**
     * Logs out a user by revoking their refresh token.
     *
     * @param refreshToken the refresh token
     * @return true if the token was revoked, false otherwise
     */
    @Transactional
    public Mono<Boolean> logout(String refreshToken) {
        return Mono.fromCallable(() -> {
            boolean result = refreshTokenService.revokeRefreshToken(refreshToken);
            
            if (result) {
                // Deactivate the session
                UserSession session = sessionService.getSessionByRefreshToken(refreshToken).block();
                if (session != null) {
                    sessionService.deactivateSession(session.getSessionToken(), session.getUserId()).block();
                }
                
                // Audit the logout
                RefreshToken token = refreshTokenService.findByToken(refreshToken)
                        .orElse(null);
                
                if (token != null) {
                    Map<String, Object> metadata = Map.of(
                            "userId", token.getUserId().toString()
                    );
                    
                    auditEventPublisher.publishAuditEvent(
                            "USER_LOGOUT",
                            "/api/auth/logout",
                            "POST",
                            token.getUserId(),
                            null,
                            "SUCCESS",
                            null,
                            null,
                            metadata
                    );
                    
                    logger.info("User logged out: {}", token.getUserId());
                }
            }
            
            return result;
        });
    }

    /**
     * Logs out a user from all devices by revoking all their refresh tokens.
     *
     * @param userId the user ID
     * @return the number of revoked tokens
     */
    @Transactional
    public Mono<Integer> logoutFromAllDevices(UUID userId) {
        return Mono.fromCallable(() -> {
            int result = refreshTokenService.revokeAllUserTokens(userId);
            
            if (result > 0) {
                // Deactivate all sessions
                sessionService.deactivateAllUserSessions(userId).block();
                
                // Audit the logout
                Map<String, Object> metadata = Map.of(
                        "userId", userId.toString(),
                        "tokenCount", result
                );
                
                auditEventPublisher.publishAuditEvent(
                        "USER_LOGOUT_ALL_DEVICES",
                        "/api/auth/logout-all",
                        "POST",
                        userId,
                        null,
                        "SUCCESS",
                        null,
                        null,
                        metadata
                );
                
                logger.info("User logged out from all devices: {}", userId);
            }
            
            return result;
        });
    }

    /**
     * Gets the current authenticated user.
     *
     * @return the current user
     */
    public Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication().getPrincipal())
                .cast(com.codebridge.core.security.UserPrincipal.class)
                .flatMap(userPrincipal -> Mono.fromCallable(() -> 
                        userRepository.findByIdAndActiveTrue(userPrincipal.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()))
                ));
    }

    /**
     * Gets the current user's roles.
     *
     * @return list of role names
     */
    public Mono<List<String>> getCurrentUserRoles() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication().getAuthorities())
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(authority -> authority.replace("ROLE_", ""))
                        .collect(Collectors.toList()));
    }
}

