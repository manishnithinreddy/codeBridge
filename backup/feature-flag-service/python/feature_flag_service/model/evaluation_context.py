"""
Evaluation context module for the Feature Flag Service.
"""

from typing import Dict, Optional

from pydantic import BaseModel, Field


class ServiceContext(BaseModel):
    """
    Model for a service context.
    """
    service_name: Optional[str] = None
    service_version: Optional[str] = None
    instance_id: Optional[str] = None
    environment: Optional[str] = None
    metrics: Dict[str, str] = Field(default_factory=dict)

    def set_metric(self, key: str, value: str) -> None:
        """
        Set a service metric.
        """
        self.metrics[key] = value


class EvaluationContext(BaseModel):
    """
    Model for an evaluation context.
    """
    user_id: Optional[str] = None
    session_id: Optional[str] = None
    attributes: Dict[str, str] = Field(default_factory=dict)
    numeric_attributes: Dict[str, float] = Field(default_factory=dict)
    boolean_attributes: Dict[str, bool] = Field(default_factory=dict)
    service_context: Optional[ServiceContext] = None

    def to_map(self) -> Dict:
        """
        Convert the evaluation context to a map for rule evaluation.
        """
        result = {}

        # Add user and session IDs
        if self.user_id:
            result["userId"] = self.user_id
        if self.session_id:
            result["sessionId"] = self.session_id

        # Add attributes
        result.update(self.attributes)
        result.update(self.numeric_attributes)
        result.update(self.boolean_attributes)

        # Add service context
        if self.service_context:
            service = {}
            
            if self.service_context.service_name:
                service["name"] = self.service_context.service_name
            if self.service_context.service_version:
                service["version"] = self.service_context.service_version
            if self.service_context.instance_id:
                service["instanceId"] = self.service_context.instance_id
            if self.service_context.environment:
                service["environment"] = self.service_context.environment
            
            # Add metrics
            if self.service_context.metrics:
                service["metrics"] = self.service_context.metrics
            
            result["service"] = service

        return result

    def set_attribute(self, key: str, value: str) -> None:
        """
        Set a string attribute.
        """
        self.attributes[key] = value

    def set_numeric_attribute(self, key: str, value: float) -> None:
        """
        Set a numeric attribute.
        """
        self.numeric_attributes[key] = value

    def set_boolean_attribute(self, key: str, value: bool) -> None:
        """
        Set a boolean attribute.
        """
        self.boolean_attributes[key] = value

    def set_service_context(self, service_context: ServiceContext) -> None:
        """
        Set the service context.
        """
        self.service_context = service_context

