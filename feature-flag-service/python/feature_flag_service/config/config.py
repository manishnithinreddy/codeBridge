"""
Configuration module for the Feature Flag Service.
"""

import os
from pathlib import Path
from typing import Dict, Any

import yaml
from dotenv import load_dotenv


def load_config() -> Dict[str, Any]:
    """
    Load configuration from environment variables and config files.
    """
    # Load environment variables from .env file if it exists
    load_dotenv()

    # Default configuration
    config = {
        "server": {
            "port": 8090,
            "context_path": "/feature-flag",
        },
        "redis": {
            "host": "localhost",
            "port": 6379,
            "password": "",
            "database": 0,
            "timeout": 2000,
            "pool": {
                "max_active": 8,
                "max_idle": 8,
                "min_idle": 2,
                "max_wait": -1,
            },
        },
        "grpc": {
            "server": {
                "port": 9090,
            },
        },
        "feature_flag": {
            "cache": {
                "ttl_seconds": 60,
            },
            "defaults": {
                "namespace": "default",
            },
            "pubsub": {
                "channel": "feature-flag-updates",
            },
        },
    }

    # Load configuration from file
    config_paths = [
        Path("config/application.yaml"),
        Path("/etc/feature-flag-service/application.yaml"),
    ]

    for config_path in config_paths:
        if config_path.exists():
            with open(config_path, "r") as f:
                file_config = yaml.safe_load(f)
                if file_config:
                    _merge_config(config, file_config)
            break

    # Override with environment variables
    _override_from_env(config)

    return config


def _merge_config(config: Dict[str, Any], override: Dict[str, Any]) -> None:
    """
    Merge override configuration into base configuration.
    """
    for key, value in override.items():
        if key in config and isinstance(config[key], dict) and isinstance(value, dict):
            _merge_config(config[key], value)
        else:
            config[key] = value


def _override_from_env(config: Dict[str, Any], prefix: str = "FF_") -> None:
    """
    Override configuration with environment variables.
    """
    for key, value in os.environ.items():
        if key.startswith(prefix):
            # Remove prefix and convert to lowercase
            env_key = key[len(prefix):].lower()
            
            # Split by underscore to navigate nested config
            parts = env_key.split("_")
            
            # Navigate to the correct config section
            current = config
            for part in parts[:-1]:
                if part not in current:
                    current[part] = {}
                current = current[part]
            
            # Set the value
            last_key = parts[-1]
            
            # Try to convert to appropriate type
            if value.lower() in ("true", "yes", "1"):
                current[last_key] = True
            elif value.lower() in ("false", "no", "0"):
                current[last_key] = False
            elif value.isdigit():
                current[last_key] = int(value)
            elif value.replace(".", "", 1).isdigit() and value.count(".") == 1:
                current[last_key] = float(value)
            else:
                current[last_key] = value

