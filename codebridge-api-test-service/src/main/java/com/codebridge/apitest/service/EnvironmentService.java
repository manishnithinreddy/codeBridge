package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.EnvironmentRequest;
import com.codebridge.apitest.dto.EnvironmentResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.Environment;
import com.codebridge.apitest.repository.EnvironmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for environment operations.
 */
@Service
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final ObjectMapper objectMapper;

    public EnvironmentService(EnvironmentRepository environmentRepository, ObjectMapper objectMapper) {
        this.environmentRepository = environmentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all environments for a project.
     *
     * @param projectId the project ID
     * @return list of environment responses
     */
    public List<EnvironmentResponse> getEnvironments(Long projectId) {
        return environmentRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get an environment by ID.
     *
     * @param id the environment ID
     * @param projectId the project ID
     * @return the environment response
     * @throws ResourceNotFoundException if the environment is not found
     */
    public EnvironmentResponse getEnvironment(Long id, Long projectId) {
        Environment environment = environmentRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found"));
        return mapToResponse(environment);
    }

    /**
     * Create a new environment.
     *
     * @param request the environment request
     * @param projectId the project ID
     * @return the created environment response
     */
    @Transactional
    public EnvironmentResponse createEnvironment(EnvironmentRequest request, Long projectId) {
        Environment environment = new Environment();
        environment.setName(request.getName());
        environment.setDescription(request.getDescription());
        environment.setProjectId(projectId);
        environment.setBaseUrl(request.getBaseUrl());
        
        try {
            if (request.getVariables() != null) {
                environment.setVariables(objectMapper.writeValueAsString(request.getVariables()));
            }
            if (request.getHeaders() != null) {
                environment.setHeaders(objectMapper.writeValueAsString(request.getHeaders()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON", e);
        }
        
        // Check if this is the first environment for the project
        List<Environment> existingEnvironments = environmentRepository.findByProjectId(projectId);
        if (existingEnvironments.isEmpty()) {
            environment.setIsDefault(true);
        } else {
            environment.setIsDefault(request.getIsDefault() != null && request.getIsDefault());
            
            // If this is set as default, unset any existing default
            if (environment.getIsDefault()) {
                existingEnvironments.stream()
                        .filter(Environment::getIsDefault)
                        .forEach(e -> {
                            e.setIsDefault(false);
                            environmentRepository.save(e);
                        });
            }
        }
        
        Environment savedEnvironment = environmentRepository.save(environment);
        return mapToResponse(savedEnvironment);
    }

    /**
     * Update an environment.
     *
     * @param id the environment ID
     * @param request the environment request
     * @param projectId the project ID
     * @return the updated environment response
     * @throws ResourceNotFoundException if the environment is not found
     */
    @Transactional
    public EnvironmentResponse updateEnvironment(Long id, EnvironmentRequest request, Long projectId) {
        Environment environment = environmentRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found"));
        
        environment.setName(request.getName());
        environment.setDescription(request.getDescription());
        environment.setBaseUrl(request.getBaseUrl());
        
        try {
            if (request.getVariables() != null) {
                environment.setVariables(objectMapper.writeValueAsString(request.getVariables()));
            }
            if (request.getHeaders() != null) {
                environment.setHeaders(objectMapper.writeValueAsString(request.getHeaders()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON", e);
        }
        
        // Handle default status
        if (request.getIsDefault() != null && request.getIsDefault() && !environment.getIsDefault()) {
            // Unset any existing default
            environmentRepository.findByProjectIdAndIsDefault(projectId, true)
                    .ifPresent(e -> {
                        e.setIsDefault(false);
                        environmentRepository.save(e);
                    });
            environment.setIsDefault(true);
        }
        
        Environment updatedEnvironment = environmentRepository.save(environment);
        return mapToResponse(updatedEnvironment);
    }

    /**
     * Delete an environment.
     *
     * @param id the environment ID
     * @param projectId the project ID
     * @throws ResourceNotFoundException if the environment is not found
     */
    @Transactional
    public void deleteEnvironment(Long id, Long projectId) {
        Environment environment = environmentRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found"));
        
        // If this is the default environment and there are others, set another as default
        if (environment.getIsDefault()) {
            List<Environment> otherEnvironments = environmentRepository.findByProjectId(projectId).stream()
                    .filter(e -> !e.getId().equals(id))
                    .collect(Collectors.toList());
            
            if (!otherEnvironments.isEmpty()) {
                Environment newDefault = otherEnvironments.get(0);
                newDefault.setIsDefault(true);
                environmentRepository.save(newDefault);
            }
        }
        
        environmentRepository.delete(environment);
    }

    /**
     * Get the default environment for a project.
     *
     * @param projectId the project ID
     * @return the default environment response
     * @throws ResourceNotFoundException if no default environment is found
     */
    public EnvironmentResponse getDefaultEnvironment(Long projectId) {
        Environment environment = environmentRepository.findByProjectIdAndIsDefault(projectId, true)
                .orElseThrow(() -> new ResourceNotFoundException("No default environment found"));
        return mapToResponse(environment);
    }

    /**
     * Set an environment as the default for a project.
     *
     * @param id the environment ID
     * @param projectId the project ID
     * @return the updated environment response
     * @throws ResourceNotFoundException if the environment is not found
     */
    @Transactional
    public EnvironmentResponse setDefaultEnvironment(Long id, Long projectId) {
        Environment environment = environmentRepository.findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found"));
        
        // Unset any existing default
        environmentRepository.findByProjectIdAndIsDefault(projectId, true)
                .ifPresent(e -> {
                    if (!e.getId().equals(id)) {
                        e.setIsDefault(false);
                        environmentRepository.save(e);
                    }
                });
        
        environment.setIsDefault(true);
        Environment updatedEnvironment = environmentRepository.save(environment);
        return mapToResponse(updatedEnvironment);
    }

    /**
     * Map an environment entity to a response DTO.
     *
     * @param environment the environment entity
     * @return the environment response DTO
     */
    private EnvironmentResponse mapToResponse(Environment environment) {
        EnvironmentResponse response = new EnvironmentResponse();
        response.setId(environment.getId());
        response.setName(environment.getName());
        response.setDescription(environment.getDescription());
        response.setBaseUrl(environment.getBaseUrl());
        response.setIsDefault(environment.getIsDefault());
        
        try {
            if (environment.getVariables() != null) {
                response.setVariables(objectMapper.readValue(environment.getVariables(), 
                        new TypeReference<Map<String, String>>() {}));
            }
            if (environment.getHeaders() != null) {
                response.setHeaders(objectMapper.readValue(environment.getHeaders(), 
                        new TypeReference<Map<String, String>>() {}));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON", e);
        }
        
        return response;
    }
}

