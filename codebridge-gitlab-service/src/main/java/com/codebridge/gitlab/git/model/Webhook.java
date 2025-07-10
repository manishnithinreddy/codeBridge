package com.codebridge.gitlab.git.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "webhooks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Webhook extends BaseEntity {

    public enum WebhookStatus {
        ACTIVE,
        INACTIVE,
        ERROR
    }

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "remote_id")
    private String remoteId;

    @Column(name = "payload_url", nullable = false)
    private String payloadUrl;

    @Column(name = "secret_token")
    private String secretToken;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "events", nullable = false, columnDefinition = "TEXT")
    private String events;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookStatus status;

    @Column(name = "last_triggered_at")
    private java.time.LocalDateTime lastTriggeredAt;

    @Column(name = "last_response_code")
    private Integer lastResponseCode;

    @Column(name = "last_response_message", columnDefinition = "TEXT")
    private String lastResponseMessage;

    @Column(name = "failure_count")
    private Integer failureCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;
}
