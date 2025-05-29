package com.codebridge.apitest.service;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import java.util.Map;

// Consider adding @Service if it needs to be injected later
public class JavaScriptExecutorService {

    public static class ScriptExecutionException extends RuntimeException {
        public ScriptExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public Object executeScript(String script, Map<String, Object> bindings) {
        try (Context context = Context.newBuilder("js").allowAllAccess(true).build()) {
            Value scriptBindings = context.getBindings("js");
            if (bindings != null) {
                for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                    scriptBindings.putMember(entry.getKey(), entry.getValue());
                }
            }
            Value result = context.eval("js", script);
            if (result == null) {
                return null;
            }
            // Convert GraalVM Value to standard Java objects if possible
            if (result.isHostObject()) {
                return result.asHostObject();
            } else if (result.isString()) {
                return result.asString();
            } else if (result.isBoolean()) {
                return result.asBoolean();
            } else if (result.isNumber()) {
                return result.asDouble(); // Or choose another appropriate number type
            }
            // For more complex objects, you might need more sophisticated conversion
            // or return the Value object itself if the caller can handle it.
            // For now, if it's none of the above, and not null, try returning as host object if applicable or toString.
            return result.toString(); // Fallback, might need refinement
        } catch (PolyglotException e) {
            throw new ScriptExecutionException("Error executing JavaScript: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ScriptExecutionException("Error setting up JavaScript context: " + e.getMessage(), e);
        }
    }
}
