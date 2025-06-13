package com.codebridge.apitest.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for mapping tests to collections.
 * Includes order and test-specific scripts.
 */
@Entity
@Table(name = "collection_tests")
public class CollectionTest {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID collectionId;

    @Column(nullable = false)
    private UUID testId;

    @Column(name = "test_order", nullable = false)
    private int testOrder;

    @Column
    @Lob
    private String preRequestScript;

    @Column
    @Lob
    private String postRequestScript;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(UUID collectionId) {
        this.collectionId = collectionId;
    }

    public UUID getTestId() {
        return testId;
    }

    public void setTestId(UUID testId) {
        this.testId = testId;
    }

    public int getTestOrder() {
        return testOrder;
    }

    public void setTestOrder(int testOrder) {
        this.testOrder = testOrder;
    }

    // For backward compatibility
    public int getOrder() {
        return testOrder;
    }

    // For backward compatibility
    public void setOrder(int order) {
        this.testOrder = order;
    }

    public String getPreRequestScript() {
        return preRequestScript;
    }

    public void setPreRequestScript(String preRequestScript) {
        this.preRequestScript = preRequestScript;
    }

    public String getPostRequestScript() {
        return postRequestScript;
    }

    public void setPostRequestScript(String postRequestScript) {
        this.postRequestScript = postRequestScript;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

