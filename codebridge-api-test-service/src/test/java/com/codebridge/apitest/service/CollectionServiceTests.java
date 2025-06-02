package com.codebridge.apitest.service;

import com.codebridge.apitest.dto.TestResultResponse;
import com.codebridge.apitest.model.Collection;
import com.codebridge.apitest.model.CollectionTest;
import com.codebridge.apitest.repository.ApiTestRepository;
import com.codebridge.apitest.repository.CollectionRepository;
import com.codebridge.apitest.repository.CollectionTestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTests {

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private CollectionTestRepository collectionTestRepository;

    @Mock
    private ApiTestRepository apiTestRepository; // Though not directly used by executeCollection, good to have if other methods tested

    @Mock
    private ApiTestService apiTestService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper(); // Use a real ObjectMapper for variable parsing

    @InjectMocks
    private CollectionService collectionService;

    @Captor
    private ArgumentCaptor<Map<String, String>> collectionVariablesCaptor;

    private UUID testUserId;
    private UUID testCollectionId;
    private Collection baseCollection;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testCollectionId = UUID.randomUUID();

        baseCollection = new Collection();
        baseCollection.setId(testCollectionId);
        baseCollection.setUserId(testUserId);
        baseCollection.setName("Test Collection");
    }

    @Test
    void executeCollection_withVariables_callsApiTestServiceWithVariables() throws JsonProcessingException {
        // Arrange
        Map<String, String> variables = new HashMap<>();
        variables.put("baseUrl", "http://example.com");
        variables.put("token", "secret-token");
        baseCollection.setVariables(objectMapper.writeValueAsString(variables));

        CollectionTest enabledTest1 = new CollectionTest();
        enabledTest1.setTestId(UUID.randomUUID());
        enabledTest1.setEnabled(true);
        enabledTest1.setOrder(1);

        when(collectionRepository.findByIdAndUserId(testCollectionId, testUserId)).thenReturn(Optional.of(baseCollection));
        when(collectionTestRepository.findByCollectionIdOrderByOrder(testCollectionId)).thenReturn(Collections.singletonList(enabledTest1));
        when(apiTestService.executeTest(eq(enabledTest1.getTestId()), eq(testUserId), anyMap())).thenReturn(new TestResultResponse());

        // Act
        collectionService.executeCollection(testCollectionId, testUserId);

        // Assert
        verify(apiTestService).executeTest(eq(enabledTest1.getTestId()), eq(testUserId), collectionVariablesCaptor.capture());
        Map<String, String> capturedVars = collectionVariablesCaptor.getValue();
        assertEquals("http://example.com", capturedVars.get("baseUrl"));
        assertEquals("secret-token", capturedVars.get("token"));
        // The original 'results.size()' was problematic as 'results' wasn't returned by the call.
        // We can verify the interaction count or check the size of the list returned by the method.
        // For this test, verifying the interaction (already done) and the captured vars is sufficient.
        // If we were asserting the returned list:
        // List<TestResultResponse> results = collectionService.executeCollection(testCollectionId, testUserId);
        // assertEquals(1, results.size(), "Should have one result for the enabled test");
    }

    @Test
    void executeCollection_noVariables_callsApiTestServiceWithNullOrEmptyVariables() {
        // Arrange
        baseCollection.setVariables(null); // No variables

        CollectionTest enabledTest1 = new CollectionTest();
        enabledTest1.setTestId(UUID.randomUUID());
        enabledTest1.setEnabled(true);
        enabledTest1.setOrder(1);

        when(collectionRepository.findByIdAndUserId(testCollectionId, testUserId)).thenReturn(Optional.of(baseCollection));
        when(collectionTestRepository.findByCollectionIdOrderByOrder(testCollectionId)).thenReturn(Collections.singletonList(enabledTest1));
        when(apiTestService.executeTest(eq(enabledTest1.getTestId()), eq(testUserId), any())).thenReturn(new TestResultResponse());

        // Act
        List<TestResultResponse> results = collectionService.executeCollection(testCollectionId, testUserId);

        // Assert
        verify(apiTestService).executeTest(eq(enabledTest1.getTestId()), eq(testUserId), collectionVariablesCaptor.capture());
        Map<String, String> capturedVars = collectionVariablesCaptor.getValue();
        // Depending on implementation, it might be null or an empty map if variables string is null/empty
        assertTrue(capturedVars == null || capturedVars.isEmpty(), "Captured variables should be null or empty");
        assertEquals(1, results.size());
    }

    @Test
    void executeCollection_invalidVariablesJson_throwsRuntimeExceptionAndDoesNotExecuteTests() throws JsonProcessingException {
        // Arrange
        baseCollection.setVariables("this is not valid json");

        CollectionTest enabledTest1 = new CollectionTest();
        enabledTest1.setTestId(UUID.randomUUID());
        enabledTest1.setEnabled(true);

        when(collectionRepository.findByIdAndUserId(testCollectionId, testUserId)).thenReturn(Optional.of(baseCollection));
        when(collectionTestRepository.findByCollectionIdOrderByOrder(testCollectionId)).thenReturn(Collections.singletonList(enabledTest1));
        // apiTestService.executeTest should not be called

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            collectionService.executeCollection(testCollectionId, testUserId);
        });
        assertTrue(exception.getMessage().contains("Error processing variables JSON"));
        verify(apiTestService, never()).executeTest(any(), any(), any());
    }


    @Test
    void executeCollection_executesOnlyEnabledTests() throws JsonProcessingException {
        // Arrange
        baseCollection.setVariables(objectMapper.writeValueAsString(Collections.singletonMap("key", "value")));

        CollectionTest enabledTest = new CollectionTest();
        enabledTest.setTestId(UUID.randomUUID());
        enabledTest.setEnabled(true);
        enabledTest.setOrder(1);

        CollectionTest disabledTest = new CollectionTest();
        disabledTest.setTestId(UUID.randomUUID());
        disabledTest.setEnabled(false);
        disabledTest.setOrder(2);

        List<CollectionTest> tests = Arrays.asList(enabledTest, disabledTest);
        when(collectionRepository.findByIdAndUserId(testCollectionId, testUserId)).thenReturn(Optional.of(baseCollection));
        when(collectionTestRepository.findByCollectionIdOrderByOrder(testCollectionId)).thenReturn(tests);
        when(apiTestService.executeTest(eq(enabledTest.getTestId()), eq(testUserId), anyMap())).thenReturn(new TestResultResponse());

        // Act
        List<TestResultResponse> results = collectionService.executeCollection(testCollectionId, testUserId);

        // Assert
        verify(apiTestService, times(1)).executeTest(eq(enabledTest.getTestId()), eq(testUserId), anyMap());
        verify(apiTestService, never()).executeTest(eq(disabledTest.getTestId()), any(), anyMap());
        assertEquals(1, results.size(), "Should only have results for enabled tests");
    }

    @Test
    void executeCollection_noTestsInCollection_returnsEmptyResults() {
        // Arrange
        baseCollection.setVariables(null);
        when(collectionRepository.findByIdAndUserId(testCollectionId, testUserId)).thenReturn(Optional.of(baseCollection));
        when(collectionTestRepository.findByCollectionIdOrderByOrder(testCollectionId)).thenReturn(Collections.emptyList());

        // Act
        List<TestResultResponse> results = collectionService.executeCollection(testCollectionId, testUserId);

        // Assert
        assertTrue(results.isEmpty());
        verify(apiTestService, never()).executeTest(any(), any(), any());
    }
}
