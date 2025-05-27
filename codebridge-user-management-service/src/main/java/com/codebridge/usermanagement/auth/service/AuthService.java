package com.codebridge.usermanagement.auth.service;

import com.codebridge.usermanagement.auth.model.Role;
import com.codebridge.usermanagement.auth.model.User;
import com.codebridge.usermanagement.auth.repository.RoleRepository;
import com.codebridge.usermanagement.auth.repository.UserRepository;
import com.codebridge.usermanagement.common.exception.ResourceNotFoundException;
import com.codebridge.usermanagement.common.util.JwtUtils;
import com.codebridge.usermanagement.session.model.UserSession;
import com.codebridge.usermanagement.session.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for authentication operations.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Autowired
    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserSessionService sessionService,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Register a new user.
     *
     * @param email The email
     * @param password The password
     * @return The user
     */
    @Transactional
    public User registerUser(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        // Assign default role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));
        user.addRole(userRole);

        return userRepository.save(user);
    }

    /**
     * Authenticate a user.
     *
     * @param email The email
     * @param password The password
     * @param ipAddress The IP address
     * @param userAgent The user agent
     * @return The session
     */
    @Transactional
    public UserSession authenticateUser(String email, String password, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        if (!user.isAccountNonLocked()) {
            throw new BadCredentialsException("Account is locked");
        }

        if (!user.isAccountNonExpired()) {
            throw new BadCredentialsException("Account is expired");
        }

        if (!user.isCredentialsNonExpired()) {
            throw new BadCredentialsException("Credentials are expired");
        }

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT token
        String token = jwtUtils.generateToken(user.getId());

        // Create session
        return sessionService.createSession(user.getId(), token, ipAddress, userAgent);
    }

    /**
     * Logout a user.
     *
     * @param token The token
     * @return True if successful, false otherwise
     */
    @Transactional
    public boolean logoutUser(String token) {
        Optional<UserSession> session = sessionService.findByToken(token);
        if (session.isPresent()) {
            sessionService.deactivateSession(session.get().getId());
            return true;
        }
        return false;
    }

    /**
     * Logout all sessions for a user.
     *
     * @param userId The user ID
     * @return The number of sessions logged out
     */
    @Transactional
    public int logoutAllUserSessions(UUID userId) {
        return sessionService.deactivateAllUserSessions(userId);
    }

    /**
     * Validate a token.
     *
     * @param token The token
     * @return The user ID if valid, empty otherwise
     */
    public Optional<UUID> validateToken(String token) {
        Optional<UserSession> session = sessionService.findByToken(token);
        if (session.isPresent() && session.get().isActive() && !session.get().isExpired()) {
            UserSession userSession = session.get();
            
            // Update last accessed time
            userSession.updateLastAccessed();
            sessionService.updateSession(userSession);
            
            return Optional.of(userSession.getUserId());
        }
        return Optional.empty();
    }
}

