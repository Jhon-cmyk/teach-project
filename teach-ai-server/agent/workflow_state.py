from __future__ import annotations

import time
import uuid
from collections import OrderedDict
from dataclasses import dataclass, field
from enum import Enum
from threading import RLock
from typing import Any

from .observability import emit_observation, resolve_correlation_id, safe_session_id


class WorkflowState(str, Enum):
    RECEIVED = "RECEIVED"
    PLANNING = "PLANNING"
    RETRIEVING = "RETRIEVING"
    EXECUTING_TOOLS = "EXECUTING_TOOLS"
    GENERATING = "GENERATING"
    CHECKING = "CHECKING"
    WAITING_CONFIRMATION = "WAITING_CONFIRMATION"
    SAVED = "SAVED"
    RETRYING = "RETRYING"
    DEGRADED = "DEGRADED"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


class FailureCategory(str, Enum):
    INVALID_REQUEST = "invalid_request"
    MODEL_FAILURE = "model_failure"
    RETRIEVAL_FAILURE = "retrieval_failure"
    TOOL_FAILURE = "tool_failure"
    BUSINESS_FAILURE = "business_failure"
    CANCELLED = "cancelled"


TERMINAL_STATES = frozenset(
    {
        WorkflowState.SAVED,
        WorkflowState.COMPLETED,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    }
)

LEGAL_TRANSITIONS = {
    WorkflowState.RECEIVED: {
        WorkflowState.PLANNING,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.PLANNING: {
        WorkflowState.RETRIEVING,
        WorkflowState.GENERATING,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.RETRIEVING: {
        WorkflowState.EXECUTING_TOOLS,
        WorkflowState.DEGRADED,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.EXECUTING_TOOLS: {
        WorkflowState.GENERATING,
        WorkflowState.DEGRADED,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.GENERATING: {
        WorkflowState.CHECKING,
        WorkflowState.RETRYING,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.CHECKING: {
        WorkflowState.WAITING_CONFIRMATION,
        WorkflowState.COMPLETED,
        WorkflowState.DEGRADED,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.WAITING_CONFIRMATION: {
        WorkflowState.SAVED,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.RETRYING: {
        WorkflowState.GENERATING,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.DEGRADED: {
        WorkflowState.EXECUTING_TOOLS,
        WorkflowState.GENERATING,
        WorkflowState.CHECKING,
        WorkflowState.FAILED,
        WorkflowState.CANCELLED,
    },
    WorkflowState.SAVED: set(),
    WorkflowState.COMPLETED: set(),
    WorkflowState.FAILED: set(),
    WorkflowState.CANCELLED: set(),
}


class WorkflowStateError(RuntimeError):
    pass


class WorkflowLimitError(WorkflowStateError):
    pass


@dataclass(frozen=True)
class WorkflowLimits:
    max_tool_calls: int = 6
    max_retries: int = 1

    def __post_init__(self):
        if self.max_tool_calls < 1 or self.max_tool_calls > 20:
            raise ValueError("max_tool_calls must be between 1 and 20")
        if self.max_retries < 0 or self.max_retries > 3:
            raise ValueError("max_retries must be between 0 and 3")


@dataclass(frozen=True)
class StateTransition:
    sequence: int
    previous_state: WorkflowState | None
    current_state: WorkflowState
    timestamp_ms: int
    reason: str
    failure_category: FailureCategory | None = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "sequence": self.sequence,
            "previousState": (
                self.previous_state.value
                if self.previous_state is not None
                else None
            ),
            "currentState": self.current_state.value,
            "timestamp": self.timestamp_ms,
            "reason": self.reason,
            "failureCategory": (
                self.failure_category.value
                if self.failure_category is not None
                else None
            ),
        }


@dataclass
class WorkflowRun:
    agent_type: str
    actor_id: int | None = None
    request_id: str = field(default_factory=lambda: uuid.uuid4().hex)
    trace_id: str = field(default_factory=lambda: uuid.uuid4().hex)
    session_id: str = field(default_factory=lambda: uuid.uuid4().hex)
    limits: WorkflowLimits = field(default_factory=WorkflowLimits)
    state: WorkflowState = WorkflowState.RECEIVED
    retry_count: int = 0
    tool_call_count: int = 0
    retrieval_engine: str | None = None
    retrieval_result_count: int = 0
    retrieval_duration_ms: int = 0
    model_first_token_ms: int | None = None
    model_total_duration_ms: int = 0
    input_tokens: int = 0
    output_tokens: int = 0
    token_count_source: str = "estimated"
    degraded: bool = False
    error_type: str | None = None
    tool_observations: list[dict[str, Any]] = field(default_factory=list)
    history: list[StateTransition] = field(default_factory=list)
    _lock: RLock = field(default_factory=RLock, repr=False)

    def __post_init__(self):
        self.trace_id = resolve_correlation_id(self.trace_id)
        self.session_id = safe_session_id(self.session_id)
        if not self.history:
            self.history.append(
                StateTransition(
                    sequence=0,
                    previous_state=None,
                    current_state=WorkflowState.RECEIVED,
                    timestamp_ms=_now_ms(),
                    reason="request_received",
                )
            )
        self._emit("workflow_state")

    @property
    def is_terminal(self) -> bool:
        return self.state in TERMINAL_STATES

    def bind_actor(self, actor_id: int) -> None:
        with self._lock:
            if self.actor_id is not None and self.actor_id != actor_id:
                raise WorkflowStateError("Workflow actor cannot be changed.")
            self.actor_id = actor_id

    def transition(
        self,
        target: WorkflowState,
        reason: str,
        failure_category: FailureCategory | None = None,
    ) -> StateTransition:
        with self._lock:
            if target not in LEGAL_TRANSITIONS[self.state]:
                raise WorkflowStateError(
                    "Illegal workflow transition: %s -> %s"
                    % (self.state.value, target.value)
                )
            transition = StateTransition(
                sequence=len(self.history),
                previous_state=self.state,
                current_state=target,
                timestamp_ms=_now_ms(),
                reason=reason,
                failure_category=failure_category,
            )
            self.state = target
            if target == WorkflowState.DEGRADED:
                self.degraded = True
            if failure_category is not None:
                self.error_type = failure_category.value
            self.history.append(transition)
            self._emit("workflow_state")
            return transition

    def record_tool_call(self) -> None:
        with self._lock:
            if self.tool_call_count >= self.limits.max_tool_calls:
                raise WorkflowLimitError(
                    "Maximum tool call count (%s) reached."
                    % self.limits.max_tool_calls
                )
            self.tool_call_count += 1

    def record_retry(self) -> None:
        with self._lock:
            if self.retry_count >= self.limits.max_retries:
                raise WorkflowLimitError(
                    "Maximum retry count (%s) reached."
                    % self.limits.max_retries
                )
            self.retry_count += 1
            self._emit("workflow_retry")

    def record_retrieval(
        self,
        engine: str,
        result_count: int,
        duration_ms: int,
        *,
        degraded: bool = False,
        error_type: str | None = None,
    ) -> None:
        with self._lock:
            self.retrieval_engine = str(engine or "unknown")[:48]
            self.retrieval_result_count = max(0, int(result_count or 0))
            self.retrieval_duration_ms = max(0, int(duration_ms or 0))
            if degraded:
                self.degraded = True
            if error_type:
                self.error_type = str(error_type)[:64]
            self._emit("workflow_retrieval")

    def record_tool(
        self,
        name: str,
        duration_ms: int,
        status: str,
        error_type: str | None = None,
    ) -> None:
        with self._lock:
            item = {
                "tool_name": str(name or "unknown")[:64],
                "tool_duration_ms": max(0, int(duration_ms or 0)),
                "tool_status": "success" if status == "success" else "failed",
                "error_type": str(error_type)[:64] if error_type else None,
            }
            self.tool_observations.append(item)
            if item["tool_status"] == "failed":
                self.degraded = True
                self.error_type = item["error_type"] or FailureCategory.TOOL_FAILURE.value
            self._emit("workflow_tool", item)

    def record_model(
        self,
        *,
        first_token_ms: int | None,
        total_duration_ms: int,
        input_tokens: int,
        output_tokens: int,
    ) -> None:
        with self._lock:
            self.model_first_token_ms = (
                max(0, int(first_token_ms))
                if first_token_ms is not None
                else None
            )
            self.model_total_duration_ms = max(0, int(total_duration_ms or 0))
            self.input_tokens = max(0, int(input_tokens or 0))
            self.output_tokens = max(0, int(output_tokens or 0))
            self._emit("workflow_model")

    def state_event(self, transition: StateTransition | None = None) -> dict[str, Any]:
        with self._lock:
            current = transition or self.history[-1]
            return {
                "type": "workflow_state",
                "requestId": self.request_id,
                "traceId": self.trace_id,
                "sessionId": self.session_id,
                **current.to_dict(),
                "retryCount": self.retry_count,
                "maxRetries": self.limits.max_retries,
                "toolCallCount": self.tool_call_count,
                "maxToolCalls": self.limits.max_tool_calls,
            }

    def snapshot(self) -> dict[str, Any]:
        with self._lock:
            return {
                "requestId": self.request_id,
                "agentType": self.agent_type,
                "state": self.state.value,
                "retryCount": self.retry_count,
                "maxRetries": self.limits.max_retries,
                "toolCallCount": self.tool_call_count,
                "maxToolCalls": self.limits.max_tool_calls,
                "history": [item.to_dict() for item in self.history],
                "observability": self.observability_snapshot(),
            }

    def observability_snapshot(self) -> dict[str, Any]:
        with self._lock:
            return {
                "trace_id": self.trace_id,
                "session_id": self.session_id,
                "request_id": self.request_id,
                "agent_type": self.agent_type,
                "state": self.state.value,
                "retrieval_engine": self.retrieval_engine,
                "retrieval_result_count": self.retrieval_result_count,
                "retrieval_duration_ms": self.retrieval_duration_ms,
                "model_first_token_ms": self.model_first_token_ms,
                "model_total_duration_ms": self.model_total_duration_ms,
                "input_tokens": self.input_tokens,
                "output_tokens": self.output_tokens,
                "token_count_source": self.token_count_source,
                "retry_count": self.retry_count,
                "degraded": self.degraded,
                "error_type": self.error_type,
                "tool_calls": [dict(item) for item in self.tool_observations],
            }

    def _emit(self, event_name: str, extra: dict[str, Any] | None = None) -> None:
        fields = self.observability_snapshot()
        fields.update(extra or {})
        emit_observation(event_name, fields)


class WorkflowRunStore:
    def __init__(self, maximum_runs: int = 500):
        if maximum_runs < 1:
            raise ValueError("maximum_runs must be positive")
        self._maximum_runs = maximum_runs
        self._runs: OrderedDict[str, WorkflowRun] = OrderedDict()
        self._lock = RLock()

    def create(
        self,
        agent_type: str,
        *,
        limits: WorkflowLimits | None = None,
        trace_id: str | None = None,
        session_id: str | None = None,
    ) -> WorkflowRun:
        run = WorkflowRun(
            agent_type=agent_type,
            limits=limits or WorkflowLimits(),
            trace_id=resolve_correlation_id(trace_id),
            session_id=safe_session_id(session_id),
        )
        with self._lock:
            self._runs[run.request_id] = run
            while len(self._runs) > self._maximum_runs:
                self._runs.popitem(last=False)
        return run

    def get(self, request_id: str) -> WorkflowRun | None:
        with self._lock:
            run = self._runs.get(request_id)
            if run is not None:
                self._runs.move_to_end(request_id)
            return run

    def mark_saved(
        self,
        request_id: str,
        *,
        actor_id: int,
        resource_id: int,
        confirmed: bool,
    ) -> WorkflowRun:
        run = self.get(request_id)
        if run is None:
            raise WorkflowStateError("Workflow run was not found.")
        if run.actor_id != actor_id:
            raise WorkflowStateError("Workflow run does not belong to this actor.")
        if not confirmed:
            raise WorkflowStateError("Explicit confirmation is required.")
        if run.state == WorkflowState.SAVED:
            return run
        run.transition(
            WorkflowState.SAVED,
            "resource_saved:%s" % resource_id,
        )
        return run


WORKFLOW_RUN_STORE = WorkflowRunStore()


def _now_ms() -> int:
    return int(time.time() * 1000)
