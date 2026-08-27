import json
import unittest
from unittest.mock import patch

from agent.observability import emit_observation
from agent.workflow_state import WORKFLOW_RUN_STORE
from agent.workflows import run_prepare_agent


class AgentObservabilityTest(unittest.TestCase):

    def events(self, generator):
        return [json.loads(line) for line in generator]

    @patch("agent.workflows.stream_deepseek")
    @patch("agent.workflows.QdrantPrepareIndex")
    def test_workflow_snapshot_contains_trace_and_stage_metrics(
        self,
        qdrant_index,
        model,
    ):
        qdrant_index.return_value.retrieve.return_value = []
        model.return_value = iter(["# 教案\n", "二叉树遍历"])
        payload = {
            "agentType": "plan",
            "teacherId": 1001,
            "actorRole": "teacher",
            "traceId": "trace-observe-001",
            "sessionId": "sessionhash001",
            "form": {"topic": "二叉树遍历"},
            "context": {
                "graphNodes": [],
                "resources": [
                    {
                        "id": 11,
                        "teacherId": 1001,
                        "type": "plan",
                        "title": "二叉树教案",
                        "content": "前序遍历",
                    }
                ],
                "cases": [],
            },
        }

        events = self.events(run_prepare_agent(payload, "secret-api-key"))

        done = next(event for event in events if event["type"] == "done")
        observation = done["workflow"]["observability"]
        self.assertEqual("trace-observe-001", observation["trace_id"])
        self.assertEqual("sessionhash001", observation["session_id"])
        self.assertEqual("WAITING_CONFIRMATION", observation["state"])
        self.assertEqual("qdrant", observation["retrieval_engine"])
        self.assertEqual(0, observation["retrieval_result_count"])
        self.assertGreaterEqual(observation["retrieval_duration_ms"], 0)
        self.assertGreaterEqual(observation["model_first_token_ms"], 0)
        self.assertGreaterEqual(observation["model_total_duration_ms"], 0)
        self.assertGreater(observation["input_tokens"], 0)
        self.assertGreater(observation["output_tokens"], 0)
        self.assertEqual("estimated", observation["token_count_source"])
        self.assertEqual(1, len(observation["tool_calls"]))
        self.assertEqual(
            "retrieve_course_material",
            observation["tool_calls"][0]["tool_name"],
        )
        self.assertEqual("success", observation["tool_calls"][0]["tool_status"])

    def test_structured_log_allowlist_drops_sensitive_content(self):
        with self.assertLogs("teach.agent.observability", level="INFO") as logs:
            emit_observation(
                "workflow_model",
                {
                    "trace_id": "trace-safe-001",
                    "input_tokens": 12,
                    "prompt": "PRIVATE_STUDENT_CONTENT",
                    "api_key": "__TEST_API_KEY__",
                    "password": "__TEST_PASSWORD__",
                },
            )

        rendered = "\n".join(logs.output)
        self.assertIn("trace-safe-001", rendered)
        self.assertIn("input_tokens", rendered)
        self.assertNotIn("PRIVATE_STUDENT_CONTENT", rendered)
        self.assertNotIn("__TEST_API_KEY__", rendered)
        self.assertNotIn("__TEST_PASSWORD__", rendered)

    def test_store_query_data_does_not_include_actor_or_content(self):
        run = WORKFLOW_RUN_STORE.create(
            "plan",
            trace_id="trace-safe-002",
            session_id="sessionhash002",
        )
        run.bind_actor(1001)
        snapshot = run.snapshot()
        rendered = json.dumps(snapshot)

        self.assertNotIn("actor_id", rendered)
        self.assertNotIn("teacherId", rendered)
        self.assertNotIn("prompt", rendered)
        self.assertNotIn("content", rendered)


class AgentRunQueryEndpointTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        from app import app

        app.config["TESTING"] = True
        cls.client = app.test_client()

    def test_owner_can_query_run_and_trace_header_is_preserved(self):
        run = WORKFLOW_RUN_STORE.create(
            "plan",
            trace_id="trace-query-001",
            session_id="sessionhash003",
        )
        run.bind_actor(1001)

        response = self.client.get(
            "/agent/runs/%s?teacherId=1001" % run.request_id,
            headers={"X-Trace-Id": "trace-query-001"},
        )

        self.assertEqual(200, response.status_code)
        self.assertEqual("trace-query-001", response.headers["X-Trace-Id"])
        data = response.get_json()["data"]
        self.assertEqual(run.request_id, data["requestId"])
        self.assertEqual("trace-query-001", data["observability"]["trace_id"])

    def test_other_teacher_cannot_query_run(self):
        run = WORKFLOW_RUN_STORE.create("plan")
        run.bind_actor(1001)

        response = self.client.get(
            "/agent/runs/%s?teacherId=2002" % run.request_id
        )

        self.assertEqual(404, response.status_code)


if __name__ == "__main__":
    unittest.main()
