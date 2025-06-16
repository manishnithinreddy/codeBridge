import httpx
import logging
from typing import Dict, Optional

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel

from ai_service.config.settings import Settings

# Configure logging
logger = logging.getLogger(__name__)

# Create router
router = APIRouter()

# Security scheme
security = HTTPBearer()

# Settings
settings = Settings()

# Models
class TokenValidationRequest(BaseModel):
    token: str

class TokenValidationResponse(BaseModel):
    valid: bool
    user_id: Optional[str] = None
    username: Optional[str] = None
    email: Optional[str] = None
    session_id: Optional[str] = None
    expires_at: Optional[int] = None
    error: Optional[str] = None

class UserClaims(BaseModel):
    user_id: str
    username: str
    email: str
    session_id: str
    expires_at: int

# Dependency for token validation
async def validate_token(
    credentials: HTTPAuthorizationCredentials = Depends(security),
) -> UserClaims:
    """Validate JWT token with session service."""
    token = credentials.credentials
    
    # Create validation request
    req = TokenValidationRequest(token=token)
    
    try:
        # Send request to session service
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{settings.session_service_url}/validate-token",
                json=req.dict(),
                timeout=5.0,
            )
            
            # Check response status
            if response.status_code != status.HTTP_200_OK:
                logger.error(f"Token validation failed with status: {response.status_code}")
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Invalid or expired token",
                )
            
            # Parse response
            validation_resp = TokenValidationResponse(**response.json())
            
            # Check if token is valid
            if not validation_resp.valid:
                logger.error(f"Invalid token: {validation_resp.error}")
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail=validation_resp.error or "Invalid token",
                )
            
            # Return user claims
            return UserClaims(
                user_id=validation_resp.user_id,
                username=validation_resp.username,
                email=validation_resp.email,
                session_id=validation_resp.session_id,
                expires_at=validation_resp.expires_at,
            )
    except httpx.RequestError as e:
        logger.error(f"Failed to connect to session service: {e}")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Failed to connect to authentication service",
        )

# Endpoints
@router.post("/validate", response_model=TokenValidationResponse)
async def validate_token_endpoint(request: TokenValidationRequest):
    """Validate a JWT token."""
    try:
        # Create validation request
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{settings.session_service_url}/validate-token",
                json=request.dict(),
                timeout=5.0,
            )
            
            # Return response
            return TokenValidationResponse(**response.json())
    except httpx.RequestError as e:
        logger.error(f"Failed to connect to session service: {e}")
        return TokenValidationResponse(
            valid=False,
            error="Failed to connect to authentication service",
        )

