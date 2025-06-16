package com.codebridge.docker.controller;

import com.codebridge.docker.model.ImageInfo;
import com.codebridge.docker.service.DockerImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Docker image operations.
 */
@Slf4j
@RestController
@RequestMapping("/images")
@Tag(name = "Docker Images", description = "Docker image operations")
public class DockerImageController {

    private final DockerImageService dockerImageService;

    public DockerImageController(DockerImageService dockerImageService) {
        this.dockerImageService = dockerImageService;
    }

    /**
     * Gets all images.
     *
     * @param showAll Whether to show all images (including intermediate images)
     * @return ResponseEntity containing list of image information
     */
    @GetMapping
    @Operation(
        summary = "Get all images",
        description = "Retrieves all Docker images, optionally including intermediate images",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Images retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageInfo.class))
            )
        }
    )
    public ResponseEntity<List<ImageInfo>> getImages(
            @Parameter(description = "Whether to show all images (including intermediate images)")
            @RequestParam(defaultValue = "false") boolean showAll) {
        log.info("Getting all images, showAll: {}", showAll);
        List<ImageInfo> images = dockerImageService.getImages(showAll);
        return ResponseEntity.ok(images);
    }

    /**
     * Gets a specific image by ID or name.
     *
     * @param imageIdOrName ID or name of the image
     * @return ResponseEntity containing image information
     */
    @GetMapping("/{imageIdOrName}")
    @Operation(
        summary = "Get a specific image",
        description = "Retrieves a specific Docker image by ID or name",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Image retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageInfo.class))
            ),
            @ApiResponse(responseCode = "404", description = "Image not found")
        }
    )
    public ResponseEntity<ImageInfo> getImage(
            @Parameter(description = "ID or name of the image", required = true)
            @PathVariable String imageIdOrName) {
        log.info("Getting image: {}", imageIdOrName);
        ImageInfo image = dockerImageService.getImage(imageIdOrName);
        
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(image);
    }

    /**
     * Pulls an image from a registry.
     *
     * @param imageName Name of the image to pull
     * @param tag Tag of the image to pull
     * @param registry Registry to pull from
     * @param username Username for registry authentication
     * @param password Password for registry authentication
     * @return ResponseEntity containing pulled image information
     */
    @PostMapping("/pull")
    @Operation(
        summary = "Pull an image from a registry",
        description = "Pulls a Docker image from a registry",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Image pulled successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageInfo.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Failed to pull image")
        }
    )
    public ResponseEntity<ImageInfo> pullImage(
            @Parameter(description = "Name of the image to pull", required = true)
            @RequestParam String imageName,
            @Parameter(description = "Tag of the image to pull")
            @RequestParam(required = false) String tag,
            @Parameter(description = "Registry to pull from")
            @RequestParam(required = false) String registry,
            @Parameter(description = "Username for registry authentication")
            @RequestParam(required = false) String username,
            @Parameter(description = "Password for registry authentication")
            @RequestParam(required = false) String password) {
        log.info("Pulling image: {}:{} from registry: {}", imageName, tag, registry);
        ImageInfo image = dockerImageService.pullImage(imageName, tag, registry, username, password);
        
        if (image == null) {
            return ResponseEntity.internalServerError().build();
        }
        
        return ResponseEntity.ok(image);
    }

    /**
     * Pushes an image to a registry.
     *
     * @param imageName Name of the image to push
     * @param tag Tag of the image to push
     * @param registry Registry to push to
     * @param username Username for registry authentication
     * @param password Password for registry authentication
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/push")
    @Operation(
        summary = "Push an image to a registry",
        description = "Pushes a Docker image to a registry",
        responses = {
            @ApiResponse(responseCode = "200", description = "Image pushed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Failed to push image")
        }
    )
    public ResponseEntity<Void> pushImage(
            @Parameter(description = "Name of the image to push", required = true)
            @RequestParam String imageName,
            @Parameter(description = "Tag of the image to push")
            @RequestParam(required = false) String tag,
            @Parameter(description = "Registry to push to")
            @RequestParam(required = false) String registry,
            @Parameter(description = "Username for registry authentication", required = true)
            @RequestParam String username,
            @Parameter(description = "Password for registry authentication", required = true)
            @RequestParam String password) {
        log.info("Pushing image: {}:{} to registry: {}", imageName, tag, registry);
        boolean success = dockerImageService.pushImage(imageName, tag, registry, username, password);
        
        if (!success) {
            return ResponseEntity.internalServerError().build();
        }
        
        return ResponseEntity.ok().build();
    }

    /**
     * Gets history for an image.
     *
     * @param imageIdOrName ID or name of the image
     * @return ResponseEntity containing image history
     */
    @GetMapping("/{imageIdOrName}/history")
    @Operation(
        summary = "Get history for an image",
        description = "Retrieves the history for a Docker image",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Image history retrieved successfully",
                content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "404", description = "Image not found")
        }
    )
    public ResponseEntity<List<Map<String, Object>>> getImageHistory(
            @Parameter(description = "ID or name of the image", required = true)
            @PathVariable String imageIdOrName) {
        log.info("Getting history for image: {}", imageIdOrName);
        List<Map<String, Object>> history = dockerImageService.getImageHistory(imageIdOrName);
        
        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(history);
    }

    /**
     * Searches for images in Docker Hub.
     *
     * @param term Search term
     * @param limit Maximum number of results
     * @return ResponseEntity containing search results
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search for images in Docker Hub",
        description = "Searches for Docker images in Docker Hub",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Search results retrieved successfully",
                content = @Content(mediaType = "application/json")
            )
        }
    )
    public ResponseEntity<List<Map<String, Object>>> searchImages(
            @Parameter(description = "Search term", required = true)
            @RequestParam String term,
            @Parameter(description = "Maximum number of results")
            @RequestParam(defaultValue = "25") int limit) {
        log.info("Searching for images with term: {}, limit: {}", term, limit);
        List<Map<String, Object>> searchResults = dockerImageService.searchImages(term, limit);
        return ResponseEntity.ok(searchResults);
    }

    /**
     * Removes an image.
     *
     * @param imageIdOrName ID or name of the image
     * @param force Whether to force removal
     * @param noPrune Whether to prevent pruning
     * @return ResponseEntity indicating success or failure
     */
    @DeleteMapping("/{imageIdOrName}")
    @Operation(
        summary = "Remove an image",
        description = "Removes a Docker image",
        responses = {
            @ApiResponse(responseCode = "200", description = "Image removed successfully"),
            @ApiResponse(responseCode = "404", description = "Image not found"),
            @ApiResponse(responseCode = "500", description = "Failed to remove image")
        }
    )
    public ResponseEntity<Void> removeImage(
            @Parameter(description = "ID or name of the image", required = true)
            @PathVariable String imageIdOrName,
            @Parameter(description = "Whether to force removal")
            @RequestParam(defaultValue = "false") boolean force,
            @Parameter(description = "Whether to prevent pruning")
            @RequestParam(defaultValue = "false") boolean noPrune) {
        log.info("Removing image: {}, force: {}, noPrune: {}", imageIdOrName, force, noPrune);
        boolean success = dockerImageService.removeImage(imageIdOrName, force, noPrune);
        
        if (!success) {
            return ResponseEntity.internalServerError().build();
        }
        
        return ResponseEntity.ok().build();
    }

    /**
     * Tags an image.
     *
     * @param imageIdOrName ID or name of the image
     * @param repositoryName Repository name for the new tag
     * @param tag New tag
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/{imageIdOrName}/tag")
    @Operation(
        summary = "Tag an image",
        description = "Tags a Docker image",
        responses = {
            @ApiResponse(responseCode = "200", description = "Image tagged successfully"),
            @ApiResponse(responseCode = "404", description = "Image not found"),
            @ApiResponse(responseCode = "500", description = "Failed to tag image")
        }
    )
    public ResponseEntity<Void> tagImage(
            @Parameter(description = "ID or name of the image", required = true)
            @PathVariable String imageIdOrName,
            @Parameter(description = "Repository name for the new tag", required = true)
            @RequestParam String repositoryName,
            @Parameter(description = "New tag", required = true)
            @RequestParam String tag) {
        log.info("Tagging image: {} as {}:{}", imageIdOrName, repositoryName, tag);
        boolean success = dockerImageService.tagImage(imageIdOrName, repositoryName, tag);
        
        if (!success) {
            return ResponseEntity.internalServerError().build();
        }
        
        return ResponseEntity.ok().build();
    }
}

