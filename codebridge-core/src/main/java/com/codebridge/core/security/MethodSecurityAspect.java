package com.codebridge.core.security;

import com.codebridge.core.audit.AuditEventPublisher;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aspect for method-level security.
 * Enforces role-based access control on methods annotated with @SecuredMethod.
 */
@Aspect
@Component
public class MethodSecurityAspect {

    private static final Logger logger = LoggerFactory.getLogger(MethodSecurityAspect.class);

    private final AuditEventPublisher auditEventPublisher;
    private final String serviceName;

    public MethodSecurityAspect(AuditEventPublisher auditEventPublisher, String serviceName) {
        this.auditEventPublisher = auditEventPublisher;
        this.serviceName = serviceName;
    }

    /**
     * Enforces security on methods annotated with @SecuredMethod.
     *
     * @param joinPoint the join point
     * @return the result of the method execution
     * @throws Throwable if an error occurs
     */
    @Around("@annotation(com.codebridge.core.security.SecuredMethod)")
    public Object enforceMethodSecurity(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        SecuredMethod securedMethod = method.getAnnotation(SecuredMethod.class);
        String[] requiredRoles = securedMethod.roles();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            logSecurityViolation(method, null, "No authentication found");
            throw new AccessDeniedException("Authentication required");
        }
        
        if (requiredRoles.length > 0) {
            boolean hasRequiredRole = false;
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            
            for (String requiredRole : requiredRoles) {
                for (GrantedAuthority authority : authorities) {
                    if (authority.getAuthority().equals(requiredRole)) {
                        hasRequiredRole = true;
                        break;
                    }
                }
                if (hasRequiredRole) {
                    break;
                }
            }
            
            if (!hasRequiredRole) {
                logSecurityViolation(method, authentication, "Missing required role");
                throw new AccessDeniedException("Access denied: missing required role");
            }
        }
        
        // Log method access
        logMethodAccess(method, authentication);
        
        return joinPoint.proceed();
    }

    /**
     * Logs a security violation.
     *
     * @param method the method
     * @param authentication the authentication
     * @param reason the reason for the violation
     */
    private void logSecurityViolation(Method method, Authentication authentication, String reason) {
        logger.warn("Security violation: {} on method {}.{} - {}",
                authentication != null ? authentication.getName() : "anonymous",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                reason);
        
        if (auditEventPublisher != null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("method", method.getDeclaringClass().getName() + "." + method.getName());
            metadata.put("reason", reason);
            
            UUID userId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                userId = UUID.fromString(userPrincipal.getId());
            }
            
            auditEventPublisher.publishErrorEvent(
                    "SECURITY_VIOLATION",
                    method.getDeclaringClass().getName() + "." + method.getName(),
                    "METHOD",
                    userId,
                    null,
                    reason,
                    null,
                    metadata
            );
        }
    }

    /**
     * Logs a method access.
     *
     * @param method the method
     * @param authentication the authentication
     */
    private void logMethodAccess(Method method, Authentication authentication) {
        if (logger.isDebugEnabled()) {
            logger.debug("Method access: {} on method {}.{}",
                    authentication.getName(),
                    method.getDeclaringClass().getSimpleName(),
                    method.getName());
        }
        
        if (auditEventPublisher != null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("method", method.getDeclaringClass().getName() + "." + method.getName());
            metadata.put("roles", Arrays.toString(method.getAnnotation(SecuredMethod.class).roles()));
            
            UUID userId = null;
            UUID teamId = null;
            if (authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                userId = UUID.fromString(userPrincipal.getId());
                if (userPrincipal.getTeamId() != null) {
                    teamId = UUID.fromString(userPrincipal.getTeamId());
                }
            }
            
            auditEventPublisher.publishAuditEvent(
                    "METHOD_ACCESS",
                    method.getDeclaringClass().getName() + "." + method.getName(),
                    "METHOD",
                    userId,
                    teamId,
                    "SUCCESS",
                    null,
                    null,
                    metadata
            );
        }
    }
}

