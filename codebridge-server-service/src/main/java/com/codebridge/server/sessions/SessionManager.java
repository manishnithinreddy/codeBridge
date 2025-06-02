package com.codebridge.server.sessions;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A generic interface for managing sessions.
 *
 * @param <W> The type of session wrapper being managed (e.g., SshSessionWrapper).
 */
public interface SessionManager<W> {

    /**
     * Retrieves an active session by its key.
     * The implementation should ensure that the returned session is valid and connected.
     *
     * @param key The SessionKey to look up.
     * @return An Optional containing the session wrapper if found and valid, otherwise Optional.empty().
     */
    Optional<W> getSession(SessionKey key);

    /**
     * Creates and stores a new session using the provided factory.
     * The factory is responsible for the actual session establishment (e.g., network connection).
     *
     * @param key The SessionKey for the new session.
     * @param sessionFactory A Supplier that produces a new session wrapper instance (W).
     * @return The newly created and stored session wrapper.
     * @throws Exception if the sessionFactory fails to create a session.
     */
    W createSession(SessionKey key, Supplier<W> sessionFactory) throws Exception;

    /**
     * Stores or updates a session in the manager.
     * This might be used, for example, after a session's lastAccessedTime has been updated.
     *
     * @param key The SessionKey for the session.
     * @param sessionWrapper The session wrapper to store.
     */
    void storeSession(SessionKey key, W sessionWrapper);

    /**
     * Removes a session from the manager and ensures its underlying resources are released.
     * Implementations should call a method like disconnect() or close() on the session wrapper.
     *
     * @param key The SessionKey of the session to release.
     * @return true if a session was found and released, false otherwise.
     */
    boolean releaseSession(SessionKey key);

    /**
     * Iterates through all managed sessions and removes any that have expired
     * based on their last accessed time and a configured timeout policy.
     * This method is typically called periodically by a background task.
     */
    void cleanupExpiredSessions();
}
