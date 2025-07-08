package com.codebridge.security.identity.service;

import com.codebridge.security.auth.model.User;
import com.codebridge.security.auth.repository.UserRepository;
import com.codebridge.security.identity.dto.SignupRequest;
import com.codebridge.security.rbac.model.Role;
import com.codebridge.security.rbac.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Service for user registration operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user.
     *
     * @param signupRequest The signup request
     * @return The created user
     * @throws RuntimeException if username or email already exists
     */
    @Transactional
    public User registerUser(SignupRequest signupRequest) {
        log.info("Attempting to register user with username: {}", signupRequest.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            log.warn("Registration failed: Username '{}' already exists", signupRequest.getUsername());
            throw new RuntimeException("Username is already taken!");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            log.warn("Registration failed: Email '{}' already exists", signupRequest.getEmail());
            throw new RuntimeException("Email is already in use!");
        }

        // Create new user
        User user = User.builder()
                .username(signupRequest.getUsername())
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .firstName(signupRequest.getFirstName())
                .lastName(signupRequest.getLastName())
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .mfaEnabled(false)
                .failedAttempts(0)
                .lastPasswordChange(LocalDateTime.now())
                .roles(new HashSet<>())
                .passwordHistory(new java.util.ArrayList<>())
                .build();

        // Assign default roles
        Set<Role> userRoles = assignDefaultRoles(signupRequest.getRoles());
        user.setRoles(userRoles);

        // Save user
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {} and username: {}", savedUser.getId(), savedUser.getUsername());

        return savedUser;
    }

    /**
     * Assigns default roles to a new user.
     *
     * @param requestedRoles The roles requested in the signup request
     * @return The set of roles to assign
     */
    private Set<Role> assignDefaultRoles(Set<String> requestedRoles) {
        Set<Role> roles = new HashSet<>();

        // If no roles specified, assign default USER role
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            Role userRole = roleRepository.findByName("USER")
                    .orElseGet(() -> createDefaultUserRole());
            roles.add(userRole);
            log.info("Assigned default USER role");
        } else {
            // Process requested roles
            for (String roleName : requestedRoles) {
                // Only allow USER role for self-registration (security measure)
                if ("USER".equals(roleName.toUpperCase())) {
                    Role role = roleRepository.findByName("USER")
                            .orElseGet(() -> createDefaultUserRole());
                    roles.add(role);
                    log.info("Assigned requested role: {}", roleName);
                } else {
                    log.warn("Ignoring unauthorized role request: {}", roleName);
                }
            }

            // If no valid roles were assigned, assign default USER role
            if (roles.isEmpty()) {
                Role userRole = roleRepository.findByName("USER")
                        .orElseGet(() -> createDefaultUserRole());
                roles.add(userRole);
                log.info("No valid roles found, assigned default USER role");
            }
        }

        return roles;
    }

    /**
     * Creates a default USER role if it doesn't exist.
     *
     * @return The USER role
     */
    private Role createDefaultUserRole() {
        log.info("Creating default USER role");
        Role userRole = Role.builder()
                .name("USER")
                .description("Default user role with basic permissions")
                .permissions(new HashSet<>())
                .children(new HashSet<>())
                .build();
        return roleRepository.save(userRole);
    }

    /**
     * Validates if a username is available.
     *
     * @param username The username to check
     * @return true if available, false otherwise
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Validates if an email is available.
     *
     * @param email The email to check
     * @return true if available, false otherwise
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
}
