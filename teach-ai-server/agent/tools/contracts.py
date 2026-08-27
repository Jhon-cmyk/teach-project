from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor, TimeoutError as FutureTimeoutError
from dataclasses import asdict, dataclass, field, is_dataclass
from enum import Enum
from threading import Lock
from typing import Any, Callable, Mapping, MutableMapping, Type


class ToolErrorCode(str, Enum):
    INVALID_ARGUMENT = "invalid_argument"
    PERMISSION_DENIED = "permission_denied"
    NOT_FOUND = "not_found"
    CONFIRMATION_REQUIRED = "confirmation_required"
    TIMEOUT = "timeout"
    DEPENDENCY_UNAVAILABLE = "dependency_unavailable"
    CONFLICT = "conflict"
    INTERNAL_ERROR = "internal_error"


class ToolExecutionError(RuntimeError):
    def __init__(
        self,
        code: ToolErrorCode,
        message: str,
        *,
        retryable: bool = False,
    ):
        super().__init__(message)
        self.code = code
        self.retryable = retryable

    def to_dict(self) -> dict[str, Any]:
        return {
            "code": self.code.value,
            "message": str(self),
            "retryable": self.retryable,
        }


class ToolInput:
    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "ToolInput":
        raise NotImplementedError

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        raise NotImplementedError


class ToolOutput:
    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        raise NotImplementedError

    def to_dict(self) -> dict[str, Any]:
        return _json_value(self)


@dataclass
class ToolContext:
    """Trusted execution scope created by the application, never by model arguments."""

    actor_id: int
    actor_role: str
    context_data: Mapping[str, Any]
    course_id: int | None = None
    class_id: int | None = None
    student_id: int | None = None
    allowed_student_ids: frozenset[int] = frozenset()
    confirmed_actions: frozenset[str] = frozenset()
    artifacts: MutableMapping[str, ToolOutput] = field(default_factory=dict)

    @classmethod
    def from_agent_payload(cls, payload: Mapping[str, Any]) -> "ToolContext":
        if not isinstance(payload, Mapping):
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "Agent payload must be an object.",
            )
        actor_id = _positive_int(payload.get("teacherId"), "teacherId")
        role = str(payload.get("actorRole") or "teacher").strip().lower()
        if role not in {"teacher", "admin"}:
            raise ToolExecutionError(
                ToolErrorCode.PERMISSION_DENIED,
                "Prepare Agent only accepts a trusted teacher or admin context.",
            )
        context_data = payload.get("context") or {}
        if not isinstance(context_data, Mapping):
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "Agent context must be an object.",
            )
        return cls(
            actor_id=actor_id,
            actor_role=role,
            context_data=context_data,
            course_id=_optional_positive_int(payload.get("courseId"), "courseId"),
            class_id=_optional_positive_int(payload.get("classId"), "classId"),
            allowed_student_ids=_positive_int_set(
                context_data.get("authorizedStudentIds") or (),
                "authorizedStudentIds",
            ),
            confirmed_actions=_string_set(
                payload.get("confirmedActions") or (),
                "confirmedActions",
            ),
        )


@dataclass(frozen=True)
class ToolDefinition:
    name: str
    description: str
    input_type: Type[ToolInput]
    output_type: Type[ToolOutput]
    allowed_roles: frozenset[str]
    data_scope: str
    timeout_seconds: float
    error_types: frozenset[ToolErrorCode]
    retryable_errors: frozenset[ToolErrorCode]
    confirmation_required: bool
    handler: Callable[[ToolContext, ToolInput], ToolOutput]

    def descriptor(self) -> dict[str, Any]:
        return {
            "name": self.name,
            "description": self.description,
            "inputSchema": self.input_type.json_schema(),
            "outputSchema": self.output_type.json_schema(),
            "permission": {
                "allowedRoles": sorted(self.allowed_roles),
                "dataScope": self.data_scope,
                "confirmationRequired": self.confirmation_required,
            },
            "timeoutMs": int(self.timeout_seconds * 1000),
            "errorTypes": sorted(code.value for code in self.error_types),
            "retryableErrors": sorted(code.value for code in self.retryable_errors),
        }


class ToolRegistry:
    def __init__(self, max_workers: int = 4):
        self._definitions: dict[str, ToolDefinition] = {}
        self._lock = Lock()
        self._executor = ThreadPoolExecutor(
            max_workers=max_workers,
            thread_name_prefix="agent-tool",
        )

    def register(self, definition: ToolDefinition) -> None:
        with self._lock:
            if definition.name in self._definitions:
                raise ValueError("Duplicate tool: %s" % definition.name)
            self._definitions[definition.name] = definition

    def get(self, name: str) -> ToolDefinition:
        definition = self._definitions.get(name)
        if definition is None:
            raise ToolExecutionError(
                ToolErrorCode.NOT_FOUND,
                "Unknown tool: %s" % name,
            )
        return definition

    def descriptors(self) -> list[dict[str, Any]]:
        return [
            self._definitions[name].descriptor()
            for name in sorted(self._definitions)
        ]

    def invoke(
        self,
        name: str,
        arguments: Mapping[str, Any],
        context: ToolContext,
    ) -> ToolOutput:
        definition = self.get(name)
        if context.actor_role not in definition.allowed_roles:
            raise ToolExecutionError(
                ToolErrorCode.PERMISSION_DENIED,
                "Role '%s' cannot use tool '%s'."
                % (context.actor_role, definition.name),
            )
        try:
            tool_input = definition.input_type.from_payload(arguments)
        except ToolExecutionError:
            raise
        except Exception as exc:
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "Invalid arguments for '%s': %s" % (definition.name, exc),
            ) from exc

        future = self._executor.submit(definition.handler, context, tool_input)
        try:
            output = future.result(timeout=definition.timeout_seconds)
        except FutureTimeoutError as exc:
            future.cancel()
            raise ToolExecutionError(
                ToolErrorCode.TIMEOUT,
                "Tool '%s' timed out." % definition.name,
                retryable=ToolErrorCode.TIMEOUT in definition.retryable_errors,
            ) from exc
        except ToolExecutionError:
            raise
        except Exception as exc:
            raise ToolExecutionError(
                ToolErrorCode.INTERNAL_ERROR,
                "Tool '%s' failed." % definition.name,
            ) from exc

        if not isinstance(output, definition.output_type):
            raise ToolExecutionError(
                ToolErrorCode.INTERNAL_ERROR,
                "Tool '%s' returned an invalid output type." % definition.name,
            )
        return output


def strict_fields(
    payload: Mapping[str, Any],
    allowed: set[str],
    required: set[str] | None = None,
) -> None:
    if not isinstance(payload, Mapping):
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "Tool arguments must be an object.",
        )
    unknown = set(payload) - allowed
    if unknown:
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "Unknown argument(s): %s" % ", ".join(sorted(unknown)),
        )
    missing = (required or set()) - set(payload)
    if missing:
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "Missing argument(s): %s" % ", ".join(sorted(missing)),
        )


def required_text(value: Any, name: str, max_length: int) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "%s must be a non-empty string." % name,
        )
    result = value.strip()
    if len(result) > max_length:
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "%s must not exceed %s characters." % (name, max_length),
        )
    return result


def optional_text(value: Any, name: str, max_length: int) -> str | None:
    if value is None:
        return None
    return required_text(value, name, max_length)


def bounded_int(
    value: Any,
    name: str,
    *,
    default: int,
    minimum: int,
    maximum: int,
) -> int:
    if value is None:
        return default
    if isinstance(value, bool) or not isinstance(value, int):
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "%s must be an integer." % name,
        )
    if value < minimum or value > maximum:
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "%s must be between %s and %s." % (name, minimum, maximum),
        )
    return value


def string_tuple(
    value: Any,
    name: str,
    *,
    maximum_items: int,
    item_max_length: int,
) -> tuple[str, ...]:
    if value is None:
        return ()
    if not isinstance(value, (list, tuple)):
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "%s must be an array." % name,
        )
    if len(value) > maximum_items:
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "%s accepts at most %s items." % (name, maximum_items),
        )
    return tuple(
        required_text(item, "%s[]" % name, item_max_length)
        for item in value
    )


def positive_int(value: Any, name: str) -> int:
    return _positive_int(value, name)


def optional_positive_int(value: Any, name: str) -> int | None:
    return _optional_positive_int(value, name)


def _positive_int(value: Any, name: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "%s must be a positive integer." % name,
        )
    return value


def _optional_positive_int(value: Any, name: str) -> int | None:
    if value is None:
        return None
    return _positive_int(value, name)


def _positive_int_set(value: Any, name: str) -> frozenset[int]:
    if not isinstance(value, (list, tuple, set, frozenset)):
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "%s must be an array." % name,
        )
    return frozenset(_positive_int(item, "%s[]" % name) for item in value)


def _string_set(value: Any, name: str) -> frozenset[str]:
    if not isinstance(value, (list, tuple, set, frozenset)):
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "%s must be an array." % name,
        )
    return frozenset(
        required_text(item, "%s[]" % name, 64)
        for item in value
    )


def _json_value(value: Any) -> Any:
    if is_dataclass(value):
        return {
            key: _json_value(item)
            for key, item in asdict(value).items()
        }
    if isinstance(value, Enum):
        return value.value
    if isinstance(value, tuple):
        return [_json_value(item) for item in value]
    if isinstance(value, list):
        return [_json_value(item) for item in value]
    if isinstance(value, dict):
        return {key: _json_value(item) for key, item in value.items()}
    return value
