"""
gRPC service implementation for the Feature Flag Service.
"""

import asyncio
import logging
import time
from typing import Dict, List, Optional

from feature_flag_service.grpc.feature_flag_service_pb2 import (
    EvaluationContext,
    EvaluationReason,
    FlagEvaluationRequest,
    FlagEvaluationResponse,
    FlagMetadata,
    FlagRequest,
    FlagResponse,
    FlagSummary,
    FlagUpdateEvent,
    HealthCheckRequest,
    HealthCheckResponse,
    ListFlagsRequest,
    ListFlagsResponse,
    MultiFlagRequest,
    MultiFlagResponse,
    ServiceContext,
    SetFlagRequest,
    SetFlagResponse,
)
from feature_flag_service.model.evaluation_context import EvaluationContext as ModelEvaluationContext
from feature_flag_service.model.evaluation_context import ServiceContext as ModelServiceContext
from feature_flag_service.model.feature_flag import FeatureFlag
from feature_flag_service.model.flag_value import FlagValue, ValueType
from feature_flag_service.service.feature_flag_service import FeatureFlagService

logger = logging.getLogger(__name__)


class FeatureFlagGrpcService:
    """
    gRPC service implementation for the Feature Flag Service.
    """

    def __init__(self, service: FeatureFlagService):
        """
        Initialize the service.
        """
        self.service = service
        self.subscribers = {}

    async def GetFlag(self, request: FlagRequest, context) -> FlagResponse:
        """
        Get a flag by key.
        """
        try:
            flag = await self.service.get_flag(request.flag_key, request.namespace)
            if not flag:
                return FlagResponse(
                    flag_key=request.flag_key,
                    exists=False,
                )

            return FlagResponse(
                flag_key=flag.key,
                exists=True,
                metadata=self._create_flag_metadata(flag),
                value=self._create_flag_value(flag.value),
            )
        except Exception as e:
            logger.error(f"Error getting flag: {e}")
            context.set_code(500)
            context.set_details(str(e))
            return FlagResponse(
                flag_key=request.flag_key,
                exists=False,
            )

    async def GetFlags(self, request: MultiFlagRequest, context) -> MultiFlagResponse:
        """
        Get multiple flags by keys.
        """
        try:
            flags = await self.service.get_flags(request.flag_keys, request.namespace)
            responses = []

            for flag in flags:
                responses.append(FlagResponse(
                    flag_key=flag.key,
                    exists=True,
                    metadata=self._create_flag_metadata(flag),
                    value=self._create_flag_value(flag.value),
                ))

            # Add empty responses for missing flags
            existing_keys = {flag.key for flag in flags}
            for key in request.flag_keys:
                if key not in existing_keys:
                    responses.append(FlagResponse(
                        flag_key=key,
                        exists=False,
                    ))

            return MultiFlagResponse(flags=responses)
        except Exception as e:
            logger.error(f"Error getting flags: {e}")
            context.set_code(500)
            context.set_details(str(e))
            return MultiFlagResponse()

    async def EvaluateFlag(self, request: FlagEvaluationRequest, context) -> FlagEvaluationResponse:
        """
        Evaluate a flag with context.
        """
        try:
            # Convert gRPC context to model context
            model_context = self._create_model_context(request.context)

            # Evaluate the flag
            result = await self.service.evaluate_flag(request.flag_key, request.namespace, model_context)

            # Convert result to gRPC response
            response = FlagEvaluationResponse(
                flag_key=result.flag_key,
                variation_id=result.variation_id or "",
                rule_id=result.rule_id or "",
            )

            # Add reason
            if result.reason:
                response.reason = EvaluationReason(
                    type=result.reason.type,
                    rule_id=result.reason.rule_id or "",
                    description=result.reason.description or "",
                    error_message=result.reason.error_message or "",
                )

            # Add value
            if result.value:
                response.value = self._create_flag_value(result.value)

            return response
        except Exception as e:
            logger.error(f"Error evaluating flag: {e}")
            context.set_code(500)
            context.set_details(str(e))
            return FlagEvaluationResponse(
                flag_key=request.flag_key,
                reason=EvaluationReason(
                    type="ERROR",
                    error_message=str(e),
                ),
            )

    async def SetFlag(self, request: SetFlagRequest, context) -> SetFlagResponse:
        """
        Set a flag.
        """
        try:
            # Create the flag
            flag = FeatureFlag(
                key=request.flag_key,
                namespace=request.namespace,
                value=self._create_model_flag_value(request.value),
                description=request.description,
                tags=request.tags,
                temporary=request.temporary,
                version=request.version,
            )

            # Set expiration time if provided
            if request.expiration_time > 0:
                flag.expires_at = request.expiration_time

            # Set the flag
            await self.service.set_flag(flag)

            return SetFlagResponse(
                success=True,
                version=flag.version or "",
            )
        except Exception as e:
            logger.error(f"Error setting flag: {e}")
            context.set_code(500)
            context.set_details(str(e))
            return SetFlagResponse(
                success=False,
                message=str(e),
            )

    async def StreamFlagUpdates(self, request, context):
        """
        Stream flag updates.
        """
        # Create a unique subscriber ID
        subscriber_id = f"grpc-{id(context)}"

        # Create a queue for updates
        queue = asyncio.Queue()
        self.subscribers[subscriber_id] = queue

        # Subscribe to flag updates
        async def on_flag_update(flag):
            # Create an update event
            event = FlagUpdateEvent(
                event_type="UPDATE",
                flag_key=flag.key,
                namespace=flag.namespace,
                version=flag.version or "",
                timestamp=int(time.time() * 1000),
            )

            # Add value if present
            if flag.value:
                event.value = self._create_flag_value(flag.value)

            # Add to queue
            await queue.put(event)

        # Subscribe to updates
        await self.service.subscribe(on_flag_update)

        try:
            # Stream updates
            while True:
                # Check if the context is cancelled
                if context.is_active():
                    # Get the next update
                    event = await queue.get()
                    yield event
                else:
                    # Context is cancelled, stop streaming
                    break
        finally:
            # Unsubscribe and clean up
            await self.service.unsubscribe()
            if subscriber_id in self.subscribers:
                del self.subscribers[subscriber_id]

    async def ListFlags(self, request: ListFlagsRequest, context) -> ListFlagsResponse:
        """
        List flags.
        """
        try:
            # Get flags
            if request.tag_filter:
                # Parse tag filter
                tag_parts = request.tag_filter.split("=", 1)
                if len(tag_parts) != 2:
                    context.set_code(400)
                    context.set_details("Invalid tag filter format, expected key=value")
                    return ListFlagsResponse()

                tag_key, tag_value = tag_parts
                flags = await self.service.list_flags_by_tag(request.namespace, tag_key, tag_value)
            else:
                # List flags by namespace and prefix
                flags = await self.service.list_flags(request.namespace, request.prefix)

            # Convert to summaries
            summaries = []
            for flag in flags:
                summaries.append(FlagSummary(
                    flag_key=flag.key,
                    namespace=flag.namespace,
                    description=flag.description or "",
                    value_type=flag.value.type,
                    updated_at=int(flag.updated_at.timestamp() * 1000),
                    temporary=flag.temporary,
                    expiration_time=int(flag.expires_at.timestamp() * 1000) if flag.expires_at else 0,
                    tags=flag.tags,
                ))

            # Apply pagination
            page_size = request.page_size or 100
            page_token = request.page_token or "0"
            start_index = int(page_token)
            end_index = start_index + page_size
            paginated_summaries = summaries[start_index:end_index]

            # Create next page token
            next_page_token = str(end_index) if end_index < len(summaries) else ""

            return ListFlagsResponse(
                flags=paginated_summaries,
                total_count=len(summaries),
                next_page_token=next_page_token,
            )
        except Exception as e:
            logger.error(f"Error listing flags: {e}")
            context.set_code(500)
            context.set_details(str(e))
            return ListFlagsResponse()

    async def HealthCheck(self, request: HealthCheckRequest, context) -> HealthCheckResponse:
        """
        Health check.
        """
        return HealthCheckResponse(
            status="UP",
            version="1.0.0",
            details={
                "service": "feature-flag-service",
            },
        )

    def _create_flag_metadata(self, flag: FeatureFlag) -> FlagMetadata:
        """
        Create flag metadata from a model flag.
        """
        return FlagMetadata(
            description=flag.description or "",
            created_at=int(flag.created_at.timestamp() * 1000),
            updated_at=int(flag.updated_at.timestamp() * 1000),
            created_by=flag.created_by or "",
            updated_by=flag.updated_by or "",
            version=flag.version or "",
            tags=flag.tags,
        )

    def _create_flag_value(self, value: FlagValue):
        """
        Create a gRPC flag value from a model flag value.
        """
        # This would normally convert to the appropriate gRPC value type
        # For this example, we'll just return a placeholder
        return {
            "type": value.type,
            "value": str(value.value),
        }

    def _create_model_flag_value(self, value_dict: Dict) -> FlagValue:
        """
        Create a model flag value from a gRPC flag value.
        """
        # This would normally convert from the gRPC value type
        # For this example, we'll just create a simple value
        value_type = value_dict.get("type", "STRING")
        value = value_dict.get("value", "")

        if value_type == "BOOLEAN":
            return FlagValue.boolean(value.lower() == "true")
        elif value_type == "STRING":
            return FlagValue.string(value)
        elif value_type == "INTEGER":
            return FlagValue.integer(int(value))
        elif value_type == "DOUBLE":
            return FlagValue.double(float(value))
        elif value_type == "JSON":
            return FlagValue.json(value)
        else:
            return FlagValue.string(value)

    def _create_model_context(self, context: EvaluationContext) -> ModelEvaluationContext:
        """
        Create a model evaluation context from a gRPC context.
        """
        model_context = ModelEvaluationContext(
            user_id=context.user_id,
            session_id=context.session_id,
            attributes=context.attributes,
            numeric_attributes=context.numeric_attributes,
            boolean_attributes=context.boolean_attributes,
        )

        # Add service context if present
        if context.service_context:
            model_context.service_context = ModelServiceContext(
                service_name=context.service_context.service_name,
                service_version=context.service_context.service_version,
                instance_id=context.service_context.instance_id,
                environment=context.service_context.environment,
                metrics=context.service_context.metrics,
            )

        return model_context

