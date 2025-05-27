package com.codebridge.webhook.service;

import com.codebridge.core.security.SecretStorageService;
import com.codebridge.webhook.model.Webhook;
import com.codebridge.webhook.model.WebhookEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Service for sending webhook events to external services.
 */
@Service
public class WebhookSenderService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookSenderService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    private static final String EVENT_TYPE_HEADER = "X-Webhook-Event";
    private static final String EVENT_ID_HEADER = "X-Webhook-ID";

    private final WebClient.Builder webClientBuilder;
    private final SecretStorageService secretStorageService;
    private final ObjectMapper objectMapper;

    public WebhookSenderService(WebClient.Builder webClientBuilder,
                               SecretStorageService secretStorageService,
                               ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.secretStorageService = secretStorageService;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a webhook event to the configured URL.
     *
     * @param webhook the webhook configuration
     * @param event the webhook event
     * @return true if the event was sent successfully, false otherwise
     */
    public boolean sendWebhookEvent(Webhook webhook, WebhookEvent event) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(webhook.getUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(EVENT_TYPE_HEADER, event.getEventType())
                    .defaultHeader(EVENT_ID_HEADER, event.getId().toString())
                    .build();
            
            // Add custom headers if configured
            Map<String, String> customHeaders = parseCustomHeaders(webhook.getHeaders());
            
            // Add signature header if secret is configured
            String signature = null;
            if (webhook.getSecret() != null && !webhook.getSecret().isEmpty()) {
                String secret = secretStorageService.decryptSecret(webhook.getSecret());
                signature = generateSignature(event.getPayload(), secret);
            }
            
            // Build the request
            WebClient.RequestHeadersSpec<?> requestSpec = webClient
                    .post()
                    .bodyValue(event.getPayload());
            
            // Add custom headers
            if (customHeaders != null) {
                for (Map.Entry<String, String> header : customHeaders.entrySet()) {
                    requestSpec = requestSpec.header(header.getKey(), header.getValue());
                }
            }
            
            // Add signature header
            if (signature != null) {
                requestSpec = requestSpec.header(SIGNATURE_HEADER, signature);
            }
            
            // Send the request
            return requestSpec
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(webhook.getTimeoutSeconds()))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(10)))
                    .onErrorResume(e -> {
                        logger.error("Error sending webhook event: {}", event.getId(), e);
                        return Mono.empty();
                    })
                    .map(response -> true)
                    .defaultIfEmpty(false)
                    .block();
        } catch (Exception e) {
            logger.error("Error sending webhook event: {}", event.getId(), e);
            return false;
        }
    }

    /**
     * Parses custom headers from a JSON string.
     *
     * @param headersJson the JSON string containing headers
     * @return map of headers
     */
    private Map<String, String> parseCustomHeaders(String headersJson) {
        if (headersJson == null || headersJson.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(headersJson, Map.class);
        } catch (Exception e) {
            logger.error("Error parsing custom headers", e);
            return null;
        }
    }

    /**
     * Generates a signature for a payload using HMAC-SHA256.
     *
     * @param payload the payload
     * @param secret the secret
     * @return the signature
     */
    private String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            logger.error("Error generating signature", e);
            return null;
        }
    }
}

