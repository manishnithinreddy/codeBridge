package com.codebridge.session.service.command;

import com.codebridge.session.dto.ops.CommandRequest;
import com.codebridge.session.dto.ops.CommandResponse;
import com.codebridge.session.exception.RemoteOperationException;
import com.codebridge.session.model.SessionKey;
import com.codebridge.session.service.connection.SshConnectionPool;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Default implementation of the SshCommandQueue interface.
 * This implementation uses a ThreadPoolExecutor to process commands asynchronously
 * while maintaining backward compatibility with synchronous execution.
 */
@Service
public class DefaultSshCommandQueue implements SshCommandQueue {
    private static final Logger logger = LoggerFactory.getLogger(DefaultSshCommandQueue.class);
    private static final int DEFAULT_COMMAND_TIMEOUT_MS = 60000; // 60 seconds
    private static final int JSCH_CHANNEL_CONNECT_TIMEOUT_MS = 5000; // 5 seconds for channel

    private final SshConnectionPool connectionPool;
    private final ThreadPoolExecutor executor;
    private final MeterRegistry meterRegistry;
    private final int maxQueueSize;
    private final AtomicInteger activeCommandCount = new AtomicInteger(0);
    private final AtomicInteger queuedCommandCount = new AtomicInteger(0);

    // Metrics
    private final Timer commandExecutionTimer;
    private final Counter commandSuccessCounter;
    private final Counter commandFailureCounter;
    private final Counter commandTimeoutCounter;
    private final Counter commandRejectedCounter;

    @Autowired
    public DefaultSshCommandQueue(
            SshConnectionPool connectionPool,
            MeterRegistry meterRegistry,
            @Value("${codebridge.session.command.corePoolSize:10}") int corePoolSize,
            @Value("${codebridge.session.command.maxPoolSize:50}") int maxPoolSize,
            @Value("${codebridge.session.command.queueCapacity:1000}") int queueCapacity,
            @Value("${codebridge.session.command.keepAliveSeconds:60}") int keepAliveSeconds) {
        
        this.connectionPool = connectionPool;
        this.meterRegistry = meterRegistry;
        this.maxQueueSize = queueCapacity;
        
        // Create a bounded queue for commands
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(queueCapacity);
        
        // Create the thread pool with a custom rejection policy
        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                workQueue,
                new ThreadPoolExecutor.CallerRunsPolicy() // Fall back to caller thread if queue is full
        );
        
        // Initialize metrics
        this.commandExecutionTimer = Timer.builder("ssh.command.execution.time")
                .description("Time taken to execute SSH commands")
                .register(meterRegistry);
        
        this.commandSuccessCounter = Counter.builder("ssh.command.success.count")
                .description("Number of successful SSH commands")
                .register(meterRegistry);
        
        this.commandFailureCounter = Counter.builder("ssh.command.failure.count")
                .description("Number of failed SSH commands")
                .register(meterRegistry);
        
        this.commandTimeoutCounter = Counter.builder("ssh.command.timeout.count")
                .description("Number of SSH commands that timed out")
                .register(meterRegistry);
        
        this.commandRejectedCounter = Counter.builder("ssh.command.rejected.count")
                .description("Number of SSH commands rejected due to queue capacity")
                .register(meterRegistry);
        
        // Register gauges for queue metrics
        meterRegistry.gauge("ssh.command.active.count", activeCommandCount);
        meterRegistry.gauge("ssh.command.queued.count", queuedCommandCount);
    }

    @Override
    public void submitCommand(SessionKey sessionKey, CommandRequest command, Consumer<CommandResponse> callback) {
        if (executor.getQueue().size() >= maxQueueSize) {
            commandRejectedCounter.increment();
            callback.accept(new CommandResponse(
                    "",
                    "Command rejected: Queue capacity exceeded",
                    -1,
                    0
            ));
            return;
        }
        
        queuedCommandCount.incrementAndGet();
        executor.execute(() -> {
            queuedCommandCount.decrementAndGet();
            activeCommandCount.incrementAndGet();
            try {
                CommandResponse response = executeCommand(sessionKey, command);
                callback.accept(response);
            } catch (Exception e) {
                logger.error("Error executing command for session key {}: {}", sessionKey, e.getMessage(), e);
                commandFailureCounter.increment();
                callback.accept(new CommandResponse(
                        "",
                        "Command execution error: " + e.getMessage(),
                        -1,
                        0
                ));
            } finally {
                activeCommandCount.decrementAndGet();
            }
        });
    }

    @Override
    public CompletableFuture<CommandResponse> submitCommandAsync(SessionKey sessionKey, CommandRequest command) {
        CompletableFuture<CommandResponse> future = new CompletableFuture<>();
        submitCommand(sessionKey, command, future::complete);
        return future;
    }

    @Override
    public CommandResponse executeCommandSync(SessionKey sessionKey, CommandRequest command) {
        try {
            return executeCommand(sessionKey, command);
        } catch (Exception e) {
            logger.error("Error executing command synchronously for session key {}: {}", sessionKey, e.getMessage(), e);
            commandFailureCounter.increment();
            return new CommandResponse(
                    "",
                    "Command execution error: " + e.getMessage(),
                    -1,
                    0
            );
        }
    }

    @Override
    public int getQueueSize() {
        return queuedCommandCount.get();
    }

    @Override
    public int getActiveCommandCount() {
        return activeCommandCount.get();
    }

    /**
     * Internal method to execute a command using a JSch session.
     *
     * @param sessionKey The session key identifying the SSH session
     * @param commandRequest The command to execute
     * @return The command response
     * @throws Exception If there's an error executing the command
     */
    private CommandResponse executeCommand(SessionKey sessionKey, CommandRequest commandRequest) throws Exception {
        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();
        
        Session jschSession = null;
        ChannelExec channel = null;
        String stdoutString = "";
        String stderrString = "";
        int exitStatus = -1;
        
        try {
            // Get a connection from the pool
            jschSession = connectionPool.acquireConnection(sessionKey, null); // We assume the connection exists
            
            channel = (ChannelExec) jschSession.openChannel("exec");
            channel.setCommand(commandRequest.getCommand());
            
            ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();
            channel.setOutputStream(stdoutStream);
            channel.setErrStream(stderrStream);
            
            logger.debug("Executing command for session key {}: '{}'", sessionKey, commandRequest.getCommand());
            channel.connect(JSCH_CHANNEL_CONNECT_TIMEOUT_MS);
            
            int timeout = commandRequest.getTimeout() != null ? commandRequest.getTimeout() : DEFAULT_COMMAND_TIMEOUT_MS;
            long endTime = System.currentTimeMillis() + timeout;
            while (!channel.isClosed() && System.currentTimeMillis() < endTime) {
                Thread.sleep(100); // Poll interval
            }
            
            if (!channel.isClosed()) {
                logger.warn("Command timeout for session key {}: {}", sessionKey, commandRequest.getCommand());
                commandTimeoutCounter.increment();
                throw new RemoteOperationException("Command execution timed out after " + timeout + "ms.");
            }
            
            exitStatus = channel.getExitStatus();
            stdoutString = stdoutStream.toString(StandardCharsets.UTF_8);
            stderrString = stderrStream.toString(StandardCharsets.UTF_8);
            
            // Update metrics based on command result
            if (exitStatus == 0) {
                commandSuccessCounter.increment();
            } else {
                commandFailureCounter.increment();
            }
            
            logger.info("Command for session key {} executed with exit status: {}", sessionKey, exitStatus);
            
        } catch (JSchException e) {
            logger.error("JSchException for session key {}: {}", sessionKey, e.getMessage(), e);
            commandFailureCounter.increment();
            throw new RemoteOperationException("SSH operation failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.warn("Command execution interrupted for session key {}: {}", sessionKey, e.getMessage());
            Thread.currentThread().interrupt();
            commandFailureCounter.increment();
            throw new RemoteOperationException("Command execution was interrupted.", e);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            
            // Release the connection back to the pool
            if (jschSession != null) {
                connectionPool.releaseConnection(sessionKey, false);
            }
        }
        
        long durationMs = System.currentTimeMillis() - startTime;
        sample.stop(commandExecutionTimer);
        
        return new CommandResponse(stdoutString, stderrString, exitStatus, durationMs);
    }
}

