package com.codebridge.auth.service;

import com.codebridge.auth.dto.SignUpRequest;
import com.codebridge.auth.exception.AuthenticationException;
import com.codebridge.auth.exception.ResourceNotFoundException;
import com.codebridge.auth.model.Role;
import com.codebridge.auth.model.User;
import com.codebridge.auth.model.UserRole;
import com.codebridge.auth.repository.RoleRepository;
import com.codebridge.auth.repository.UserRepository;
import com.codebridge.auth.repository.UserRoleRepository;
import com.codebridge.auth.security.JwtTokenProvider;
import com.codebridge.auth.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for authentication operations.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Registers a new user.
     *
     * @param signUpRequest the sign-up request
     * @return the created user
     */
    @Transactional
    public Mono<User> registerUser(SignUpRequest signUpRequest) {
        // Check if username is already taken
        return Mono.fromCallable(() -> userRepository.existsByUsername(signUpRequest.getUsername()))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new AuthenticationException("Username is already taken"));
                    }
                    
                    // Check if email is already in use
                    return Mono.fromCallable(() -> userRepository.existsByEmail(signUpRequest.getEmail()));
                })
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new AuthenticationException("Email is already in use"));
                    }
                    
                    // Create new user
                    User user = new User();
                    user.setId(UUID.randomUUID());
                    user.setName(signUpRequest.getName());
                    user.setUsername(signUpRequest.getUsername());
                    user.setEmail(signUpRequest.getEmail());
                    user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
                    
                    if (signUpRequest.getTeamId() != null) {
                        user.setTeamId(UUID.fromString(signUpRequest.getTeamId()));
                    }
                    
                    return Mono.fromCallable(() -> userRepository.save(user));
                })
                .flatMap(user -> {
                    // Assign default role (USER)
                    return Mono.fromCallable(() -> roleRepository.findByName("ROLE_USER")
                            .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_USER")))
                            .flatMap(role -> {
                                UserRole userRole = new UserRole();
                                userRole.setId(UUID.randomUUID());
                                userRole.setUserId(user.getId());
                                userRole.setRoleId(role.getId());
                                
                                return Mono.fromCallable(() -> {
                                    userRoleRepository.save(userRole);
                                    return user;
                                });
                            });
                });
    }

    /**
     * Refreshes a JWT token.
     *
     * @param refreshToken the refresh token
     * @return the authentication object
     */
    public Mono<Authentication> refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            return Mono.error(new AuthenticationException("Invalid refresh token"));
        }
        
        Authentication authentication = tokenProvider.getAuthentication(refreshToken);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        return Mono.fromCallable(() -> userRepository.findById(UUID.fromString(userPrincipal.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId())))
                .flatMap(user -> {
                    return Mono.fromCallable(() -> userRoleRepository.findByUserId(user.getId()))
                            .flatMap(userRoles -> {
                                List<UUID> roleIds = userRoles.stream()
                                        .map(UserRole::getRoleId)
                                        .collect(Collectors.toList());
                                
                                return Mono.fromCallable(() -> roleRepository.findAllById(roleIds))
                                        .map(roles -> {
                                            List<GrantedAuthority> authorities = roles.stream()
                                                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                                                    .collect(Collectors.toList());
                                            
                                            UserPrincipal principal = new UserPrincipal(
                                                    user.getId().toString(),
                                                    user.getUsername(),
                                                    user.getPassword(),
                                                    authorities
                                            );
                                            
                                            if (user.getTeamId() != null) {
                                                principal.setTeamId(user.getTeamId().toString());
                                            }
                                            
                                            return new UsernamePasswordAuthenticationToken(
                                                    principal,
                                                    null,
                                                    authorities
                                            );
                                        });
                            });
                });
    }
}

