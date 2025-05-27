package com.codebridge.security.controller;

import com.codebridge.core.security.UserPrincipal;
import com.codebridge.security.dto.JwtAuthenticationResponse;
import com.codebridge.security.dto.LoginRequest;
import com.codebridge.security.dto.LogoutRequest;
import com.codebridge.security.dto.RefreshTokenRequest;
import com.codebridge.security.dto.SignUpRequest;
import com.codebridge.security.exception.AuthenticationException;
import com.codebridge.security.model.RefreshToken;
import com.codebridge.security.model.User;
import com.codebridge.security.model.UserSession;
import com.codebridge.security.service.AuthService;
import com.codebridge.security.service.JwtTokenProvider;
import com.codebridge.security.service.RefreshTokenService;
import com.codebridge.security.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
 * Controller for authentication operations.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;

    public AuthController(AuthService authService,
                          JwtTokenProvider tokenProvider,
                          RefreshTokenService refreshTokenService,
                          SessionService sessionService) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.sessionService = sessionService;
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param loginRequest the login request
     * @param request the HTTP request
     * @return the JWT authentication response
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<JwtAuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        
        return authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword())
                .flatMap(authentication -> {
                    String accessToken = tokenProvider.generateToken(authentication);
                    
                    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                    UUID userId = UUID.fromString(userPrincipal.getId());
                    
                    // Create refresh token
                    RefreshToken refreshToken = refreshTokenService.createRefreshToken(userId);
                    
                    // Create session
                    return sessionService.createSession(
                            userId,
                            request.getRemoteAddr(),
                            request.getHeader("User-Agent"),
                            loginRequest.getDeviceInfo(),
                            loginRequest.getGeoLocation(),
                            refreshToken.getToken()
                    ).map(session -> {
                        List<String> roles = authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toList());
                        
                        return ResponseEntity.ok(new JwtAuthenticationResponse(
                                accessToken,
                                refreshToken.getToken(),
                                userPrincipal.getId(),
                                userPrincipal.getUsername(),
                                roles,
                                session.getSessionToken()
                        ));
                    });
                });
    }

    /**
     * Registers a new user.
     *
     * @param signUpRequest the sign-up request
     * @return the response entity
     */
    @PostMapping("/signup")
    public Mono<ResponseEntity<?>> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        return authService.registerUser(signUpRequest)
                .map(user -> ResponseEntity.ok().body("User registered successfully"));
    }

    /**
     * Refreshes an authentication token.
     *
     * @param refreshTokenRequest the refresh token request
     * @return the JWT authentication response
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<JwtAuthenticationResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        
        return authService.refreshToken(refreshTokenRequest.getRefreshToken())
                .flatMap(authentication -> {
                    String accessToken = tokenProvider.generateToken(authentication);
                    
                    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                    
                    return sessionService.getSessionByRefreshToken(refreshTokenRequest.getRefreshToken())
                            .map(session -> {
                                List<String> roles = authentication.getAuthorities().stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .collect(Collectors.toList());
                                
                                return ResponseEntity.ok(new JwtAuthenticationResponse(
                                        accessToken,
                                        refreshTokenRequest.getRefreshToken(),
                                        userPrincipal.getId(),
                                        userPrincipal.getUsername(),
                                        roles,
                                        session.getSessionToken()
                                ));
                            });
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

