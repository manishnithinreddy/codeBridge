package com.codebridge.docker.service;

import com.codebridge.docker.model.DockerImage;
import com.codebridge.docker.model.DockerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing Docker registries and images.
 */
@Service
public class DockerRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(DockerRegistryService.class);

    private final RestTemplate restTemplate;
    
    // In-memory storage for demo purposes - in production, use a database
    private final List<DockerRegistry> registries = new ArrayList<>();
    
    @Autowired
    public DockerRegistryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get all Docker registries.
     *
     * @return List of Docker registries
     */
    public List<DockerRegistry> getAllRegistries() {
        return Collections.unmodifiableList(registries);
    }

    /**
     * Get a Docker registry by ID.
     *
     * @param id The registry ID
     * @return The Docker registry
     */
    public DockerRegistry getRegistryById(String id) {
        return registries.stream()
                .filter(registry -> registry.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Add a new Docker registry.
     *
     * @param registry The Docker registry to add
     * @return The added Docker registry
     */
    public DockerRegistry addRegistry(DockerRegistry registry) {
        registry.setId(UUID.randomUUID().toString());
        registry.setCreatedAt(java.time.LocalDateTime.now());
        registry.setUpdatedAt(java.time.LocalDateTime.now());
        registries.add(registry);
        return registry;
    }

    /**
     * Update a Docker registry.
     *
     * @param id The registry ID
     * @param registry The updated Docker registry
     * @return The updated Docker registry
     */
    public DockerRegistry updateRegistry(String id, DockerRegistry registry) {
        DockerRegistry existingRegistry = getRegistryById(id);
        if (existingRegistry == null) {
            return null;
        }
        
        registry.setId(id);
        registry.setCreatedAt(existingRegistry.getCreatedAt());
        registry.setUpdatedAt(java.time.LocalDateTime.now());
        
        registries.remove(existingRegistry);
        registries.add(registry);
        
        return registry;
    }

    /**
     * Delete a Docker registry.
     *
     * @param id The registry ID
     * @return True if deleted successfully
     */
    public boolean deleteRegistry(String id) {
        DockerRegistry registry = getRegistryById(id);
        if (registry == null) {
            return false;
        }
        
        return registries.remove(registry);
    }

    /**
     * Test connection to a Docker registry.
     *
     * @param registry The Docker registry to test
     * @return True if connection is successful
     */
    public boolean testRegistryConnection(DockerRegistry registry) {
        try {
            HttpHeaders headers = createAuthHeaders(registry);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            String url = registry.getUrl() + "/v2/";
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Error testing connection to registry {}: {}", registry.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Get images from a Docker registry.
     *
     * @param registryId The registry ID
     * @param namespace The namespace (optional)
     * @return List of Docker images
     */
    public List<DockerImage> getImages(String registryId, String namespace) {
        DockerRegistry registry = getRegistryById(registryId);
        if (registry == null) {
            return Collections.emptyList();
        }
        
        try {
            HttpHeaders headers = createAuthHeaders(registry);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            String url = registry.getUrl() + "/v2/_catalog";
            if (namespace != null && !namespace.isEmpty()) {
                url += "?n=" + namespace;
            }
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Collections.emptyList();
            }
            
            List<String> repositories = (List<String>) response.getBody().get("repositories");
            List<DockerImage> images = new ArrayList<>();
            
            for (String repo : repositories) {
                DockerImage image = new DockerImage();
                image.setId(UUID.randomUUID().toString());
                image.setRegistryId(registryId);
                image.setName(repo);
                image.setFullName(registry.getUrl() + "/" + repo);
                images.add(image);
            }
            
            return images;
        } catch (Exception e) {
            logger.error("Error fetching images from registry {}: {}", registry.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get tags for a Docker image.
     *
     * @param registryId The registry ID
     * @param imageName The image name
     * @return List of tags
     */
    public List<String> getImageTags(String registryId, String imageName) {
        DockerRegistry registry = getRegistryById(registryId);
        if (registry == null) {
            return Collections.emptyList();
        }
        
        try {
            HttpHeaders headers = createAuthHeaders(registry);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            String url = registry.getUrl() + "/v2/" + imageName + "/tags/list";
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Collections.emptyList();
            }
            
            return (List<String>) response.getBody().get("tags");
        } catch (Exception e) {
            logger.error("Error fetching tags for image {} from registry {}: {}", 
                    imageName, registry.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Create authentication headers for Docker registry.
     *
     * @param registry The Docker registry
     * @return The HTTP headers
     */
    private HttpHeaders createAuthHeaders(DockerRegistry registry) {
        HttpHeaders headers = new HttpHeaders();
        
        if ("basic".equalsIgnoreCase(registry.getAuthType())) {
            String auth = registry.getUsername() + ":" + registry.getPassword();
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            headers.set("Authorization", "Basic " + new String(encodedAuth));
        } else if ("token".equalsIgnoreCase(registry.getAuthType())) {
            headers.set("Authorization", "Bearer " + registry.getPassword());
        }
        
        return headers;
    }
}

