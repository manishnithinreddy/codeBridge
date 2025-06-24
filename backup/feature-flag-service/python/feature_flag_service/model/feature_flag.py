"""
Feature flag module for the Feature Flag Service.
"""

from datetime import datetime
from typing import Dict, List, Optional

from pydantic import BaseModel, Field

from feature_flag_service.model.flag_rule import FlagRule
from feature_flag_service.model.flag_value import FlagValue


class FeatureFlag(BaseModel):
    """
    Model for a feature flag.
    """
    key: str
    namespace: str
    value: FlagValue
    description: Optional[str] = None
    tags: Dict[str, str] = Field(default_factory=dict)
    temporary: bool = False
    expires_at: Optional[datetime] = None
    version: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
    created_by: Optional[str] = None
    updated_by: Optional[str] = None
    rules: List[FlagRule] = Field(default_factory=list)

    def is_expired(self) -> bool:
        """
        Check if the flag is expired.
        """
        return self.temporary and self.expires_at is not None and datetime.now() > self.expires_at

    def set_expiration(self, duration_seconds: int) -> None:
        """
        Set the expiration time for a temporary flag.
        """
        self.temporary = True
        self.expires_at = datetime.now().timestamp() + duration_seconds

    def add_tag(self, key: str, value: str) -> None:
        """
        Add a tag to the flag.
        """
        self.tags[key] = value

    def remove_tag(self, key: str) -> None:
        """
        Remove a tag from the flag.
        """
        if key in self.tags:
            del self.tags[key]

    def add_rule(self, rule: FlagRule) -> None:
        """
        Add a rule to the flag.
        """
        self.rules.append(rule)

    def remove_rule(self, rule_id: str) -> None:
        """
        Remove a rule from the flag.
        """
        self.rules = [rule for rule in self.rules if rule.id != rule_id]

