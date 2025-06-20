package com.codebridge.common.integration;

import java.util.Map;

/**
 * Abstract base class for integration hooks.
 * Provides common functionality for all integration hooks.
 */
public abstract class AbstractIntegrationHook {

    /**
     * Gets the name of the integration hook.
     *
     * @return The name of the integration hook
     */
    public abstract String getName();

    /**
     * Gets the description of the integration hook.
     *
     * @return The description of the integration hook
     */
    public abstract String getDescription();

    /**
     * Gets the version of the integration hook.
     *
     * @return The version of the integration hook
     */
    public abstract String getVersion();

    /**
     * Gets the service name of the integration hook.
     *
     * @return The service name of the integration hook
     */
    public abstract String getService();

    /**
     * Gets the required parameters for the integration hook.
     *
     * @return The required parameters for the integration hook
     */
    public abstract String[] getRequiredParams();

    /**
     * Executes the integration hook with the provided parameters.
     *
     * @param params The parameters for the integration hook
     * @return The result of the execution
     * @throws Exception If an error occurs during execution
     */
    public Map<String, Object> execute(Map<String, Object> params) throws Exception {
        // Validate required parameters
        for (String requiredParam : getRequiredParams()) {
            if (!params.containsKey(requiredParam) || params.get(requiredParam) == null) {
                throw new IllegalArgumentException("Missing required parameter: " + requiredParam);
            }
        }
        
        // Execute the hook
        return doExecute(params);
    }
    
    /**
     * Executes the integration hook with the provided parameters.
     * This method should be implemented by subclasses.
     *
     * @param params The parameters for the integration hook
     * @return The result of the execution
     * @throws Exception If an error occurs during execution
     */
    protected abstract Map<String, Object> doExecute(Map<String, Object> params) throws Exception;
}

