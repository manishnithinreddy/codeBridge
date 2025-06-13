package com.codebridge.monitoring.scalability.filter;

import com.codebridge.monitoring.scalability.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Filter for handling idempotent requests.
 * Ensures that requests with the same idempotency key are only processed once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Set<String> IDEMPOTENT_METHODS = new HashSet<>(Arrays.asList("POST", "PUT", "PATCH", "DELETE"));

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Value("${codebridge.scalability.idempotency.enabled}")
    private boolean enabled;

    @Value("${codebridge.scalability.idempotency.header-name}")
    private String headerName;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        if (!enabled || !IDEMPOTENT_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(headerName);
        
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if we've seen this key before
        if (idempotencyService.exists(idempotencyKey)) {
            Optional<IdempotencyResponse> cachedResponse = idempotencyService.getResult(
                    idempotencyKey, IdempotencyResponse.class);
            
            if (cachedResponse.isPresent()) {
                IdempotencyResponse storedResponse = cachedResponse.get();
                response.setStatus(storedResponse.getStatus());
                
                for (String headerName : storedResponse.getHeaders().keySet()) {
                    response.setHeader(headerName, storedResponse.getHeaders().get(headerName));
                }
                
                response.getWriter().write(storedResponse.getBody());
                return;
            }
        }

        // Wrap the response to capture the output
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        try {
            filterChain.doFilter(request, responseWrapper);
            
            // Store the response for future requests with the same idempotency key
            byte[] responseContent = responseWrapper.getContentAsByteArray();
            String responseBody = new String(responseContent, responseWrapper.getCharacterEncoding());
            
            IdempotencyResponse idempotencyResponse = new IdempotencyResponse();
            idempotencyResponse.setStatus(responseWrapper.getStatus());
            idempotencyResponse.setBody(responseBody);
            
            // Only store successful responses
            if (responseWrapper.getStatus() < HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                idempotencyService.recordKey(idempotencyKey, idempotencyResponse);
            }
            
            // Copy content to the original response
            responseWrapper.copyBodyToResponse();
        } catch (Exception e) {
            log.error("Error processing request with idempotency key: {}", idempotencyKey, e);
            throw e;
        }
    }
}

