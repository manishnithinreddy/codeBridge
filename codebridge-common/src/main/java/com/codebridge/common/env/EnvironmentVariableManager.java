package com.codebridge.common.env;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for environment variables.
 * Provides methods for getting, setting, and sharing environment variables between services.
 */
@Component
public class EnvironmentVariableManager {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentVariableManager.class);
    private final Map<String, String> sharedVariables = new ConcurrentHashMap<>();
    private final Environment environment;

    /**
     * Creates a new environment variable manager.
     *
     * @param environment The Spring environment
     */
    @Autowired
    public EnvironmentVariableManager(Environment environment) {
        this.environment = environment;
    }

    /**
     * Gets an environment variable.
     *
     * @param name The variable name
     * @return The variable value, or null if not found
     */
    public String getVariable(String name) {
        // First, check shared variables
        String value = sharedVariables.get(name);
        if (value != null) {
            return value;
        }
        
        // Then, check system environment variables
        value = environment.getProperty(name);
        return value;
    }

    /**
     * Gets an environment variable with a default value.
     *
     * @param name The variable name
     * @param defaultValue The default value
     * @return The variable value, or the default value if not found
     */
    public String getVariable(String name, String defaultValue) {
        String value = getVariable(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Sets a shared environment variable.
     *
     * @param name The variable name
     * @param value The variable value
     */
    public void setSharedVariable(String name, String value) {
        sharedVariables.put(name, value);
        logger.debug("Set shared variable: {}", name);
    }

    /**
     * Removes a shared environment variable.
     *
     * @param name The variable name
     * @return The removed variable value, or null if not found
     */
    public String removeSharedVariable(String name) {
        String value = sharedVariables.remove(name);
        if (value != null) {
            logger.debug("Removed shared variable: {}", name);
        }
        return value;
    }

    /**
     * Gets all shared environment variables.
     *
     * @return The shared variables
     */
    public Map<String, String> getSharedVariables() {
        return new HashMap<>(sharedVariables);
    }

    /**
     * Gets all environment variables (system and shared).
     *
     * @return The environment variables
     */
    public Map<String, String> getAllVariables() {
        Map<String, String> variables = new HashMap<>();
        
        // Add system environment variables
        System.getenv().forEach(variables::put);
        
        // Add Spring environment properties
        for (String name : environment.getActiveProfiles()) {
            variables.put("spring.profiles.active." + name, "true");
        }
        
        // Add shared variables (overriding system variables if necessary)
        variables.putAll(sharedVariables);
        
        return variables;
    }

    /**
     * Clears all shared environment variables.
     */
    public void clearSharedVariables() {
        sharedVariables.clear();
        logger.debug("Cleared all shared variables");
    }
}

