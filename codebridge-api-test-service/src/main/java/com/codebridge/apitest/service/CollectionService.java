package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.ApiTestResponse;
import com.codebridge.apitest.dto.CollectionRequest;
import com.codebridge.apitest.dto.CollectionResponse;
import com.codebridge.apitest.dto.CollectionTestRequest;
import com.codebridge.apitest.dto.CollectionTestResponse;
import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.exception.ResourceNotFoundException;
import com.codebridge.apitest.model.ApiTest;
import com.codebridge.apitest.model.Collection;
import com.codebridge.apitest.model.CollectionTest;
import com.codebridge.apitest.repository.ApiTestRepository;
import com.codebridge.apitest.repository.CollectionRepository;
import com.codebridge.apitest.repository.CollectionTestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for collection operations.
 */
@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionTestRepository collectionTestRepository;
    private final ApiTestRepository apiTestRepository;
    private final ApiTestService apiTestService;
    private final ObjectMapper objectMapper;

    public CollectionService(
            CollectionRepository collectionRepository,
            CollectionTestRepository collectionTestRepository,
            ApiTestRepository apiTestRepository,
            ApiTestService apiTestService,
            ObjectMapper objectMapper) {
        this.collectionRepository = collectionRepository;
        this.collectionTestRepository = collectionTestRepository;
        this.apiTestRepository = apiTestRepository;
        this.apiTestService = apiTestService;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new collection.
     *
     * @param request the collection request
     * @param userId the user ID
     * @return the created collection
     */
    @Transactional
    public CollectionResponse createCollection(CollectionRequest request, UUID userId) {
        Collection collection = new Collection();
        collection.setId(UUID.randomUUID());
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        collection.setUserId(userId);
        
        try {
            if (request.getVariables() != null) {
                collection.setVariables(objectMapper.writeValueAsString(request.getVariables()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing variables JSON", e);
        }
        
        collection.setPreRequestScript(request.getPreRequestScript());
        collection.setPostRequestScript(request.getPostRequestScript());
        collection.setShared(request.isShared());
        
        Collection savedCollection = collectionRepository.save(collection);
        return mapToCollectionResponse(savedCollection, new ArrayList<>());
    }

    /**
     * Get all collections for a user.
     *
     * @param userId the user ID
     * @return list of collections
     */
    public List<CollectionResponse> getAllCollections(UUID userId) {
        List<Collection> collections = collectionRepository.findByUserId(userId);
        return collections.stream()
                .map(collection -> {
                    List<CollectionTest> tests = collectionTestRepository.findByCollectionIdOrderByOrder(collection.getId());
                    return mapToCollectionResponse(collection, tests);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get collection by ID.
     *
     * @param id the collection ID
     * @param userId the user ID
     * @return the collection
     */
    public CollectionResponse getCollectionById(UUID id, UUID userId) {
        Collection collection = collectionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", "id", id));
        
        List<CollectionTest> tests = collectionTestRepository.findByCollectionIdOrderByOrder(id);
        return mapToCollectionResponse(collection, tests);
    }

    /**
     * Update a collection.
     *
     * @param id the collection ID
     * @param request the collection request
     * @param userId the user ID
     * @return the updated collection
     */
    @Transactional
    public CollectionResponse updateCollection(UUID id, CollectionRequest request, UUID userId) {
        Collection collection = collectionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", "id", id));
        
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        
        try {
            if (request.getVariables() != null) {
                collection.setVariables(objectMapper.writeValueAsString(request.getVariables()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing variables JSON", e);
        }
        
        collection.setPreRequestScript(request.getPreRequestScript());
        collection.setPostRequestScript(request.getPostRequestScript());
        collection.setShared(request.isShared());
        
        Collection updatedCollection = collectionRepository.save(collection);
        List<CollectionTest> tests = collectionTestRepository.findByCollectionIdOrderByOrder(id);
        return mapToCollectionResponse(updatedCollection, tests);
    }

    /**
     * Delete a collection.
     *
     * @param id the collection ID
     * @param userId the user ID
     */
    @Transactional
    public void deleteCollection(UUID id, UUID userId) {
        Collection collection = collectionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", "id", id));
        
        // Delete all associated collection tests first
        collectionTestRepository.deleteByCollectionId(id);
        
        // Then delete the collection
        collectionRepository.delete(collection);
    }

    /**
     * Add a test to a collection.
     *
     * @param collectionId the collection ID
     * @param request the collection test request
     * @param userId the user ID
     * @return the updated collection
     */
    @Transactional
    public CollectionResponse addTestToCollection(UUID collectionId, CollectionTestRequest request, UUID userId) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", "id", collectionId));
        
        ApiTest apiTest = apiTestRepository.findByIdAndUserId(request.getTestId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiTest", "id", request.getTestId()));
        
        // Check if test is already in collection
        collectionTestRepository.findByCollectionIdAndTestId(collectionId, request.getTestId())
                .ifPresent(existingTest -> {
                    throw new IllegalStateException("Test is already in this collection");
                });
        
        CollectionTest collectionTest = new CollectionTest();
        collectionTest.setId(UUID.randomUUID());
        collectionTest.setCollectionId(collectionId);
        collectionTest.setTestId(request.getTestId());
        collectionTest.setOrder(request.getOrder());
        collectionTest.setPreRequestScript(request.getPreRequestScript());
        collectionTest.setPostRequestScript(request.getPostRequestScript());
        collectionTest.setEnabled(request.isEnabled());
        
        collectionTestRepository.save(collectionTest);
        
        List<CollectionTest> tests = collectionTestRepository.findByCollectionIdOrderByOrder(collectionId);
        return mapToCollectionResponse(collection, tests);
    }

    /**
     * Remove a test from a collection.
     *
     * @param collectionId the collection ID
     * @param testId the test ID
     * @param userId the user ID
     * @return the updated collection
     */
    @Transactional
    public CollectionResponse removeTestFromCollection(UUID collectionId, UUID testId, UUID userId) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", "id", collectionId));
        
        collectionTestRepository.findByCollectionIdAndTestId(collectionId, testId)
                .orElseThrow(() -> new ResourceNotFoundException("CollectionTest", "testId", testId));
        
        collectionTestRepository.deleteByCollectionIdAndTestId(collectionId, testId);
        
        List<CollectionTest> tests = collectionTestRepository.findByCollectionIdOrderByOrder(collectionId);
        return mapToCollectionResponse(collection, tests);
    }

    /**
     * Execute all tests in a collection.
     *
     * @param id the collection ID
     * @param userId the user ID
     * @return list of test results
     */
    @Transactional
    public List<TestResultResponse> executeCollection(UUID id, UUID userId) {
        Collection collection = collectionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", "id", id));
        
        List<CollectionTest> tests = collectionTestRepository.findByCollectionIdOrderByOrder(id);
        
        List<TestResultResponse> results = new ArrayList<>();
        
        // Get collection variables
        Map<String, String> collectionVariables = null;
        if (collection.getVariables() != null) {
            try {
                collectionVariables = objectMapper.readValue(collection.getVariables(), 
                        new TypeReference<Map<String, String>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing variables JSON", e);
            }
        }
        
        // Execute pre-request script for collection (future implementation)
        
        // Execute each test in order
        for (CollectionTest test : tests) {
            if (test.isEnabled()) {
                // Execute pre-request script for test (future implementation)
                
                // Execute the test
                TestResultResponse result = apiTestService.executeTest(test.getTestId(), userId);
                results.add(result);
                
                // Execute post-request script for test (future implementation)
            }
        }
        
        // Execute post-request script for collection (future implementation)
        
        return results;
    }

    /**
     * Maps a Collection entity to a CollectionResponse DTO.
     *
     * @param collection the collection entity
     * @param collectionTests the collection tests
     * @return the collection response DTO
     */
    private CollectionResponse mapToCollectionResponse(Collection collection, List<CollectionTest> collectionTests) {
        CollectionResponse response = new CollectionResponse();
        response.setId(collection.getId());
        response.setName(collection.getName());
        response.setDescription(collection.getDescription());
        
        if (collection.getVariables() != null) {
            try {
                response.setVariables(objectMapper.readValue(collection.getVariables(), 
                        new TypeReference<Map<String, String>>() {}));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error processing variables JSON", e);
            }
        }
        
        response.setPreRequestScript(collection.getPreRequestScript());
        response.setPostRequestScript(collection.getPostRequestScript());
        response.setShared(collection.isShared());
        response.setCreatedAt(collection.getCreatedAt());
        response.setUpdatedAt(collection.getUpdatedAt());
        
        // Map collection tests
        if (!collectionTests.isEmpty()) {
            List<CollectionTestResponse> testResponses = collectionTests.stream()
                    .map(this::mapToCollectionTestResponse)
                    .collect(Collectors.toList());
            response.setTests(testResponses);
        }
        
        return response;
    }

    /**
     * Maps a CollectionTest entity to a CollectionTestResponse DTO.
     *
     * @param collectionTest the collection test entity
     * @return the collection test response DTO
     */
    private CollectionTestResponse mapToCollectionTestResponse(CollectionTest collectionTest) {
        CollectionTestResponse response = new CollectionTestResponse();
        response.setId(collectionTest.getId());
        response.setTestId(collectionTest.getTestId());
        response.setOrder(collectionTest.getOrder());
        response.setPreRequestScript(collectionTest.getPreRequestScript());
        response.setPostRequestScript(collectionTest.getPostRequestScript());
        response.setEnabled(collectionTest.isEnabled());
        response.setCreatedAt(collectionTest.getCreatedAt());
        response.setUpdatedAt(collectionTest.getUpdatedAt());
        
        // Load the associated API test
        apiTestRepository.findById(collectionTest.getTestId()).ifPresent(apiTest -> {
            ApiTestResponse testResponse = apiTestService.mapToApiTestResponse(apiTest);
            response.setTest(testResponse);
        });
        
        return response;
    }
}

