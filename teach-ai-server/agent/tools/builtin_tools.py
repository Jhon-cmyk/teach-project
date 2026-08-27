from __future__ import annotations

import re
import uuid
from dataclasses import dataclass
from typing import Any, Mapping

from .contracts import (
    ToolContext,
    ToolDefinition,
    ToolErrorCode,
    ToolExecutionError,
    ToolInput,
    ToolOutput,
    ToolRegistry,
    bounded_int,
    optional_positive_int,
    optional_text,
    positive_int,
    required_text,
    strict_fields,
    string_tuple,
)


READ_ERRORS = frozenset(
    {
        ToolErrorCode.INVALID_ARGUMENT,
        ToolErrorCode.PERMISSION_DENIED,
        ToolErrorCode.TIMEOUT,
        ToolErrorCode.DEPENDENCY_UNAVAILABLE,
        ToolErrorCode.INTERNAL_ERROR,
    }
)
WRITE_ERRORS = frozenset(
    {
        ToolErrorCode.INVALID_ARGUMENT,
        ToolErrorCode.PERMISSION_DENIED,
        ToolErrorCode.CONFIRMATION_REQUIRED,
        ToolErrorCode.CONFLICT,
        ToolErrorCode.TIMEOUT,
        ToolErrorCode.INTERNAL_ERROR,
    }
)
READ_RETRYABLE = frozenset(
    {ToolErrorCode.TIMEOUT, ToolErrorCode.DEPENDENCY_UNAVAILABLE}
)
NO_RETRY = frozenset()


@dataclass(frozen=True)
class MaterialSearchInput(ToolInput):
    query: str
    top_k: int = 5
    resource_types: tuple[str, ...] = ()

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "MaterialSearchInput":
        strict_fields(payload, {"query", "topK", "resourceTypes"}, {"query"})
        resource_types = string_tuple(
            payload.get("resourceTypes"),
            "resourceTypes",
            maximum_items=6,
            item_max_length=32,
        )
        allowed_types = {"plan", "quiz", "anim", "coding", "micro_video"}
        unsupported = set(resource_types) - allowed_types
        if unsupported:
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "Unsupported resourceTypes: %s" % ", ".join(sorted(unsupported)),
            )
        return cls(
            query=required_text(payload.get("query"), "query", 500),
            top_k=bounded_int(
                payload.get("topK"),
                "topK",
                default=5,
                minimum=1,
                maximum=10,
            ),
            resource_types=resource_types,
        )

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "query": {"type": "string", "minLength": 1, "maxLength": 500},
                "topK": {"type": "integer", "minimum": 1, "maximum": 10, "default": 5},
                "resourceTypes": {
                    "type": "array",
                    "maxItems": 6,
                    "items": {
                        "type": "string",
                        "enum": ["plan", "quiz", "anim", "coding", "micro_video"],
                    },
                },
            },
            ["query"],
        )


@dataclass(frozen=True)
class MaterialItem(ToolOutput):
    resource_id: int
    resource_type: str
    title: str
    snippet: str

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "resource_id": {"type": "integer"},
                "resource_type": {"type": "string"},
                "title": {"type": "string"},
                "snippet": {"type": "string"},
            },
            ["resource_id", "resource_type", "title", "snippet"],
        )


@dataclass(frozen=True)
class MaterialSearchOutput(ToolOutput):
    items: tuple[MaterialItem, ...]
    total: int

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "items": {"type": "array", "items": MaterialItem.json_schema()},
                "total": {"type": "integer", "minimum": 0},
            },
            ["items", "total"],
        )


@dataclass(frozen=True)
class TeachingCaseSearchInput(ToolInput):
    query: str
    top_k: int = 3

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "TeachingCaseSearchInput":
        strict_fields(payload, {"query", "topK"}, {"query"})
        return cls(
            query=required_text(payload.get("query"), "query", 500),
            top_k=bounded_int(
                payload.get("topK"),
                "topK",
                default=3,
                minimum=1,
                maximum=5,
            ),
        )

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "query": {"type": "string", "minLength": 1, "maxLength": 500},
                "topK": {"type": "integer", "minimum": 1, "maximum": 5, "default": 3},
            },
            ["query"],
        )


@dataclass(frozen=True)
class TeachingCaseItem(ToolOutput):
    case_id: int
    title: str
    summary: str
    scope: str
    parse_status: str

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "case_id": {"type": "integer"},
                "title": {"type": "string"},
                "summary": {"type": "string"},
                "scope": {"type": "string"},
                "parse_status": {"type": "string"},
            },
            ["case_id", "title", "summary", "scope", "parse_status"],
        )


@dataclass(frozen=True)
class TeachingCaseSearchOutput(ToolOutput):
    items: tuple[TeachingCaseItem, ...]
    total: int

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _list_output_schema(TeachingCaseItem.json_schema())


@dataclass(frozen=True)
class CourseGraphQueryInput(ToolInput):
    query: str | None = None
    max_nodes: int = 8
    include_relations: bool = True

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "CourseGraphQueryInput":
        strict_fields(payload, {"query", "maxNodes", "includeRelations"})
        include_relations = payload.get("includeRelations", True)
        if not isinstance(include_relations, bool):
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "includeRelations must be a boolean.",
            )
        return cls(
            query=optional_text(payload.get("query"), "query", 500),
            max_nodes=bounded_int(
                payload.get("maxNodes"),
                "maxNodes",
                default=8,
                minimum=1,
                maximum=20,
            ),
            include_relations=include_relations,
        )

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "query": {"type": "string", "minLength": 1, "maxLength": 500},
                "maxNodes": {"type": "integer", "minimum": 1, "maximum": 20, "default": 8},
                "includeRelations": {"type": "boolean", "default": True},
            },
            [],
        )


@dataclass(frozen=True)
class CourseGraphNodeItem(ToolOutput):
    node_id: str
    name: str
    category: str
    description: str
    difficulty: str
    relations: str

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "node_id": {"type": "string"},
                "name": {"type": "string"},
                "category": {"type": "string"},
                "description": {"type": "string"},
                "difficulty": {"type": "string"},
                "relations": {"type": "string"},
            },
            ["node_id", "name", "category", "description", "difficulty", "relations"],
        )


@dataclass(frozen=True)
class CourseGraphQueryOutput(ToolOutput):
    items: tuple[CourseGraphNodeItem, ...]
    total: int

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _list_output_schema(CourseGraphNodeItem.json_schema())


@dataclass(frozen=True)
class StudentWeaknessQueryInput(ToolInput):
    scope: str
    student_id: int | None = None
    top_k: int = 5

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "StudentWeaknessQueryInput":
        strict_fields(payload, {"scope", "studentId", "topK"}, {"scope"})
        scope = required_text(payload.get("scope"), "scope", 16).lower()
        if scope not in {"student", "class"}:
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "scope must be 'student' or 'class'.",
            )
        student_id = optional_positive_int(payload.get("studentId"), "studentId")
        if scope == "student" and student_id is None:
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "studentId is required when scope is 'student'.",
            )
        if scope == "class" and student_id is not None:
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "studentId is not allowed when scope is 'class'.",
            )
        return cls(
            scope=scope,
            student_id=student_id,
            top_k=bounded_int(
                payload.get("topK"),
                "topK",
                default=5,
                minimum=1,
                maximum=10,
            ),
        )

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "scope": {"type": "string", "enum": ["student", "class"]},
                "studentId": {"type": "integer", "minimum": 1},
                "topK": {"type": "integer", "minimum": 1, "maximum": 10, "default": 5},
            },
            ["scope"],
        )


@dataclass(frozen=True)
class StudentWeaknessItem(ToolOutput):
    knowledge_point: str
    mastery_rate: float
    evidence_count: int
    student_id: int | None = None

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "knowledge_point": {"type": "string"},
                "mastery_rate": {"type": "number", "minimum": 0, "maximum": 1},
                "evidence_count": {"type": "integer", "minimum": 0},
                "student_id": {"type": ["integer", "null"]},
            },
            ["knowledge_point", "mastery_rate", "evidence_count", "student_id"],
        )


@dataclass(frozen=True)
class StudentWeaknessQueryOutput(ToolOutput):
    scope: str
    items: tuple[StudentWeaknessItem, ...]
    total: int

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "scope": {"type": "string", "enum": ["student", "class"]},
                "items": {"type": "array", "items": StudentWeaknessItem.json_schema()},
                "total": {"type": "integer", "minimum": 0},
            },
            ["scope", "items", "total"],
        )


@dataclass(frozen=True)
class QuizQuestionInput(ToolInput):
    question_type: str
    stem: str
    answer: str
    choices: tuple[str, ...] = ()
    analysis: str | None = None

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "QuizQuestionInput":
        strict_fields(
            payload,
            {"questionType", "stem", "answer", "choices", "analysis"},
            {"questionType", "stem", "answer"},
        )
        question_type = required_text(
            payload.get("questionType"),
            "questionType",
            32,
        )
        if question_type not in {"choice", "true_false", "short_answer", "coding"}:
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "Unsupported questionType: %s" % question_type,
            )
        choices = string_tuple(
            payload.get("choices"),
            "choices",
            maximum_items=8,
            item_max_length=500,
        )
        if question_type == "choice" and len(choices) < 2:
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "Choice questions require at least two choices.",
            )
        if question_type != "choice" and choices:
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "choices are only allowed for choice questions.",
            )
        return cls(
            question_type=question_type,
            stem=required_text(payload.get("stem"), "stem", 2000),
            answer=required_text(payload.get("answer"), "answer", 4000),
            choices=choices,
            analysis=optional_text(payload.get("analysis"), "analysis", 4000),
        )

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "questionType": {
                    "type": "string",
                    "enum": ["choice", "true_false", "short_answer", "coding"],
                },
                "stem": {"type": "string", "minLength": 1, "maxLength": 2000},
                "answer": {"type": "string", "minLength": 1, "maxLength": 4000},
                "choices": {
                    "type": "array",
                    "maxItems": 8,
                    "items": {"type": "string", "maxLength": 500},
                },
                "analysis": {"type": "string", "maxLength": 4000},
            },
            ["questionType", "stem", "answer"],
        )


@dataclass(frozen=True)
class QuizDraftInput(ToolInput):
    title: str
    difficulty: str
    knowledge_points: tuple[str, ...]
    questions: tuple[QuizQuestionInput, ...]

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "QuizDraftInput":
        strict_fields(
            payload,
            {"title", "difficulty", "knowledgePoints", "questions"},
            {"title", "difficulty", "knowledgePoints", "questions"},
        )
        difficulty = required_text(payload.get("difficulty"), "difficulty", 16)
        if difficulty not in {"easy", "medium", "hard", "mixed"}:
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "difficulty must be easy, medium, hard, or mixed.",
            )
        raw_questions = payload.get("questions")
        if not isinstance(raw_questions, list) or not 1 <= len(raw_questions) <= 30:
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "questions must contain between 1 and 30 items.",
            )
        knowledge_points = string_tuple(
            payload.get("knowledgePoints"),
            "knowledgePoints",
            maximum_items=20,
            item_max_length=100,
        )
        if not knowledge_points:
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "knowledgePoints must contain at least one item.",
            )
        return cls(
            title=required_text(payload.get("title"), "title", 200),
            difficulty=difficulty,
            knowledge_points=knowledge_points,
            questions=tuple(
                QuizQuestionInput.from_payload(item)
                for item in raw_questions
            ),
        )

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "title": {"type": "string", "minLength": 1, "maxLength": 200},
                "difficulty": {
                    "type": "string",
                    "enum": ["easy", "medium", "hard", "mixed"],
                },
                "knowledgePoints": {
                    "type": "array",
                    "minItems": 1,
                    "maxItems": 20,
                    "items": {"type": "string", "maxLength": 100},
                },
                "questions": {
                    "type": "array",
                    "minItems": 1,
                    "maxItems": 30,
                    "items": QuizQuestionInput.json_schema(),
                },
            },
            ["title", "difficulty", "knowledgePoints", "questions"],
        )


@dataclass(frozen=True)
class QuizQuestionOutput(ToolOutput):
    question_type: str
    stem: str
    answer: str
    choices: tuple[str, ...]
    analysis: str | None

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "question_type": {"type": "string"},
                "stem": {"type": "string"},
                "answer": {"type": "string"},
                "choices": {"type": "array", "items": {"type": "string"}},
                "analysis": {"type": ["string", "null"]},
            },
            ["question_type", "stem", "answer", "choices", "analysis"],
        )


@dataclass(frozen=True)
class QuizDraftOutput(ToolOutput):
    draft_id: str
    draft_type: str
    status: str
    title: str
    difficulty: str
    knowledge_points: tuple[str, ...]
    questions: tuple[QuizQuestionOutput, ...]

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "draft_id": {"type": "string"},
                "draft_type": {"type": "string"},
                "status": {"type": "string", "enum": ["session_draft"]},
                "title": {"type": "string"},
                "difficulty": {"type": "string"},
                "knowledge_points": {"type": "array", "items": {"type": "string"}},
                "questions": {
                    "type": "array",
                    "minItems": 1,
                    "items": QuizQuestionOutput.json_schema(),
                },
            },
            [
                "draft_id",
                "draft_type",
                "status",
                "title",
                "difficulty",
                "knowledge_points",
                "questions",
            ],
        )


@dataclass(frozen=True)
class LessonPlanDraftInput(ToolInput):
    title: str
    content_markdown: str
    confirmed: bool

    @classmethod
    def from_payload(cls, payload: Mapping[str, Any]) -> "LessonPlanDraftInput":
        strict_fields(
            payload,
            {"title", "contentMarkdown", "confirmed"},
            {"title", "contentMarkdown", "confirmed"},
        )
        confirmed = payload.get("confirmed")
        if not isinstance(confirmed, bool):
            raise ToolExecutionError(
                ToolErrorCode.INVALID_ARGUMENT,
                "confirmed must be a boolean.",
            )
        return cls(
            title=required_text(payload.get("title"), "title", 200),
            content_markdown=required_text(
                payload.get("contentMarkdown"),
                "contentMarkdown",
                100_000,
            ),
            confirmed=confirmed,
        )

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "title": {"type": "string", "minLength": 1, "maxLength": 200},
                "contentMarkdown": {
                    "type": "string",
                    "minLength": 1,
                    "maxLength": 100000,
                },
                "confirmed": {
                    "type": "boolean",
                    "description": "Must be true only after explicit user confirmation.",
                },
            },
            ["title", "contentMarkdown", "confirmed"],
        )


@dataclass(frozen=True)
class LessonPlanDraftOutput(ToolOutput):
    draft_id: str
    status: str
    title: str
    content_markdown: str
    content_length: int

    @classmethod
    def json_schema(cls) -> dict[str, Any]:
        return _object_schema(
            {
                "draft_id": {"type": "string"},
                "status": {"type": "string", "enum": ["session_draft"]},
                "title": {"type": "string"},
                "content_markdown": {"type": "string"},
                "content_length": {"type": "integer", "minimum": 1},
            },
            ["draft_id", "status", "title", "content_markdown", "content_length"],
        )


def build_default_tool_registry() -> ToolRegistry:
    registry = ToolRegistry()
    registry.register(
        ToolDefinition(
            name="retrieve_course_material",
            description="Search materials already scoped to the current teacher.",
            input_type=MaterialSearchInput,
            output_type=MaterialSearchOutput,
            allowed_roles=frozenset({"teacher", "admin"}),
            data_scope="trusted current-teacher context only; identity parameters are not accepted",
            timeout_seconds=2.0,
            error_types=READ_ERRORS,
            retryable_errors=READ_RETRYABLE,
            confirmation_required=False,
            handler=_retrieve_course_material,
        )
    )
    registry.register(
        ToolDefinition(
            name="retrieve_teaching_case",
            description="Search approved platform cases or cases owned by the current teacher.",
            input_type=TeachingCaseSearchInput,
            output_type=TeachingCaseSearchOutput,
            allowed_roles=frozenset({"teacher", "admin"}),
            data_scope="trusted approved-platform/current-teacher case context only",
            timeout_seconds=2.0,
            error_types=READ_ERRORS,
            retryable_errors=READ_RETRYABLE,
            confirmation_required=False,
            handler=_retrieve_teaching_case,
        )
    )
    registry.register(
        ToolDefinition(
            name="query_course_graph",
            description="Query knowledge nodes and relations in the current course context.",
            input_type=CourseGraphQueryInput,
            output_type=CourseGraphQueryOutput,
            allowed_roles=frozenset({"teacher", "admin"}),
            data_scope="trusted current-course graph context only",
            timeout_seconds=2.0,
            error_types=READ_ERRORS,
            retryable_errors=READ_RETRYABLE,
            confirmation_required=False,
            handler=_query_course_graph,
        )
    )
    registry.register(
        ToolDefinition(
            name="query_student_weakness",
            description="Query weaknesses for the current class or an explicitly authorized student.",
            input_type=StudentWeaknessQueryInput,
            output_type=StudentWeaknessQueryOutput,
            allowed_roles=frozenset({"teacher", "admin", "student"}),
            data_scope="trusted current-class context and authorized student IDs only",
            timeout_seconds=2.0,
            error_types=READ_ERRORS,
            retryable_errors=READ_RETRYABLE,
            confirmation_required=False,
            handler=_query_student_weakness,
        )
    )
    registry.register(
        ToolDefinition(
            name="create_quiz_draft",
            description="Create a validated quiz draft in the current Agent session.",
            input_type=QuizDraftInput,
            output_type=QuizDraftOutput,
            allowed_roles=frozenset({"teacher", "admin"}),
            data_scope="request-local draft store; no database write",
            timeout_seconds=1.0,
            error_types=WRITE_ERRORS,
            retryable_errors=NO_RETRY,
            confirmation_required=False,
            handler=_create_quiz_draft,
        )
    )
    registry.register(
        ToolDefinition(
            name="save_lesson_plan_draft",
            description="Keep an explicitly confirmed lesson-plan draft in the current Agent session.",
            input_type=LessonPlanDraftInput,
            output_type=LessonPlanDraftOutput,
            allowed_roles=frozenset({"teacher", "admin"}),
            data_scope="request-local confirmed draft store; no database write",
            timeout_seconds=1.0,
            error_types=WRITE_ERRORS,
            retryable_errors=NO_RETRY,
            confirmation_required=True,
            handler=_save_lesson_plan_draft,
        )
    )
    return registry


def _retrieve_course_material(
    context: ToolContext,
    request: MaterialSearchInput,
) -> MaterialSearchOutput:
    candidates = []
    for item in _trusted_items(context, "resources"):
        if not _is_owned_by_actor(item, context.actor_id):
            continue
        resource_type = str(item.get("type") or "unknown")
        if request.resource_types and resource_type not in request.resource_types:
            continue
        text = "\n".join(
            str(item.get(key) or "")
            for key in ("title", "content", "paramsJson")
        )
        candidates.append((_score(request.query, text), item))
    selected = _ranked(candidates, request.top_k)
    items = tuple(
        MaterialItem(
            resource_id=positive_int(item.get("id"), "resource.id"),
            resource_type=str(item.get("type") or "unknown"),
            title=str(item.get("title") or "Untitled material"),
            snippet=str(item.get("content") or item.get("paramsJson") or "")[:500],
        )
        for item in selected
    )
    return MaterialSearchOutput(items=items, total=len(items))


def _retrieve_teaching_case(
    context: ToolContext,
    request: TeachingCaseSearchInput,
) -> TeachingCaseSearchOutput:
    candidates = []
    for item in _trusted_items(context, "cases"):
        if not _case_is_visible(item, context.actor_id):
            continue
        text = "\n".join(
            str(item.get(key) or "")
            for key in ("title", "summary", "content", "courseName", "category")
        )
        candidates.append((_score(request.query, text), item))
    selected = _ranked(candidates, request.top_k)
    items = tuple(
        TeachingCaseItem(
            case_id=positive_int(item.get("id"), "case.id"),
            title=str(item.get("title") or "Untitled case"),
            summary=str(item.get("summary") or item.get("content") or "")[:800],
            scope=str(item.get("scope") or "mine"),
            parse_status=str(item.get("parseStatus") or "unknown"),
        )
        for item in selected
    )
    return TeachingCaseSearchOutput(items=items, total=len(items))


def _query_course_graph(
    context: ToolContext,
    request: CourseGraphQueryInput,
) -> CourseGraphQueryOutput:
    candidates = []
    for item in _trusted_items(context, "graphNodes"):
        text = "\n".join(
            str(item.get(key) or "")
            for key in (
                "name",
                "category",
                "description",
                "learningContent",
                "commonMistakes",
                "teachingTips",
                "graphRelationText",
            )
        )
        candidates.append((_score(request.query or "", text), item))
    selected = _ranked(candidates, request.max_nodes, include_zero=not request.query)
    items = tuple(
        CourseGraphNodeItem(
            node_id=required_text(item.get("id"), "graphNode.id", 128),
            name=str(item.get("name") or "Unnamed node"),
            category=str(item.get("category") or ""),
            description=str(item.get("description") or "")[:1000],
            difficulty=str(item.get("difficulty") or ""),
            relations=(
                str(item.get("graphRelationText") or "")[:1000]
                if request.include_relations
                else ""
            ),
        )
        for item in selected
    )
    return CourseGraphQueryOutput(items=items, total=len(items))


def _query_student_weakness(
    context: ToolContext,
    request: StudentWeaknessQueryInput,
) -> StudentWeaknessQueryOutput:
    if request.scope == "student":
        _check_student_access(context, request.student_id)
    elif context.actor_role == "student":
        raise ToolExecutionError(
            ToolErrorCode.PERMISSION_DENIED,
            "Students cannot query class-level weakness data.",
        )

    items = []
    for item in _trusted_items(context, "studentWeaknesses"):
        item_scope = str(item.get("scope") or "student")
        item_student_id = optional_positive_int(item.get("studentId"), "weakness.studentId")
        if request.scope == "student":
            if item_scope != "student" or item_student_id != request.student_id:
                continue
        elif item_scope != "class":
            continue
        mastery_rate = item.get("masteryRate", 0)
        if isinstance(mastery_rate, bool) or not isinstance(mastery_rate, (int, float)):
            continue
        mastery_rate = max(0.0, min(float(mastery_rate), 1.0))
        evidence_count = item.get("evidenceCount", 0)
        if isinstance(evidence_count, bool) or not isinstance(evidence_count, int):
            evidence_count = 0
        items.append(
            StudentWeaknessItem(
                knowledge_point=required_text(
                    item.get("knowledgePoint"),
                    "weakness.knowledgePoint",
                    200,
                ),
                mastery_rate=mastery_rate,
                evidence_count=max(0, evidence_count),
                student_id=item_student_id if request.scope == "student" else None,
            )
        )
    items.sort(key=lambda item: (item.mastery_rate, -item.evidence_count))
    selected = tuple(items[:request.top_k])
    return StudentWeaknessQueryOutput(
        scope=request.scope,
        items=selected,
        total=len(selected),
    )


def _create_quiz_draft(
    context: ToolContext,
    request: QuizDraftInput,
) -> QuizDraftOutput:
    draft_id = "quiz-%s" % uuid.uuid4().hex
    output = QuizDraftOutput(
        draft_id=draft_id,
        draft_type="quiz",
        status="session_draft",
        title=request.title,
        difficulty=request.difficulty,
        knowledge_points=request.knowledge_points,
        questions=tuple(
            QuizQuestionOutput(
                question_type=question.question_type,
                stem=question.stem,
                answer=question.answer,
                choices=question.choices,
                analysis=question.analysis,
            )
            for question in request.questions
        ),
    )
    context.artifacts[draft_id] = output
    return output


def _save_lesson_plan_draft(
    context: ToolContext,
    request: LessonPlanDraftInput,
) -> LessonPlanDraftOutput:
    if (
        not request.confirmed
        or "save_lesson_plan_draft" not in context.confirmed_actions
    ):
        raise ToolExecutionError(
            ToolErrorCode.CONFIRMATION_REQUIRED,
            "Lesson-plan drafts require a trusted explicit user confirmation.",
        )
    draft_id = "lesson-plan-%s" % uuid.uuid4().hex
    output = LessonPlanDraftOutput(
        draft_id=draft_id,
        status="session_draft",
        title=request.title,
        content_markdown=request.content_markdown,
        content_length=len(request.content_markdown),
    )
    context.artifacts[draft_id] = output
    return output


def _trusted_items(context: ToolContext, name: str) -> list[Mapping[str, Any]]:
    raw_items = context.context_data.get(name) or []
    if not isinstance(raw_items, list):
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "Trusted context field '%s' must be an array." % name,
        )
    return [item for item in raw_items if isinstance(item, Mapping)]


def _is_owned_by_actor(item: Mapping[str, Any], actor_id: int) -> bool:
    owner = item.get("teacherId")
    return owner is None or owner == actor_id


def _case_is_visible(item: Mapping[str, Any], actor_id: int) -> bool:
    scope = str(item.get("scope") or "mine")
    if scope == "platform":
        status = str(item.get("status") or "approved")
        return status == "approved"
    return _is_owned_by_actor(item, actor_id)


def _check_student_access(context: ToolContext, student_id: int | None) -> None:
    if student_id is None:
        raise ToolExecutionError(
            ToolErrorCode.INVALID_ARGUMENT,
            "studentId is required.",
        )
    if context.actor_role == "student":
        if context.student_id != student_id:
            raise ToolExecutionError(
                ToolErrorCode.PERMISSION_DENIED,
                "Students can only query their own weakness data.",
            )
        return
    if student_id not in context.allowed_student_ids:
        raise ToolExecutionError(
            ToolErrorCode.PERMISSION_DENIED,
            "The requested student is outside the authorized class scope.",
        )


def _score(query: str, text: str) -> int:
    normalized_query = (query or "").strip().lower()
    normalized_text = (text or "").lower()
    if not normalized_query:
        return 1
    score = 12 if normalized_query in normalized_text else 0
    tokens = {
        token
        for token in re.split(r"[\s,，、;；:：|/]+", normalized_query)
        if len(token) >= 2
    }
    for segment in re.findall(r"[\u4e00-\u9fff]{2,}", normalized_query):
        tokens.update(
            segment[index:index + 2]
            for index in range(len(segment) - 1)
        )
    for token in tokens:
        if token in normalized_text:
            score += min(len(token) * 2, 16)
    return score


def _ranked(
    candidates: list[tuple[int, Mapping[str, Any]]],
    limit: int,
    *,
    include_zero: bool = False,
) -> list[Mapping[str, Any]]:
    candidates.sort(key=lambda item: item[0], reverse=True)
    return [
        item
        for score, item in candidates
        if include_zero or score > 0
    ][:limit]


def _object_schema(
    properties: dict[str, Any],
    required: list[str],
) -> dict[str, Any]:
    return {
        "type": "object",
        "properties": properties,
        "required": required,
        "additionalProperties": False,
    }


def _list_output_schema(item_schema: dict[str, Any]) -> dict[str, Any]:
    return _object_schema(
        {
            "items": {"type": "array", "items": item_schema},
            "total": {"type": "integer", "minimum": 0},
        },
        ["items", "total"],
    )
