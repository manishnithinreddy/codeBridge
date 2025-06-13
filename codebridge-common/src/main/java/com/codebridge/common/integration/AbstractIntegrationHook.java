package com.codebridge.common.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

/**
 * Abstract base class for integration hooks.
 * Provides common functionality for all integration hooks.
 */
public abstract class AbstractIntegrationHook implements IntegrationHook {

    private static final Logger logger = LoggerFactory.getLogger(AbstractIntegrationHook.class);

    /**
     * Validates the parameters for the integration hook.
     *
     * @param params The parameters
     * @return True if the parameters are valid, false otherwise
     */
    @Override
    public boolean validateParams(Map<String, Object> params) {
        if (params == null) {
            logger.error("Parameters cannot be null");
            return false;
        }

        String[] requiredParams = getRequiredParams();
        if (requiredParams == null || requiredParams.length == 0) {
            return true;
        }

        for (String param : requiredParams) {
            if (!params.containsKey(param) || params.get(param) == null) {
                logger.error("Missing required parameter: {}", param);
                return false;
            }
        }

        return true;
    }

    /**
     * Executes the integration hook with the provided parameters.
     *
     * @param params The parameters
     * @return The result
     * @throws IntegrationException If an error occurs during execution
     */
    @Override
    public Map<String, Object> execute(Map<String, Object> params) throws IntegrationException {
        if (!validateParams(params)) {
            throw new IntegrationException(
                    "Invalid parameters for integration hook: " + getName(),
                    "INVALID_PARAMS",
                    getService()
            );
        }

        logger.info("Executing integration hook: {} ({})", getName(), getService());
        try {
            Map<String, Object> result = doExecute(params);
            logger.info("Integration hook execution completed: {} ({})", getName(), getService());
            return result;
        } catch (Exception e) {
            logger.error("Error executing integration hook: {} ({})", getName(), getService(), e);
            throw new IntegrationException(
                    "Error executing integration hook: " + e.getMessage(),
                    e,
                    "EXECUTION_ERROR",
                    getService()
            );
        }
    }

    /**
     * Executes the integration hook with the provided parameters.
     * This method should be implemented by subclasses.
     *
     * @param params The parameters
     * @return The result
     * @throws Exception If an error occurs during execution
     */
    protected abstract Map<String, Object> doExecute(Map<String, Object> params) throws Exception;
}

