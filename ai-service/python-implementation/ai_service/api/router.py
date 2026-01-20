from fastapi import APIRouter

from ai_service.api.v1 import auth, completion, embedding, models

# Create API router
api_router = APIRouter()

# Include API endpoints
api_router.include_router(auth.router, prefix="/auth", tags=["Authentication"])
api_router.include_router(models.router, prefix="/models", tags=["Models"])
api_router.include_router(completion.router, prefix="/completion", tags=["Completion"])
api_router.include_router(embedding.router, prefix="/embedding", tags=["Embedding"])

