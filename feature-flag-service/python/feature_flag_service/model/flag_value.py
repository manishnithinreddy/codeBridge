"""
Flag value module for the Feature Flag Service.
"""

import json
from typing import Any, Dict, Optional, Union

from pydantic import BaseModel, Field, validator

from feature_flag_service.model.value_type import ValueType


class FlagValue(BaseModel):
    """
    Model for a feature flag value.
    """
    type: ValueType
    value: Any

    @validator("value")
    def validate_value_type(cls, value, values):
        """
        Validate that the value matches the type.
        """
        if "type" not in values:
            return value

        value_type = values["type"]

        if value_type == ValueType.BOOLEAN and not isinstance(value, bool):
            try:
                if isinstance(value, str):
                    if value.lower() == "true":
                        return True
                    elif value.lower() == "false":
                        return False
                raise ValueError(f"Cannot convert {value} to boolean")
            except Exception as e:
                raise ValueError(f"Value must be a boolean for type {value_type}: {e}")

        elif value_type == ValueType.STRING and not isinstance(value, str):
            try:
                return str(value)
            except Exception as e:
                raise ValueError(f"Value must be a string for type {value_type}: {e}")

        elif value_type == ValueType.INTEGER and not isinstance(value, int):
            try:
                return int(value)
            except Exception as e:
                raise ValueError(f"Value must be an integer for type {value_type}: {e}")

        elif value_type == ValueType.DOUBLE and not isinstance(value, (int, float)):
            try:
                return float(value)
            except Exception as e:
                raise ValueError(f"Value must be a number for type {value_type}: {e}")

        elif value_type == ValueType.JSON and not isinstance(value, (str, dict, list)):
            try:
                if isinstance(value, (dict, list)):
                    return json.dumps(value)
                return str(value)
            except Exception as e:
                raise ValueError(f"Value must be valid JSON for type {value_type}: {e}")

        return value

    def get_boolean_value(self) -> bool:
        """
        Get the boolean value.
        """
        if self.type != ValueType.BOOLEAN:
            raise ValueError("Flag value is not a boolean")
        return bool(self.value)

    def get_string_value(self) -> str:
        """
        Get the string value.
        """
        if self.type != ValueType.STRING:
            raise ValueError("Flag value is not a string")
        return str(self.value)

    def get_integer_value(self) -> int:
        """
        Get the integer value.
        """
        if self.type != ValueType.INTEGER:
            raise ValueError("Flag value is not an integer")
        return int(self.value)

    def get_double_value(self) -> float:
        """
        Get the double value.
        """
        if self.type != ValueType.DOUBLE:
            raise ValueError("Flag value is not a double")
        return float(self.value)

    def get_json_value(self) -> str:
        """
        Get the JSON value.
        """
        if self.type != ValueType.JSON:
            raise ValueError("Flag value is not JSON")
        
        if isinstance(self.value, str):
            # Validate that it's valid JSON
            try:
                json.loads(self.value)
                return self.value
            except json.JSONDecodeError:
                raise ValueError("Invalid JSON string")
        
        # Convert to JSON string
        return json.dumps(self.value)

    @classmethod
    def boolean(cls, value: bool) -> "FlagValue":
        """
        Create a boolean flag value.
        """
        return cls(type=ValueType.BOOLEAN, value=value)

    @classmethod
    def string(cls, value: str) -> "FlagValue":
        """
        Create a string flag value.
        """
        return cls(type=ValueType.STRING, value=value)

    @classmethod
    def integer(cls, value: int) -> "FlagValue":
        """
        Create an integer flag value.
        """
        return cls(type=ValueType.INTEGER, value=value)

    @classmethod
    def double(cls, value: float) -> "FlagValue":
        """
        Create a double flag value.
        """
        return cls(type=ValueType.DOUBLE, value=value)

    @classmethod
    def json(cls, value: Union[str, Dict, list]) -> "FlagValue":
        """
        Create a JSON flag value.
        """
        if isinstance(value, (dict, list)):
            value = json.dumps(value)
        return cls(type=ValueType.JSON, value=value)

