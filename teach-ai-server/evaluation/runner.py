from __future__ import annotations

import argparse
import json
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from agent.retriever import ContextRetriever, chunk_document
from agent.tools import ToolContext
from agent.workflows import (
    NO_RETRIEVAL_AGENT_TYPES,
    _build_evidence_pack,
    _collect_structured_tool_results,
    _quality,
    _referenced_evidence_ids,
)


EVALUATION_ROOT = Path(__file__).resolve().parent
DEFAULT_SUITE_PATH = EVALUATION_ROOT / "suite_v1.json"
DEFAULT_CORPUS_PATH = EVALUATION_ROOT / "corpus_v1.json"
DEFAULT_BASELINE_PATH = EVALUATION_ROOT / "baselines" / "agent_eval_v1.json"
DEFAULT_OUTPUT_DIR = EVALUATION_ROOT.parent.parent / "outputs" / "agent-evaluation"

RATE_METRICS = (
    "retrieval_hit_rate",
    "citation_coverage_rate",
    "tool_call_success_rate",
    "format_pass_rate",
    "task_completion_rate",
)


class EvaluationConfigurationError(ValueError):
    pass


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def load_suite(
    suite_path: Path = DEFAULT_SUITE_PATH,
    corpus_path: Path = DEFAULT_CORPUS_PATH,
) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    suite = load_json(Path(suite_path))
    corpus = load_json(Path(corpus_path))
    validate_suite(suite, corpus)
    return suite, corpus


def validate_suite(suite: dict[str, Any], corpus: list[dict[str, Any]]) -> None:
    if not isinstance(suite, dict) or not str(suite.get("suiteVersion") or ""):
        raise EvaluationConfigurationError("suiteVersion is required")
    if suite.get("executionMode") != "offline_deterministic":
        raise EvaluationConfigurationError("default evaluation must be deterministic and offline")
    if not suite.get("promptContractVersion") or not suite.get("model"):
        raise EvaluationConfigurationError("prompt and model versions are required")
    tasks = suite.get("tasks")
    if not isinstance(tasks, list) or len(tasks) < 20:
        raise EvaluationConfigurationError("at least 20 fixed tasks are required")
    task_ids = [str(task.get("id") or "") for task in tasks]
    if any(not task_id for task_id in task_ids) or len(task_ids) != len(set(task_ids)):
        raise EvaluationConfigurationError("task ids must be non-empty and unique")
    if not isinstance(corpus, list) or not corpus:
        raise EvaluationConfigurationError("evaluation corpus is required")

    corpus_keys = {_source_key(document) for document in corpus}
    for task in tasks:
        required = {
            "agentType", "teacherId", "courseId", "query", "form",
            "contextSources", "expectedSources", "expectedTools", "formatProfile",
        }
        missing = required - set(task)
        if missing:
            raise EvaluationConfigurationError(
                "%s is missing %s" % (task.get("id"), ",".join(sorted(missing)))
            )
        referenced = set(task.get("contextSources") or ()) | set(task.get("expectedSources") or ())
        unknown = referenced - corpus_keys
        if unknown:
            raise EvaluationConfigurationError(
                "%s references unknown sources: %s"
                % (task["id"], ",".join(sorted(unknown)))
            )


def evaluate_suite(
    suite: dict[str, Any],
    corpus: list[dict[str, Any]],
) -> dict[str, Any]:
    chunks = [
        chunk
        for document in corpus
        for chunk in chunk_document(document)
    ]
    by_key = {_source_key(document): document for document in corpus}
    task_results = [
        evaluate_task(task, suite, chunks, by_key)
        for task in suite["tasks"]
    ]
    task_count = len(task_results)
    aggregate = {
        metric: round(
            sum(float(item["metrics"][metric]) for item in task_results) / task_count,
            4,
        )
        for metric in RATE_METRICS
    }
    aggregate.update({
        "degraded_rate": round(
            sum(1 for item in task_results if item["metrics"]["degraded"]) / task_count,
            4,
        ),
        "isolation_violation_count": sum(
            item["metrics"]["isolation_violation_count"]
            for item in task_results
        ),
        "average_first_token_ms": round(
            sum(item["metrics"]["first_token_ms"] for item in task_results) / task_count,
            2,
        ),
        "average_total_duration_ms": round(
            sum(item["metrics"]["total_duration_ms"] for item in task_results) / task_count,
            2,
        ),
        "failed_task_count": sum(1 for item in task_results if not item["passed"]),
    })
    return {
        "suiteVersion": suite["suiteVersion"],
        "promptContractVersion": suite["promptContractVersion"],
        "corpusVersion": suite["corpusVersion"],
        "executionMode": suite["executionMode"],
        "model": suite["model"],
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "taskCount": task_count,
        "aggregate": aggregate,
        "tasks": task_results,
        "failures": [item for item in task_results if not item["passed"]],
    }


def evaluate_task(
    task: dict[str, Any],
    suite: dict[str, Any],
    chunks: list[dict[str, Any]],
    corpus_by_key: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    started = time.perf_counter()
    agent_type = task["agentType"]
    options = {
        "mode": "auto",
        "courseId": task["courseId"],
        "topK": int(suite["retrieval"].get("topK") or 4),
    }
    if agent_type in NO_RETRIEVAL_AGENT_TYPES:
        retrieved = []
        retrieval_engine = "off"
    else:
        retrieved = ContextRetriever(
            max_items=options["topK"],
            min_keyword_score=float(
                suite["retrieval"].get("minimumKeywordScore") or 0.2
            ),
            allow_model_load=False,
        ).retrieve(
            task["query"],
            chunks,
            teacher_id=task["teacherId"],
            options=options,
        )
        retrieval_engine = suite["retrieval"]["engine"]
    retrieval_finished = time.perf_counter()

    evidence = _build_evidence_pack(agent_type, retrieved)
    payload = _tool_payload(task, corpus_by_key)
    tool_context = ToolContext.from_agent_payload(payload)
    tool_results = _collect_structured_tool_results(
        payload,
        task["query"],
        tool_context,
    )

    first_token_started = time.perf_counter()
    output = _offline_output(task, evidence)
    first_token_ms = int((time.perf_counter() - first_token_started) * 1000)
    quality = _quality(agent_type, output, task["form"], evidence)
    finished = time.perf_counter()

    retrieved_keys = {_source_key(item) for item in retrieved}
    expected_sources = set(task.get("expectedSources") or ())
    expected_tools = set(task.get("expectedTools") or ())
    successful_tools = {
        item["tool"]
        for item in tool_results
        if item.get("status") == "success"
    }
    referenced_ids = _referenced_evidence_ids(output)
    cited_sources = {
        item["sourceKey"]
        for item in evidence
        if item.get("evidenceId") in referenced_ids
    }
    isolation_violations = _isolation_violations(
        retrieved,
        task["teacherId"],
        task["courseId"],
        set(suite.get("globalForbiddenSources") or ()),
    )
    retrieval_hit_rate = _coverage(expected_sources, retrieved_keys)
    citation_coverage_rate = _coverage(expected_sources, cited_sources)
    tool_success_rate = _coverage(expected_tools, successful_tools)
    format_pass = bool(quality["checks"]) and all(
        check.get("passed") for check in quality["checks"]
    )
    completed = (
        retrieval_hit_rate == 1.0
        and citation_coverage_rate == 1.0
        and tool_success_rate == 1.0
        and format_pass
        and not isolation_violations
    )
    reasons = []
    if retrieval_hit_rate < 1:
        reasons.append("retrieval_miss")
    if citation_coverage_rate < 1:
        reasons.append("citation_missing")
    if tool_success_rate < 1:
        reasons.append("tool_failure")
    if not format_pass:
        reasons.append("format_invalid")
    if isolation_violations:
        reasons.append("isolation_violation")

    return {
        "taskId": task["id"],
        "agentType": agent_type,
        "passed": completed,
        "failureCategories": reasons,
        "expectedSources": sorted(expected_sources),
        "retrievedSources": sorted(retrieved_keys),
        "citedSources": sorted(cited_sources),
        "expectedTools": sorted(expected_tools),
        "successfulTools": sorted(successful_tools),
        "qualityScore": quality["score"],
        "failedQualityChecks": [
            check["name"] for check in quality["checks"] if not check.get("passed")
        ],
        "metrics": {
            "retrieval_hit_rate": retrieval_hit_rate,
            "citation_coverage_rate": citation_coverage_rate,
            "tool_call_success_rate": tool_success_rate,
            "format_pass_rate": 1.0 if format_pass else 0.0,
            "task_completion_rate": 1.0 if completed else 0.0,
            "retrieval_engine": retrieval_engine,
            "retrieval_duration_ms": int((retrieval_finished - started) * 1000),
            "first_token_ms": first_token_ms,
            "total_duration_ms": int((finished - started) * 1000),
            "degraded": False,
            "isolation_violation_count": len(isolation_violations),
        },
    }


def compare_with_baseline(
    report: dict[str, Any],
    baseline: dict[str, Any],
) -> list[str]:
    if report.get("suiteVersion") != baseline.get("suiteVersion"):
        return ["suite_version_mismatch"]
    failures = []
    thresholds = baseline.get("thresholds") or {}
    aggregate = report["aggregate"]
    for metric in RATE_METRICS:
        minimum = float(thresholds.get(metric, 0))
        if float(aggregate.get(metric, 0)) < minimum:
            failures.append(
                "%s %.4f < %.4f"
                % (metric, float(aggregate.get(metric, 0)), minimum)
            )
    maximum_degraded = float(thresholds.get("maximum_degraded_rate", 1))
    if float(aggregate.get("degraded_rate", 0)) > maximum_degraded:
        failures.append("degraded_rate exceeds baseline")
    maximum_isolation = int(thresholds.get("maximum_isolation_violations", 0))
    if int(aggregate.get("isolation_violation_count", 0)) > maximum_isolation:
        failures.append("isolation violations exceed baseline")
    maximum_failed_tasks = int(thresholds.get("maximum_failed_tasks", 0))
    if int(aggregate.get("failed_task_count", 0)) > maximum_failed_tasks:
        failures.append("failed task count exceeds baseline")
    return failures


def write_report(report: dict[str, Any], output_dir: Path) -> tuple[Path, Path]:
    output_dir.mkdir(parents=True, exist_ok=True)
    report_path = output_dir / "agent-eval-current.json"
    failures_path = output_dir / "agent-eval-failures.json"
    report_path.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    failures_path.write_text(
        json.dumps(
            {
                "suiteVersion": report["suiteVersion"],
                "generatedAt": report["generatedAt"],
                "failures": report["failures"],
            },
            ensure_ascii=False,
            indent=2,
        ) + "\n",
        encoding="utf-8",
    )
    return report_path, failures_path


def _tool_payload(
    task: dict[str, Any],
    corpus_by_key: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    context = {"resources": [], "cases": [], "graphNodes": []}
    for source_key in task.get("contextSources") or ():
        document = corpus_by_key[source_key]
        source_type = document["sourceType"]
        if source_type == "ai_resource":
            context["resources"].append({
                "id": int(document["sourceId"]),
                "teacherId": document.get("teacherId"),
                "type": document["resourceType"],
                "title": document["title"],
                "content": document["content"],
                "courseId": document.get("courseId"),
            })
        elif source_type == "case":
            context["cases"].append({
                "id": int(document["sourceId"]),
                "scope": document["scope"],
                "status": "approved",
                "title": document["title"],
                "summary": document["content"],
                "content": document["content"],
                "parseStatus": "ok",
                "pdfParseOk": True,
            })
        elif source_type == "graph_node":
            context["graphNodes"].append({
                "id": document["sourceId"],
                "name": document["title"],
                "category": "evaluation",
                "description": document["content"],
                "difficulty": "medium",
                "graphRelationText": "固定评测关系",
            })
    return {
        "agentType": task["agentType"],
        "teacherId": task["teacherId"],
        "actorRole": "teacher",
        "courseId": task["courseId"],
        "form": task["form"],
        "context": context,
    }


def _offline_output(task: dict[str, Any], evidence: list[dict[str, Any]]) -> str:
    references = " ".join(
        "【参考：%s】" % item["evidenceId"]
        for item in evidence
    )
    agent_type = task["agentType"]
    if agent_type == "plan":
        return "\n".join([
            "# 教学主题",
            task["form"].get("topic") or task["query"],
            "## 课时信息",
            "1 课时",
            "## 学情分析",
            "学生已经具备相关先修知识。",
            "## 教学目标",
            "理解核心概念并完成迁移应用。 " + references,
            "## 教学重难点",
            "掌握关键过程。",
            "## 教学准备",
            "固定案例与课堂练习。",
            "## 教学过程",
            "教师讲解、学生练习并进行反馈。 " + references,
            "## 作业/课后任务",
            "完成一道应用题。",
        ])
    if agent_type in {"quiz", "quiz_optimize"}:
        total = sum(int(value) for value in (task["form"].get("typeCounts") or {}).values())
        questions = [
            "%s. 关于%s的说法，正确的是？ %s" % (index, task["query"], references)
            for index in range(1, total + 1)
        ]
        return "\n".join(questions + ["---", "## 答案与解析", "1. A。依据核心知识进行判断。 " + references])
    if agent_type in {"anim", "anim_repair", "anim_optimize"}:
        profile = task["formatProfile"]
        step_count = 5 if profile == "concept" else 3
        steps = []
        for index in range(step_count):
            step = {
                "title": "步骤%s" % (index + 1),
                "desc": "展示关键变化",
                "stageCaption": "观察第%s步" % (index + 1),
                "motion": {"type": "observe"},
            }
            if profile == "concept":
                step["visual"] = {
                    "type": "highlight-card",
                    "mainValue": str(index + 1),
                    "label": "概念",
                    "tone": "info",
                }
            steps.append(step)
        return json.dumps(
            {"templateType": profile, "title": task["query"], "steps": steps},
            ensure_ascii=False,
        )
    if agent_type == "coding":
        languages = task["form"].get("languages") or []
        return json.dumps({
            "title": task["query"],
            "description": "实现指定算法，并满足输入输出约束。",
            "testCases": [
                {"input": str(index), "output": str(index), "isSample": index < 2}
                for index in range(4)
            ],
            "templates": [
                {"language": language, "code": "// solution template"}
                for language in languages
            ],
        }, ensure_ascii=False)
    if agent_type == "micro_video":
        return json.dumps({
            "title": task["query"],
            "scenes": [
                {
                    "narration": "讲解第%s部分。%s" % (index + 1, references),
                    "visual": "展示第%s个关键画面" % (index + 1),
                }
                for index in range(3)
            ],
        }, ensure_ascii=False)
    raise EvaluationConfigurationError("unsupported agent type: %s" % agent_type)


def _isolation_violations(
    retrieved: list[dict[str, Any]],
    teacher_id: int,
    course_id: int,
    forbidden_sources: set[str],
) -> list[str]:
    violations = []
    for item in retrieved:
        key = _source_key(item)
        wrong_owner = (
            item.get("scope") != "platform"
            and item.get("teacherId") != teacher_id
        )
        wrong_course = (
            item.get("sourceType") != "graph_node"
            and str(item.get("courseId") or "") != str(course_id)
        )
        if key in forbidden_sources or wrong_owner or wrong_course:
            violations.append(key)
    return sorted(set(violations))


def _coverage(expected: set[str], actual: set[str]) -> float:
    if not expected:
        return 1.0
    return round(len(expected & actual) / len(expected), 4)


def _source_key(document: dict[str, Any]) -> str:
    return "%s-%s" % (document.get("sourceType"), document.get("sourceId"))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Run the fixed Agent evaluation suite")
    parser.add_argument("--suite", type=Path, default=DEFAULT_SUITE_PATH)
    parser.add_argument("--corpus", type=Path, default=DEFAULT_CORPUS_PATH)
    parser.add_argument("--baseline", type=Path, default=DEFAULT_BASELINE_PATH)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--check", action="store_true", help="fail when baseline thresholds regress")
    args = parser.parse_args(argv)

    suite, corpus = load_suite(args.suite, args.corpus)
    report = evaluate_suite(suite, corpus)
    report_path, failures_path = write_report(report, args.output_dir)
    regressions = []
    if args.check:
        if not args.baseline.exists():
            regressions = ["baseline_missing"]
        else:
            regressions = compare_with_baseline(report, load_json(args.baseline))

    print(json.dumps({
        "suiteVersion": report["suiteVersion"],
        "taskCount": report["taskCount"],
        "aggregate": report["aggregate"],
        "report": str(report_path),
        "failures": str(failures_path),
        "regressions": regressions,
    }, ensure_ascii=False, indent=2))
    return 1 if regressions or report["aggregate"]["failed_task_count"] else 0


if __name__ == "__main__":
    sys.exit(main())
