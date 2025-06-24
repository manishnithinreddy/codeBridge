"""
Generated gRPC code for the Feature Flag Service.
This is a placeholder for the generated code.
"""

# This file would normally be generated from the proto file using protoc.
# For this example, we'll create a simplified version manually.

# pylint: disable=invalid-name,missing-class-docstring,missing-function-docstring

class FlagRequest:
    def __init__(self, flag_key="", namespace=""):
        self.flag_key = flag_key
        self.namespace = namespace


class FlagMetadata:
    def __init__(self, description="", created_at=0, updated_at=0, created_by="", updated_by="", version="", tags=None):
        self.description = description
        self.created_at = created_at
        self.updated_at = updated_at
        self.created_by = created_by
        self.updated_by = updated_by
        self.version = version
        self.tags = tags or {}


class FlagResponse:
    def __init__(self, flag_key="", exists=False, metadata=None, value=None):
        self.flag_key = flag_key
        self.exists = exists
        self.metadata = metadata
        self.value = value


class MultiFlagRequest:
    def __init__(self, flag_keys=None, namespace=""):
        self.flag_keys = flag_keys or []
        self.namespace = namespace


class MultiFlagResponse:
    def __init__(self, flags=None):
        self.flags = flags or []


class ServiceContext:
    def __init__(self, service_name="", service_version="", instance_id="", environment="", metrics=None):
        self.service_name = service_name
        self.service_version = service_version
        self.instance_id = instance_id
        self.environment = environment
        self.metrics = metrics or {}


class EvaluationContext:
    def __init__(self, user_id="", session_id="", attributes=None, numeric_attributes=None, boolean_attributes=None, service_context=None):
        self.user_id = user_id
        self.session_id = session_id
        self.attributes = attributes or {}
        self.numeric_attributes = numeric_attributes or {}
        self.boolean_attributes = boolean_attributes or {}
        self.service_context = service_context


class FlagEvaluationRequest:
    def __init__(self, flag_key="", namespace="", context=None):
        self.flag_key = flag_key
        self.namespace = namespace
        self.context = context or EvaluationContext()


class EvaluationReason:
    def __init__(self, type="", rule_id="", description="", error_message=""):
        self.type = type
        self.rule_id = rule_id
        self.description = description
        self.error_message = error_message


class FlagEvaluationResponse:
    def __init__(self, flag_key="", variation_id="", rule_id="", reason=None, value=None):
        self.flag_key = flag_key
        self.variation_id = variation_id
        self.rule_id = rule_id
        self.reason = reason
        self.value = value


class SetFlagRequest:
    def __init__(self, flag_key="", namespace="", description="", tags=None, temporary=False, expiration_time=0, version="", value=None):
        self.flag_key = flag_key
        self.namespace = namespace
        self.description = description
        self.tags = tags or {}
        self.temporary = temporary
        self.expiration_time = expiration_time
        self.version = version
        self.value = value


class SetFlagResponse:
    def __init__(self, success=False, message="", version=""):
        self.success = success
        self.message = message
        self.version = version


class FlagStreamRequest:
    def __init__(self, flag_keys=None, namespace=""):
        self.flag_keys = flag_keys or []
        self.namespace = namespace


class FlagUpdateEvent:
    def __init__(self, event_type="", flag_key="", namespace="", version="", timestamp=0, value=None):
        self.event_type = event_type
        self.flag_key = flag_key
        self.namespace = namespace
        self.version = version
        self.timestamp = timestamp
        self.value = value


class ListFlagsRequest:
    def __init__(self, namespace="", prefix="", tag_filter="", page_size=0, page_token=""):
        self.namespace = namespace
        self.prefix = prefix
        self.tag_filter = tag_filter
        self.page_size = page_size
        self.page_token = page_token


class FlagSummary:
    def __init__(self, flag_key="", namespace="", description="", value_type="", updated_at=0, temporary=False, expiration_time=0, tags=None):
        self.flag_key = flag_key
        self.namespace = namespace
        self.description = description
        self.value_type = value_type
        self.updated_at = updated_at
        self.temporary = temporary
        self.expiration_time = expiration_time
        self.tags = tags or {}


class ListFlagsResponse:
    def __init__(self, flags=None, total_count=0, next_page_token=""):
        self.flags = flags or []
        self.total_count = total_count
        self.next_page_token = next_page_token


class HealthCheckRequest:
    def __init__(self):
        pass


class HealthCheckResponse:
    def __init__(self, status="", version="", details=None):
        self.status = status
        self.version = version
        self.details = details or {}

