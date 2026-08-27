import unittest
from unittest.mock import patch

from agent.workflows import _retrieve_context


class AgentRetrievalTest(unittest.TestCase):

    @patch("agent.workflows.ContextRetriever")
    @patch("agent.workflows.QdrantPrepareIndex")
    def test_qdrant_results_are_used_when_vector_retrieval_succeeds(
        self,
        qdrant_index,
        context_retriever,
    ):
        expected = [
            {
                "sourceType": "case",
                "sourceId": "7",
                "title": "二叉树遍历案例",
                "content": "前序遍历按照根、左、右的顺序访问节点。",
                "score": 0.92,
            }
        ]
        qdrant_index.return_value.retrieve.return_value = expected

        items, engine = _retrieve_context(
            {
                "agentType": "plan",
                "teacherId": 1001,
                "retrievalOptions": {"mode": "auto", "topK": 4},
            },
            "二叉树前序遍历怎么讲",
            [],
        )

        self.assertEqual("qdrant", engine)
        self.assertEqual(expected, items)
        context_retriever.assert_not_called()

    @patch("agent.workflows.QdrantPrepareIndex")
    def test_keyword_retrieval_is_used_when_qdrant_is_unavailable(self, qdrant_index):
        qdrant_index.return_value.retrieve.side_effect = RuntimeError("qdrant unavailable")
        documents = [
            {
                "teacherId": 1001,
                "scope": "mine",
                "sourceType": "case",
                "sourceId": "7",
                "sourceKey": "case-7",
                "documentId": "teacher:1001:case:7",
                "title": "二叉树遍历案例",
                "content": "二叉树前序遍历按照根节点、左子树、右子树的顺序访问。",
            },
            {
                "teacherId": 1001,
                "scope": "mine",
                "sourceType": "ai_resource",
                "sourceId": "9",
                "sourceKey": "ai_resource-9",
                "documentId": "teacher:1001:ai_resource:9",
                "title": "无关资源",
                "content": "计算机网络中的 TCP 三次握手。",
            },
        ]

        items, engine = _retrieve_context(
            {
                "agentType": "plan",
                "teacherId": 1001,
                "retrievalOptions": {"mode": "auto", "topK": 2},
            },
            "二叉树前序遍历",
            documents,
        )

        self.assertEqual("fallback_keyword", engine)
        self.assertGreaterEqual(len(items), 1)
        self.assertEqual("7", items[0]["sourceId"])
        self.assertIn(items[0]["reason"], {"fallback_keyword", "case_semantic"})
        self.assertTrue(items[0]["snippet"])
        self.assertEqual("case-7", items[0]["chunkId"])


if __name__ == "__main__":
    unittest.main()
