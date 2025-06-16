import logging
from typing import Dict, List, Optional, Union

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field

from ai_service.api.v1.auth import UserClaims, validate_token
from ai_service.services.completion_service import CompletionService

# Configure logging
logger = logging.getLogger(__name__)

# Create router
router = APIRouter()

# Create completion service
completion_service = CompletionService()

# Models
class CompletionMessage(BaseModel):
    role: str = Field(..., description="The role of the message author (system, user, assistant)")
    content: str = Field(..., description="The content of the message")

class CompletionRequest(BaseModel):
    model: str = Field(..., description="The model to use for completion")
    messages: List[CompletionMessage] = Field(..., description="The messages to generate a completion for")
    temperature: Optional[float] = Field(0.7, description="The sampling temperature")
    max_tokens: Optional[int] = Field(None, description="The maximum number of tokens to generate")
    top_p: Optional[float] = Field(1.0, description="The nucleus sampling probability")
    frequency_penalty: Optional[float] = Field(0.0, description="The frequency penalty")
    presence_penalty: Optional[float] = Field(0.0, description="The presence penalty")
    stop: Optional[Union[str, List[str]]] = Field(None, description="The stop sequence(s)")
    stream: Optional[bool] = Field(False, description="Whether to stream the response")
    metadata: Optional[Dict] = Field(None, description="Additional metadata")

class CompletionResponse(BaseModel):
    id: str = Field(..., description="The ID of the completion")
    model: str = Field(..., description="The model used for completion")
    choices: List[Dict] = Field(..., description="The completion choices")
    usage: Dict = Field(..., description="The token usage information")

# Endpoints
@router.post("", response_model=CompletionResponse)
async def create_completion(
    request: CompletionRequest,
    user: UserClaims = Depends(validate_token),
):
    """Create a completion."""
    try:
        # Check if streaming is requested (not supported in this endpoint)
        if request.stream:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Streaming is not supported in this endpoint. Use /stream endpoint instead.",
            )
        
        # Generate completion
        completion = await completion_service.create_completion(
            model=request.model,
            messages=request.messages,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
            top_p=request.top_p,
            frequency_penalty=request.frequency_penalty,
            presence_penalty=request.presence_penalty,
            stop=request.stop,
            user_id=user.user_id,
            metadata=request.metadata,
        )
        
        return completion
    except ValueError as e:
        logger.error(f"Invalid request: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )
    except Exception as e:
        logger.error(f"Failed to create completion: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to create completion: {str(e)}",
        )

