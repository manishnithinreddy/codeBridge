package com.codebridge.docker.service;

import com.codebridge.docker.model.ImageInfo;

import java.util.List;

public interface DockerImageService {
    
    /**
     * Get a list of all images
     * 
     * @param all If true, show all images (default shows just tagged images)
     * @return List of image information
     */
    List<ImageInfo> getImages(boolean all);
    
    /**
     * Get detailed information about a specific image
     * 
     * @param imageId ID or name of the image
     * @return Image details
     */
    ImageInfo getImage(String imageId);
    
    /**
     * Pull an image from a registry
     * 
     * @param imageName Name of the image to pull
     * @param tag Tag of the image (default: latest)
     * @return true if successful
     */
    boolean pullImage(String imageName, String tag);
    
    /**
     * Remove an image
     * 
     * @param imageId ID or name of the image
     * @param force Force removal of the image
     * @return true if successful
     */
    boolean removeImage(String imageId, boolean force);
    
    /**
     * Build an image from a Dockerfile
     * 
     * @param dockerfilePath Path to the Dockerfile
     * @param imageName Name for the built image
     * @param tag Tag for the built image
     * @return ID of the built image
     */
    String buildImage(String dockerfilePath, String imageName, String tag);
}

