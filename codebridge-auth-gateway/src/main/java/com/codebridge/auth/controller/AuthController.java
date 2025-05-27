package com.codebridge.auth.controller;

import com.codebridge.auth.dto.JwtAuthenticationResponse;
import com.codebridge.auth.dto.LoginRequest;
import com.codebridge.auth.dto.LogoutRequest;
import com.codebridge.auth.dto.RefreshTokenRequest;
import com.codebridge.auth.dto.SignUpRequest;
import com.codebridge.auth.exception.AuthenticationException;
import com.codebridge.auth.security.JwtTokenProvider;
import com.codebridge.auth.security.UserPrincipal;
import com.codebridge.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuthService authService;

    public AuthController(ReactiveAuthenticationManager authenticationManager, 
                          JwtTokenProvider tokenProvider,
                          AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.authService = authService;
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param loginRequest the login request
     * @return the JWT authentication response
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<JwtAuthenticationResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        ).map(authentication -> {
            String accessToken = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);
            
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            List<String> roles = userPrincipal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(new JwtAuthenticationResponse(
                    accessToken,
                    refreshToken,
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    roles
            ));
        }).switchIfEmpty(Mono.error(new AuthenticationException("Invalid username or password")));
    }

    /**
     * Registers a new user.
     *
     * @param signUpRequest the sign-up request
     * @return the response entity
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<?>> register(@Valid @RequestBody SignUpRequest signUpRequest) {
        return authService.registerUser(signUpRequest)
                .map(user -> ResponseEntity.ok().body("User registered successfully"));
    }

    /**
     * Refreshes a JWT token.
     *
     * @param refreshTokenRequest the refresh token request
     * @return the JWT authentication response
     */
    @PostMapping("/refresh-token")
    public Mono<ResponseEntity<JwtAuthenticationResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        return authService.refreshToken(refreshTokenRequest.getRefreshToken())
                .map(authentication -> {
                    String accessToken = tokenProvider.generateToken(authentication);
                    String refreshToken = tokenProvider.generateRefreshToken(authentication);
                    
                    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                    List<String> roles = userPrincipal.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList());
                    
                    return ResponseEntity.ok(new JwtAuthenticationResponse(
                            accessToken,
                            refreshToken,
                            userPrincipal.getId(),
                            userPrincipal.getUsername(),
                            roles
                    ));
                });
    }

    /**
     * Logs out a user by revoking their refresh token.
     *
     * @param logoutRequest the logout request
     * @return the response entity
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<?>> logout(@Valid @RequestBody LogoutRequest logoutRequest) {
        return authService.logout(logoutRequest.getRefreshToken())
                .map(success -> {
                    if (success) {
                        return ResponseEntity.ok().body("Logged out successfully");
                    } else {
                        return ResponseEntity.badRequest().body("Invalid refresh token");
                    }
                });
    }

    /**
     * Logs out a user from all devices by revoking all their refresh tokens.
     *
     * @param userPrincipal the authenticated user
     * @return the response entity
     */
    @PostMapping("/logout-all")
    public Mono<ResponseEntity<?>> logoutFromAllDevices(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return authService.logoutFromAllDevices(UUID.fromString(userPrincipal.getId()))
                .map(count -> ResponseEntity.ok().body("Logged out from " + count + " devices"));
    }
}
