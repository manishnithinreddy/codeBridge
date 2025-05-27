package com.codebridge.usermanagement.auth.controller;

import com.codebridge.usermanagement.auth.model.User;
import com.codebridge.usermanagement.auth.service.AuthService;
import com.codebridge.usermanagement.session.model.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for authentication operations.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     *
     * @param request The registration request
     * @return The response
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationRequest request) {
        try {
            User user = authService.registerUser(request.getEmail(), request.getPassword());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Login a user.
     *
     * @param request The login request
     * @param httpRequest The HTTP request
     * @return The response
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            
            UserSession session = authService.authenticateUser(
                    request.getEmail(),
                    request.getPassword(),
                    ipAddress,
                    userAgent);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", session.getToken());
            response.put("expiresAt", session.getExpiresAt());
            
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * Logout a user.
     *
     * @param request The logout request
     * @return The response
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@Valid @RequestBody LogoutRequest request) {
        boolean success = authService.logoutUser(request.getToken());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Validate a token.
     *
     * @param request The validation request
     * @return The response
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@Valid @RequestBody TokenValidationRequest request) {
        boolean valid = authService.validateToken(request.getToken()).isPresent();
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Registration request.
     */
    public static class RegistrationRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Size(min = 8, max = 100)
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Login request.
     */
    public static class LoginRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Logout request.
     */
    public static class LogoutRequest {
        @NotBlank
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    /**
     * Token validation request.
     */
    public static class TokenValidationRequest {
        @NotBlank
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}

