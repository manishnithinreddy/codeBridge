package com.codebridge.aidb.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = {
    "codebridge.ai.text-to-sql.endpoint-url=https://test-ai-url.com/api",
    "codebridge.ai.text-to-sql.api-key=test-api-key"
})
class AiConfigPropertiesTests {

    @Autowired
    private AiConfigProperties aiConfigProperties;

    @Test
    void whenPropertiesSet_thenLoadsCorrectly() {
        assertEquals("https://test-ai-url.com/api", aiConfigProperties.getEndpointUrl());
        assertEquals("test-api-key", aiConfigProperties.getApiKey());
    }
}
