"""
Value type module for the Feature Flag Service.
"""

from enum import Enum, auto


class ValueType(str, Enum):
    """
    Enum for feature flag value types.
    """
    BOOLEAN = "BOOLEAN"
    STRING = "STRING"
    INTEGER = "INTEGER"
    DOUBLE = "DOUBLE"
    JSON = "JSON"

