# AI Service (Python Implementation)

This is the Python implementation of the CodeBridge AI Service, responsible for providing AI capabilities such as text completion and embeddings.

## Features

- Text completion using various models
- Text embedding generation
- Model information retrieval
- Authentication via JWT tokens from Session Service
- Support for multiple model providers (currently OpenAI)

## Architecture

The service follows a clean architecture approach:

- **API Layer**: FastAPI endpoints and request/response models
- **Service Layer**: Business logic for AI operations
- **Model Layer**: Domain models and entities

## Prerequisites

- Python 3.10 or higher
- OpenAI API key (for OpenAI models)

## Configuration

Configuration is loaded from environment variables or a `.env` file:

```env
# General settings
DEBUG=False
ENVIRONMENT=development
PORT=8082

# Session service settings
SESSION_SERVICE_URL=http://localhost:8080/api

# Redis settings (for caching)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DB=0

# Model settings
MODEL_CACHE_DIR=./model_cache
DEFAULT_MODEL=gpt-3.5-turbo

# OpenAI settings
OPENAI_API_KEY=your-api-key

# Logging settings
LOG_LEVEL=INFO
```

## Building and Running

### Local Development

```bash
# Install dependencies
pip install -r requirements.txt

# Run the service
uvicorn ai_service.main:app --reload
```

### Docker

```bash
# Build the Docker image
docker build -t codebridge/ai-service-python .

# Run the container
docker run -p 8082:8082 -e OPENAI_API_KEY=your-api-key codebridge/ai-service-python
```

## API Endpoints

All endpoints require authentication via JWT token in the Authorization header.

### Model Information

- `GET /api/models`: List available models
- `GET /api/models/{model_id}`: Get model information

### Text Completion

- `POST /api/completion`: Generate text completion

### Text Embedding

- `POST /api/embedding`: Generate text embeddings

### Authentication

- `POST /api/auth/validate`: Validate a JWT token

### Health Check

- `GET /health`: Health check endpoint

## Scalability Considerations

This implementation is designed for horizontal scalability:

- **Stateless Design**: No local state is maintained
- **Redis Caching**: Optional caching for improved performance
- **Graceful Shutdown**: Proper handling of shutdown signals
- **Health Checks**: Monitoring endpoint for load balancers

## Security Features

- **JWT Authentication**: Token-based authentication
- **Token Validation**: Validation with Session Service
- **Input Validation**: Comprehensive request validation
- **CORS Protection**: Configurable CORS headers

## Monitoring and Observability

- **Structured Logging**: JSON-formatted logs for easy parsing
- **Request Logging**: Detailed request logs
- **Health Checks**: Endpoint for monitoring service health

