package com.codebridge.common.integration;

import java.util.Map;

/**
 * Base interface for all integration hooks.
 * Integration hooks provide a standardized way for services to communicate with each other.
 */
public interface IntegrationHook {

    /**
     * Gets the name of the integration hook.
     *
     * @return The name
     */
    String getName();

    /**
     * Gets the description of the integration hook.
     *
     * @return The description
     */
    String getDescription();

    /**
     * Gets the version of the integration hook.
     *
     * @return The version
     */
    String getVersion();

    /**
     * Gets the service that owns this integration hook.
     *
     * @return The service name
     */
    String getService();

    /**
     * Executes the integration hook with the provided parameters.
     *
     * @param params The parameters
     * @return The result
     * @throws IntegrationException If an error occurs during execution
     */
    Map<String, Object> execute(Map<String, Object> params) throws IntegrationException;

    /**
     * Validates the parameters for the integration hook.
     *
     * @param params The parameters
     * @return True if the parameters are valid, false otherwise
     */
    boolean validateParams(Map<String, Object> params);

    /**
     * Gets the required parameters for the integration hook.
     *
     * @return The required parameters
     */
    String[] getRequiredParams();
}

