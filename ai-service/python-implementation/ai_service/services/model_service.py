import logging
from typing import Dict, List, Optional

from ai_service.config.settings import Settings
from ai_service.models.model_info import ModelInfo

# Configure logging
logger = logging.getLogger(__name__)

# Load settings
settings = Settings()

class ModelService:
    """Service for managing AI models."""
    
    def __init__(self):
        """Initialize the model service."""
        # Define available models
        self.models = {
            "gpt-3.5-turbo": ModelInfo(
                id="gpt-3.5-turbo",
                name="GPT-3.5 Turbo",
                description="GPT-3.5 Turbo is a powerful language model optimized for chat.",
                provider="openai",
                type="chat",
                context_length=4096,
                capabilities=["chat", "completion"],
                parameters={
                    "temperature": {"default": 0.7, "min": 0.0, "max": 2.0},
                    "max_tokens": {"default": 256, "min": 1, "max": 4096},
                    "top_p": {"default": 1.0, "min": 0.0, "max": 1.0},
                    "frequency_penalty": {"default": 0.0, "min": -2.0, "max": 2.0},
                    "presence_penalty": {"default": 0.0, "min": -2.0, "max": 2.0},
                },
            ),
            "gpt-4": ModelInfo(
                id="gpt-4",
                name="GPT-4",
                description="GPT-4 is a large multimodal model that can solve difficult problems with greater accuracy.",
                provider="openai",
                type="chat",
                context_length=8192,
                capabilities=["chat", "completion"],
                parameters={
                    "temperature": {"default": 0.7, "min": 0.0, "max": 2.0},
                    "max_tokens": {"default": 256, "min": 1, "max": 8192},
                    "top_p": {"default": 1.0, "min": 0.0, "max": 1.0},
                    "frequency_penalty": {"default": 0.0, "min": -2.0, "max": 2.0},
                    "presence_penalty": {"default": 0.0, "min": -2.0, "max": 2.0},
                },
            ),
            "text-embedding-ada-002": ModelInfo(
                id="text-embedding-ada-002",
                name="Text Embedding Ada 002",
                description="Text Embedding Ada 002 is a powerful embedding model.",
                provider="openai",
                type="embedding",
                context_length=8191,
                capabilities=["embedding"],
                parameters={},
            ),
        }
    
    def list_models(self) -> List[ModelInfo]:
        """List available models."""
        return list(self.models.values())
    
    def get_model(self, model_id: str) -> Optional[ModelInfo]:
        """Get model information by ID."""
        return self.models.get(model_id)

