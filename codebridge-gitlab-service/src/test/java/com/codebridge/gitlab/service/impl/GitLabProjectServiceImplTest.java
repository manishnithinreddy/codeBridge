package com.codebridge.gitlab.service.impl;

import com.codebridge.gitlab.model.GitLabProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitLabProjectServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GitLabProjectServiceImpl gitLabProjectService;

    private GitLabProject project1;
    private GitLabProject project2;

    @BeforeEach
    void setUp() {
        // Setup test data
        project1 = new GitLabProject();
        project1.setId(1);
        project1.setName("Project 1");
        project1.setDescription("Description 1");
        
        project2 = new GitLabProject();
        project2.setId(2);
        project2.setName("Project 2");
        project2.setDescription("Description 2");
    }

    @Test
    void getProjects_ReturnsListOfProjects() {
        // Arrange
        List<GitLabProject> projects = Arrays.asList(project1, project2);
        ResponseEntity<List<GitLabProject>> responseEntity = new ResponseEntity<>(projects, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // Act
        List<GitLabProject> result = gitLabProjectService.getProjects("token", null, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals("Project 1", result.get(0).getName());
        assertEquals(2, result.get(1).getId());
        assertEquals("Project 2", result.get(1).getName());
    }

    @Test
    void getProject_ReturnsProject() {
        // Arrange
        ResponseEntity<GitLabProject> responseEntity = new ResponseEntity<>(project1, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(GitLabProject.class)))
                .thenReturn(responseEntity);

        // Act
        GitLabProject result = gitLabProjectService.getProject("token", 1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Project 1", result.getName());
        assertEquals("Description 1", result.getDescription());
    }

    @Test
    void createProject_ReturnsCreatedProject() {
        // Arrange
        Map<String, Object> projectParams = new HashMap<>();
        projectParams.put("name", "New Project");
        projectParams.put("description", "New Description");
        
        ResponseEntity<GitLabProject> responseEntity = new ResponseEntity<>(project1, HttpStatus.CREATED);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GitLabProject.class)))
                .thenReturn(responseEntity);

        // Act
        GitLabProject result = gitLabProjectService.createProject("token", projectParams);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Project 1", result.getName());
    }

    @Test
    void archiveProject_ReturnsArchivedProject() {
        // Arrange
        ResponseEntity<GitLabProject> responseEntity = new ResponseEntity<>(project1, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GitLabProject.class)))
                .thenReturn(responseEntity);

        // Act
        GitLabProject result = gitLabProjectService.archiveProject("token", 1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Project 1", result.getName());
    }

    @Test
    void unarchiveProject_ReturnsUnarchivedProject() {
        // Arrange
        ResponseEntity<GitLabProject> responseEntity = new ResponseEntity<>(project1, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GitLabProject.class)))
                .thenReturn(responseEntity);

        // Act
        GitLabProject result = gitLabProjectService.unarchiveProject("token", 1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Project 1", result.getName());
    }
}

