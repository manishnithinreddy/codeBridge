package com.codebridge.apitest.service;

import com.codebridge.apitest.service.JavaScriptExecutorService.ScriptExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaScriptExecutorServiceTests {

    private JavaScriptExecutorService javaScriptExecutorService;

    @BeforeEach
    void setUp() {
        javaScriptExecutorService = new JavaScriptExecutorService();
    }

    @Test
    void testExecuteSimpleArithmeticScript_Success() {
        String script = "2 + 2";
        Object result = javaScriptExecutorService.executeScript(script, null);
        assertNotNull(result);
        // GraalVM might return Integer or Double depending on context
        assertTrue(result instanceof Number);
        assertEquals(4.0, ((Number) result).doubleValue());
    }

    @Test
    void testExecuteScriptWithBindings_Success() {
        String script = "let greeting = 'Hello, ' + name + '!'; greeting;";
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("name", "World");
        Object result = javaScriptExecutorService.executeScript(script, bindings);
        assertEquals("Hello, World!", result);
    }

    @Test
    void testExecuteScriptReturningString() {
        String script = "'test string'";
        Object result = javaScriptExecutorService.executeScript(script, null);
        assertEquals("test string", result);
    }

    @Test
    void testExecuteScriptReturningNumber() {
        String script = "123.45";
        Object result = javaScriptExecutorService.executeScript(script, null);
        assertTrue(result instanceof Double);
        assertEquals(123.45, (Double) result, 0.001);
    }
    
    @Test
    void testExecuteScriptReturningInteger() {
        String script = "123";
        Object result = javaScriptExecutorService.executeScript(script, null);
         // Depending on GraalVM's JS implementation, integers might be represented as Double internally
        if (result instanceof Integer) {
            assertEquals(123, (Integer) result);
        } else if (result instanceof Double) {
            assertEquals(123.0, (Double) result, 0.001);
        } else {
            fail("Result is not an Integer or Double: " + result.getClass().getName());
        }
    }

    @Test
    void testExecuteScriptReturningBooleanTrue() {
        String script = "true";
        Object result = javaScriptExecutorService.executeScript(script, null);
        assertTrue(result instanceof Boolean);
        assertEquals(true, result);
    }

    @Test
    void testExecuteScriptReturningBooleanFalse() {
        String script = "false";
        Object result = javaScriptExecutorService.executeScript(script, null);
        assertTrue(result instanceof Boolean);
        assertEquals(false, result);
    }
    
    @Test
    void testExecuteScriptReturningNull() {
        String script = "null;"; // Explicitly return null
        Object result = javaScriptExecutorService.executeScript(script, null);
        assertNull(result);
    }

    @Test
    void testExecuteScriptThrowingException_ThrowsScriptExecutionException() {
        String script = "throw new Error('Test error from script');";
        ScriptExecutionException exception = assertThrows(
                ScriptExecutionException.class,
                () -> javaScriptExecutorService.executeScript(script, null)
        );
        assertTrue(exception.getMessage().contains("Error executing JavaScript"));
        assertTrue(exception.getCause().getMessage().contains("Error: Test error from script"));
    }

    @Test
    void testExecuteInvalidSyntaxScript_ThrowsScriptExecutionException() {
        String script = "let a = ;"; // Invalid syntax
        ScriptExecutionException exception = assertThrows(
                ScriptExecutionException.class,
                () -> javaScriptExecutorService.executeScript(script, null)
        );
        assertTrue(exception.getMessage().contains("Error executing JavaScript"));
        // The exact message from GraalVM can vary, so check for a general indication of syntax error
        assertTrue(exception.getCause().getMessage().toLowerCase().contains("syntax error"));
    }
    
    @Test
    void testExecuteScriptWithBindingAccessingNonExistentMember() {
        // Accessing a property of a non-existent member in bindings
        String script = "request.body.test"; 
        Map<String, Object> bindings = new HashMap<>();
        // request is not in bindings
        ScriptExecutionException exception = assertThrows(
                ScriptExecutionException.class,
                () -> javaScriptExecutorService.executeScript(script, bindings)
        );
        assertTrue(exception.getMessage().contains("Error executing JavaScript"));
        // Error message might be like "ReferenceError: request is not defined" or similar
        assertTrue(exception.getCause().getMessage().contains("request")); 
    }

    @Test
    void testExecuteScriptWithComplexObjectInBindingAndReturn() {
        String script = "person.age = person.age + 1; person.name = person.name.toUpperCase(); person;";
        Map<String, Object> bindings = new HashMap<>();
        Map<String, Object> person = new HashMap<>();
        person.put("name", "John Doe");
        person.put("age", 30);
        bindings.put("person", person);

        Object result = javaScriptExecutorService.executeScript(script, bindings);
        
        // The result from GraalVM for a JS object is a Value that acts like a Map
        assertTrue(result instanceof Map, "Result should be a Map-like object");
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        assertEquals("JOHN DOE", resultMap.get("name"));
        // Numbers from JS might be Doubles
        assertEquals(31.0, ((Number)resultMap.get("age")).doubleValue());
    }
    
    @Test
    void testExecuteScriptModifyingBindings() {
        String script = "variables.put('newVar', 'newValueFromScript'); variables.set('existingVar', 'modifiedValue');";
        Map<String, Object> bindings = new HashMap<>();
        Map<String, String> scriptVariables = new HashMap<>();
        scriptVariables.put("existingVar", "initialValue");
        
        bindings.put("variables", scriptVariables); // Pass the actual map

        javaScriptExecutorService.executeScript(script, bindings);

        // Check if the original map was modified (GraalVM allows this if host objects are mutable and access is allowed)
        assertEquals("newValueFromScript", scriptVariables.get("newVar"));
        assertEquals("modifiedValue", scriptVariables.get("existingVar"));
    }
}
