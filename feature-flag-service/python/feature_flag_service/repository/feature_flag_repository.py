"""
Feature flag repository interface for the Feature Flag Service.
"""

from abc import ABC, abstractmethod
from typing import Callable, List, Optional

from feature_flag_service.model.feature_flag import FeatureFlag


class FeatureFlagRepository(ABC):
    """
    Interface for feature flag storage.
    """

    @abstractmethod
    async def get_flag(self, key: str, namespace: str) -> Optional[FeatureFlag]:
        """
        Get a flag by key and namespace.
        """
        pass

    @abstractmethod
    async def get_flags(self, keys: List[str], namespace: str) -> List[FeatureFlag]:
        """
        Get multiple flags by keys and namespace.
        """
        pass

    @abstractmethod
    async def set_flag(self, flag: FeatureFlag) -> None:
        """
        Set a flag.
        """
        pass

    @abstractmethod
    async def delete_flag(self, key: str, namespace: str) -> None:
        """
        Delete a flag.
        """
        pass

    @abstractmethod
    async def list_flags(self, namespace: str, prefix: Optional[str] = None) -> List[FeatureFlag]:
        """
        List flags by namespace and optional prefix.
        """
        pass

    @abstractmethod
    async def list_flags_by_tag(self, namespace: str, tag_key: str, tag_value: str) -> List[FeatureFlag]:
        """
        List flags by tag.
        """
        pass

    @abstractmethod
    async def subscribe(self, callback: Callable[[FeatureFlag], None]) -> None:
        """
        Subscribe to flag updates.
        """
        pass

    @abstractmethod
    async def unsubscribe(self) -> None:
        """
        Unsubscribe from flag updates.
        """
        pass

