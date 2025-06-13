# CodeBridge Scalability and High Availability Module

This module implements Phase 8 of the CodeBridge project, focusing on Scalability and High Availability features.

## Features

### Horizontal Scaling

- **Load Balancing**: Multiple strategies including Round Robin, Least Connections, Weighted, and IP Hash
- **Auto-Scaling**: Metric-based scaling decisions with configurable thresholds and cooldown periods
- **Session Management**: Distributed session support with Redis, Hazelcast, and JDBC backends

### Data Resilience

- **Replication**: Data replication with configurable consistency levels (ONE, QUORUM, ALL)
- **Backup and Recovery**: Scheduled backups with verification and point-in-time recovery
- **Data Partitioning**: Horizontal data sharding with support for hash, range, and list partitioning strategies

### High Availability

- **Idempotency Support**: Ensures operations are only executed once, even if requests are retried
- **Circuit Breakers**: Prevents cascading failures by failing fast when dependencies are unavailable
- **Rate Limiting**: Protects services from being overwhelmed by too many requests

## Configuration

The module is highly configurable through the `application.yml` file and environment variables:

### Session Configuration

```yaml
codebridge:
  scalability:
    session:
      store-type: redis  # Options: redis, hazelcast, jdbc
      cookie:
        secure: true
        http-only: true
        same-site: lax
        max-age: 1800
```

### Load Balancing Configuration

```yaml
codebridge:
  scalability:
    load-balancing:
      strategy: round-robin  # Options: round-robin, least-connections, weighted, ip-hash
      sticky-sessions: true
      health-check-interval-seconds: 30
```

### Auto-Scaling Configuration

```yaml
codebridge:
  scalability:
    auto-scaling:
      enabled: true
      cpu-threshold: 70
      memory-threshold: 80
      min-instances: 2
      max-instances: 10
      scale-up-cooldown-seconds: 300
      scale-down-cooldown-seconds: 600
```

### Data Resilience Configuration

```yaml
codebridge:
  scalability:
    data-resilience:
      replication:
        enabled: true
        read-from-replicas: true
        consistency-level: QUORUM  # Options: ONE, QUORUM, ALL
      backup:
        enabled: true
        schedule: "0 0 2 * * ?"  # Cron expression
        retention-days: 30
        verify: true
      partitioning:
        enabled: true
        strategy: hash  # Options: hash, range, list
        shard-count: 4
```

### Idempotency Configuration

```yaml
codebridge:
  scalability:
    idempotency:
      enabled: true
      header-name: X-Idempotency-Key
      storage-type: redis  # Options: redis, hazelcast, jdbc
      expiration-hours: 24
```

## Architecture

The module is designed with the following components:

1. **Session Management**: Configurable session store with support for multiple backends
2. **Load Balancing**: Client-side load balancing with health checking and multiple strategies
3. **Auto-Scaling**: Metric-based scaling decisions with configurable thresholds
4. **Data Resilience**: Replication, backup, and partitioning services
5. **Idempotency**: Filter and service for ensuring idempotent operations

## Usage

### Session Management

The module automatically configures session management based on the configured store type:

```java
@Controller
public class UserController {
    
    @GetMapping("/user")
    public String getUserInfo(HttpSession session) {
        // Session is automatically distributed
        User user = (User) session.getAttribute("user");
        // ...
    }
}
```

### Load Balancing

The module provides a `ServiceInstanceSelector` for client-side load balancing:

```java
@Service
public class UserService {
    
    private final ServiceInstanceSelector serviceInstanceSelector;
    private final RestTemplate restTemplate;
    
    @Autowired
    public UserService(ServiceInstanceSelector serviceInstanceSelector, RestTemplate restTemplate) {
        this.serviceInstanceSelector = serviceInstanceSelector;
        this.restTemplate = restTemplate;
    }
    
    public User getUser(String userId) {
        Optional<ServiceInstance> instance = serviceInstanceSelector.selectInstance("user-service", userId);
        
        if (instance.isPresent()) {
            String url = instance.get().getUri() + "/users/" + userId;
            return restTemplate.getForObject(url, User.class);
        }
        
        throw new ServiceUnavailableException("No instances available for user-service");
    }
}
```

### Idempotency

The module automatically handles idempotent requests when the idempotency header is present:

```
POST /api/orders
X-Idempotency-Key: 123e4567-e89b-12d3-a456-426614174000
Content-Type: application/json

{
  "productId": "product-123",
  "quantity": 1
}
```

If the same request is sent again with the same idempotency key, the original response will be returned without executing the operation again.

### Data Resilience

The module provides services for data replication, backup, and partitioning:

```java
@Service
public class OrderService {
    
    private final ReplicationService replicationService;
    private final DataPartitioningService dataPartitioningService;
    
    @Autowired
    public OrderService(ReplicationService replicationService, DataPartitioningService dataPartitioningService) {
        this.replicationService = replicationService;
        this.dataPartitioningService = dataPartitioningService;
    }
    
    public void createOrder(Order order) {
        // Determine the shard for the order
        int shardId = dataPartitioningService.getShardForKey(order.getCustomerId());
        
        // Execute the insert on the appropriate shard
        dataPartitioningService.executeUpdateOnShard(
            shardId,
            "INSERT INTO orders (id, customer_id, product_id, quantity) VALUES (?, ?, ?, ?)",
            order.getId(), order.getCustomerId(), order.getProductId(), order.getQuantity()
        );
        
        // Replicate the data to all replicas
        replicationService.replicateData(
            "INSERT INTO orders (id, customer_id, product_id, quantity) VALUES (?, ?, ?, ?)",
            order.getId(), order.getCustomerId(), order.getProductId(), order.getQuantity()
        );
    }
}
```

## Deployment

The module is designed to be deployed in a containerized environment such as Kubernetes or Docker Swarm. It integrates with Eureka for service discovery and supports auto-scaling through external orchestration platforms.

## Monitoring

The module exposes metrics through Spring Boot Actuator and Prometheus endpoints:

- `/actuator/health`: Health check endpoint
- `/actuator/info`: Information about the application
- `/actuator/metrics`: Metrics endpoint
- `/actuator/prometheus`: Prometheus metrics endpoint

## Dependencies

- Spring Boot 2.7.x
- Spring Cloud
- Resilience4j
- Hazelcast
- Redis
- Bucket4j
- PostgreSQL

