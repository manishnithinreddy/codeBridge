import logging
import os
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from ai_service.api.router import api_router
from ai_service.config.settings import Settings

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

# Load settings
settings = Settings()

# Startup and shutdown events
@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Load models, establish connections, etc.
    logger.info("Starting AI service...")
    
    # Add startup logic here
    
    yield
    
    # Shutdown: Release resources, close connections, etc.
    logger.info("Shutting down AI service...")
    
    # Add shutdown logic here

# Create FastAPI app
app = FastAPI(
    title="CodeBridge AI Service",
    description="AI service for the CodeBridge platform",
    version="1.0.0",
    lifespan=lifespan,
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include API router
app.include_router(api_router, prefix="/api")

# Health check endpoint
@app.get("/health")
async def health_check():
    return {
        "status": "UP",
        "service": "ai-service",
        "version": "1.0.0",
    }

# Global exception handler
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"error": "Internal server error", "detail": str(exc)},
    )

if __name__ == "__main__":
    # Run the application
    port = int(os.getenv("PORT", "8082"))
    uvicorn.run("ai_service.main:app", host="0.0.0.0", port=port, reload=settings.debug)

