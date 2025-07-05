package com.codebridge.docker.service.impl;

import com.codebridge.docker.model.ImageInfo;
import com.codebridge.docker.service.DockerImageService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.SearchItem;
import com.github.dockerjava.core.command.PushImageResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of DockerImageService using Docker Java client.
 */
@Slf4j
@Service
public class DockerImageServiceImpl implements DockerImageService {

    private final DockerClient dockerClient;

    public DockerImageServiceImpl(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public List<ImageInfo> getImages(boolean showAll) {
        log.info("Getting all images, showAll: {}", showAll);
        
        try {
            List<Image> images = dockerClient.listImagesCmd()
                    .withShowAll(showAll)
                    .exec();
            
            return images.stream()
                    .map(this::mapToImageInfo)
                    .collect(Collectors.toList());
        } catch (DockerException e) {
            log.error("Error getting images", e);
            return Collections.emptyList();
        }
    }

    @Override
    public ImageInfo getImage(String imageIdOrName) {
        log.info("Getting image: {}", imageIdOrName);
        
        try {
            List<Image> images = dockerClient.listImagesCmd()
                    .withImageNameFilter(imageIdOrName)
                    .exec();
            
            if (images.isEmpty()) {
                return null;
            }
            
            return mapToImageInfo(images.get(0));
        } catch (DockerException e) {
            log.error("Error getting image: {}", imageIdOrName, e);
            return null;
        }
    }

    @Override
    public ImageInfo pullImage(String imageName, String tag, String registry, String username, String password) {
        log.info("Pulling image: {}:{} from registry: {}", imageName, tag, registry);
        
        try {
            String fullImageName = imageName;
            if (tag != null && !tag.isEmpty()) {
                fullImageName += ":" + tag;
            }
            
            if (registry != null && !registry.isEmpty()) {
                fullImageName = registry + "/" + fullImageName;
            }
            
            PullImageResultCallback callback = new PullImageResultCallback();
            
            if (username != null && !username.isEmpty() && password != null) {
                AuthConfig authConfig = new AuthConfig()
                        .withUsername(username)
                        .withPassword(password);
                
                if (registry != null && !registry.isEmpty()) {
                    authConfig.withRegistryAddress(registry);
                }
                
                dockerClient.pullImageCmd(fullImageName)
                        .withAuthConfig(authConfig)
                        .exec(callback);
            } else {
                dockerClient.pullImageCmd(fullImageName)
                        .exec(callback);
            }
            
            callback.awaitCompletion(5, TimeUnit.MINUTES);
            
            return getImage(fullImageName);
        } catch (DockerException | InterruptedException e) {
            log.error("Error pulling image: {}:{} from registry: {}", imageName, tag, registry, e);
            return null;
        }
    }

    @Override
    public boolean pushImage(String imageName, String tag, String registry, String username, String password) {
        log.info("Pushing image: {}:{} to registry: {}", imageName, tag, registry);
        
        try {
            String fullImageName = imageName;
            if (tag != null && !tag.isEmpty()) {
                fullImageName += ":" + tag;
            }
            
            if (registry != null && !registry.isEmpty()) {
                fullImageName = registry + "/" + fullImageName;
            }
            
            PushImageResultCallback callback = new PushImageResultCallback();
            
            AuthConfig authConfig = new AuthConfig()
                    .withUsername(username)
                    .withPassword(password);
            
            if (registry != null && !registry.isEmpty()) {
                authConfig.withRegistryAddress(registry);
            }
            
            dockerClient.pushImageCmd(fullImageName)
                    .withAuthConfig(authConfig)
                    .exec(callback);
            
            callback.awaitCompletion(5, TimeUnit.MINUTES);
            
            return true;
        } catch (DockerException | InterruptedException e) {
            log.error("Error pushing image: {}:{} to registry: {}", imageName, tag, registry, e);
            return false;
        }
    }

    @Override
    public ImageInfo buildImage(String dockerfilePath, String imageName, String tag, Map<String, String> buildArgs) {
        log.info("Building image: {}:{} from Dockerfile: {}", imageName, tag, dockerfilePath);
        
        try {
            String fullImageName = imageName;
            if (tag != null && !tag.isEmpty()) {
                fullImageName += ":" + tag;
            }
            
            BuildImageResultCallback callback = new BuildImageResultCallback();
            
            BuildImageCmd buildCmd = dockerClient.buildImageCmd()
                    .withDockerfile(new File(dockerfilePath))
                    .withTags(Collections.singleton(fullImageName));
            
            // Add build args individually for compatibility
            if (buildArgs != null && !buildArgs.isEmpty()) {
                for (Map.Entry<String, String> entry : buildArgs.entrySet()) {
                    buildCmd.withBuildArg(entry.getKey(), entry.getValue());
                }
            }
            
            String imageId = buildCmd.exec(callback).awaitImageId();
            
            return getImage(imageId);
        } catch (DockerException e) {
            log.error("Error building image: {}:{} from Dockerfile: {}", imageName, tag, dockerfilePath, e);
            return null;
        }
    }

    @Override
    public boolean tagImage(String imageIdOrName, String repositoryName, String tag) {
        log.info("Tagging image: {} as {}:{}", imageIdOrName, repositoryName, tag);
        
        try {
            dockerClient.tagImageCmd(imageIdOrName, repositoryName, tag)
                    .exec();
            
            return true;
        } catch (DockerException e) {
            log.error("Error tagging image: {} as {}:{}", imageIdOrName, repositoryName, tag, e);
            return false;
        }
    }

    @Override
    public boolean removeImage(String imageIdOrName, boolean force, boolean noPrune) {
        log.info("Removing image: {}, force: {}, noPrune: {}", imageIdOrName, force, noPrune);
        
        try {
            dockerClient.removeImageCmd(imageIdOrName)
                    .withForce(force)
                    .withNoPrune(noPrune)
                    .exec();
            
            return true;
        } catch (DockerException e) {
            log.error("Error removing image: {}", imageIdOrName, e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getImageHistory(String imageIdOrName) {
        log.info("Getting history for image: {}", imageIdOrName);
        
        try {
            // Note: historyCmd was removed in newer docker-java versions
            // For now, return basic image information instead
            var imageResponse = dockerClient.inspectImageCmd(imageIdOrName).exec();
            Map<String, Object> historyMap = new HashMap<>();
            historyMap.put("id", imageResponse.getId());
            historyMap.put("created", imageResponse.getCreated());
            historyMap.put("size", imageResponse.getSize());
            historyMap.put("comment", "Image inspection (history API not available)");
            return Collections.singletonList(historyMap);
            /*
            return dockerClient.historyCmd(imageIdOrName)
                    .exec()
                    .stream()
                    .map(history -> {
                        Map<String, Object> historyMap = new HashMap<>();
                        historyMap.put("id", history.getId());
                        historyMap.put("created", history.getCreated());
                        historyMap.put("createdBy", history.getCreatedBy());
                        historyMap.put("size", history.getSize());
                        historyMap.put("tags", history.getTags());
                        historyMap.put("comment", history.getComment());
                        return historyMap;
                    })
                    .collect(Collectors.toList());
            */
        } catch (DockerException e) {
            log.error("Error getting history for image: {}", imageIdOrName, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> searchImages(String term, int limit) {
        log.info("Searching for images with term: {}, limit: {}", term, limit);
        
        try {
            List<SearchItem> searchResults = dockerClient.searchImagesCmd(term)
                    .withLimit(limit)
                    .exec();
            
            return searchResults.stream()
                    .map(item -> {
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("name", item.getName());
                        resultMap.put("description", item.getDescription());
                        resultMap.put("official", item.isOfficial());
                        // Note: isAutomated() method removed in newer docker-java versions
                        resultMap.put("automated", false);
                        resultMap.put("starCount", item.getStarCount());
                        return resultMap;
                    })
                    .collect(Collectors.toList());
        } catch (DockerException e) {
            log.error("Error searching for images with term: {}", term, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Maps Docker Image to ImageInfo.
     *
     * @param image Docker Image
     * @return ImageInfo
     */
    private ImageInfo mapToImageInfo(Image image) {
        ImageInfo info = new ImageInfo();
        info.setId(image.getId());
        info.setParentId(image.getParentId());
        info.setRepoTags(image.getRepoTags() != null ? Arrays.asList(image.getRepoTags()) : Collections.emptyList());
        info.setRepoDigests(image.getRepoDigests() != null ? Arrays.asList(image.getRepoDigests()) : Collections.emptyList());
        info.setCreated(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(image.getCreated()), 
                ZoneId.systemDefault()));
        info.setSize(image.getSize());
        info.setVirtualSize(image.getVirtualSize());
        info.setLabels(image.getLabels());
        
        return info;
    }
}
