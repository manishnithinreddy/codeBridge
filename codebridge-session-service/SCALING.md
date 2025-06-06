# Scaling the Session Service

This document outlines the scaling improvements implemented in the Session Service to prepare for handling large numbers of concurrent connections (up to 200,000+).

## Phase 1 Improvements

The following improvements have been implemented in Phase 1:

### 1. Connection Management

- **Connection Pooling**: Implemented a `SshConnectionPool` that manages SSH connections with configurable limits.
- **Ephemeral Connections**: Modified the connection lifecycle to support more ephemeral connections.
- **Aggressive Timeouts**: Added configurable timeouts for idle connections.
- **Circuit Breaker Pattern**: Implemented a circuit breaker to prevent cascading failures when connection establishment fails.

### 2. Async Operations

- **Command Queue**: Implemented a `SshCommandQueue` interface and `DefaultSshCommandQueue` implementation for processing commands asynchronously.
- **Async API**: Added support for asynchronous command execution with `CompletableFuture`.
- **Thread Pool Management**: Configured thread pools with proper sizing and rejection policies.

### 3. Monitoring and Metrics

- **Detailed Metrics**: Added metrics for connection counts, command execution times, and resource usage.
- **Health Endpoints**: Implemented health check endpoints that provide visibility into system health.
- **JVM Metrics**: Added JVM metrics for memory, GC, threads, and other JVM statistics.

## Configuration

The scaling improvements can be configured using the following properties:

```properties
# Connection Pool Configuration
codebridge.session.ssh.maxConnectionsPerInstance=1000
codebridge.session.ssh.connectionIdleTimeoutMs=300000
codebridge.session.ssh.cleanupIntervalMs=60000

# Command Queue Configuration
codebridge.session.command.corePoolSize=10
codebridge.session.command.maxPoolSize=50
codebridge.session.command.queueCapacity=1000
codebridge.session.command.keepAliveSeconds=60
```

To enable the scaling profile, add `spring.profiles.active=scaling` to your application properties or use the `--spring.profiles.active=scaling` command line argument.

## Future Phases

The Phase 1 improvements lay the groundwork for future scaling enhancements:

### Phase 2: Scaling Infrastructure

- Load balancing for Session Service
- Redis clustering for session metadata
- Consistent hashing for routing
- Chunked transfers for large files
- Resource quotas

### Phase 3: Advanced Scaling

- Lightweight agent deployment
- Async SSH with Netty or Apache Mina SSHD
- Command queue with Kafka or RabbitMQ
- Service mesh for sophisticated routing

## Monitoring

The Session Service now exposes metrics via Prometheus endpoints. You can access the metrics at `/actuator/prometheus`.

Key metrics to monitor:

- `ssh.connection.active.count`: Number of active SSH connections
- `ssh.connection.creation.count`: Number of new SSH connections created
- `ssh.connection.reuse.count`: Number of times SSH connections were reused
- `ssh.command.execution.time`: Time taken to execute SSH commands
- `ssh.sftp.list.time`: Time taken to list files via SFTP
- `ssh.sftp.download.time`: Time taken to download files via SFTP
- `ssh.sftp.upload.time`: Time taken to upload files via SFTP

## Health Checks

The Session Service now provides detailed health information via the `/api/health/details` endpoint, including:

- Connection pool metrics
- Command queue metrics
- JVM metrics

This information is useful for monitoring the health of the service and diagnosing issues.

