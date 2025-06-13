package com.codebridge.scalability.service;

import java.util.Optional;

/**
 * Service for managing idempotency keys to ensure operations are only executed once.
 */
public interface IdempotencyService {

    /**
     * Records a new idempotency key with the given result.
     *
     * @param key    the idempotency key
     * @param result the result to store
     * @param <T>    the type of the result
     * @return true if the key was recorded, false if it already exists
     */
    <T> boolean recordKey(String key, T result);

    /**
     * Gets the result associated with an idempotency key.
     *
     * @param key the idempotency key
     * @param <T> the type of the result
     * @return the result, or empty if the key doesn't exist
     */
    <T> Optional<T> getResult(String key, Class<T> resultClass);

    /**
     * Checks if an idempotency key exists.
     *
     * @param key the idempotency key
     * @return true if the key exists, false otherwise
     */
    boolean exists(String key);

    /**
     * Removes an idempotency key.
     *
     * @param key the idempotency key
     */
    void removeKey(String key);
}

