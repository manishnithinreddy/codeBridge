import logging
from typing import Dict, List, Optional

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field

from ai_service.api.v1.auth import UserClaims, validate_token
from ai_service.services.embedding_service import EmbeddingService

# Configure logging
logger = logging.getLogger(__name__)

# Create router
router = APIRouter()

# Create embedding service
embedding_service = EmbeddingService()

# Models
class EmbeddingRequest(BaseModel):
    model: str = Field(..., description="The model to use for embedding")
    input: List[str] = Field(..., description="The text to embed")
    metadata: Optional[Dict] = Field(None, description="Additional metadata")

class EmbeddingResponse(BaseModel):
    id: str = Field(..., description="The ID of the embedding")
    model: str = Field(..., description="The model used for embedding")
    data: List[Dict] = Field(..., description="The embedding data")
    usage: Dict = Field(..., description="The token usage information")

# Endpoints
@router.post("", response_model=EmbeddingResponse)
async def create_embedding(
    request: EmbeddingRequest,
    user: UserClaims = Depends(validate_token),
):
    """Create embeddings for text."""
    try:
        # Generate embeddings
        embedding = await embedding_service.create_embedding(
            model=request.model,
            input=request.input,
            user_id=user.user_id,
            metadata=request.metadata,
        )
        
        return embedding
    except ValueError as e:
        logger.error(f"Invalid request: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )
    except Exception as e:
        logger.error(f"Failed to create embedding: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to create embedding: {str(e)}",
        )

