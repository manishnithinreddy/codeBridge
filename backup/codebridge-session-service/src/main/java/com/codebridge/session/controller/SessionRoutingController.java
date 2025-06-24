package com.codebridge.session.controller;

import com.codebridge.session.routing.SessionRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;

/**
 * Controller that handles routing of session-based requests to the correct service instance.
 * This enables horizontal scaling by ensuring that requests for a specific session
 * are always routed to the instance that owns that session.
 */
@RestController
@RequestMapping("/api/router")
public class SessionRoutingController {
    private static final Logger logger = LoggerFactory.getLogger(SessionRoutingController.class);

    private final SessionRouter sessionRouter;

    @Autowired
    public SessionRoutingController(SessionRouter sessionRouter) {
        this.sessionRouter = sessionRouter;
    }

    /**
     * Routes a GET request to the correct service instance based on the session token.
     *
     * @param sessionToken The session token
     * @param path The API path (without the session token)
     * @param request The HTTP request
     * @return The response from the correct instance
     */
    @GetMapping("/ssh/{sessionToken}/**")
    public ResponseEntity<Object> routeGetRequest(
            @PathVariable String sessionToken,
            HttpServletRequest request) {
        
        String path = extractPath(request);
        HttpHeaders headers = extractHeaders(request);
        
        ResponseEntity<Object> response = sessionRouter.routeRequest(
                sessionToken,
                path,
                HttpMethod.GET,
                null,
                headers,
                Object.class);
        
        if (response == null) {
            // This instance owns the session, so forward the request to the local controller
            return ResponseEntity.ok().body(Map.of(
                "message", "This instance owns the session, request would be handled locally",
                "sessionToken", sessionToken,
                "path", path
            ));
        }
        
        return response;
    }

    /**
     * Routes a POST request to the correct service instance based on the session token.
     *
     * @param sessionToken The session token
     * @param requestBody The request body
     * @param request The HTTP request
     * @return The response from the correct instance
     */
    @PostMapping("/ssh/{sessionToken}/**")
    public ResponseEntity<Object> routePostRequest(
            @PathVariable String sessionToken,
            @RequestBody(required = false) Object requestBody,
            HttpServletRequest request) {
        
        String path = extractPath(request);
        HttpHeaders headers = extractHeaders(request);
        
        ResponseEntity<Object> response = sessionRouter.routeRequest(
                sessionToken,
                path,
                HttpMethod.POST,
                requestBody,
                headers,
                Object.class);
        
        if (response == null) {
            // This instance owns the session, so forward the request to the local controller
            return ResponseEntity.ok().body(Map.of(
                "message", "This instance owns the session, request would be handled locally",
                "sessionToken", sessionToken,
                "path", path
            ));
        }
        
        return response;
    }

    /**
     * Extracts the API path from the request.
     *
     * @param request The HTTP request
     * @return The API path
     */
    private String extractPath(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestURI.substring(contextPath.length());
        
        // Remove the /api/router prefix
        path = path.replaceFirst("/api/router", "");
        
        return path;
    }

    /**
     * Extracts the HTTP headers from the request.
     *
     * @param request The HTTP request
     * @return The HTTP headers
     */
    private HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.add(headerName, request.getHeader(headerName));
        }
        
        return headers;
    }
}

