package com.codebridge.scalability.loadbalancer.impl;

import com.codebridge.scalability.loadbalancer.ServiceInstanceSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Optional;

/**
 * Service instance selector that implements sticky sessions.
 * Ensures that requests from the same session are routed to the same instance.
 */
@RequiredArgsConstructor
public class StickySessionServiceInstanceSelector implements ServiceInstanceSelector {

    private static final String SESSION_ATTRIBUTE_PREFIX = "sticky-session-instance-";
    
    private final ServiceInstanceSelector delegate;

    @Override
    public Optional<ServiceInstance> selectInstance(String serviceId, String requestId) {
        // Try to get the instance from the session
        Optional<String> instanceIdFromSession = getInstanceIdFromSession(serviceId);
        
        if (instanceIdFromSession.isPresent()) {
            String instanceId = instanceIdFromSession.get();
            
            // Check if the instance is still healthy
            Optional<ServiceInstance> instance = delegate.selectInstance(serviceId, instanceId);
            
            if (instance.isPresent()) {
                return instance;
            }
        }
        
        // If no instance in session or the instance is unhealthy, select a new one
        Optional<ServiceInstance> instance = delegate.selectInstance(serviceId, requestId);
        
        // Store the selected instance in the session
        instance.ifPresent(i -> storeInstanceInSession(serviceId, i.getInstanceId()));
        
        return instance;
    }

    @Override
    public void markInstanceUnhealthy(String serviceId, String instanceId) {
        delegate.markInstanceUnhealthy(serviceId, instanceId);
        
        // Remove the instance from the session if it's unhealthy
        removeInstanceFromSession(serviceId);
    }

    @Override
    public void markInstanceHealthy(String serviceId, String instanceId) {
        delegate.markInstanceHealthy(serviceId, instanceId);
    }
    
    private Optional<String> getInstanceIdFromSession(String serviceId) {
        HttpServletRequest request = getCurrentRequest();
        
        if (request != null) {
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                String attributeName = getSessionAttributeName(serviceId);
                String instanceId = (String) session.getAttribute(attributeName);
                
                if (instanceId != null && !instanceId.isEmpty()) {
                    return Optional.of(instanceId);
                }
            }
        }
        
        return Optional.empty();
    }
    
    private void storeInstanceInSession(String serviceId, String instanceId) {
        HttpServletRequest request = getCurrentRequest();
        
        if (request != null) {
            HttpSession session = request.getSession(true);
            String attributeName = getSessionAttributeName(serviceId);
            session.setAttribute(attributeName, instanceId);
        }
    }
    
    private void removeInstanceFromSession(String serviceId) {
        HttpServletRequest request = getCurrentRequest();
        
        if (request != null) {
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                String attributeName = getSessionAttributeName(serviceId);
                session.removeAttribute(attributeName);
            }
        }
    }
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            return attributes.getRequest();
        }
        
        return null;
    }
    
    private String getSessionAttributeName(String serviceId) {
        return SESSION_ATTRIBUTE_PREFIX + serviceId;
    }
}

