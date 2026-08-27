import hashlib
import logging
import math
import os
import re
import uuid

LOGGER = logging.getLogger(__name__)

WEAK_QUERY_TERMS = {
    "基础知识", "新授课", "复习课", "习题课", "实验课",
    "本科一年级", "本科二年级", "本科三年级", "本科四年级",
    "较弱", "一般", "较好", "标准版", "标准规范", "简洁实用", "详细展开",
    "讲授演示法", "案例教学法", "项目驱动法", "任务驱动法", "探究式学习", "合作学习",
    "课堂提问设计", "板书设计", "随堂练习", "分层任务",
    "概念抽象", "理解困难", "迁移应用弱", "计算易错",
    "无", "无特别要求",
}

QDRANT_URL = os.getenv("QDRANT_URL", "http://localhost:6333")
QDRANT_API_KEY = os.getenv("QDRANT_API_KEY") or None
QDRANT_COLLECTION = os.getenv("QDRANT_COLLECTION", "teach_prepare_docs")
QDRANT_TIMEOUT_SECONDS = float(os.getenv("QDRANT_TIMEOUT_SECONDS", "2"))
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "BAAI/bge-small-zh-v1.5")
VECTOR_SIZE = int(os.getenv("QDRANT_VECTOR_SIZE", "512"))
CHUNK_SIZE = int(os.getenv("RAG_CHUNK_SIZE", "900"))
CHUNK_OVERLAP = int(os.getenv("RAG_CHUNK_OVERLAP", "120"))
METADATA_SCHEMA_VERSION = 1
ALLOWED_SOURCE_TYPES = frozenset({"ai_resource", "case", "graph_node"})


def _safe_text(value):
    if value is None:
        return ""
    if isinstance(value, str):
        return value
    return str(value)


def _tokenize(text):
    text = _safe_text(text).lower()
    words = re.findall(r"[a-zA-Z0-9_]+|[\u4e00-\u9fff]", text)
    return [w for w in words if w.strip()]


def _query_terms(query):
    text = _safe_text(query).lower()
    parts = re.split(r"[\s,，、;；:：\[\]{}()（）\"'|/]+", text)
    terms = []
    for part in parts:
        part = part.strip()
        if len(part) >= 2 and part not in WEAK_QUERY_TERMS:
            terms.append(part)
        for cjk_sequence in re.findall(r"[\u4e00-\u9fff]{4,}", part):
            for size in (2, 3, 4):
                terms.extend(
                    cjk_sequence[index:index + size]
                    for index in range(len(cjk_sequence) - size + 1)
                    if cjk_sequence[index:index + size] not in WEAK_QUERY_TERMS
                )
    return list(dict.fromkeys(terms))[:64]


def _source_key(doc):
    return "%s-%s" % (doc.get("sourceType") or "context", doc.get("sourceId") or "")


def normalize_document(document, *, default_teacher_id=None, default_course_id=None):
    if not isinstance(document, dict):
        raise ValueError("document must be an object")
    source_type = _safe_text(document.get("sourceType")).strip()
    if source_type not in ALLOWED_SOURCE_TYPES:
        raise ValueError("unsupported sourceType: %s" % (source_type or "<empty>"))
    source_id = _safe_text(document.get("sourceId")).strip()
    if not source_id or len(source_id) > 128:
        raise ValueError("sourceId is required and must not exceed 128 characters")

    scope = _safe_text(document.get("scope") or "mine").strip().lower()
    if scope not in {"mine", "platform"}:
        raise ValueError("scope must be mine or platform")
    teacher_id = document.get("teacherId", default_teacher_id)
    if scope == "platform":
        teacher_id = None
    elif isinstance(teacher_id, bool) or not isinstance(teacher_id, int) or teacher_id <= 0:
        raise ValueError("mine-scope documents require a positive teacherId")

    content = _safe_text(document.get("content")).strip()
    if not content:
        provided_chunks = document.get("chunks") or []
        if isinstance(provided_chunks, list):
            content = "\n".join(
                _safe_text(item).strip()
                for item in provided_chunks
                if _safe_text(item).strip()
            )
    if not content:
        raise ValueError("document content is required")

    course_id = document.get("courseId", default_course_id)
    course_id = "" if course_id is None else _safe_text(course_id).strip()
    if len(course_id) > 128:
        raise ValueError("courseId must not exceed 128 characters")
    resource_type = _safe_text(
        document.get("resourceType")
        or document.get("type")
        or source_type
    ).strip()
    if not resource_type or len(resource_type) > 64:
        raise ValueError("resourceType is required and must not exceed 64 characters")

    owner_key = "platform" if scope == "platform" else "teacher:%s" % teacher_id
    source_key = "%s-%s" % (source_type, source_id)
    normalized = {
        "metadataSchemaVersion": METADATA_SCHEMA_VERSION,
        "documentId": "%s:%s:%s" % (owner_key, source_type, source_id),
        "teacherId": teacher_id,
        "courseId": course_id,
        "courseName": _safe_text(document.get("courseName")).strip()[:200],
        "sourceType": source_type,
        "sourceId": source_id,
        "sourceKey": source_key,
        "title": _safe_text(document.get("title") or source_type).strip()[:500],
        "content": content,
        "resourceType": resource_type,
        "scope": scope,
        "updatedAt": _safe_text(document.get("updatedAt")).strip()[:64],
        "sourceUrl": _safe_text(document.get("sourceUrl")).strip()[:2000],
        "sourceName": _safe_text(document.get("sourceName")).strip()[:500],
        "graphNodeId": _safe_text(document.get("graphNodeId")).strip()[:128],
        "pdfParseOk": bool(document.get("pdfParseOk", True)),
        "parseStatus": _safe_text(document.get("parseStatus") or "ok").strip()[:32],
        "graphRelationText": _safe_text(document.get("graphRelationText")).strip(),
        "mappedResourceText": _safe_text(document.get("mappedResourceText")).strip(),
    }
    normalized["contentHash"] = hashlib.sha256(
        content.encode("utf-8")
    ).hexdigest()
    return normalized


def chunk_document(document):
    normalized = normalize_document(document)
    chunks = _chunk_text(normalized["content"])
    result = []
    chunk_count = len(chunks)
    for index, content in enumerate(chunks):
        item = dict(normalized)
        item["content"] = content
        item["chunkIndex"] = index
        item["chunkCount"] = chunk_count
        item["chunkId"] = "%s:chunk:%s" % (normalized["documentId"], index)
        item["chunkHash"] = hashlib.sha256(
            content.encode("utf-8")
        ).hexdigest()
        result.append(item)
    return result


def filter_documents(documents, teacher_id=None, options=None):
    options = options or {}
    mode = options.get("mode") or "auto"
    course_id = _safe_text(options.get("courseId")).strip()
    course_name = _safe_text(options.get("courseName")).strip().lower()
    source_types = _filter_values(options.get("sourceTypes"))
    resource_types = _filter_values(options.get("resourceTypes"))

    result = []
    for document in documents:
        scope = document.get("scope")
        owner = document.get("teacherId")
        if mode == "mineOnly":
            if teacher_id is None or owner != teacher_id:
                continue
        elif mode == "platformOnly":
            if scope != "platform":
                continue
        elif scope != "platform" and (teacher_id is None or owner != teacher_id):
            continue
        if (
            course_id
            and _safe_text(document.get("courseId")).strip() != course_id
            and document.get("sourceType") != "graph_node"
        ):
            continue
        if (
            course_name
            and _safe_text(document.get("courseName")).strip().lower() != course_name
            and document.get("sourceType") != "graph_node"
        ):
            continue
        if source_types and document.get("sourceType") not in source_types:
            continue
        if resource_types and document.get("resourceType") not in resource_types:
            continue
        result.append(document)
    return result


def _filter_values(value):
    if value is None:
        return set()
    if isinstance(value, str):
        value = [value]
    if not isinstance(value, (list, tuple, set)):
        return set()
    return {
        _safe_text(item).strip()
        for item in value
        if _safe_text(item).strip()
    }


def _diversify_chunks(documents, limit, max_chunks_per_document=2):
    result = []
    counts = {}
    for document in documents:
        document_id = (
            document.get("documentId")
            or document.get("sourceKey")
            or _source_key(document)
        )
        count = counts.get(document_id, 0)
        if count >= max_chunks_per_document:
            continue
        counts[document_id] = count + 1
        result.append(document)
        if len(result) >= limit:
            break
    return result


def _graph_policy(options):
    policy = (options or {}).get("graphPolicy") or "auto"
    if policy in {"auto", "graphFirst", "resourceFirst", "off"}:
        return policy
    return "auto"


def _source_weight(source_type, graph_policy):
    if source_type == "graph_node":
        if graph_policy == "graphFirst":
            return 1.22
        if graph_policy == "resourceFirst":
            return 0.78
        return 0.96
    if source_type == "case":
        return 1.08 if graph_policy != "graphFirst" else 1.0
    if source_type == "ai_resource":
        return 1.12 if graph_policy == "resourceFirst" else 1.0
    return 1.0


def _reason_for(doc, graph_policy, pinned):
    source_key = doc.get("sourceKey")
    if source_key in pinned:
        return "pinned"
    source_type = doc.get("sourceType")
    if source_type == "graph_node":
        if doc.get("mappedResourceText"):
            return "graph_resource"
        if doc.get("graphRelationText"):
            return "graph_relation"
        if graph_policy == "graphFirst":
            return "graph_priority"
        return "graph_semantic"
    if source_type == "case":
        return "case_semantic"
    if source_type == "ai_resource":
        return "resource_semantic"
    return "semantic"


def _chunk_text(text, max_chars=CHUNK_SIZE, overlap=CHUNK_OVERLAP):
    text = re.sub(r"\s+", " ", _safe_text(text)).strip()
    if not text:
        return []
    if len(text) <= max_chars:
        return [text]

    chunks = []
    start = 0
    while start < len(text):
        end = min(start + max_chars, len(text))
        window = text[start:end]
        cut = max(window.rfind("。"), window.rfind("！"), window.rfind("？"), window.rfind("."), window.rfind("\n"))
        if cut > max_chars * 0.55 and end < len(text):
            end = start + cut + 1
            window = text[start:end]
        chunks.append(window.strip())
        if end >= len(text):
            break
        start = max(end - overlap, start + 1)
    return [chunk for chunk in chunks if chunk]


def _snippet(content, query, max_chars=260):
    content = re.sub(r"\s+", " ", _safe_text(content)).strip()
    if len(content) <= max_chars:
        return content
    lower = content.lower()
    hit = -1
    for term in _query_terms(query):
        hit = lower.find(term.lower())
        if hit >= 0:
            break
    if hit < 0:
        return content[:max_chars].rstrip() + "..."
    start = max(hit - max_chars // 3, 0)
    end = min(start + max_chars, len(content))
    prefix = "..." if start > 0 else ""
    suffix = "..." if end < len(content) else ""
    return prefix + content[start:end].strip() + suffix


class EmbeddingModel:
    _model = None
    _model_failed = False

    @classmethod
    def load(cls):
        if cls._model_failed:
            return None
        if cls._model is not None:
            return cls._model
        try:
            from sentence_transformers import SentenceTransformer

            cls._model = SentenceTransformer(EMBEDDING_MODEL)
            return cls._model
        except Exception:
            cls._model_failed = True
            return None

    @classmethod
    def encode(cls, texts):
        model = cls.load()
        if model is None:
            return None
        vectors = model.encode(texts, normalize_embeddings=True)
        return [list(map(float, vector)) for vector in vectors]


class QdrantPrepareIndex:
    _client_instance = None
    _ready_collections = set()

    def __init__(self, collection=QDRANT_COLLECTION):
        self.collection = collection

    def available(self):
        return self._client() is not None and EmbeddingModel.load() is not None

    def upsert_documents(self, documents):
        if not documents:
            return {"indexed": 0, "chunks": 0, "rejected": 0}
        client = self._client()
        if client is None or EmbeddingModel.load() is None:
            return {"indexed": 0, "chunks": 0, "rejected": 0, "fallback": True}
        try:
            self._ensure_collection(client)

            points = []
            indexed = 0
            rejected = 0
            errors = []
            for doc in documents:
                try:
                    normalized = normalize_document(doc)
                    chunk_items = chunk_document(normalized)
                except ValueError as exc:
                    rejected += 1
                    errors.append(str(exc))
                    continue
                indexed += 1
                self.delete_documents(
                    normalized.get("teacherId"),
                    normalized["sourceType"],
                    normalized["sourceId"],
                    scope=normalized["scope"],
                )
                if normalized["sourceType"] == "case":
                    if normalized["scope"] == "platform":
                        raw_teacher_id = doc.get("teacherId")
                        if (
                            isinstance(raw_teacher_id, int)
                            and not isinstance(raw_teacher_id, bool)
                            and raw_teacher_id > 0
                        ):
                            self.delete_documents(
                                raw_teacher_id,
                                "case",
                                normalized["sourceId"],
                                scope="mine",
                            )
                    else:
                        self.delete_documents(
                            None,
                            "case",
                            normalized["sourceId"],
                            scope="platform",
                        )
                vectors = EmbeddingModel.encode(
                    [item["content"] for item in chunk_items]
                )
                if not vectors:
                    rejected += 1
                    indexed -= 1
                    errors.append(
                        "embedding unavailable for %s" % normalized["sourceKey"]
                    )
                    continue
                for chunk_item, vector in zip(chunk_items, vectors):
                    point_id = str(uuid.uuid5(
                        uuid.NAMESPACE_URL,
                        chunk_item["chunkId"],
                    ))
                    from qdrant_client.models import PointStruct

                    points.append(
                        PointStruct(
                            id=point_id,
                            vector=vector,
                            payload=chunk_item,
                        )
                    )

            if points:
                client.upsert(collection_name=self.collection, points=points)
            result = {
                "indexed": indexed,
                "chunks": len(points),
                "rejected": rejected,
            }
            if errors:
                result["errors"] = errors[:10]
            return result
        except Exception as exc:
            LOGGER.warning(
                "Qdrant index upsert failed, collection=%s, cause=%s",
                self.collection,
                exc.__class__.__name__,
            )
            return {
                "indexed": 0,
                "chunks": 0,
                "rejected": len(documents),
                "fallback": True,
                "error": exc.__class__.__name__,
            }

    def delete_documents(
        self,
        teacher_id=None,
        source_type=None,
        source_id=None,
        scope=None,
    ):
        client = self._client()
        if client is None:
            return {"deleted": False, "fallback": True}
        try:
            self._ensure_collection(client)
            from qdrant_client.models import FieldCondition, Filter, FilterSelector, MatchValue

            must = []
            if teacher_id is not None:
                must.append(FieldCondition(key="teacherId", match=MatchValue(value=teacher_id)))
            if scope:
                must.append(FieldCondition(key="scope", match=MatchValue(value=scope)))
            if source_type:
                must.append(FieldCondition(key="sourceType", match=MatchValue(value=source_type)))
            if source_id is not None:
                must.append(FieldCondition(key="sourceId", match=MatchValue(value=str(source_id))))
            if not must:
                return {"deleted": False, "error": "delete filter is empty"}
            client.delete(
                collection_name=self.collection,
                points_selector=FilterSelector(filter=Filter(must=must)),
            )
            return {"deleted": True}
        except Exception as exc:
            LOGGER.warning(
                "Qdrant index delete failed, collection=%s, cause=%s",
                self.collection,
                exc.__class__.__name__,
            )
            return {"deleted": False, "fallback": True, "error": str(exc)[:200]}

    def retrieve(self, query, teacher_id=None, options=None):
        options = options or {}
        mode = options.get("mode") or "auto"
        graph_policy = _graph_policy(options)
        if mode == "off" or graph_policy == "off":
            return []
        client = self._client()
        if client is None:
            return None
        try:
            self._ensure_collection(client)
            vectors = EmbeddingModel.encode([query])
            if not vectors:
                return None

            from qdrant_client.models import FieldCondition, Filter, MatchAny, MatchValue

            must = []
            if mode == "mineOnly":
                if teacher_id is None:
                    return []
                must.append(FieldCondition(key="teacherId", match=MatchValue(value=teacher_id)))
            elif mode == "platformOnly":
                must.append(FieldCondition(key="scope", match=MatchValue(value="platform")))
            elif teacher_id is not None:
                must.append(Filter(should=[
                    FieldCondition(key="teacherId", match=MatchValue(value=teacher_id)),
                    FieldCondition(key="scope", match=MatchValue(value="platform")),
                ]))
            else:
                must.append(FieldCondition(key="scope", match=MatchValue(value="platform")))

            graph_node_id = options.get("graphNodeId")
            if graph_node_id:
                must.append(FieldCondition(key="graphNodeId", match=MatchValue(value=str(graph_node_id))))

            course_id = _safe_text(options.get("courseId")).strip()
            if course_id:
                must.append(Filter(should=[
                    FieldCondition(key="courseId", match=MatchValue(value=course_id)),
                    FieldCondition(key="sourceType", match=MatchValue(value="graph_node")),
                ]))
            course_name = _safe_text(options.get("courseName")).strip()
            if course_name:
                must.append(Filter(should=[
                    FieldCondition(key="courseName", match=MatchValue(value=course_name)),
                    FieldCondition(key="sourceType", match=MatchValue(value="graph_node")),
                ]))
            source_types = sorted(_filter_values(options.get("sourceTypes")))
            if source_types:
                must.append(FieldCondition(key="sourceType", match=MatchAny(any=source_types)))
            resource_types = sorted(_filter_values(options.get("resourceTypes")))
            if resource_types:
                must.append(FieldCondition(key="resourceType", match=MatchAny(any=resource_types)))

            search_filter = Filter(must=must or None) if must else None
            top_k = int(options.get("topK") or 6)
            excluded = set(str(item) for item in (options.get("excludedSources") or []) if item)
            pinned = set(str(item) for item in (options.get("pinnedSources") or []) if item)
            limit = max(top_k * 4, top_k + len(pinned), 12)

            hits = client.search(
                collection_name=self.collection,
                query_vector=vectors[0],
                query_filter=search_filter,
                limit=limit,
                with_payload=True,
            )
            docs = []
            keyword = ContextRetriever(max_items=limit, min_keyword_score=0.0, graph_policy=graph_policy)
            for hit in hits:
                payload = dict(hit.payload or {})
                if payload.get("sourceKey") in excluded:
                    continue
                keyword_score = keyword._keyword_score(query, payload)
                pin_boost = 0.25 if payload.get("sourceKey") in pinned else 0
                source_weight = _source_weight(payload.get("sourceType"), graph_policy)
                score = (float(hit.score or 0) * 0.72 + min(keyword_score, 1.0) * 0.28) * source_weight + pin_boost
                payload["score"] = round(score, 4)
                payload["snippet"] = _snippet(payload.get("content"), query)
                payload["reason"] = _reason_for(payload, graph_policy, pinned)
                docs.append(payload)

            docs.sort(key=lambda item: item.get("score") or 0, reverse=True)
            return _diversify_chunks(docs, top_k)
        except Exception as exc:
            LOGGER.warning(
                "Qdrant retrieval unavailable, collection=%s, cause=%s",
                self.collection,
                exc.__class__.__name__,
            )
            return None

    def _client(self):
        if QdrantPrepareIndex._client_instance is not None:
            return QdrantPrepareIndex._client_instance
        try:
            from qdrant_client import QdrantClient

            QdrantPrepareIndex._client_instance = QdrantClient(
                url=QDRANT_URL,
                api_key=QDRANT_API_KEY,
                timeout=QDRANT_TIMEOUT_SECONDS,
            )
            return QdrantPrepareIndex._client_instance
        except Exception:
            return None

    def _ensure_collection(self, client):
        if self.collection in QdrantPrepareIndex._ready_collections:
            return
        from qdrant_client.models import Distance, VectorParams

        try:
            client.get_collection(self.collection)
        except Exception:
            client.create_collection(
                collection_name=self.collection,
                vectors_config=VectorParams(size=VECTOR_SIZE, distance=Distance.COSINE),
            )
        QdrantPrepareIndex._ready_collections.add(self.collection)


class ContextRetriever:
    """Small local retriever with optional sentence-transformers support."""

    _model = None
    _model_failed = False

    def __init__(self, max_items=8, min_keyword_score=0.2, allow_model_load=True, graph_policy="auto"):
        self.max_items = max_items
        self.min_keyword_score = min_keyword_score
        self.allow_model_load = allow_model_load
        self.graph_policy = graph_policy

    def retrieve(self, query, documents, teacher_id=None, options=None):
        options = options or {}
        docs = filter_documents(documents, teacher_id=teacher_id, options=options)
        excluded = set(
            str(item)
            for item in (options.get("excludedSources") or [])
            if item
        )
        pinned = set(
            str(item)
            for item in (options.get("pinnedSources") or [])
            if item
        )
        docs = [
            doc
            for doc in docs
            if _safe_text(doc.get("content")).strip()
            and doc.get("sourceKey") not in excluded
        ]
        if not docs:
            return []

        keyword_scores = [self._keyword_score(query, doc) for doc in docs]
        filtered = [
            (keyword_score, doc)
            for keyword_score, doc in zip(keyword_scores, docs)
            if keyword_score >= self.min_keyword_score
        ]
        if not filtered:
            return []

        docs = [doc for _, doc in filtered]
        keyword_scores = [score for score, _ in filtered]

        semantic = self._semantic_scores(query, docs)
        if semantic is None:
            scored = list(zip(keyword_scores, docs))
        else:
            scored = [
                (((semantic_score * 0.7) + (keyword_score * 0.3)) * _source_weight(doc.get("sourceType"), self.graph_policy), doc)
                for semantic_score, keyword_score, doc in zip(semantic, keyword_scores, docs)
            ]

        scored.sort(key=lambda item: item[0], reverse=True)
        result = []
        for score, doc in scored:
            item = dict(doc)
            pin_boost = 0.25 if item.get("sourceKey") in pinned else 0
            item["score"] = round(float(score) + pin_boost, 4)
            item["snippet"] = _snippet(item.get("content"), query)
            item["reason"] = _reason_for(item, self.graph_policy, pinned)
            result.append(item)
        result.sort(key=lambda item: item.get("score") or 0, reverse=True)
        return _diversify_chunks(result, self.max_items)

    def _semantic_scores(self, query, docs):
        model = self._load_model()
        if model is None:
            return None
        texts = [query] + [doc.get("content", "") for doc in docs]
        vectors = model.encode(texts, normalize_embeddings=True)
        query_vec = vectors[0]
        return [float(sum(a * b for a, b in zip(query_vec, vec))) for vec in vectors[1:]]

    def _load_model(self):
        if not self.allow_model_load and EmbeddingModel._model is None:
            return None
        return EmbeddingModel.load()

    def _keyword_score(self, query, doc):
        query_terms = _query_terms(query)
        content = _safe_text(doc.get("content", "")).lower()
        title = _safe_text(doc.get("title", "")).lower()
        if not query_terms or not content:
            return 0.0

        score = 0.0
        for term in query_terms:
            weight = min(len(term), 12)
            if term in title:
                score += weight * 2.5
            count = content.count(term)
            if count:
                score += weight * min(count, 3)

        if score <= 0:
            return 0.0

        length_penalty = math.log(len(_tokenize(content)) + 10)
        type_boost = {
            "graph_node": 1.0,
            "case": 1.12,
            "ai_resource": 1.0,
        }.get(doc.get("sourceType"), 1.0)
        return type_boost * score / length_penalty


def build_documents(payload):
    context = payload.get("context") or {}
    raw_documents = []
    teacher_id = payload.get("teacherId")

    for node in context.get("graphNodes") or []:
        content = "\n".join(
            part
            for part in [
                node.get("name"),
                node.get("description"),
                node.get("difficulty"),
                node.get("importance"),
                node.get("commonMistakes"),
                node.get("teachingTips"),
                node.get("resourceSummary"),
                node.get("learningContent"),
                node.get("graphRelationText"),
                node.get("mappedResourceText"),
            ]
            if part
        )
        scope = node.get("scope") or "platform"
        raw_documents.append(
            {
                "teacherId": teacher_id if scope == "mine" else None,
                "courseId": node.get("courseId"),
                "courseName": node.get("courseName"),
                "sourceType": "graph_node",
                "sourceId": node.get("id"),
                "title": node.get("name") or "Knowledge node",
                "content": content,
                "resourceType": "graph_node",
                "graphNodeId": node.get("id"),
                "graphRelationText": node.get("graphRelationText") or "",
                "mappedResourceText": node.get("mappedResourceText") or "",
                "scope": scope,
            }
        )

    for resource in context.get("resources") or []:
        raw_documents.append(
            {
                "teacherId": teacher_id,
                "courseId": resource.get("courseId"),
                "courseName": resource.get("courseName"),
                "sourceType": "ai_resource",
                "sourceId": resource.get("id"),
                "title": resource.get("title") or "AI resource",
                "content": resource.get("content") or resource.get("paramsJson") or "",
                "resourceType": resource.get("type") or "ai_resource",
                "scope": "mine",
            }
        )

    for case in context.get("cases") or []:
        case_chunks = case.get("contentChunks") or case.get("chunks") or []
        content = case.get("content") or ""
        if not _safe_text(content).strip() and isinstance(case_chunks, list):
            content = "\n".join(
                _safe_text(item).strip()
                for item in case_chunks
                if _safe_text(item).strip()
            )
        scope = case.get("scope") or "mine"
        raw_documents.append(
            {
                "teacherId": teacher_id if scope == "mine" else None,
                "courseId": case.get("courseId"),
                "courseName": case.get("courseName"),
                "sourceType": "case",
                "sourceId": case.get("id"),
                "title": case.get("title") or "Teaching case",
                "pdfParseOk": case.get("pdfParseOk", True),
                "parseStatus": case.get("parseStatus") or "ok",
                "content": content,
                "resourceType": "case",
                "scope": scope,
                "sourceUrl": case.get("sourceUrl") or "",
                "sourceName": case.get("sourceName") or "",
            }
        )

    docs = []
    for document in raw_documents:
        try:
            docs.extend(chunk_document(document))
        except ValueError:
            continue
    return docs
