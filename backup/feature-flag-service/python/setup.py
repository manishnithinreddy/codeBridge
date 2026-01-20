from setuptools import setup, find_packages

setup(
    name="feature-flag-service",
    version="1.0.0",
    packages=find_packages(),
    install_requires=[
        "fastapi>=0.95.0",
        "uvicorn>=0.21.1",
        "redis>=4.5.4",
        "pydantic>=1.10.7",
        "grpcio>=1.54.0",
        "grpcio-tools>=1.54.0",
        "protobuf>=4.22.3",
        "python-dotenv>=1.0.0",
        "pyyaml>=6.0",
    ],
    entry_points={
        "console_scripts": [
            "feature-flag-service=feature_flag_service.main:main",
        ],
    },
)

