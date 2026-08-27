import json
import logging
import re
import uuid


LOGGER = logging.getLogger("teach.agent.observability")
SAFE_CORRELATION_ID = re.compile(r"^[A-Za-z0-9._-]{8,64}$")
SAFE_EVENT_NAME = re.compile(r"^[a-z_]{3,48}$")


def resolve_correlation_id(candidate):
    value = str(candidate or "").strip()
    if SAFE_CORRELATION_ID.fullmatch(value):
        return value
    return uuid.uuid4().hex


def safe_session_id(candidate):
    value = str(candidate or "").strip()
    if SAFE_CORRELATION_ID.fullmatch(value):
        return value
    return uuid.uuid4().hex


def emit_observation(event_name, fields):
    """Log only an explicit metrics allowlist; never serialize request content."""
    if not SAFE_EVENT_NAME.fullmatch(str(event_name or "")):
        event_name = "invalid_event"
    allowed = {
        "trace_id",
        "session_id",
        "request_id",
        "agent_type",
        "state",
        "tool_name",
        "tool_duration_ms",
        "tool_status",
        "retrieval_engine",
        "retrieval_result_count",
        "retrieval_duration_ms",
        "model_first_token_ms",
        "model_total_duration_ms",
        "input_tokens",
        "output_tokens",
        "retry_count",
        "degraded",
        "error_type",
    }
    record = {"event": event_name}
    for key in allowed:
        value = (fields or {}).get(key)
        if value is not None:
            record[key] = value
    LOGGER.info(json.dumps(record, ensure_ascii=False, separators=(",", ":")))


def estimate_tokens(text):
    """Conservative deterministic estimate used when the provider omits usage."""
    value = str(text or "")
    if not value:
        return 0
    ascii_chars = sum(1 for char in value if ord(char) < 128)
    non_ascii_chars = len(value) - ascii_chars
    return max(1, (ascii_chars + 3) // 4 + non_ascii_chars)
