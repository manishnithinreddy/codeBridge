package com.codebridge.common.env;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for managing environment variables.
 * Provides endpoints for getting, setting, and sharing environment variables between services.
 */
@RestController
@RequestMapping("/api/env")
public class EnvironmentVariableController {

    private final EnvironmentVariableManager environmentVariableManager;

    /**
     * Creates a new environment variable controller.
     *
     * @param environmentVariableManager The environment variable manager
     */
    @Autowired
    public EnvironmentVariableController(EnvironmentVariableManager environmentVariableManager) {
        this.environmentVariableManager = environmentVariableManager;
    }

    /**
     * Gets all shared environment variables.
     *
     * @return The shared variables
     */
    @GetMapping("/shared")
    public ResponseEntity<Map<String, String>> getSharedVariables() {
        return ResponseEntity.ok(environmentVariableManager.getSharedVariables());
    }

    /**
     * Gets a shared environment variable.
     *
     * @param name The variable name
     * @return The variable value
     */
    @GetMapping("/shared/{name}")
    public ResponseEntity<String> getSharedVariable(@PathVariable String name) {
        String value = environmentVariableManager.getVariable(name);
        if (value != null) {
            return ResponseEntity.ok(value);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Sets a shared environment variable.
     *
     * @param name The variable name
     * @param value The variable value
     * @return The response entity
     */
    @PostMapping("/shared/{name}")
    public ResponseEntity<Void> setSharedVariable(@PathVariable String name, @RequestBody String value) {
        environmentVariableManager.setSharedVariable(name, value);
        return ResponseEntity.ok().build();
    }

    /**
     * Sets multiple shared environment variables.
     *
     * @param variables The variables
     * @return The response entity
     */
    @PostMapping("/shared")
    public ResponseEntity<Void> setSharedVariables(@RequestBody Map<String, String> variables) {
        variables.forEach(environmentVariableManager::setSharedVariable);
        return ResponseEntity.ok().build();
    }

    /**
     * Removes a shared environment variable.
     *
     * @param name The variable name
     * @return The response entity
     */
    @DeleteMapping("/shared/{name}")
    public ResponseEntity<Void> removeSharedVariable(@PathVariable String name) {
        String value = environmentVariableManager.removeSharedVariable(name);
        if (value != null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Clears all shared environment variables.
     *
     * @return The response entity
     */
    @DeleteMapping("/shared")
    public ResponseEntity<Void> clearSharedVariables() {
        environmentVariableManager.clearSharedVariables();
        return ResponseEntity.ok().build();
    }
}

