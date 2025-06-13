package com.codebridge.session.service.command;

import com.codebridge.session.dto.ops.CommandOutputMessage;
import com.codebridge.session.dto.ops.CommandRequest;
import com.codebridge.session.dto.ops.CommandResponse;
import com.codebridge.session.model.SessionKey;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Interface for a command queue that can process SSH commands asynchronously.
 * This prepares the system for future scaling by allowing command execution
 * to be decoupled from the request handling.
 */
public interface SshCommandQueue {

    /**
     * Submits a command for execution and returns immediately.
     * The result will be delivered via the provided callback.
     *
     * @param sessionKey The session key identifying the SSH session
     * @param command The command to execute
     * @param callback The callback to invoke when the command completes
     */
    void submitCommand(SessionKey sessionKey, CommandRequest command, Consumer<CommandResponse> callback);

    /**
     * Submits a command for execution and returns a CompletableFuture that will
     * be completed when the command execution is done.
     *
     * @param sessionKey The session key identifying the SSH session
     * @param command The command to execute
     * @return A CompletableFuture that will be completed with the command response
     */
    CompletableFuture<CommandResponse> submitCommandAsync(SessionKey sessionKey, CommandRequest command);

    /**
     * Submits a command for streaming execution and returns immediately.
     * The output will be delivered incrementally via the provided output handler.
     *
     * @param sessionKey The session key identifying the SSH session
     * @param command The command to execute
     * @param outputHandler The handler to receive incremental command output
     * @return A CompletableFuture that will be completed with the final command response
     */
    CompletableFuture<CommandResponse> submitStreamingCommand(
            SessionKey sessionKey, 
            CommandRequest command, 
            Consumer<CommandOutputMessage> outputHandler);

    /**
     * Executes a command synchronously and returns the result.
     * This is a blocking operation and should be used with caution.
     *
     * @param sessionKey The session key identifying the SSH session
     * @param command The command to execute
     * @return The command response
     */
    CommandResponse executeCommandSync(SessionKey sessionKey, CommandRequest command);

    /**
     * Gets the current size of the command queue.
     *
     * @return The number of commands waiting to be executed
     */
    int getQueueSize();

    /**
     * Gets the number of commands currently being executed.
     *
     * @return The number of commands in execution
     */
    int getActiveCommandCount();

    /**
     * Cancels a running command.
     *
     * @param sessionKey The session key identifying the SSH session
     * @param commandId The ID of the command to cancel
     * @return true if the command was cancelled, false otherwise
     */
    boolean cancelCommand(SessionKey sessionKey, String commandId);
}
