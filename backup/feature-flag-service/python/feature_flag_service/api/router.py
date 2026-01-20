"""
API router for the Feature Flag Service.
"""

from fastapi import APIRouter, Depends, HTTPException
from typing import Dict, List, Optional

from feature_flag_service.api.schemas import (
    EvaluateFlagRequest,
    EvaluateFlagResponse,
    FeatureFlagResponse,
    SetFlagRequest,
    SetFlagResponse,
)
from feature_flag_service.model.evaluation_context import EvaluationContext, ServiceContext
from feature_flag_service.model.feature_flag import FeatureFlag
from feature_flag_service.model.flag_value import FlagValue
from feature_flag_service.service.feature_flag_service import FeatureFlagService


def get_service() -> FeatureFlagService:
    """
    Get the feature flag service.
    This would typically be injected via dependency injection.
    """
    # This is a placeholder - in a real app, this would be injected
    raise NotImplementedError("Service dependency not configured")


def create_api_router() -> APIRouter:
    """
    Create the API router.
    """
    router = APIRouter(prefix="/api/v1")

    @router.get("/flags/{key}", response_model=FeatureFlagResponse)
    async def get_flag(
        key: str,
        namespace: Optional[str] = None,
        service: FeatureFlagService = Depends(get_service),
    ):
        """
        Get a flag by key.
        """
        flag = await service.get_flag(key, namespace)
        if not flag:
            raise HTTPException(status_code=404, detail="Flag not found")
        return flag

    @router.get("/flags", response_model=List[FeatureFlagResponse])
    async def get_flags(
        keys: Optional[str] = None,
        namespace: Optional[str] = None,
        prefix: Optional[str] = None,
        tag: Optional[str] = None,
        service: FeatureFlagService = Depends(get_service),
    ):
        """
        Get multiple flags.
        """
        if keys:
            # Get flags by keys
            key_list = keys.split(",")
            return await service.get_flags(key_list, namespace)
        elif tag:
            # Get flags by tag
            tag_parts = tag.split("=", 1)
            if len(tag_parts) != 2:
                raise HTTPException(status_code=400, detail="Invalid tag format, expected key=value")
            tag_key, tag_value = tag_parts
            return await service.list_flags_by_tag(namespace, tag_key, tag_value)
        else:
            # List flags
            return await service.list_flags(namespace, prefix)

    @router.put("/flags/{key}", response_model=SetFlagResponse)
    async def set_flag(
        key: str,
        request: SetFlagRequest,
        service: FeatureFlagService = Depends(get_service),
    ):
        """
        Set a flag.
        """
        # Create the flag
        flag = FeatureFlag(
            key=key,
            namespace=request.namespace,
            value=_create_flag_value(request.value_type, request.value),
            description=request.description,
            tags=request.tags,
            temporary=request.temporary,
            version=request.version,
        )

        # Set expiration time if provided
        if request.expiration_time:
            flag.expires_at = request.expiration_time

        # Set the flag
        await service.set_flag(flag)

        return SetFlagResponse(success=True, version=flag.version)

    @router.delete("/flags/{key}")
    async def delete_flag(
        key: str,
        namespace: Optional[str] = None,
        service: FeatureFlagService = Depends(get_service),
    ):
        """
        Delete a flag.
        """
        await service.delete_flag(key, namespace)
        return {"success": True}

    @router.post("/flags/{key}/evaluate", response_model=EvaluateFlagResponse)
    async def evaluate_flag(
        key: str,
        request: EvaluateFlagRequest,
        service: FeatureFlagService = Depends(get_service),
    ):
        """
        Evaluate a flag.
        """
        # Create the evaluation context
        context = EvaluationContext(
            user_id=request.user_id,
            session_id=request.session_id,
            attributes=request.attributes,
            numeric_attributes=request.numeric_attributes,
            boolean_attributes=request.boolean_attributes,
        )

        # Add service context if provided
        if request.service_context:
            context.service_context = ServiceContext(
                service_name=request.service_context.service_name,
                service_version=request.service_context.service_version,
                instance_id=request.service_context.instance_id,
                environment=request.service_context.environment,
                metrics=request.service_context.metrics,
            )

        # Evaluate the flag
        result = await service.evaluate_flag(key, request.namespace, context)

        return result

    return router


def _create_flag_value(value_type: str, value: any) -> FlagValue:
    """
    Create a flag value from a type and value.
    """
    if value_type == "BOOLEAN":
        return FlagValue.boolean(value)
    elif value_type == "STRING":
        return FlagValue.string(value)
    elif value_type == "INTEGER":
        return FlagValue.integer(value)
    elif value_type == "DOUBLE":
        return FlagValue.double(value)
    elif value_type == "JSON":
        return FlagValue.json(value)
    else:
        raise HTTPException(status_code=400, detail=f"Unsupported value type: {value_type}")

