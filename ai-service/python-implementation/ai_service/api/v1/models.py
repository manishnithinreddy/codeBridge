import logging
from typing import Dict, List, Optional

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel

from ai_service.api.v1.auth import UserClaims, validate_token
from ai_service.services.model_service import ModelService

# Configure logging
logger = logging.getLogger(__name__)

# Create router
router = APIRouter()

# Create model service
model_service = ModelService()

# Models
class ModelInfo(BaseModel):
    id: str
    name: str
    description: Optional[str] = None
    provider: str
    type: str
    context_length: int
    capabilities: List[str]
    parameters: Optional[Dict] = None

class ModelsResponse(BaseModel):
    models: List[ModelInfo]

# Endpoints
@router.get("", response_model=ModelsResponse)
async def list_models(user: UserClaims = Depends(validate_token)):
    """List available models."""
    try:
        models = model_service.list_models()
        return ModelsResponse(models=models)
    except Exception as e:
        logger.error(f"Failed to list models: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to list models: {str(e)}",
        )

@router.get("/{model_id}", response_model=ModelInfo)
async def get_model(
    model_id: str,
    user: UserClaims = Depends(validate_token),
):
    """Get model information."""
    try:
        model = model_service.get_model(model_id)
        if not model:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Model {model_id} not found",
            )
        return model
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to get model {model_id}: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get model: {str(e)}",
        )

