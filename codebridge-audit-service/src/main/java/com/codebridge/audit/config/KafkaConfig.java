package com.codebridge.audit.config;

import com.codebridge.audit.dto.AuditEventDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.function.Consumer;

/**
 * Configuration for Kafka message processing.
 */
@Configuration
public class KafkaConfig {

    private final com.codebridge.audit.service.AuditService auditService;

    public KafkaConfig(com.codebridge.audit.service.AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Consumer function for processing audit events from Kafka.
     *
     * @return a consumer function that processes audit events
     */
    @Bean
    public Consumer<Message<AuditEventDto>> auditEventConsumer() {
        return message -> {
            AuditEventDto auditEventDto = message.getPayload();
            auditService.logAuditEvent(auditEventDto);
        };
    }

    /**
     * Helper method to create a message from an audit event.
     *
     * @param auditEventDto the audit event data
     * @return a message containing the audit event
     */
    public static Message<AuditEventDto> createAuditEventMessage(AuditEventDto auditEventDto) {
        return MessageBuilder.withPayload(auditEventDto).build();
    }
}

