import logging
import uuid
from typing import Dict, List, Optional, Union

import openai
from pydantic import BaseModel

from ai_service.api.v1.completion import CompletionMessage
from ai_service.config.settings import Settings
from ai_service.services.model_service import ModelService

# Configure logging
logger = logging.getLogger(__name__)

# Load settings
settings = Settings()

# Set OpenAI API key
if settings.openai_api_key:
    openai.api_key = settings.openai_api_key

class CompletionService:
    """Service for generating completions."""
    
    def __init__(self):
        """Initialize the completion service."""
        self.model_service = ModelService()
    
    async def create_completion(
        self,
        model: str,
        messages: List[CompletionMessage],
        temperature: float = 0.7,
        max_tokens: Optional[int] = None,
        top_p: float = 1.0,
        frequency_penalty: float = 0.0,
        presence_penalty: float = 0.0,
        stop: Optional[Union[str, List[str]]] = None,
        user_id: str = None,
        metadata: Optional[Dict] = None,
    ) -> Dict:
        """Create a completion."""
        # Validate model
        model_info = self.model_service.get_model(model)
        if not model_info:
            raise ValueError(f"Model {model} not found")
        
        if "completion" not in model_info.capabilities:
            raise ValueError(f"Model {model} does not support completion")
        
        # Prepare messages
        openai_messages = [
            {"role": msg.role, "content": msg.content}
            for msg in messages
        ]
        
        try:
            # Generate completion based on provider
            if model_info.provider == "openai":
                return await self._create_openai_completion(
                    model=model,
                    messages=openai_messages,
                    temperature=temperature,
                    max_tokens=max_tokens,
                    top_p=top_p,
                    frequency_penalty=frequency_penalty,
                    presence_penalty=presence_penalty,
                    stop=stop,
                    user=user_id,
                )
            else:
                raise ValueError(f"Unsupported model provider: {model_info.provider}")
        except Exception as e:
            logger.error(f"Failed to create completion: {e}", exc_info=True)
            raise
    
    async def _create_openai_completion(
        self,
        model: str,
        messages: List[Dict],
        temperature: float = 0.7,
        max_tokens: Optional[int] = None,
        top_p: float = 1.0,
        frequency_penalty: float = 0.0,
        presence_penalty: float = 0.0,
        stop: Optional[Union[str, List[str]]] = None,
        user: Optional[str] = None,
    ) -> Dict:
        """Create a completion using OpenAI API."""
        try:
            # Create completion
            response = await openai.ChatCompletion.acreate(
                model=model,
                messages=messages,
                temperature=temperature,
                max_tokens=max_tokens,
                top_p=top_p,
                frequency_penalty=frequency_penalty,
                presence_penalty=presence_penalty,
                stop=stop,
                user=user,
            )
            
            # Return response
            return {
                "id": response.id,
                "model": response.model,
                "choices": [
                    {
                        "index": choice.index,
                        "message": {
                            "role": choice.message.role,
                            "content": choice.message.content,
                        },
                        "finish_reason": choice.finish_reason,
                    }
                    for choice in response.choices
                ],
                "usage": {
                    "prompt_tokens": response.usage.prompt_tokens,
                    "completion_tokens": response.usage.completion_tokens,
                    "total_tokens": response.usage.total_tokens,
                },
            }
        except openai.error.OpenAIError as e:
            logger.error(f"OpenAI API error: {e}", exc_info=True)
            raise ValueError(f"OpenAI API error: {str(e)}")
        except Exception as e:
            logger.error(f"Failed to create OpenAI completion: {e}", exc_info=True)
            raise

