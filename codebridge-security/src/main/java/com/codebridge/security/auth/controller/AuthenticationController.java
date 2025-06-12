package com.codebridge.security.auth.controller;

import com.codebridge.security.auth.dto.AuthenticationRequest;
import com.codebridge.security.auth.dto.AuthenticationResponse;
import com.codebridge.security.auth.dto.MfaVerificationRequest;
import com.codebridge.security.auth.dto.RefreshTokenRequest;
import com.codebridge.security.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * Authenticates a user.
     *
     * @param request The authentication request
     * @return The authentication response
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    /**
     * Verifies an MFA code.
     *
     * @param request The MFA verification request
     * @return The authentication response
     */
    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthenticationResponse> verifyMfa(@Valid @RequestBody MfaVerificationRequest request) {
        return ResponseEntity.ok(authenticationService.verifyMfa(request));
    }

    /**
     * Refreshes an access token.
     *
     * @param request The refresh token request
     * @return The authentication response
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refreshToken(request));
    }

    /**
     * Logs out a user.
     *
     * @return The response entity
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            authenticationService.logout(authentication.getName());
        }
        return ResponseEntity.ok().build();
    }
}

