"""
Redis implementation of the feature flag repository.
"""

import asyncio
import json
import logging
from datetime import datetime
from typing import Callable, Dict, List, Optional, Set

import redis.asyncio as redis

from feature_flag_service.model.feature_flag import FeatureFlag
from feature_flag_service.repository.feature_flag_repository import FeatureFlagRepository

logger = logging.getLogger(__name__)


class RedisFeatureFlagRepository(FeatureFlagRepository):
    """
    Redis implementation of the feature flag repository.
    """

    def __init__(
        self,
        host: str = "localhost",
        port: int = 6379,
        password: Optional[str] = None,
        db: int = 0,
        ttl_seconds: int = 0,
    ):
        """
        Initialize the repository.
        """
        self.redis = redis.Redis(
            host=host,
            port=port,
            password=password,
            db=db,
            decode_responses=True,
        )
        self.ttl_seconds = ttl_seconds
        self.pubsub = None
        self.callbacks: Set[Callable[[FeatureFlag], None]] = set()
        self.pubsub_task = None

    async def get_flag(self, key: str, namespace: str) -> Optional[FeatureFlag]:
        """
        Get a flag by key and namespace.
        """
        redis_key = self._get_flag_key(key, namespace)
        data = await self.redis.get(redis_key)

        if not data:
            return None

        try:
            flag_dict = json.loads(data)
            return FeatureFlag.parse_obj(flag_dict)
        except Exception as e:
            logger.error(f"Failed to parse flag data: {e}")
            return None

    async def get_flags(self, keys: List[str], namespace: str) -> List[FeatureFlag]:
        """
        Get multiple flags by keys and namespace.
        """
        if not keys:
            return []

        # Construct Redis keys
        redis_keys = [self._get_flag_key(key, namespace) for key in keys]

        # Get flags from Redis
        data_list = await self.redis.mget(redis_keys)

        # Parse flags
        flags = []
        for data in data_list:
            if not data:
                continue

            try:
                flag_dict = json.loads(data)
                flags.append(FeatureFlag.parse_obj(flag_dict))
            except Exception as e:
                logger.error(f"Failed to parse flag data: {e}")

        return flags

    async def set_flag(self, flag: FeatureFlag) -> None:
        """
        Set a flag.
        """
        # Update timestamps
        flag.updated_at = datetime.now()

        # Serialize the flag
        flag_dict = flag.dict()
        data = json.dumps(flag_dict)

        # Construct the Redis key
        redis_key = self._get_flag_key(flag.key, flag.namespace)

        # Set the flag in Redis
        if flag.temporary and flag.expires_at:
            # Use the flag's expiration time
            ttl = int((flag.expires_at - datetime.now()).total_seconds())
            if ttl <= 0:
                # Flag is already expired
                return
            await self.redis.setex(redis_key, ttl, data)
        elif self.ttl_seconds > 0:
            # Use the repository's TTL
            await self.redis.setex(redis_key, self.ttl_seconds, data)
        else:
            # No TTL
            await self.redis.set(redis_key, data)

        # Add to index
        await self._add_to_index(flag)

        # Publish update
        await self._publish_update(flag)

    async def delete_flag(self, key: str, namespace: str) -> None:
        """
        Delete a flag.
        """
        # Get the flag first to remove from indexes
        flag = await self.get_flag(key, namespace)
        if not flag:
            return

        # Construct the Redis key
        redis_key = self._get_flag_key(key, namespace)

        # Delete the flag from Redis
        await self.redis.delete(redis_key)

        # Remove from index
        await self._remove_from_index(flag)

        # Publish deletion
        flag.value = None  # Clear the value to indicate deletion
        await self._publish_update(flag)

    async def list_flags(self, namespace: str, prefix: Optional[str] = None) -> List[FeatureFlag]:
        """
        List flags by namespace and optional prefix.
        """
        # Construct the Redis key pattern
        pattern = f"{self._get_namespace_key(namespace)}:*"
        if prefix:
            pattern = f"{self._get_namespace_key(namespace)}:{prefix}*"

        # Scan for matching keys
        cursor = 0
        keys = []

        while True:
            cursor, batch = await self.redis.scan(cursor, match=pattern, count=100)
            keys.extend(batch)

            if cursor == 0:
                break

        if not keys:
            return []

        # Get flags from Redis
        data_list = await self.redis.mget(keys)

        # Parse flags
        flags = []
        for data in data_list:
            if not data:
                continue

            try:
                flag_dict = json.loads(data)
                flags.append(FeatureFlag.parse_obj(flag_dict))
            except Exception as e:
                logger.error(f"Failed to parse flag data: {e}")

        return flags

    async def list_flags_by_tag(self, namespace: str, tag_key: str, tag_value: str) -> List[FeatureFlag]:
        """
        List flags by tag.
        """
        # Construct the Redis key for the tag index
        index_key = self._get_tag_index_key(namespace, tag_key, tag_value)

        # Get flag keys from the index
        flag_keys = await self.redis.smembers(index_key)

        if not flag_keys:
            return []

        # Get flags from Redis
        data_list = await self.redis.mget(flag_keys)

        # Parse flags
        flags = []
        for data in data_list:
            if not data:
                continue

            try:
                flag_dict = json.loads(data)
                flags.append(FeatureFlag.parse_obj(flag_dict))
            except Exception as e:
                logger.error(f"Failed to parse flag data: {e}")

        return flags

    async def subscribe(self, callback: Callable[[FeatureFlag], None]) -> None:
        """
        Subscribe to flag updates.
        """
        # Add the callback
        self.callbacks.add(callback)

        # Start the subscription if not already started
        if not self.pubsub:
            self.pubsub = self.redis.pubsub()
            await self.pubsub.subscribe("feature-flag-updates")

            # Start a task to handle messages
            if not self.pubsub_task:
                self.pubsub_task = asyncio.create_task(self._handle_messages())

    async def unsubscribe(self) -> None:
        """
        Unsubscribe from flag updates.
        """
        # Clear callbacks
        self.callbacks.clear()

        # Close the subscription if it exists
        if self.pubsub:
            await self.pubsub.unsubscribe()
            self.pubsub = None

        # Cancel the task if it exists
        if self.pubsub_task:
            self.pubsub_task.cancel()
            self.pubsub_task = None

    async def _handle_messages(self) -> None:
        """
        Handle messages from the Redis PubSub.
        """
        try:
            async for message in self.pubsub.listen():
                if message["type"] != "message":
                    continue

                try:
                    # Parse the flag
                    flag_dict = json.loads(message["data"])
                    flag = FeatureFlag.parse_obj(flag_dict)

                    # Call the callbacks
                    for callback in self.callbacks:
                        try:
                            callback(flag)
                        except Exception as e:
                            logger.error(f"Error in flag update callback: {e}")
                except Exception as e:
                    logger.error(f"Failed to parse flag update: {e}")
        except asyncio.CancelledError:
            # Task was cancelled, exit gracefully
            pass
        except Exception as e:
            logger.error(f"Error in PubSub message handler: {e}")

    async def _add_to_index(self, flag: FeatureFlag) -> None:
        """
        Add a flag to the indexes.
        """
        # Add to namespace index
        namespace_key = self._get_namespace_index_key(flag.namespace)
        await self.redis.sadd(namespace_key, flag.key)

        # Add to tag indexes
        for tag_key, tag_value in flag.tags.items():
            tag_index_key = self._get_tag_index_key(flag.namespace, tag_key, tag_value)
            flag_key = self._get_flag_key(flag.key, flag.namespace)
            await self.redis.sadd(tag_index_key, flag_key)

    async def _remove_from_index(self, flag: FeatureFlag) -> None:
        """
        Remove a flag from the indexes.
        """
        # Remove from namespace index
        namespace_key = self._get_namespace_index_key(flag.namespace)
        await self.redis.srem(namespace_key, flag.key)

        # Remove from tag indexes
        for tag_key, tag_value in flag.tags.items():
            tag_index_key = self._get_tag_index_key(flag.namespace, tag_key, tag_value)
            flag_key = self._get_flag_key(flag.key, flag.namespace)
            await self.redis.srem(tag_index_key, flag_key)

    async def _publish_update(self, flag: FeatureFlag) -> None:
        """
        Publish a flag update.
        """
        # Serialize the flag
        flag_dict = flag.dict()
        data = json.dumps(flag_dict)

        # Publish the update
        await self.redis.publish("feature-flag-updates", data)

    def _get_flag_key(self, key: str, namespace: str) -> str:
        """
        Get the Redis key for a flag.
        """
        return f"feature-flag:{namespace}:{key}"

    def _get_namespace_key(self, namespace: str) -> str:
        """
        Get the Redis key prefix for a namespace.
        """
        return f"feature-flag:{namespace}"

    def _get_namespace_index_key(self, namespace: str) -> str:
        """
        Get the Redis key for a namespace index.
        """
        return f"feature-flag:index:namespace:{namespace}"

    def _get_tag_index_key(self, namespace: str, tag_key: str, tag_value: str) -> str:
        """
        Get the Redis key for a tag index.
        """
        return f"feature-flag:index:tag:{namespace}:{tag_key}:{tag_value}"

