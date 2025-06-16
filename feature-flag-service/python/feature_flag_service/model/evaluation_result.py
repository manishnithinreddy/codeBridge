"""
Evaluation result module for the Feature Flag Service.
"""

from enum import Enum
from typing import Optional

from pydantic import BaseModel

from feature_flag_service.model.flag_value import FlagValue


class ReasonType(str, Enum):
    """
    Enum for evaluation reason types.
    """
    DEFAULT = "DEFAULT"
    RULE_MATCH = "RULE_MATCH"
    PREREQUISITE_FAILED = "PREREQUISITE_FAILED"
    ERROR = "ERROR"
    DISABLED = "DISABLED"
    FALLTHROUGH = "FALLTHROUGH"
    TARGET_MATCH = "TARGET_MATCH"


class EvaluationReason(BaseModel):
    """
    Model for an evaluation reason.
    """
    type: ReasonType
    rule_id: Optional[str] = None
    description: Optional[str] = None
    error_message: Optional[str] = None


class EvaluationResult(BaseModel):
    """
    Model for an evaluation result.
    """
    flag_key: str
    value: Optional[FlagValue] = None
    variation_id: Optional[str] = None
    rule_id: Optional[str] = None
    reason: Optional[EvaluationReason] = None

    @classmethod
    def with_default_value(cls, flag_key: str, value: Optional[FlagValue] = None) -> "EvaluationResult":
        """
        Create a result with a default value.
        """
        return cls(
            flag_key=flag_key,
            value=value,
            reason=EvaluationReason(
                type=ReasonType.DEFAULT,
                description="Default value used",
            ),
        )

    @classmethod
    def with_rule_match(cls, flag_key: str, value: FlagValue, rule_id: str, variation_id: Optional[str] = None) -> "EvaluationResult":
        """
        Create a result with a rule match.
        """
        return cls(
            flag_key=flag_key,
            value=value,
            rule_id=rule_id,
            variation_id=variation_id,
            reason=EvaluationReason(
                type=ReasonType.RULE_MATCH,
                rule_id=rule_id,
                description=f"Rule {rule_id} matched",
            ),
        )

    @classmethod
    def with_error(cls, flag_key: str, error_message: str) -> "EvaluationResult":
        """
        Create a result with an error.
        """
        return cls(
            flag_key=flag_key,
            reason=EvaluationReason(
                type=ReasonType.ERROR,
                error_message=error_message,
            ),
        )

    @classmethod
    def with_disabled(cls, flag_key: str, description: str) -> "EvaluationResult":
        """
        Create a result for a disabled flag.
        """
        return cls(
            flag_key=flag_key,
            reason=EvaluationReason(
                type=ReasonType.DISABLED,
                description=description,
            ),
        )

    @classmethod
    def with_fallthrough(cls, flag_key: str, value: FlagValue) -> "EvaluationResult":
        """
        Create a result with a fallthrough value.
        """
        return cls(
            flag_key=flag_key,
            value=value,
            reason=EvaluationReason(
                type=ReasonType.FALLTHROUGH,
                description="No rules matched, using fallthrough value",
            ),
        )

