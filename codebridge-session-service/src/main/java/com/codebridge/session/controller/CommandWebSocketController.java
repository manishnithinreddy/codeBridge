package com.codebridge.session.controller;

import com.codebridge.session.dto.ops.CommandOutputMessage;
import com.codebridge.session.dto.ops.CommandRequest;
import com.codebridge.session.dto.ops.CommandResponse;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.service.command.SshCommandQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * WebSocket controller for handling command execution with real-time output streaming.
 */
@Controller
public class CommandWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(CommandWebSocketController.class);

    private final SshCommandQueue commandQueue;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public CommandWebSocketController(SshCommandQueue commandQueue, SimpMessagingTemplate messagingTemplate) {
        this.commandQueue = commandQueue;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handles command execution requests via WebSocket.
     * Streams the command output back to the client in real-time.
     *
     * @param sessionId the SSH session ID
     * @param command the command request
     * @param authentication the authenticated user
     */
    @MessageMapping("/command/{sessionId}")
    public void executeCommand(
            @DestinationVariable UUID sessionId,
            @Payload CommandRequest command,
            Authentication authentication) {
        
        logger.info("Received streaming command request for session {}: {}", sessionId, command.getCommand());
        
        // Generate a command ID if not provided
        if (command.getCommandId() == null || command.getCommandId().isEmpty()) {
            command.setCommandId(UUID.randomUUID().toString());
        }
        
        // Create a session key from the session ID and user ID
        SessionKey sessionKey = new SessionKey(sessionId, UUID.fromString(authentication.getName()));
        
        // Create the destination for sending output messages
        String destination = "/queue/command/" + sessionId + "/" + command.getCommandId();
        
        // Submit the command for streaming execution
        CompletableFuture<CommandResponse> future = commandQueue.submitStreamingCommand(
                sessionKey,
                command,
                outputMessage -> sendOutputMessage(destination, outputMessage)
        );
        
        // Handle the final response
        future.whenComplete((response, error) -> {
            if (error != null) {
                logger.error("Error executing command: {}", error.getMessage(), error);
                
                // Send error message
                CommandOutputMessage errorMessage = CommandOutputMessage.createErrorMessage(
                        sessionId,
                        UUID.fromString(command.getCommandId()),
                        error.getMessage()
                );
                sendOutputMessage(destination, errorMessage);
            } else {
                logger.info("Command execution completed for session {}: {}", sessionId, command.getCommand());
                
                // Final response is already sent via the output handler
            }
        });
    }
    
    /**
     * Handles command cancellation requests.
     *
     * @param sessionId the SSH session ID
     * @param commandId the command ID to cancel
     * @param authentication the authenticated user
     * @return true if the command was cancelled, false otherwise
     */
    @MessageMapping("/command/{sessionId}/cancel/{commandId}")
    public boolean cancelCommand(
            @DestinationVariable UUID sessionId,
            @DestinationVariable String commandId,
            Authentication authentication) {
        
        logger.info("Received cancel command request for session {}, command {}", sessionId, commandId);
        
        // Create a session key from the session ID and user ID
        SessionKey sessionKey = new SessionKey(sessionId, UUID.fromString(authentication.getName()));
        
        // Cancel the command
        return commandQueue.cancelCommand(sessionKey, commandId);
    }
    
    /**
     * Sends a command output message to the client.
     *
     * @param destination the destination topic
     * @param message the output message
     */
    private void sendOutputMessage(String destination, CommandOutputMessage message) {
        messagingTemplate.convertAndSend(destination, message);
    }
}

