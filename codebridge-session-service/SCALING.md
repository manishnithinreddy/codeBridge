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

## Phase 2 Improvements

The following improvements have been implemented in Phase 2:

### 1. Redis Clustering

- **Redis Cluster Configuration**: Implemented `RedisClusterConfig` for scaling session metadata storage.
- **Cluster-Aware Redis Template**: Configured Redis templates to work with Redis Cluster.
- **Kubernetes Deployment**: Added Kubernetes configuration for deploying a Redis Cluster.

### 2. Load-Aware Routing

- **Session Router**: Implemented a `SessionRouter` that routes requests to the correct service instance.
- **Routing Controller**: Added a `SessionRoutingController` that handles routing of session-based requests.
- **Instance-Aware Routing**: Ensured that requests for a specific session are always routed to the instance that owns that session.

### 3. Team-Based Access Control

- **Team Server Access Model**: Implemented a `TeamServerAccess` entity for team-based access control.
- **Access Control Service**: Added a `TeamServerAccessService` for managing team-based access.
- **Time-Limited Access**: Supported time-limited access with automatic expiration.
- **Role-Based Access**: Implemented role-based access with different access levels (READ, WRITE, EXECUTE, ADMIN).

### 4. File Transfer Optimization

- **Chunked File Transfers**: Implemented a `ChunkedFileTransferService` for optimized file transfers.
- **Async File Transfers**: Added support for asynchronous file transfers with `CompletableFuture`.
- **Temporary File Management**: Implemented proper temporary file management for file transfers.

### 5. Auto-Scaling Configuration

- **Kubernetes Deployment**: Added Kubernetes configuration for deploying the Session Service.
- **Horizontal Pod Autoscaler**: Configured HPA for automatic scaling based on CPU and memory usage.
- **Resource Limits**: Set appropriate resource limits and requests for the service.
- **JVM Tuning**: Configured JVM options for optimal performance in a containerized environment.

## Configuration

The scaling improvements can be configured using the following profiles:

- **scaling**: Basic scaling configuration with connection pooling, async operations, and monitoring.
- **autoscaling**: Advanced scaling configuration with Redis clustering, load-aware routing, and auto-scaling.

To enable the scaling profile, add `spring.profiles.active=scaling` to your application properties or use the `--spring.profiles.active=scaling` command line argument.

To enable the auto-scaling profile, add `spring.profiles.active=autoscaling` to your application properties or use the `--spring.profiles.active=autoscaling` command line argument.

## Kubernetes Deployment

The Session Service can be deployed to Kubernetes using the provided configuration files:

- **session-service-deployment.yaml**: Deployment configuration for the Session Service.
- **redis-cluster.yaml**: Deployment configuration for the Redis Cluster.

These files include:

- Deployment configuration with resource limits and requests
- Horizontal Pod Autoscaler configuration
- Service configuration
- Redis Cluster configuration
- Liveness and readiness probes

## Future Phases

The Phase 1 and Phase 2 improvements lay the groundwork for future scaling enhancements:

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
- `ssh.sftp.chunked.upload.time`: Time taken to upload files via chunked SFTP
- `ssh.sftp.chunked.download.time`: Time taken to download files via chunked SFTP
- `session.routing.local`: Number of requests routed to the local instance
- `session.routing.remote`: Number of requests routed to a remote instance
- `session.routing.error`: Number of routing errors

## Health Checks

The Session Service now provides detailed health information via the `/api/health/details` endpoint, including:

- Connection pool metrics
- Command queue metrics
- JVM metrics

This information is useful for monitoring the health of the service and diagnosing issues.

