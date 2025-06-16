from typing import Dict, List, Optional

from pydantic import BaseModel, Field


class ModelInfo(BaseModel):
    """Model information."""
    
    id: str = Field(..., description="The model ID")
    name: str = Field(..., description="The model name")
    description: Optional[str] = Field(None, description="The model description")
    provider: str = Field(..., description="The model provider (e.g., openai, huggingface)")
    type: str = Field(..., description="The model type (e.g., chat, embedding)")
    context_length: int = Field(..., description="The maximum context length in tokens")
    capabilities: List[str] = Field(..., description="The model capabilities (e.g., chat, completion, embedding)")
    parameters: Optional[Dict] = Field(None, description="The model parameters")

