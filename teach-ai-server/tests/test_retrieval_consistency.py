import json
import unittest
import uuid
from pathlib import Path
from unittest.mock import patch

from qdrant_client import QdrantClient

from agent.retriever import (
    EmbeddingModel,
    METADATA_SCHEMA_VERSION,
    QdrantPrepareIndex,
    ContextRetriever,
    build_documents,
    chunk_document,
    normalize_document,
    _query_terms,
)


FIXTURE_PATH = Path(__file__).parent / "fixtures" / "retrieval_documents.json"


def fixed_vector(_text):
    return [1.0] + [0.0] * 511


class RetrievalDocumentContractTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.documents = json.loads(FIXTURE_PATH.read_text(encoding="utf-8"))

    def test_normalized_chunks_have_stable_trace_metadata(self):
        content = "第一段。" * 700
        document = dict(self.documents[0], content=content)

        first = chunk_document(document)
        second = chunk_document(document)

        self.assertGreater(len(first), 1)
        self.assertEqual(
            [item["chunkId"] for item in first],
            [item["chunkId"] for item in second],
        )
        self.assertTrue(
            all(
                item["metadataSchemaVersion"] == METADATA_SCHEMA_VERSION
                for item in first
            )
        )
        self.assertTrue(all(item["documentId"] for item in first))
        self.assertTrue(all(item["sourceKey"] == "ai_resource-11" for item in first))
        self.assertEqual(
            list(range(len(first))),
            [item["chunkIndex"] for item in first],
        )
        self.assertTrue(all(item["chunkCount"] == len(first) for item in first))
        self.assertTrue(all(len(item["contentHash"]) == 64 for item in first))
        self.assertTrue(all(len(item["chunkHash"]) == 64 for item in first))

    def test_rejects_unscoped_private_document_and_unknown_source_type(self):
        with self.assertRaises(ValueError):
            normalize_document(
                {
                    "sourceType": "ai_resource",
                    "sourceId": "1",
                    "content": "missing teacher",
                    "scope": "mine",
                }
            )
        with self.assertRaises(ValueError):
            normalize_document(
                {
                    "sourceType": "arbitrary",
                    "sourceId": "1",
                    "content": "unsupported source",
                    "scope": "platform",
                }
            )

    def test_chinese_compound_query_produces_stable_search_terms(self):
        terms = _query_terms("快速排序分区递归")

        self.assertIn("快速排序分区递归", terms)
        self.assertIn("快速排序", terms)
        self.assertIn("分区", terms)
        self.assertIn("递归", terms)
        self.assertEqual(len(terms), len(set(terms)))

    def test_context_builder_uses_same_document_and_chunk_identity(self):
        payload = {
            "teacherId": 1001,
            "context": {
                "graphNodes": [],
                "resources": [
                    {
                        "id": 11,
                        "type": "plan",
                        "title": "二叉树遍历教案",
                        "content": "前序遍历按照根、左、右的顺序访问。",
                        "courseId": 101,
                        "courseName": "数据结构",
                    }
                ],
                "cases": [],
            },
        }

        documents = build_documents(payload)

        self.assertEqual(1, len(documents))
        self.assertEqual(
            "teacher:1001:ai_resource:11",
            documents[0]["documentId"],
        )
        self.assertEqual(
            "teacher:1001:ai_resource:11:chunk:0",
            documents[0]["chunkId"],
        )

    def test_local_fallback_applies_teacher_course_and_resource_filters(self):
        chunks = [
            item
            for document in self.documents
            for item in chunk_document(document)
        ]
        retriever = ContextRetriever(
            max_items=10,
            min_keyword_score=0.0,
            allow_model_load=False,
        )

        items = retriever.retrieve(
            "二叉树",
            chunks,
            teacher_id=1001,
            options={
                "mode": "auto",
                "courseId": 101,
                "resourceTypes": ["plan"],
            },
        )

        self.assertEqual(["11"], [item["sourceId"] for item in items])
        self.assertTrue(all(item["teacherId"] == 1001 for item in items))
        self.assertTrue(all(item["courseId"] == "101" for item in items))


class QdrantIndexConsistencyTest(unittest.TestCase):

    def setUp(self):
        self.documents = json.loads(FIXTURE_PATH.read_text(encoding="utf-8"))
        self.client = QdrantClient(location=":memory:")
        QdrantPrepareIndex._client_instance = self.client
        QdrantPrepareIndex._ready_collections.clear()
        self.index = QdrantPrepareIndex(
            collection="test_%s" % uuid.uuid4().hex
        )
        self.load_patch = patch.object(
            EmbeddingModel,
            "load",
            return_value=object(),
        )
        self.encode_patch = patch.object(
            EmbeddingModel,
            "encode",
            side_effect=lambda texts: [fixed_vector(text) for text in texts],
        )
        self.load_patch.start()
        self.encode_patch.start()

    def tearDown(self):
        self.encode_patch.stop()
        self.load_patch.stop()
        self.client.close()
        QdrantPrepareIndex._client_instance = None
        QdrantPrepareIndex._ready_collections.clear()

    def test_qdrant_filters_teacher_course_source_and_resource_type(self):
        result = self.index.upsert_documents(self.documents)
        self.assertEqual(6, result["indexed"])
        self.assertEqual(0, result["rejected"])

        mine = self.index.retrieve(
            "二叉树",
            teacher_id=1001,
            options={
                "mode": "mineOnly",
                "courseId": 101,
                "sourceTypes": ["ai_resource"],
                "resourceTypes": ["plan"],
                "topK": 10,
            },
        )
        self.assertEqual(["11"], [item["sourceId"] for item in mine])

        platform_case = self.index.retrieve(
            "二叉树",
            teacher_id=1001,
            options={
                "mode": "auto",
                "courseId": 101,
                "sourceTypes": ["case"],
                "topK": 10,
            },
        )
        self.assertEqual(["21"], [item["sourceId"] for item in platform_case])
        self.assertEqual(
            "https://example.test/cases/21",
            platform_case[0]["sourceUrl"],
        )

        global_graph = self.index.retrieve(
            "二叉树",
            teacher_id=1001,
            options={
                "mode": "auto",
                "courseId": 101,
                "sourceTypes": ["graph_node"],
                "topK": 10,
            },
        )
        self.assertEqual(
            ["tree-traversal"],
            [item["sourceId"] for item in global_graph],
        )

    def test_qdrant_retrieval_failure_is_logged_before_fallback(self):
        with patch.object(
            self.index,
            "_ensure_collection",
            side_effect=RuntimeError("qdrant unavailable"),
        ):
            with self.assertLogs("agent.retriever", level="WARNING") as captured:
                result = self.index.retrieve(
                    "二叉树遍历",
                    teacher_id=1001,
                    options={"topK": 3},
                )

        self.assertIsNone(result)
        self.assertTrue(any(
            "Qdrant retrieval unavailable" in line
            and "cause=RuntimeError" in line
            for line in captured.output
        ))

    def test_upsert_replaces_old_chunks_and_delete_removes_exact_document(self):
        original = self.documents[0]
        self.index.upsert_documents([original])
        updated = dict(
            original,
            title="二叉树遍历教案（更新）",
            content="更新后的二叉树后序遍历内容。",
            updatedAt="2",
        )

        update_result = self.index.upsert_documents([updated])
        self.assertEqual(1, update_result["indexed"])

        items = self.index.retrieve(
            "二叉树",
            teacher_id=1001,
            options={
                "mode": "mineOnly",
                "sourceTypes": ["ai_resource"],
                "topK": 10,
            },
        )
        self.assertEqual(1, len(items))
        self.assertEqual("二叉树遍历教案（更新）", items[0]["title"])
        self.assertIn("更新后", items[0]["content"])

        delete_result = self.index.delete_documents(
            teacher_id=1001,
            source_type="ai_resource",
            source_id="11",
            scope="mine",
        )
        self.assertTrue(delete_result["deleted"])
        self.assertEqual(
            [],
            self.index.retrieve(
                "二叉树",
                teacher_id=1001,
                options={"mode": "mineOnly", "topK": 10},
            ),
        )

    def test_invalid_document_is_rejected_without_indexing(self):
        result = self.index.upsert_documents(
            [
                {
                    "sourceType": "ai_resource",
                    "sourceId": "99",
                    "content": "missing private owner",
                    "scope": "mine",
                }
            ]
        )

        self.assertEqual(0, result["indexed"])
        self.assertEqual(1, result["rejected"])
        self.assertTrue(result["errors"])


if __name__ == "__main__":
    unittest.main()
