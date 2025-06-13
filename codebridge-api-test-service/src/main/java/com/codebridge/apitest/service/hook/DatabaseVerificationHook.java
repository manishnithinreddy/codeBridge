package com.codebridge.apitest.service.hook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hook for database verification.
 * Allows tests to verify database state after API calls.
 */
@Component
public class DatabaseVerificationHook implements TestHook {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public DatabaseVerificationHook(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public HookResult execute(HookContext context) {
        Map<String, Object> parameters = context.getParameters();
        
        // Get query from parameters
        String query = (String) parameters.get("query");
        if (query == null || query.isEmpty()) {
            return new HookResult(false, "Query parameter is required", null);
        }
        
        try {
            // Execute query
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
            
            // Check if verification condition is met
            String condition = (String) parameters.get("condition");
            boolean conditionMet = evaluateCondition(results, condition);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("results", results);
            resultData.put("conditionMet", conditionMet);
            
            return new HookResult(
                conditionMet,
                conditionMet ? "Database verification successful" : "Database verification failed",
                resultData
            );
        } catch (Exception e) {
            return new HookResult(false, "Database verification error: " + e.getMessage(), null);
        }
    }
    
    @Override
    public HookType getType() {
        return HookType.DATABASE_VERIFICATION;
    }
    
    /**
     * Evaluate a condition against query results.
     * Supports conditions like:
     * - "count > 0" - Check if the query returned any rows
     * - "count = 5" - Check if the query returned exactly 5 rows
     * - "field:name = 'John'" - Check if any row has a field 'name' with value 'John'
     *
     * @param results the query results
     * @param condition the condition to evaluate
     * @return true if the condition is met, false otherwise
     */
    private boolean evaluateCondition(List<Map<String, Object>> results, String condition) {
        if (condition == null || condition.isEmpty()) {
            // No condition means just check if any results were returned
            return !results.isEmpty();
        }
        
        condition = condition.trim();
        
        // Handle count conditions
        if (condition.startsWith("count")) {
            int count = results.size();
            String[] parts = condition.split("\\s+", 3);
            if (parts.length < 3) {
                return false;
            }
            
            String operator = parts[1];
            int value = Integer.parseInt(parts[2]);
            
            switch (operator) {
                case "=":
                case "==":
                    return count == value;
                case ">":
                    return count > value;
                case ">=":
                    return count >= value;
                case "<":
                    return count < value;
                case "<=":
                    return count <= value;
                case "!=":
                    return count != value;
                default:
                    return false;
            }
        }
        
        // Handle field conditions
        if (condition.startsWith("field:")) {
            String[] parts = condition.split(":", 2)[1].split("\\s+", 3);
            if (parts.length < 3) {
                return false;
            }
            
            String fieldName = parts[0];
            String operator = parts[1];
            String value = parts[2].replace("'", "").replace("\"", "");
            
            for (Map<String, Object> row : results) {
                Object fieldValue = row.get(fieldName);
                if (fieldValue == null) {
                    continue;
                }
                
                String fieldValueStr = fieldValue.toString();
                
                switch (operator) {
                    case "=":
                    case "==":
                        if (fieldValueStr.equals(value)) {
                            return true;
                        }
                        break;
                    case "!=":
                        if (!fieldValueStr.equals(value)) {
                            return true;
                        }
                        break;
                    case "contains":
                        if (fieldValueStr.contains(value)) {
                            return true;
                        }
                        break;
                    case "startsWith":
                        if (fieldValueStr.startsWith(value)) {
                            return true;
                        }
                        break;
                    case "endsWith":
                        if (fieldValueStr.endsWith(value)) {
                            return true;
                        }
                        break;
                }
            }
            
            return false;
        }
        
        return false;
    }
}

