import os
from typing import Dict, List, Optional, Union

from pydantic import BaseSettings, Field, validator


class Settings(BaseSettings):
    """Application settings."""
    
    # General settings
    debug: bool = Field(False, env="DEBUG")
    environment: str = Field("development", env="ENVIRONMENT")
    
    # Server settings
    host: str = Field("0.0.0.0", env="HOST")
    port: int = Field(8082, env="PORT")
    
    # API settings
    api_prefix: str = Field("/api", env="API_PREFIX")
    
    # Session service settings
    session_service_url: str = Field("http://localhost:8080/api", env="SESSION_SERVICE_URL")
    
    # Redis settings
    redis_host: str = Field("localhost", env="REDIS_HOST")
    redis_port: int = Field(6379, env="REDIS_PORT")
    redis_password: str = Field("", env="REDIS_PASSWORD")
    redis_db: int = Field(0, env="REDIS_DB")
    
    # Model settings
    model_cache_dir: str = Field("./model_cache", env="MODEL_CACHE_DIR")
    default_model: str = Field("gpt-3.5-turbo", env="DEFAULT_MODEL")
    
    # OpenAI settings
    openai_api_key: Optional[str] = Field(None, env="OPENAI_API_KEY")
    
    # Security settings
    cors_origins: List[str] = Field(["*"], env="CORS_ORIGINS")
    
    # Logging settings
    log_level: str = Field("INFO", env="LOG_LEVEL")
    
    class Config:
        """Pydantic config."""
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = True
    
    @validator("cors_origins", pre=True)
    def assemble_cors_origins(cls, v: Union[str, List[str]]) -> List[str]:
        """Parse CORS origins from string or list."""
        if isinstance(v, str) and not v.startswith("["):
            return [i.strip() for i in v.split(",")]
        elif isinstance(v, (list, str)):
            return v
        raise ValueError(v)
    
    @validator("environment")
    def validate_environment(cls, v: str) -> str:
        """Validate environment."""
        allowed = {"development", "staging", "production"}
        if v not in allowed:
            raise ValueError(f"Environment must be one of {allowed}")
        return v
    
    @validator("log_level")
    def validate_log_level(cls, v: str) -> str:
        """Validate log level."""
        allowed = {"DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"}
        if v not in allowed:
            raise ValueError(f"Log level must be one of {allowed}")
        return v

