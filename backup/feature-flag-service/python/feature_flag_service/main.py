"""
Main entry point for the Feature Flag Service.
"""

import asyncio
import logging
import signal
import sys
from concurrent import futures
from typing import Dict, Any

import grpc
import uvicorn
from fastapi import FastAPI

from feature_flag_service.api.router import create_api_router
from feature_flag_service.config.config import load_config
from feature_flag_service.grpc.feature_flag_service_pb2_grpc import add_FeatureFlagServiceServicer_to_server
from feature_flag_service.grpc.service import FeatureFlagGrpcService
from feature_flag_service.repository.redis_feature_flag_repository import RedisFeatureFlagRepository
from feature_flag_service.service.feature_flag_service import FeatureFlagService

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


def create_app(config: Dict[str, Any]) -> FastAPI:
    """
    Create and configure the FastAPI application.
    """
    app = FastAPI(
        title="Feature Flag Service",
        description="Service for managing feature flags",
        version="1.0.0",
    )

    # Add API router
    app.include_router(create_api_router())

    # Add health check endpoint
    @app.get("/health")
    async def health_check():
        return {
            "status": "UP",
            "version": "1.0.0",
            "details": {
                "service": "feature-flag-service",
            }
        }

    return app


async def start_http_server(app: FastAPI, host: str, port: int) -> None:
    """
    Start the HTTP server.
    """
    config = uvicorn.Config(app, host=host, port=port)
    server = uvicorn.Server(config)
    await server.serve()


def start_grpc_server(service: FeatureFlagGrpcService, port: int) -> grpc.Server:
    """
    Start the gRPC server.
    """
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    add_FeatureFlagServiceServicer_to_server(service, server)
    server.add_insecure_port(f"[::]:{port}")
    server.start()
    return server


async def main_async():
    """
    Async main function.
    """
    # Load configuration
    config = load_config()
    logger.info("Configuration loaded")

    # Create repository
    repository = RedisFeatureFlagRepository(
        host=config["redis"]["host"],
        port=config["redis"]["port"],
        password=config["redis"]["password"],
        db=config["redis"]["database"],
        ttl_seconds=config["feature_flag"]["cache"]["ttl_seconds"],
    )
    logger.info("Repository created")

    # Create service
    service = FeatureFlagService(
        repository=repository,
        default_namespace=config["feature_flag"]["defaults"]["namespace"],
    )
    logger.info("Service created")

    # Create gRPC service
    grpc_service = FeatureFlagGrpcService(service)
    logger.info("gRPC service created")

    # Start gRPC server
    grpc_server = start_grpc_server(grpc_service, config["grpc"]["server"]["port"])
    logger.info(f"gRPC server started on port {config['grpc']['server']['port']}")

    # Create FastAPI app
    app = create_app(config)
    logger.info("FastAPI app created")

    # Set up shutdown handler
    def shutdown_handler():
        logger.info("Shutting down servers...")
        grpc_server.stop(0)
        logger.info("gRPC server stopped")

    # Register shutdown handler
    for sig in (signal.SIGINT, signal.SIGTERM):
        signal.signal(sig, lambda *_: shutdown_handler())

    # Start HTTP server
    logger.info(f"Starting HTTP server on port {config['server']['port']}")
    await start_http_server(app, "0.0.0.0", config["server"]["port"])


def main():
    """
    Main entry point.
    """
    try:
        asyncio.run(main_async())
    except KeyboardInterrupt:
        logger.info("Received keyboard interrupt, shutting down")
        sys.exit(0)


if __name__ == "__main__":
    main()

