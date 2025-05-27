# CodeBridge Events Service

## Overview

The Events Service is responsible for webhook event handling, audit logging, and event monitoring within the CodeBridge platform. This service consolidates the functionality previously provided by the Webhook Service and Audit Service.

## Features

- Webhook event handling
- Event processing and routing
- Event status tracking
- Retry mechanism
- Event type filtering
- Signature validation
- IP filtering
- Audit logging
- Security monitoring
- User activity tracking
- Service activity logging
- Error tracking
- Request/response logging
- IP and user agent tracking
- Metadata capture

## Architecture

The Events Service follows a layered architecture:

- **Controllers**: Handle HTTP requests and responses
- **Services**: Implement business logic
- **Repositories**: Interact with the database
- **Models**: Represent data entities
- **DTOs**: Transfer data between layers
- **Async**: Handle asynchronous event processing

## Key Components

### Models

- **Webhook**: Represents a webhook configuration
- **WebhookEvent**: Represents a webhook event
- **AuditEvent**: Represents an audit event

### Services

- **WebhookService**: Manages webhook configurations
- **WebhookEventService**: Processes webhook events
- **AuditService**: Manages audit logging
- **EventProcessingService**: Handles event processing and routing

## API Endpoints

The Events Service exposes the following API endpoints:

- `/api/events/webhooks`: Webhook configuration endpoints
- `/api/events/webhooks/{webhookId}/events`: Webhook event endpoints
- `/api/events/audit`: Audit logging endpoints
- `/api/events/audit/search`: Audit event search endpoints

## Configuration

The service can be configured using the following environment variables:

- `DB_HOST`: Database host (default: localhost)
- `DB_PORT`: Database port (default: 5432)
- `DB_NAME`: Database name (default: codebridge_events)
- `DB_USERNAME`: Database username (default: postgres)
- `DB_PASSWORD`: Database password (default: postgres)
- `SERVER_PORT`: Server port (default: 8083)
- `EUREKA_URI`: Eureka server URI (default: http://localhost:8761/eureka)

## Webhook Retry Configuration

The service includes a configurable retry mechanism for webhook events:

- `webhook.retry.max-attempts`: Maximum number of retry attempts (default: 5)
- `webhook.retry.initial-interval`: Initial retry interval in milliseconds (default: 1000)
- `webhook.retry.multiplier`: Backoff multiplier for retry intervals (default: 2.0)
- `webhook.retry.max-interval`: Maximum retry interval in milliseconds (default: 60000)

## Dependencies

- Spring Boot 3.1.0
- Spring Data JPA
- Spring Cloud Netflix Eureka Client
- Spring Retry
- PostgreSQL
- Lombok
- Flyway

