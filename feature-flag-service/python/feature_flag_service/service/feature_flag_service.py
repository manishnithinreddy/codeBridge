"""
Feature flag service for the Feature Flag Service.
"""

import logging
from datetime import datetime
from typing import Callable, List, Optional

from feature_flag_service.model.evaluation_context import EvaluationContext
from feature_flag_service.model.evaluation_result import EvaluationResult
from feature_flag_service.model.feature_flag import FeatureFlag
from feature_flag_service.repository.feature_flag_repository import FeatureFlagRepository

logger = logging.getLogger(__name__)


class FeatureFlagService:
    """
    Service for feature flag operations.
    """

    def __init__(self, repository: FeatureFlagRepository, default_namespace: str = "default"):
        """
        Initialize the service.
        """
        self.repository = repository
        self.default_namespace = default_namespace

    async def get_flag(self, key: str, namespace: Optional[str] = None) -> Optional[FeatureFlag]:
        """
        Get a flag by key and namespace.
        """
        # Use default namespace if not provided
        if namespace is None:
            namespace = self.default_namespace

        return await self.repository.get_flag(key, namespace)

    async def get_flags(self, keys: List[str], namespace: Optional[str] = None) -> List[FeatureFlag]:
        """
        Get multiple flags by keys and namespace.
        """
        # Use default namespace if not provided
        if namespace is None:
            namespace = self.default_namespace

        return await self.repository.get_flags(keys, namespace)

    async def set_flag(self, flag: FeatureFlag) -> None:
        """
        Set a flag.
        """
        # Use default namespace if not provided
        if not flag.namespace:
            flag.namespace = self.default_namespace

        # Set created time if not set
        if not flag.created_at:
            flag.created_at = datetime.now()

        # Set updated time
        flag.updated_at = datetime.now()

        await self.repository.set_flag(flag)

    async def delete_flag(self, key: str, namespace: Optional[str] = None) -> None:
        """
        Delete a flag.
        """
        # Use default namespace if not provided
        if namespace is None:
            namespace = self.default_namespace

        await self.repository.delete_flag(key, namespace)

    async def list_flags(self, namespace: Optional[str] = None, prefix: Optional[str] = None) -> List[FeatureFlag]:
        """
        List flags by namespace and optional prefix.
        """
        # Use default namespace if not provided
        if namespace is None:
            namespace = self.default_namespace

        return await self.repository.list_flags(namespace, prefix)

    async def list_flags_by_tag(self, namespace: Optional[str], tag_key: str, tag_value: str) -> List[FeatureFlag]:
        """
        List flags by tag.
        """
        # Use default namespace if not provided
        if namespace is None:
            namespace = self.default_namespace

        return await self.repository.list_flags_by_tag(namespace, tag_key, tag_value)

    async def evaluate_flag(self, key: str, namespace: Optional[str], context: EvaluationContext) -> EvaluationResult:
        """
        Evaluate a flag with context.
        """
        # Use default namespace if not provided
        if namespace is None:
            namespace = self.default_namespace

        # Get the flag
        flag = await self.repository.get_flag(key, namespace)
        if not flag:
            return EvaluationResult.with_default_value(key)

        # If flag is expired, return default result
        if flag.is_expired():
            return EvaluationResult.with_disabled(key, "Flag is expired")

        # If no rules, return the flag value
        if not flag.rules:
            return EvaluationResult.with_fallthrough(key, flag.value)

        # Convert evaluation context to map
        ctx_map = context.to_map()

        # Evaluate rules
        for rule in flag.rules:
            try:
                if rule.evaluate(ctx_map):
                    # Rule matched, return the rule's value
                    return EvaluationResult.with_rule_match(key, rule.value, rule.id)
            except Exception as e:
                logger.error(f"Error evaluating rule {rule.id}: {e}")

        # No rules matched, return the flag value
        return EvaluationResult.with_fallthrough(key, flag.value)

    async def subscribe(self, callback: Callable[[FeatureFlag], None]) -> None:
        """
        Subscribe to flag updates.
        """
        await self.repository.subscribe(callback)

    async def unsubscribe(self) -> None:
        """
        Unsubscribe from flag updates.
        """
        await self.repository.unsubscribe()

