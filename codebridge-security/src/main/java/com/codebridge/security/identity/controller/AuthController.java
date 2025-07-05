package com.codebridge.security.identity.controller;

import com.codebridge.security.identity.dto.JwtResponse;
import com.codebridge.security.identity.dto.LoginRequest;
import com.codebridge.security.identity.dto.MessageResponse;
import com.codebridge.security.identity.dto.SignupRequest;
import com.codebridge.security.auth.service.AuthenticationService;
import com.codebridge.security.auth.dto.AuthenticationRequest;
import com.codebridge.security.auth.dto.AuthenticationResponse;
import com.codebridge.security.auth.dto.RefreshTokenRequest;
import com.codebridge.security.auth.repository.UserRepository;
import com.codebridge.security.auth.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling authentication operations.
 * Provides endpoints for user login, signup, token refresh, and logout.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param loginRequest The login credentials
     * @return JWT token response
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // Convert LoginRequest to AuthenticationRequest
        AuthenticationRequest authRequest = AuthenticationRequest.builder()
                .username(loginRequest.getUsername())
                .password(loginRequest.getPassword())
                .build();
        
        // Call the service and convert response
        AuthenticationResponse authResponse = authenticationService.authenticate(authRequest);
        
        // Convert AuthenticationResponse to JwtResponse
        JwtResponse jwtResponse = JwtResponse.builder()
                .token(authResponse.getAccessToken())
                .refreshToken(authResponse.getRefreshToken())
                .type("Bearer")
                .username(authResponse.getUsername())
                .build();
        
        return ResponseEntity.ok(jwtResponse);
    }

    /**
     * Registers a new user account.
     *
     * @param signupRequest The signup information
     * @return Success message
     */
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        // TODO: Implement user registration
        // For now, return a placeholder response
        MessageResponse response = MessageResponse.builder()
                .message("User registration not yet implemented")
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes an expired JWT token.
     *
     * @param refreshToken The refresh token
     * @return New JWT token response
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(@RequestParam String refreshToken) {
        // Convert String to RefreshTokenRequest
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();
        
        // Call the service and convert response
        AuthenticationResponse authResponse = authenticationService.refreshToken(refreshRequest);
        
        // Convert AuthenticationResponse to JwtResponse
        JwtResponse jwtResponse = JwtResponse.builder()
                .token(authResponse.getAccessToken())
                .refreshToken(authResponse.getRefreshToken())
                .type("Bearer")
                .username(authResponse.getUsername())
                .build();
        
        return ResponseEntity.ok(jwtResponse);
    }

    /**
     * Logs out a user by invalidating their tokens.
     *
     * @param userId The user ID
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logoutUser(@RequestParam Long userId) {
        // Find user by ID to get username
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Call logout with username
        authenticationService.logout(user.getUsername());
        
        // Return success message
        MessageResponse response = MessageResponse.builder()
                .message("User logged out successfully")
                .build();
        
        return ResponseEntity.ok(response);
    }
}
