package com.codebridge.apitest.integration;

import com.codebridge.apitest.service.ApiTestService;
import com.codebridge.common.integration.AbstractIntegrationHook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration hook for server-related operations in the API Test Service.
 * Provides methods for validating server connectivity and executing commands.
 */
@Component
public class ServerIntegrationHook extends AbstractIntegrationHook {

    private static final String NAME = "server-integration";
    private static final String DESCRIPTION = "Provides server integration capabilities for API tests";
    private static final String VERSION = "1.0.0";
    private static final String SERVICE = "api-test-service";
    
    private static final String[] REQUIRED_PARAMS = {
        "serverId",
        "operation"
    };
    
    private final ApiTestService apiTestService;
    
    @Autowired
    public ServerIntegrationHook(ApiTestService apiTestService) {
        this.apiTestService = apiTestService;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getService() {
        return SERVICE;
    }

    @Override
    public String[] getRequiredParams() {
        return REQUIRED_PARAMS;
    }

    @Override
    protected Map<String, Object> doExecute(Map<String, Object> params) throws Exception {
        String serverId = params.get("serverId").toString();
        String operation = params.get("operation").toString();
        
        Map<String, Object> result = new HashMap<>();
        
        switch (operation) {
            case "validate-connection":
                boolean isConnected = validateServerConnection(serverId, params);
                result.put("connected", isConnected);
                break;
                
            case "execute-command":
                if (!params.containsKey("command")) {
                    throw new IllegalArgumentException("Missing required parameter: command");
                }
                String command = params.get("command").toString();
                String commandOutput = executeServerCommand(serverId, command, params);
                result.put("output", commandOutput);
                break;
                
            case "get-server-info":
                Map<String, Object> serverInfo = getServerInfo(serverId, params);
                result.putAll(serverInfo);
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported operation: " + operation);
        }
        
        return result;
    }
    
    /**
     * Validates the connection to a server.
     *
     * @param serverId The server ID
     * @param params Additional parameters
     * @return True if the connection is valid, false otherwise
     */
    private boolean validateServerConnection(String serverId, Map<String, Object> params) {
        // In a real implementation, this would use the ApiTestService to validate the connection
        // For now, we'll just return true
        return true;
    }
    
    /**
     * Executes a command on a server.
     *
     * @param serverId The server ID
     * @param command The command to execute
     * @param params Additional parameters
     * @return The command output
     */
    private String executeServerCommand(String serverId, String command, Map<String, Object> params) {
        // In a real implementation, this would use the ApiTestService to execute the command
        // For now, we'll just return a dummy response
        return "Command executed successfully: " + command;
    }
    
    /**
     * Gets information about a server.
     *
     * @param serverId The server ID
     * @param params Additional parameters
     * @return The server information
     */
    private Map<String, Object> getServerInfo(String serverId, Map<String, Object> params) {
        // In a real implementation, this would use the ApiTestService to get server info
        // For now, we'll just return dummy data
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("id", serverId);
        serverInfo.put("hostname", "server-" + serverId + ".example.com");
        serverInfo.put("status", "running");
        serverInfo.put("os", "Linux");
        serverInfo.put("cpuCores", 4);
        serverInfo.put("memoryGB", 16);
        
        return serverInfo;
    }
}

