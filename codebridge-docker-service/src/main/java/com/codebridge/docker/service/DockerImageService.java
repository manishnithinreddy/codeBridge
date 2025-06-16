package com.codebridge.docker.service;

import com.codebridge.docker.model.ImageInfo;

import java.util.List;
import java.util.Map;

/**
 * Service for Docker image operations.
 */
public interface DockerImageService {
    
    /**
     * Gets all images.
     *
     * @param showAll Whether to show all images (including intermediate images)
     * @return List of image information
     */
    List<ImageInfo> getImages(boolean showAll);
    
    /**
     * Gets a specific image by ID or name.
     *
     * @param imageIdOrName ID or name of the image
     * @return Image information or null if not found
     */
    ImageInfo getImage(String imageIdOrName);
    
    /**
     * Pulls an image from a registry.
     *
     * @param imageName Name of the image to pull
     * @param tag Tag of the image to pull
     * @param registry Registry to pull from
     * @param username Username for registry authentication
     * @param password Password for registry authentication
     * @return Pulled image information
     */
    ImageInfo pullImage(String imageName, String tag, String registry, String username, String password);
    
    /**
     * Pushes an image to a registry.
     *
     * @param imageName Name of the image to push
     * @param tag Tag of the image to push
     * @param registry Registry to push to
     * @param username Username for registry authentication
     * @param password Password for registry authentication
     * @return true if successful, false otherwise
     */
    boolean pushImage(String imageName, String tag, String registry, String username, String password);
    
    /**
     * Builds an image from a Dockerfile.
     *
     * @param dockerfilePath Path to the Dockerfile
     * @param imageName Name for the built image
     * @param tag Tag for the built image
     * @param buildArgs Build arguments
     * @return Built image information
     */
    ImageInfo buildImage(String dockerfilePath, String imageName, String tag, Map<String, String> buildArgs);
    
    /**
     * Tags an image.
     *
     * @param imageIdOrName ID or name of the image
     * @param repositoryName Repository name for the new tag
     * @param tag New tag
     * @return true if successful, false otherwise
     */
    boolean tagImage(String imageIdOrName, String repositoryName, String tag);
    
    /**
     * Removes an image.
     *
     * @param imageIdOrName ID or name of the image
     * @param force Whether to force removal
     * @param noPrune Whether to prevent pruning
     * @return true if successful, false otherwise
     */
    boolean removeImage(String imageIdOrName, boolean force, boolean noPrune);
    
    /**
     * Gets history for an image.
     *
     * @param imageIdOrName ID or name of the image
     * @return Image history as a list of maps
     */
    List<Map<String, Object>> getImageHistory(String imageIdOrName);
    
    /**
     * Searches for images in Docker Hub.
     *
     * @param term Search term
     * @param limit Maximum number of results
     * @return List of search results as maps
     */
    List<Map<String, Object>> searchImages(String term, int limit);
}

