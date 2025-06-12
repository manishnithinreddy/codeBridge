package com.codebridge.common.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registry for integration hooks.
 * Provides methods for registering, retrieving, and executing integration hooks.
 */
@Component
public class IntegrationHookRegistry {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationHookRegistry.class);
    private final Map<String, IntegrationHook> hooks = new HashMap<>();

    /**
     * Creates a new integration hook registry.
     *
     * @param hooks The integration hooks to register
     */
    @Autowired
    public IntegrationHookRegistry(List<IntegrationHook> hooks) {
        hooks.forEach(this::registerHook);
    }

    /**
     * Registers an integration hook.
     *
     * @param hook The integration hook
     */
    public void registerHook(IntegrationHook hook) {
        String key = getHookKey(hook.getService(), hook.getName());
        hooks.put(key, hook);
        logger.info("Registered integration hook: {} ({})", hook.getName(), hook.getService());
    }

    /**
     * Gets an integration hook by service and name.
     *
     * @param service The service
     * @param name The name
     * @return The integration hook, or null if not found
     */
    public IntegrationHook getHook(String service, String name) {
        String key = getHookKey(service, name);
        return hooks.get(key);
    }

    /**
     * Executes an integration hook with the provided parameters.
     *
     * @param service The service
     * @param name The name
     * @param params The parameters
     * @return The result
     * @throws IntegrationException If an error occurs during execution
     */
    public Map<String, Object> executeHook(String service, String name, Map<String, Object> params) throws IntegrationException {
        IntegrationHook hook = getHook(service, name);
        if (hook == null) {
            throw new IntegrationException(
                    "Integration hook not found: " + name + " (" + service + ")",
                    "HOOK_NOT_FOUND",
                    service
            );
        }
        return hook.execute(params);
    }

    /**
     * Gets all registered integration hooks.
     *
     * @return The integration hooks
     */
    public List<IntegrationHook> getAllHooks() {
        return hooks.values().stream().collect(Collectors.toList());
    }

    /**
     * Gets all registered integration hooks for a service.
     *
     * @param service The service
     * @return The integration hooks
     */
    public List<IntegrationHook> getHooksForService(String service) {
        return hooks.values().stream()
                .filter(hook -> hook.getService().equals(service))
                .collect(Collectors.toList());
    }

    /**
     * Gets the key for an integration hook.
     *
     * @param service The service
     * @param name The name
     * @return The key
     */
    private String getHookKey(String service, String name) {
        return service + ":" + name;
    }
}

