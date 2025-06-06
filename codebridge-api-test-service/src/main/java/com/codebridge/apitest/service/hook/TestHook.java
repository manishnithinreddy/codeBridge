package com.codebridge.apitest.service.hook;

import java.util.Map;

/**
 * Interface for test hooks.
 * Test hooks are used to perform additional actions before or after a test execution.
 */
public interface TestHook {
    
    /**
     * Execute the hook.
     *
     * @param context the hook context
     * @return the hook result
     */
    HookResult execute(HookContext context);
    
    /**
     * Get the hook type.
     *
     * @return the hook type
     */
    HookType getType();
    
    /**
     * Hook context.
     */
    class HookContext {
        private final Map<String, Object> parameters;
        private final Map<String, Object> testData;
        
        public HookContext(Map<String, Object> parameters, Map<String, Object> testData) {
            this.parameters = parameters;
            this.testData = testData;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        public Map<String, Object> getTestData() {
            return testData;
        }
    }
    
    /**
     * Hook result.
     */
    class HookResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> data;
        
        public HookResult(boolean success, String message, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Map<String, Object> getData() {
            return data;
        }
    }
    
    /**
     * Hook type.
     */
    enum HookType {
        PRE_REQUEST,
        POST_REQUEST,
        DATABASE_VERIFICATION,
        QUEUE_VERIFICATION,
        LOG_VERIFICATION,
        CUSTOM
    }
}

