package com.codebridge.common.context;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared context for cross-service communication.
 * Provides a way to share data between services during a request.
 */
public class SharedContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private final Map<String, Object> attributes;
    private final Map<String, String> metadata;

    /**
     * Creates a new shared context.
     */
    public SharedContext() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusHours(1); // Default expiration: 1 hour
        this.attributes = new HashMap<>();
        this.metadata = new HashMap<>();
    }

    /**
     * Gets the context ID.
     *
     * @return The ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the creation time.
     *
     * @return The creation time
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the expiration time.
     *
     * @return The expiration time
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * Sets the expiration time.
     *
     * @param expiresAt The expiration time
     */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Checks if the context has expired.
     *
     * @return True if the context has expired, false otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Gets an attribute from the context.
     *
     * @param key The attribute key
     * @return The attribute value, or null if not found
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Gets an attribute from the context with a specific type.
     *
     * @param key The attribute key
     * @param type The attribute type
     * @param <T> The attribute type
     * @return The attribute value, or null if not found or not of the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Sets an attribute in the context.
     *
     * @param key The attribute key
     * @param value The attribute value
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Removes an attribute from the context.
     *
     * @param key The attribute key
     * @return The removed attribute value, or null if not found
     */
    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }

    /**
     * Gets all attributes from the context.
     *
     * @return The attributes
     */
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }

    /**
     * Gets a metadata value from the context.
     *
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Sets a metadata value in the context.
     *
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadata(String key, String value) {
        metadata.put(key, value);
    }

    /**
     * Removes a metadata value from the context.
     *
     * @param key The metadata key
     * @return The removed metadata value, or null if not found
     */
    public String removeMetadata(String key) {
        return metadata.remove(key);
    }

    /**
     * Gets all metadata from the context.
     *
     * @return The metadata
     */
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }

    /**
     * Clears all attributes and metadata from the context.
     */
    public void clear() {
        attributes.clear();
        metadata.clear();
    }
}

