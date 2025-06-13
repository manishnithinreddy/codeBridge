package com.codebridge.identity.platform.controller;

import com.codebridge.identity.platform.dto.JwtResponse;
import com.codebridge.identity.platform.dto.LoginRequest;
import com.codebridge.identity.platform.dto.MessageResponse;
import com.codebridge.identity.platform.dto.SignupRequest;
import com.codebridge.identity.platform.service.AuthService;
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

    private final AuthService authService;

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param loginRequest The login credentials
     * @return JWT token response
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.authenticateUser(loginRequest));
    }

    /**
     * Registers a new user account.
     *
     * @param signupRequest The signup information
     * @return Success message
     */
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        return ResponseEntity.ok(authService.registerUser(signupRequest));
    }

    /**
     * Refreshes an expired JWT token.
     *
     * @param refreshToken The refresh token
     * @return New JWT token response
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    /**
     * Logs out a user by invalidating their tokens.
     *
     * @param userId The user ID
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logoutUser(@RequestParam Long userId) {
        return ResponseEntity.ok(authService.logoutUser(userId));
    }
}

