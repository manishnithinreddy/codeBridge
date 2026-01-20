"""
API schemas for the Feature Flag Service.
"""

from datetime import datetime
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field

from feature_flag_service.model.evaluation_result import EvaluationReason, EvaluationResult
from feature_flag_service.model.feature_flag import FeatureFlag
from feature_flag_service.model.flag_rule import FlagRule
from feature_flag_service.model.flag_value import FlagValue


# Request schemas

class ServiceContextRequest(BaseModel):
    """
    Request schema for a service context.
    """
    service_name: Optional[str] = None
    service_version: Optional[str] = None
    instance_id: Optional[str] = None
    environment: Optional[str] = None
    metrics: Dict[str, str] = Field(default_factory=dict)


class EvaluateFlagRequest(BaseModel):
    """
    Request schema for evaluating a flag.
    """
    namespace: Optional[str] = None
    user_id: Optional[str] = None
    session_id: Optional[str] = None
    attributes: Dict[str, str] = Field(default_factory=dict)
    numeric_attributes: Dict[str, float] = Field(default_factory=dict)
    boolean_attributes: Dict[str, bool] = Field(default_factory=dict)
    service_context: Optional[ServiceContextRequest] = None


class SetFlagRequest(BaseModel):
    """
    Request schema for setting a flag.
    """
    namespace: Optional[str] = None
    value: Any
    value_type: str
    description: Optional[str] = None
    tags: Dict[str, str] = Field(default_factory=dict)
    temporary: bool = False
    expiration_time: Optional[datetime] = None
    version: Optional[str] = None


# Response schemas

class FeatureFlagResponse(BaseModel):
    """
    Response schema for a feature flag.
    """
    key: str
    namespace: str
    value: FlagValue
    description: Optional[str] = None
    tags: Dict[str, str] = Field(default_factory=dict)
    temporary: bool = False
    expires_at: Optional[datetime] = None
    version: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    created_by: Optional[str] = None
    updated_by: Optional[str] = None
    rules: List[FlagRule] = Field(default_factory=list)

    class Config:
        orm_mode = True


class SetFlagResponse(BaseModel):
    """
    Response schema for setting a flag.
    """
    success: bool
    version: Optional[str] = None


class EvaluateFlagResponse(BaseModel):
    """
    Response schema for evaluating a flag.
    """
    flag_key: str
    value: Optional[FlagValue] = None
    variation_id: Optional[str] = None
    rule_id: Optional[str] = None
    reason: Optional[EvaluationReason] = None

    class Config:
        orm_mode = True

