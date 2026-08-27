import unittest

from agent.tools import (
    ToolContext,
    ToolErrorCode,
    ToolExecutionError,
    build_default_tool_registry,
)
from agent.workflows import _collect_structured_tool_results


class AgentToolRegistryTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.registry = build_default_tool_registry()

    def setUp(self):
        self.context = ToolContext(
            actor_id=1001,
            actor_role="teacher",
            allowed_student_ids=frozenset({2001}),
            confirmed_actions=frozenset({"save_lesson_plan_draft"}),
            context_data={
                "resources": [
                    {
                        "id": 11,
                        "teacherId": 1001,
                        "type": "plan",
                        "title": "二叉树遍历教案",
                        "content": "前序遍历按照根、左、右的顺序访问节点。",
                    },
                    {
                        "id": 12,
                        "teacherId": 9999,
                        "type": "plan",
                        "title": "其他教师资料",
                        "content": "二叉树遍历",
                    },
                ],
                "cases": [
                    {
                        "id": 21,
                        "scope": "platform",
                        "status": "approved",
                        "title": "二叉树课堂案例",
                        "summary": "用家族树说明层级结构。",
                        "content": "二叉树前序遍历案例。",
                        "parseStatus": "ok",
                    }
                ],
                "graphNodes": [
                    {
                        "id": "tree-traversal",
                        "name": "二叉树遍历",
                        "category": "数据结构",
                        "description": "前序、中序与后序遍历。",
                        "difficulty": "medium",
                        "graphRelationText": "前置知识：递归",
                    }
                ],
                "studentWeaknesses": [
                    {
                        "scope": "student",
                        "studentId": 2001,
                        "knowledgePoint": "递归终止条件",
                        "masteryRate": 0.42,
                        "evidenceCount": 5,
                    },
                    {
                        "scope": "class",
                        "knowledgePoint": "二叉树遍历顺序",
                        "masteryRate": 0.58,
                        "evidenceCount": 18,
                    },
                ],
            },
        )

    def assert_error(self, code, callback):
        with self.assertRaises(ToolExecutionError) as raised:
            callback()
        self.assertEqual(code, raised.exception.code)

    def test_registry_exposes_six_complete_tool_contracts(self):
        descriptors = self.registry.descriptors()

        self.assertEqual(6, len(descriptors))
        self.assertEqual(
            {
                "create_quiz_draft",
                "query_course_graph",
                "query_student_weakness",
                "retrieve_course_material",
                "retrieve_teaching_case",
                "save_lesson_plan_draft",
            },
            {item["name"] for item in descriptors},
        )
        for item in descriptors:
            self.assertFalse(item["inputSchema"]["additionalProperties"])
            self.assertGreater(item["timeoutMs"], 0)
            self.assertTrue(item["errorTypes"])
            self.assertIn("dataScope", item["permission"])
            self.assertIn("retryableErrors", item)

    def test_retrieve_course_material_success(self):
        output = self.registry.invoke(
            "retrieve_course_material",
            {"query": "二叉树遍历", "topK": 5, "resourceTypes": ["plan"]},
            self.context,
        )

        self.assertEqual(1, output.total)
        self.assertEqual(11, output.items[0].resource_id)

    def test_retrieve_course_material_rejects_identity_override(self):
        self.assert_error(
            ToolErrorCode.INVALID_ARGUMENT,
            lambda: self.registry.invoke(
                "retrieve_course_material",
                {"query": "二叉树", "teacherId": 9999},
                self.context,
            ),
        )

    def test_retrieve_teaching_case_success(self):
        output = self.registry.invoke(
            "retrieve_teaching_case",
            {"query": "二叉树案例"},
            self.context,
        )

        self.assertEqual(1, output.total)
        self.assertEqual(21, output.items[0].case_id)

    def test_retrieve_teaching_case_rejects_invalid_limit(self):
        self.assert_error(
            ToolErrorCode.INVALID_ARGUMENT,
            lambda: self.registry.invoke(
                "retrieve_teaching_case",
                {"query": "案例", "topK": 99},
                self.context,
            ),
        )

    def test_query_course_graph_success(self):
        output = self.registry.invoke(
            "query_course_graph",
            {"query": "二叉树", "includeRelations": True},
            self.context,
        )

        self.assertEqual(1, output.total)
        self.assertEqual("tree-traversal", output.items[0].node_id)
        self.assertIn("递归", output.items[0].relations)

    def test_query_course_graph_rejects_untyped_boolean(self):
        self.assert_error(
            ToolErrorCode.INVALID_ARGUMENT,
            lambda: self.registry.invoke(
                "query_course_graph",
                {"includeRelations": "yes"},
                self.context,
            ),
        )

    def test_query_student_weakness_success(self):
        output = self.registry.invoke(
            "query_student_weakness",
            {"scope": "student", "studentId": 2001},
            self.context,
        )

        self.assertEqual(1, output.total)
        self.assertEqual(2001, output.items[0].student_id)

    def test_query_student_weakness_denies_other_student(self):
        self.assert_error(
            ToolErrorCode.PERMISSION_DENIED,
            lambda: self.registry.invoke(
                "query_student_weakness",
                {"scope": "student", "studentId": 2999},
                self.context,
            ),
        )

    def test_create_quiz_draft_success(self):
        output = self.registry.invoke(
            "create_quiz_draft",
            {
                "title": "二叉树随堂练习",
                "difficulty": "medium",
                "knowledgePoints": ["前序遍历"],
                "questions": [
                    {
                        "questionType": "choice",
                        "stem": "前序遍历首先访问什么？",
                        "choices": ["根节点", "左叶子"],
                        "answer": "根节点",
                        "analysis": "顺序为根、左、右。",
                    }
                ],
            },
            self.context,
        )

        self.assertEqual("session_draft", output.status)
        self.assertEqual(1, len(output.questions))
        self.assertIs(output, self.context.artifacts[output.draft_id])

    def test_create_quiz_draft_rejects_arbitrary_question_fields(self):
        self.assert_error(
            ToolErrorCode.INVALID_ARGUMENT,
            lambda: self.registry.invoke(
                "create_quiz_draft",
                {
                    "title": "练习",
                    "difficulty": "easy",
                    "knowledgePoints": ["递归"],
                    "questions": [
                        {
                            "questionType": "short_answer",
                            "stem": "什么是递归？",
                            "answer": "函数调用自身。",
                            "studentId": 2999,
                        }
                    ],
                },
                self.context,
            ),
        )

    def test_save_lesson_plan_draft_success(self):
        output = self.registry.invoke(
            "save_lesson_plan_draft",
            {
                "title": "二叉树遍历教案",
                "contentMarkdown": "# 教学目标\n理解三种遍历顺序。",
                "confirmed": True,
            },
            self.context,
        )

        self.assertEqual("session_draft", output.status)
        self.assertIn("教学目标", output.content_markdown)
        self.assertIs(output, self.context.artifacts[output.draft_id])

    def test_save_lesson_plan_draft_requires_trusted_confirmation(self):
        unconfirmed_context = ToolContext(
            actor_id=1001,
            actor_role="teacher",
            context_data={},
        )
        self.assert_error(
            ToolErrorCode.CONFIRMATION_REQUIRED,
            lambda: self.registry.invoke(
                "save_lesson_plan_draft",
                {
                    "title": "二叉树遍历教案",
                    "contentMarkdown": "# 教案",
                    "confirmed": True,
                },
                unconfirmed_context,
            ),
        )

    def test_registry_enforces_role_boundary(self):
        student_context = ToolContext(
            actor_id=3001,
            actor_role="student",
            student_id=3001,
            context_data={},
        )
        self.assert_error(
            ToolErrorCode.PERMISSION_DENIED,
            lambda: self.registry.invoke(
                "retrieve_course_material",
                {"query": "任意资料"},
                student_context,
            ),
        )

    def test_prepare_workflow_collects_typed_read_results(self):
        results = _collect_structured_tool_results(
            {"agentType": "plan"},
            "二叉树遍历",
            self.context,
        )

        self.assertEqual(
            {
                "retrieve_course_material",
                "retrieve_teaching_case",
                "query_course_graph",
                "query_student_weakness",
            },
            {item["tool"] for item in results},
        )
        self.assertTrue(all(item["status"] == "success" for item in results))


if __name__ == "__main__":
    unittest.main()
