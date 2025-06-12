package com.codebridge.common.context;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter for propagating shared contexts between services.
 * Extracts the context ID from the request header and makes it available to the current thread.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SharedContextPropagationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SharedContextPropagationFilter.class);
    private static final String CONTEXT_ID_HEADER = "X-Shared-Context-ID";
    private static final ThreadLocal<String> currentContextId = new ThreadLocal<>();

    private final SharedContextManager contextManager;

    @Autowired
    public SharedContextPropagationFilter(SharedContextManager contextManager) {
        this.contextManager = contextManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Extract context ID from request header
            String contextId = httpRequest.getHeader(CONTEXT_ID_HEADER);
            if (contextId != null && !contextId.isEmpty()) {
                // Set the context ID for the current thread
                currentContextId.set(contextId);
                logger.debug("Propagated shared context: {}", contextId);

                // Get the context and check if it exists
                SharedContext context = contextManager.getContext(contextId);
                if (context == null) {
                    logger.warn("Shared context not found or expired: {}", contextId);
                }
            }

            // Continue the filter chain
            chain.doFilter(request, response);
        } finally {
            // Always clean up the thread local
            currentContextId.remove();
        }
    }

    /**
     * Gets the current context ID for the current thread.
     *
     * @return The current context ID, or null if not set
     */
    public static String getCurrentContextId() {
        return currentContextId.get();
    }

    /**
     * Sets the current context ID for the current thread.
     *
     * @param contextId The context ID
     */
    public static void setCurrentContextId(String contextId) {
        currentContextId.set(contextId);
    }
}

