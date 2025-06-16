import logging
import uuid
from typing import Dict, List, Optional

import numpy as np
import openai
from pydantic import BaseModel

from ai_service.config.settings import Settings
from ai_service.services.model_service import ModelService

# Configure logging
logger = logging.getLogger(__name__)

# Load settings
settings = Settings()

# Set OpenAI API key
if settings.openai_api_key:
    openai.api_key = settings.openai_api_key

class EmbeddingService:
    """Service for generating embeddings."""
    
    def __init__(self):
        """Initialize the embedding service."""
        self.model_service = ModelService()
    
    async def create_embedding(
        self,
        model: str,
        input: List[str],
        user_id: str = None,
        metadata: Optional[Dict] = None,
    ) -> Dict:
        """Create embeddings for text."""
        # Validate model
        model_info = self.model_service.get_model(model)
        if not model_info:
            raise ValueError(f"Model {model} not found")
        
        if "embedding" not in model_info.capabilities:
            raise ValueError(f"Model {model} does not support embedding")
        
        try:
            # Generate embeddings based on provider
            if model_info.provider == "openai":
                return await self._create_openai_embedding(
                    model=model,
                    input=input,
                    user=user_id,
                )
            else:
                raise ValueError(f"Unsupported model provider: {model_info.provider}")
        except Exception as e:
            logger.error(f"Failed to create embedding: {e}", exc_info=True)
            raise
    
    async def _create_openai_embedding(
        self,
        model: str,
        input: List[str],
        user: Optional[str] = None,
    ) -> Dict:
        """Create embeddings using OpenAI API."""
        try:
            # Create embeddings
            response = await openai.Embedding.acreate(
                model=model,
                input=input,
                user=user,
            )
            
            # Return response
            return {
                "id": response.id,
                "model": response.model,
                "data": [
                    {
                        "index": data.index,
                        "embedding": data.embedding,
                    }
                    for data in response.data
                ],
                "usage": {
                    "prompt_tokens": response.usage.prompt_tokens,
                    "total_tokens": response.usage.total_tokens,
                },
            }
        except openai.error.OpenAIError as e:
            logger.error(f"OpenAI API error: {e}", exc_info=True)
            raise ValueError(f"OpenAI API error: {str(e)}")
        except Exception as e:
            logger.error(f"Failed to create OpenAI embedding: {e}", exc_info=True)
            raise

