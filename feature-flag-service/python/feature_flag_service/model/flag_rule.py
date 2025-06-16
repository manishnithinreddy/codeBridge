"""
Flag rule module for the Feature Flag Service.
"""

import re
from typing import Any, Dict, List, Optional, Union

from pydantic import BaseModel, Field

from feature_flag_service.model.flag_value import FlagValue


class Condition(BaseModel):
    """
    Model for a condition in a flag rule.
    """
    attribute: str
    operator: str
    value: Any


class Variation(BaseModel):
    """
    Model for a variation in a flag rule.
    """
    id: str
    value: FlagValue
    weight: int
    description: Optional[str] = None


class FlagRule(BaseModel):
    """
    Model for a flag rule.
    """
    id: str
    name: Optional[str] = None
    description: Optional[str] = None
    priority: int
    conditions: List[Condition] = Field(default_factory=list)
    value: FlagValue
    variations: List[Variation] = Field(default_factory=list)

    # Operators
    OPERATOR_EQUALS = "equals"
    OPERATOR_NOT_EQUALS = "notEquals"
    OPERATOR_GREATER_THAN = "greaterThan"
    OPERATOR_GREATER_THAN_OR_EQUALS = "greaterThanOrEquals"
    OPERATOR_LESS_THAN = "lessThan"
    OPERATOR_LESS_THAN_OR_EQUALS = "lessThanOrEquals"
    OPERATOR_CONTAINS = "contains"
    OPERATOR_NOT_CONTAINS = "notContains"
    OPERATOR_STARTS_WITH = "startsWith"
    OPERATOR_ENDS_WITH = "endsWith"
    OPERATOR_MATCHES = "matches"
    OPERATOR_IN = "in"
    OPERATOR_NOT_IN = "notIn"

    def add_condition(self, attribute: str, operator: str, value: Any) -> None:
        """
        Add a condition to the rule.
        """
        self.conditions.append(Condition(
            attribute=attribute,
            operator=operator,
            value=value,
        ))

    def add_variation(self, id: str, value: FlagValue, weight: int, description: Optional[str] = None) -> None:
        """
        Add a variation to the rule.
        """
        self.variations.append(Variation(
            id=id,
            value=value,
            weight=weight,
            description=description,
        ))

    def evaluate(self, context: Dict[str, Any]) -> bool:
        """
        Evaluate the rule against the given context.
        """
        # If there are no conditions, the rule matches
        if not self.conditions:
            return True

        # All conditions must match
        for condition in self.conditions:
            if not self._evaluate_condition(condition, context):
                return False

        return True

    def _evaluate_condition(self, condition: Condition, context: Dict[str, Any]) -> bool:
        """
        Evaluate a single condition against the context.
        """
        # Get the attribute value from the context
        attr_value = self._get_attribute_value(condition.attribute, context)
        if attr_value is None:
            # If the attribute doesn't exist, the condition doesn't match
            return False

        # Evaluate the condition based on the operator
        if condition.operator == self.OPERATOR_EQUALS:
            return self._equals(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_NOT_EQUALS:
            return not self._equals(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_GREATER_THAN:
            return self._greater_than(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_GREATER_THAN_OR_EQUALS:
            return self._greater_than_or_equals(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_LESS_THAN:
            return self._less_than(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_LESS_THAN_OR_EQUALS:
            return self._less_than_or_equals(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_CONTAINS:
            return self._contains(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_NOT_CONTAINS:
            return not self._contains(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_STARTS_WITH:
            return self._starts_with(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_ENDS_WITH:
            return self._ends_with(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_MATCHES:
            return self._matches(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_IN:
            return self._in(attr_value, condition.value)
        elif condition.operator == self.OPERATOR_NOT_IN:
            return not self._in(attr_value, condition.value)
        else:
            # Unknown operator
            return False

    def _get_attribute_value(self, attribute: str, context: Dict[str, Any]) -> Optional[Any]:
        """
        Get the value of an attribute from the context.
        Supports nested attributes with dot notation (e.g., "user.id").
        """
        parts = attribute.split(".")
        current = context

        # Navigate through nested objects
        for i, part in enumerate(parts):
            if i == len(parts) - 1:
                # Last part, get the value
                return current.get(part)

            # Not the last part, navigate to the next level
            if part not in current or not isinstance(current[part], dict):
                return None

            current = current[part]

        return None

    def _equals(self, a: Any, b: Any) -> bool:
        """
        Check if two values are equal.
        """
        return str(a) == str(b)

    def _greater_than(self, a: Any, b: Any) -> bool:
        """
        Check if a > b.
        """
        try:
            return float(a) > float(b)
        except (ValueError, TypeError):
            return str(a) > str(b)

    def _greater_than_or_equals(self, a: Any, b: Any) -> bool:
        """
        Check if a >= b.
        """
        try:
            return float(a) >= float(b)
        except (ValueError, TypeError):
            return str(a) >= str(b)

    def _less_than(self, a: Any, b: Any) -> bool:
        """
        Check if a < b.
        """
        try:
            return float(a) < float(b)
        except (ValueError, TypeError):
            return str(a) < str(b)

    def _less_than_or_equals(self, a: Any, b: Any) -> bool:
        """
        Check if a <= b.
        """
        try:
            return float(a) <= float(b)
        except (ValueError, TypeError):
            return str(a) <= str(b)

    def _contains(self, a: Any, b: Any) -> bool:
        """
        Check if a contains b.
        """
        return str(b) in str(a)

    def _starts_with(self, a: Any, b: Any) -> bool:
        """
        Check if a starts with b.
        """
        return str(a).startswith(str(b))

    def _ends_with(self, a: Any, b: Any) -> bool:
        """
        Check if a ends with b.
        """
        return str(a).endswith(str(b))

    def _matches(self, a: Any, b: Any) -> bool:
        """
        Check if a matches the regex pattern b.
        """
        try:
            pattern = re.compile(str(b))
            return bool(pattern.match(str(a)))
        except re.error:
            return False

    def _in(self, a: Any, b: Any) -> bool:
        """
        Check if a is in the list b.
        """
        if isinstance(b, list):
            return str(a) in [str(item) for item in b]
        elif isinstance(b, str):
            items = [item.strip() for item in b.split(",")]
            return str(a) in items
        return False

