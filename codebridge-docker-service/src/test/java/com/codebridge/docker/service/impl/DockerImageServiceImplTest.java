package com.codebridge.docker.service.impl;

import com.codebridge.docker.model.ImageInfo;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.SearchItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DockerImageServiceImplTest {

    @Mock
    private DockerClient dockerClient;

    @Mock
    private ListImagesCmd listImagesCmd;

    @Mock
    private PullImageCmd pullImageCmd;

    @Mock
    private PushImageCmd pushImageCmd;

    @Mock
    private TagImageCmd tagImageCmd;

    @Mock
    private RemoveImageCmd removeImageCmd;

    @Mock
    private InspectImageCmd historyCmd;

    @Mock
    private SearchImagesCmd searchImagesCmd;

    @Mock
    private PullImageResultCallback pullImageResultCallback;

    @InjectMocks
    private DockerImageServiceImpl dockerImageService;

    private Image image1;
    private Image image2;

    @BeforeEach
    void setUp() {
        // Setup test data
        image1 = mock(Image.class);
        when(image1.getId()).thenReturn("image1_id");
        when(image1.getParentId()).thenReturn("parent_id");
        when(image1.getRepoTags()).thenReturn(new String[]{"repo:tag1"});
        when(image1.getRepoDigests()).thenReturn(new String[]{"digest1"});
        when(image1.getCreated()).thenReturn(1000000L);
        when(image1.getSize()).thenReturn(1000L);
        when(image1.getVirtualSize()).thenReturn(2000L);
        when(image1.getLabels()).thenReturn(Collections.singletonMap("key", "value"));
        
        image2 = mock(Image.class);
        when(image2.getId()).thenReturn("image2_id");
        when(image2.getRepoTags()).thenReturn(new String[]{"repo:tag2"});
        when(image2.getCreated()).thenReturn(2000000L);
    }

    @Test
    void getImages_ReturnsListOfImages() {
        // Arrange
        when(dockerClient.listImagesCmd()).thenReturn(listImagesCmd);
        when(listImagesCmd.withShowAll(anyBoolean())).thenReturn(listImagesCmd);
        when(listImagesCmd.exec()).thenReturn(Arrays.asList(image1, image2));

        // Act
        List<ImageInfo> result = dockerImageService.getImages(true);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("image1_id", result.get(0).getId());
        assertEquals("image2_id", result.get(1).getId());
        verify(listImagesCmd).withShowAll(true);
    }

    @Test
    void getImages_ExceptionThrown_ReturnsEmptyList() {
        // Arrange
        when(dockerClient.listImagesCmd()).thenReturn(listImagesCmd);
        when(listImagesCmd.withShowAll(anyBoolean())).thenReturn(listImagesCmd);
        when(listImagesCmd.exec()).thenThrow(new DockerException("Error", 500));

        // Act
        List<ImageInfo> result = dockerImageService.getImages(true);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getImage_ImageExists_ReturnsImageInfo() {
        // Arrange
        when(dockerClient.listImagesCmd()).thenReturn(listImagesCmd);
        when(listImagesCmd.withImageNameFilter(anyString())).thenReturn(listImagesCmd);
        when(listImagesCmd.exec()).thenReturn(Collections.singletonList(image1));

        // Act
        ImageInfo result = dockerImageService.getImage("image1");

        // Assert
        assertNotNull(result);
        assertEquals("image1_id", result.getId());
        assertEquals("parent_id", result.getParentId());
        assertEquals(1, result.getRepoTags().size());
        assertEquals("repo:tag1", result.getRepoTags().get(0));
    }

    @Test
    void getImage_ImageDoesNotExist_ReturnsNull() {
        // Arrange
        when(dockerClient.listImagesCmd()).thenReturn(listImagesCmd);
        when(listImagesCmd.withImageNameFilter(anyString())).thenReturn(listImagesCmd);
        when(listImagesCmd.exec()).thenReturn(Collections.emptyList());

        // Act
        ImageInfo result = dockerImageService.getImage("nonexistent");

        // Assert
        assertNull(result);
    }

    @Test
    void tagImage_Success_ReturnsTrue() {
        // Arrange
        when(dockerClient.tagImageCmd(anyString(), anyString(), anyString())).thenReturn(tagImageCmd);
        doNothing().when(tagImageCmd).exec();

        // Act
        boolean result = dockerImageService.tagImage("image1", "repo", "tag");

        // Assert
        assertTrue(result);
        verify(dockerClient).tagImageCmd("image1", "repo", "tag");
    }

    @Test
    void tagImage_ExceptionThrown_ReturnsFalse() {
        // Arrange
        when(dockerClient.tagImageCmd(anyString(), anyString(), anyString())).thenReturn(tagImageCmd);
        doThrow(new DockerException("Error", 500)).when(tagImageCmd).exec();

        // Act
        boolean result = dockerImageService.tagImage("image1", "repo", "tag");

        // Assert
        assertFalse(result);
    }

    @Test
    void removeImage_Success_ReturnsTrue() {
        // Arrange
        when(dockerClient.removeImageCmd(anyString())).thenReturn(removeImageCmd);
        when(removeImageCmd.withForce(anyBoolean())).thenReturn(removeImageCmd);
        when(removeImageCmd.withNoPrune(anyBoolean())).thenReturn(removeImageCmd);
        doNothing().when(removeImageCmd).exec();

        // Act
        boolean result = dockerImageService.removeImage("image1", true, false);

        // Assert
        assertTrue(result);
        verify(dockerClient).removeImageCmd("image1");
        verify(removeImageCmd).withForce(true);
        verify(removeImageCmd).withNoPrune(false);
    }

    @Test
    void removeImage_ExceptionThrown_ReturnsFalse() {
        // Arrange
        when(dockerClient.removeImageCmd(anyString())).thenReturn(removeImageCmd);
        when(removeImageCmd.withForce(anyBoolean())).thenReturn(removeImageCmd);
        when(removeImageCmd.withNoPrune(anyBoolean())).thenReturn(removeImageCmd);
        doThrow(new DockerException("Error", 500)).when(removeImageCmd).exec();

        // Act
        boolean result = dockerImageService.removeImage("image1", true, false);

        // Assert
        assertFalse(result);
    }

    @Test
    void searchImages_ReturnsSearchResults() {
        // Arrange
        SearchItem searchItem1 = mock(SearchItem.class);
        when(searchItem1.getName()).thenReturn("image1");
        when(searchItem1.getDescription()).thenReturn("description1");
        when(searchItem1.isOfficial()).thenReturn(true);
        when(searchItem1.getStarCount()).thenReturn(100);
        
        SearchItem searchItem2 = mock(SearchItem.class);
        when(searchItem2.getName()).thenReturn("image2");
        
        when(dockerClient.searchImagesCmd(anyString())).thenReturn(searchImagesCmd);
        when(searchImagesCmd.withLimit(anyInt())).thenReturn(searchImagesCmd);
        when(searchImagesCmd.exec()).thenReturn(Arrays.asList(searchItem1, searchItem2));

        // Act
        List<Map<String, Object>> result = dockerImageService.searchImages("test", 10);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("image1", result.get(0).get("name"));
        assertEquals("description1", result.get(0).get("description"));
        assertEquals(true, result.get(0).get("official"));
        assertEquals(100, result.get(0).get("starCount"));
        assertEquals("image2", result.get(1).get("name"));
        verify(searchImagesCmd).withLimit(10);
    }

    @Test
    void searchImages_ExceptionThrown_ReturnsEmptyList() {
        // Arrange
        when(dockerClient.searchImagesCmd(anyString())).thenReturn(searchImagesCmd);
        when(searchImagesCmd.withLimit(anyInt())).thenReturn(searchImagesCmd);
        when(searchImagesCmd.exec()).thenThrow(new DockerException("Error", 500));

        // Act
        List<Map<String, Object>> result = dockerImageService.searchImages("test", 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

