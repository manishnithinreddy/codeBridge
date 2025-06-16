# CodeBridge Service Contracts

This directory contains the service contracts for the CodeBridge microservices architecture. These contracts define the interfaces between services using Protocol Buffers (protobuf) and gRPC.

## Overview

These service contracts are the foundation of our multi-language microservices architecture. They ensure consistent interfaces across different language implementations (Java, Go, Python) and enable seamless switching between implementations using feature flags.

## Service Contracts

### 1. Server Service (`server-service-api.proto`)

The Server Service handles user access management and orchestration for session services.

**Key Responsibilities:**
- User authentication and authorization
- Access control management
- Session request handling
- User permission management

### 2. Session Service (`session-service-api.proto`)

The Session Service manages SSH/SFTP connections and session lifecycle.

**Key Responsibilities:**
- SSH/SFTP connection management
- Session state tracking
- Session activity streaming
- Session metrics collection

### 3. AI-DB-Agent Service (`ai-db-agent-api.proto`)

The AI-DB-Agent Service handles natural language to SQL conversion and AI-powered database interactions.

**Key Responsibilities:**
- Natural language to SQL conversion
- SQL query explanation
- Query validation against schema
- Query suggestions

### 4. DB-Proxy Service (`db-proxy-api.proto`)

The DB-Proxy Service manages database connections, query execution, and transaction management.

**Key Responsibilities:**
- Database connection management
- Query execution
- Transaction handling
- Connection pooling

### 5. Feature Flag Service (`feature-flag-api.proto`)

The Feature Flag Service manages feature flags for dynamic service implementation switching.

**Key Responsibilities:**
- Flag management
- Context-based flag evaluation
- Flag update streaming
- Flag versioning

## Usage

### Generating Code from Protobuf

#### Java

```bash
protoc --java_out=./java-output \
       --grpc-java_out=./java-output \
       ./service-contracts/*.proto
```

#### Go

```bash
protoc --go_out=./go-output \
       --go-grpc_out=./go-output \
       ./service-contracts/*.proto
```

#### Python

```bash
python -m grpc_tools.protoc \
       --python_out=./python-output \
       --grpc_python_out=./python-output \
       -I. ./service-contracts/*.proto
```

## Implementation Guidelines

When implementing these service contracts in different languages, ensure:

1. **Consistent Behavior**: All implementations must behave identically for the same inputs.
2. **Error Handling**: Error codes and messages should be consistent across implementations.
3. **Performance Metrics**: Include standard metrics collection for performance comparison.
4. **Feature Flag Integration**: All implementations should check the Feature Flag Service for routing decisions.
5. **State Management**: Use the shared state store for session and connection state.

## Versioning

Service contracts follow semantic versioning:

- **MAJOR**: Breaking changes that require client updates
- **MINOR**: Backwards-compatible new features
- **PATCH**: Backwards-compatible bug fixes

## Contract Evolution

When evolving these contracts:

1. Maintain backward compatibility when possible
2. Use optional fields for new features
3. Never remove or rename fields
4. Never change field numbers
5. Document all changes in the contract history

## Contract Testing

All implementations must pass the contract test suite to ensure consistent behavior across languages.

