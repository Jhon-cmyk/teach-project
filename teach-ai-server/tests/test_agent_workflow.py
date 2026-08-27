import json
import unittest
from unittest.mock import patch

from agent.tools import ToolErrorCode, ToolExecutionError
from agent.workflow_state import (
    FailureCategory,
    WORKFLOW_RUN_STORE,
    WorkflowLimitError,
    WorkflowLimits,
    WorkflowRun,
    WorkflowRunStore,
    WorkflowState,
    WorkflowStateError,
)
from agent.workflows import run_prepare_agent


class WorkflowStateTest(unittest.TestCase):

    def test_records_complete_legal_history(self):
        run = WorkflowRun(agent_type="plan", actor_id=1001)

        run.transition(WorkflowState.PLANNING, "validated")
        run.transition(WorkflowState.RETRIEVING, "retrieval")
        run.transition(WorkflowState.EXECUTING_TOOLS, "tools")
        run.transition(WorkflowState.GENERATING, "generation")
        run.transition(WorkflowState.CHECKING, "quality")
        run.transition(WorkflowState.WAITING_CONFIRMATION, "confirmation")
        run.transition(WorkflowState.SAVED, "saved")

        snapshot = run.snapshot()
        self.assertEqual("SAVED", snapshot["state"])
        self.assertEqual(
            [
                "RECEIVED",
                "PLANNING",
                "RETRIEVING",
                "EXECUTING_TOOLS",
                "GENERATING",
                "CHECKING",
                "WAITING_CONFIRMATION",
                "SAVED",
            ],
            [item["currentState"] for item in snapshot["history"]],
        )
        self.assertEqual(
            list(range(8)),
            [item["sequence"] for item in snapshot["history"]],
        )

    def test_rejects_illegal_state_transition(self):
        run = WorkflowRun(agent_type="plan")

        with self.assertRaises(WorkflowStateError):
            run.transition(WorkflowState.SAVED, "skip_confirmation")

    def test_enforces_tool_call_limit(self):
        run = WorkflowRun(
            agent_type="plan",
            limits=WorkflowLimits(max_tool_calls=2, max_retries=1),
        )
        run.record_tool_call()
        run.record_tool_call()

        with self.assertRaises(WorkflowLimitError):
            run.record_tool_call()

        self.assertEqual(2, run.tool_call_count)

    def test_enforces_retry_limit(self):
        run = WorkflowRun(
            agent_type="plan",
            limits=WorkflowLimits(max_tool_calls=6, max_retries=1),
        )
        run.record_retry()

        with self.assertRaises(WorkflowLimitError):
            run.record_retry()

        self.assertEqual(1, run.retry_count)

    def test_store_marks_only_confirmed_owner_run_as_saved(self):
        store = WorkflowRunStore()
        run = store.create("plan")
        run.bind_actor(1001)
        run.transition(WorkflowState.PLANNING, "validated")
        run.transition(WorkflowState.GENERATING, "generation")
        run.transition(WorkflowState.CHECKING, "quality")
        run.transition(WorkflowState.WAITING_CONFIRMATION, "confirmation")

        saved = store.mark_saved(
            run.request_id,
            actor_id=1001,
            resource_id=88,
            confirmed=True,
        )

        self.assertEqual(WorkflowState.SAVED, saved.state)
        self.assertEqual("resource_saved:88", saved.history[-1].reason)
        history_size = len(saved.history)

        saved_again = store.mark_saved(
            run.request_id,
            actor_id=1001,
            resource_id=88,
            confirmed=True,
        )

        self.assertIs(saved, saved_again)
        self.assertEqual(history_size, len(saved_again.history))

    def test_store_denies_other_actor_and_missing_confirmation(self):
        store = WorkflowRunStore()
        run = store.create("plan")
        run.bind_actor(1001)
        run.transition(WorkflowState.PLANNING, "validated")
        run.transition(WorkflowState.GENERATING, "generation")
        run.transition(WorkflowState.CHECKING, "quality")
        run.transition(WorkflowState.WAITING_CONFIRMATION, "confirmation")

        with self.assertRaises(WorkflowStateError):
            store.mark_saved(
                run.request_id,
                actor_id=9999,
                resource_id=88,
                confirmed=True,
            )
        with self.assertRaises(WorkflowStateError):
            store.mark_saved(
                run.request_id,
                actor_id=1001,
                resource_id=88,
                confirmed=False,
            )
        self.assertEqual(WorkflowState.WAITING_CONFIRMATION, run.state)


class PrepareWorkflowIntegrationTest(unittest.TestCase):

    def payload(self, agent_type="coding"):
        return {
            "agentType": agent_type,
            "teacherId": 1001,
            "actorRole": "teacher",
            "form": {"topic": "二叉树遍历"},
            "context": {
                "graphNodes": [],
                "resources": [],
                "cases": [],
            },
        }

    def events(self, generator):
        return [json.loads(line) for line in generator]

    @patch("agent.workflows.stream_deepseek")
    def test_success_stream_contains_reconstructable_state_history(self, model):
        model.return_value = iter(["{\"title\":\"练习\"}"])

        events = self.events(run_prepare_agent(self.payload(), "key"))

        state_events = [
            event for event in events
            if event["type"] == "workflow_state"
        ]
        self.assertEqual(
            [
                "RECEIVED",
                "PLANNING",
                "GENERATING",
                "CHECKING",
                "WAITING_CONFIRMATION",
            ],
            [event["currentState"] for event in state_events],
        )
        done = next(event for event in events if event["type"] == "done")
        self.assertTrue(done["requiresConfirmation"])
        self.assertEqual("WAITING_CONFIRMATION", done["workflow"]["state"])
        self.assertEqual(
            [event["currentState"] for event in state_events],
            [
                item["currentState"]
                for item in done["workflow"]["history"]
            ],
        )

    @patch("agent.workflows.stream_deepseek")
    def test_model_failure_retries_once_before_first_chunk(self, model):
        model.side_effect = [
            RuntimeError("temporary model failure"),
            iter(["{\"title\":\"练习\"}"]),
        ]

        events = self.events(run_prepare_agent(self.payload(), "key"))

        states = [
            event["currentState"]
            for event in events
            if event["type"] == "workflow_state"
        ]
        self.assertIn("RETRYING", states)
        self.assertEqual(2, model.call_count)
        done = next(event for event in events if event["type"] == "done")
        self.assertEqual(1, done["workflow"]["retryCount"])

    @patch("agent.workflows.stream_deepseek")
    def test_model_failure_stops_after_retry_limit(self, model):
        model.side_effect = RuntimeError("model unavailable")

        events = self.events(run_prepare_agent(self.payload(), "key"))

        states = [
            event["currentState"]
            for event in events
            if event["type"] == "workflow_state"
        ]
        self.assertEqual(1, states.count("RETRYING"))
        self.assertEqual("FAILED", states[-1])
        self.assertEqual(2, model.call_count)
        error = next(event for event in events if event["type"] == "error")
        self.assertEqual(
            FailureCategory.MODEL_FAILURE.value,
            error["failureCategory"],
        )

    @patch("agent.workflows.stream_deepseek")
    def test_partial_model_output_is_not_retried(self, model):
        def partial_stream():
            yield "partial"
            raise RuntimeError("stream interrupted")

        model.return_value = partial_stream()

        events = self.events(run_prepare_agent(self.payload(), "key"))

        states = [
            event["currentState"]
            for event in events
            if event["type"] == "workflow_state"
        ]
        self.assertNotIn("RETRYING", states)
        self.assertEqual("FAILED", states[-1])
        self.assertEqual(1, model.call_count)

    def test_invalid_request_has_explicit_failed_state(self):
        events = self.events(
            run_prepare_agent(
                {
                    "agentType": "unknown",
                    "teacherId": 1001,
                    "context": {},
                },
                "key",
            )
        )

        self.assertEqual("RECEIVED", events[0]["currentState"])
        self.assertEqual("FAILED", events[1]["currentState"])
        self.assertEqual(
            FailureCategory.INVALID_REQUEST.value,
            events[1]["failureCategory"],
        )

    @patch("agent.workflows.stream_deepseek")
    @patch("agent.workflows.ContextRetriever")
    @patch("agent.workflows.QdrantPrepareIndex")
    def test_retrieval_fallback_has_retrieval_failure_category(
        self,
        qdrant_index,
        context_retriever,
        model,
    ):
        qdrant_index.return_value.retrieve.side_effect = RuntimeError(
            "qdrant unavailable"
        )
        context_retriever.return_value.retrieve.return_value = []
        model.return_value = iter(["# 教案"])

        events = self.events(
            run_prepare_agent(self.payload(agent_type="plan"), "key")
        )

        degraded = next(
            event for event in events
            if event.get("currentState") == "DEGRADED"
        )
        self.assertEqual(
            FailureCategory.RETRIEVAL_FAILURE.value,
            degraded["failureCategory"],
        )
        self.assertIn(
            "keyword_fallback",
            degraded["reason"],
        )

    @patch("agent.workflows.stream_deepseek")
    @patch("agent.workflows.DEFAULT_TOOL_REGISTRY.invoke")
    @patch("agent.workflows.QdrantPrepareIndex")
    def test_tool_failure_is_degraded_and_categorized(
        self,
        qdrant_index,
        tool_invoke,
        model,
    ):
        payload = self.payload(agent_type="plan")
        payload["context"]["resources"] = [
            {
                "id": 11,
                "type": "plan",
                "title": "二叉树教案",
                "content": "前序遍历",
            }
        ]
        qdrant_index.return_value.retrieve.return_value = []
        tool_invoke.side_effect = ToolExecutionError(
            ToolErrorCode.INTERNAL_ERROR,
            "tool failed",
        )
        model.return_value = iter(["# 教案"])

        events = self.events(run_prepare_agent(payload, "key"))

        degraded = next(
            event for event in events
            if event.get("failureCategory") == FailureCategory.TOOL_FAILURE.value
        )
        self.assertEqual("DEGRADED", degraded["currentState"])
        done = next(event for event in events if event["type"] == "done")
        self.assertEqual("WAITING_CONFIRMATION", done["workflow"]["state"])

    @patch("agent.workflows._quality")
    @patch("agent.workflows.stream_deepseek")
    def test_quality_exception_is_business_failure(self, model, quality):
        model.return_value = iter(["{\"title\":\"练习\"}"])
        quality.side_effect = RuntimeError("quality rules unavailable")

        events = self.events(run_prepare_agent(self.payload(), "key"))

        failed = [
            event for event in events
            if event.get("currentState") == "FAILED"
        ][-1]
        self.assertEqual(
            FailureCategory.BUSINESS_FAILURE.value,
            failed["failureCategory"],
        )
        error = next(event for event in events if event["type"] == "error")
        self.assertEqual(
            FailureCategory.BUSINESS_FAILURE.value,
            error["failureCategory"],
        )

    def test_client_close_marks_active_run_cancelled(self):
        generator = run_prepare_agent(self.payload(), "key")
        first_event = json.loads(next(generator))
        request_id = first_event["requestId"]

        generator.close()

        run = WORKFLOW_RUN_STORE.get(request_id)
        self.assertIsNotNone(run)
        self.assertEqual(WorkflowState.CANCELLED, run.state)


if __name__ == "__main__":
    unittest.main()
