import json
import re
import time

from .deepseek_client import stream_deepseek
from .observability import estimate_tokens
from .retriever import ContextRetriever, QdrantPrepareIndex, build_documents
from .tools import DEFAULT_TOOL_REGISTRY, ToolContext, ToolExecutionError
from .workflow_state import (
    FailureCategory,
    WORKFLOW_RUN_STORE,
    WorkflowLimitError,
    WorkflowState,
    WorkflowStateError,
)


NO_RETRIEVAL_AGENT_TYPES = {"anim", "anim_repair", "anim_optimize", "coding"}


def _event(event_type, **kwargs):
    data = {"type": event_type, "ts": int(time.time() * 1000)}
    data.update(kwargs)
    return json.dumps(data, ensure_ascii=False) + "\n"


def _form_text(form):
    lines = []
    for key, value in (form or {}).items():
        if value is None or value == "":
            continue
        if isinstance(value, (dict, list)):
            value = json.dumps(value, ensure_ascii=False)
        lines.append("%s: %s" % (key, value))
    return "\n".join(lines)


def _as_list(value):
    if value is None:
        return []
    if isinstance(value, list):
        return [str(item) for item in value if item is not None and str(item).strip()]
    if isinstance(value, str):
        return [part.strip() for part in re.split(r"[,，、|\s]+", value) if part.strip()]
    return [str(value)]


def _query_for(payload):
    form = payload.get("form") or {}
    fields = [
        form.get("subject"),
        form.get("topic"),
        form.get("knowledgePoints"),
        form.get("description"),
        form.get("concept"),
        form.get("emphasis"),
        form.get("teachingGoal"),
        form.get("extraRequirements"),
        payload.get("sourceContent"),
    ]
    return " ".join(str(item) for item in fields if item)


def _citation_block(citations):
    if not citations:
        return "未检索到强相关平台上下文。生成时不要编造引用，也不要强行套用无关资源。"

    lines = []
    for idx, doc in enumerate(citations, 1):
        evidence_id = doc.get("evidenceId") or ("E%s" % idx)
        content = (doc.get("snippet") or doc.get("content") or "").replace("\r", " ").replace("\n", " ")
        use_for = "、".join(doc.get("useFor") or []) if isinstance(doc.get("useFor"), list) else (doc.get("useFor") or "")
        lines.append(
            "[%s] %s #%s %s score=%s\n适用用途：%s\n关键片段：%s"
            % (
                evidence_id,
                doc.get("sourceType") or "context",
                doc.get("sourceId") or "",
                doc.get("title") or "",
                doc.get("score") or "",
                use_for or "教学内容生成",
                content[:900],
            )
        )
    return "\n\n".join(lines)


def _infer_evidence_use_for(agent_type, doc):
    source_type = doc.get("sourceType") or ""
    resource_type = doc.get("resourceType") or ""
    if agent_type == "plan":
        if source_type == "case":
            return ["课堂导入", "任务情境", "课堂练习"]
        if source_type == "graph_node":
            return ["教学目标", "重难点", "易错点", "教学过程"]
        if resource_type == "quiz":
            return ["随堂练习", "课后作业"]
        if resource_type == "anim":
            return ["演示设计", "教学活动"]
        return ["教学过程", "活动设计"]
    if agent_type in {"quiz", "quiz_optimize"}:
        if source_type == "case":
            return ["案例题背景", "应用题情境"]
        if source_type == "graph_node":
            return ["考核范围", "易错点", "答案解析"]
        if resource_type == "quiz":
            return ["题型结构", "题目风格"]
        return ["题目设计", "答案解析"]
    if agent_type == "anim":
        if source_type == "graph_node":
            return ["模板选择", "步骤拆解", "视觉原语"]
        if source_type == "case":
            return ["情境化演示", "步骤说明"]
        return ["动画步骤", "讲解文案"]
    return ["内容生成"]


def _build_evidence_pack(agent_type, citations):
    evidence_pack = []
    for index, item in enumerate(citations or [], 1):
        evidence = dict(item)
        evidence["evidenceId"] = "E%s" % index
        evidence["snippet"] = (evidence.get("snippet") or evidence.get("content") or "")[:900]
        evidence["useFor"] = _infer_evidence_use_for(agent_type, evidence)
        evidence_pack.append(evidence)
    return evidence_pack


def _evidence_rules(agent_type, evidence_pack):
    if not evidence_pack:
        return "证据使用规则：本次没有强相关证据。不要输出【参考：E1】等虚假引用，也不要声称参考了平台资料。"

    ids = "、".join(item.get("evidenceId") for item in evidence_pack)
    common = (
        "证据使用规则：可用证据编号为 %s。只允许引用这些编号，禁止编造其他编号。"
        "使用证据生成内容时必须标注【参考：E编号】。不要大段复制证据原文，要转化为教学活动、题目或演示设计。"
    ) % ids
    if agent_type == "plan":
        return common + "教案中课堂导入、教学过程、易错点、练习设计凡使用证据处，在对应段落或条目开头标注【参考：E编号】。"
    if agent_type in {"quiz", "quiz_optimize"}:
        return common + "习题中题型标题、题干或答案解析使用证据时标注【参考：E编号】；案例题背景和易错解析应优先使用证据。"
    if agent_type == "anim":
        return common + "互动课件 JSON 的关键 steps 必须增加 evidenceIds 字段，例如 \"evidenceIds\":[\"E1\"]，并且只能使用上述编号。"
    return common


def _referenced_evidence_ids(text):
    result = set(re.findall(r"【参考：(E\d+)】", text or ""))
    for block in re.findall(r'"evidenceIds"\s*:\s*\[([^\]]*)\]', text or ""):
        result.update(re.findall(r"E\d+", block))
    return result


def _case_items(payload):
    context = payload.get("context") or {}
    return context.get("cases") or []


def _case_content(case):
    chunks = case.get("contentChunks") or case.get("chunks") or []
    if isinstance(chunks, list):
        joined = "\n".join(str(chunk) for chunk in chunks if chunk)
        if joined.strip():
            return joined
    return case.get("content") or ""


def _case_images(case):
    images = case.get("imageMaterials") or []
    result = []
    if isinstance(images, list):
        for item in images:
            if isinstance(item, dict) and item.get("url"):
                result.append({
                    "id": item.get("id"),
                    "caseId": item.get("caseId") or case.get("id"),
                    "title": item.get("title") or item.get("caption") or "Case image",
                    "caption": item.get("caption") or "",
                    "context": item.get("context") or "",
                    "url": item.get("url"),
                })
    if result:
        return result[:4]

    material_json = case.get("materialJson")
    try:
        materials = json.loads(material_json) if isinstance(material_json, str) else material_json
    except Exception:
        materials = []
    if isinstance(materials, list):
        for item in materials:
            if isinstance(item, dict) and item.get("type") == "image" and item.get("url"):
                result.append({
                    "id": item.get("id"),
                    "caseId": item.get("caseId") or case.get("id"),
                    "title": item.get("title") or item.get("caption") or "Case image",
                    "caption": item.get("caption") or "",
                    "context": item.get("context") or "",
                    "url": item.get("url"),
                })
    return result[:4]


def _case_image_candidates(case_analysis):
    if not case_analysis:
        return []
    if case_analysis.get("cases"):
        images = []
        for item in case_analysis.get("cases") or []:
            images.extend(item.get("imageMaterials") or [])
        return images[:4]
    return (case_analysis.get("imageMaterials") or [])[:4]


def _image_instruction_block(case_analysis):
    images = _case_image_candidates(case_analysis)
    if not images:
        return "Case image candidates: none. Do not insert Markdown images."
    lines = [
        "Case image candidates (whitelist only):",
        "You MUST insert 1-4 Markdown images in the lesson plan when candidates exist.",
        "Never invent image URLs. Every Markdown image URL must be copied exactly from this whitelist.",
    ]
    for index, item in enumerate(images[:4], 1):
        label = item.get("title") or item.get("caption") or ("Case image %s" % index)
        context = (item.get("context") or item.get("caption") or "")[:160]
        lines.append("[IMG%s] %s | %s | %s" % (index, label, item.get("url"), context))
    return "\n".join(lines)


def _allowed_case_image_urls(case_analysis):
    return {item.get("url") for item in _case_image_candidates(case_analysis) if item.get("url")}


def _markdown_image_urls(text):
    return set(re.findall(r"!\[[^\]]*]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)", text or ""))


def _missing_case_image_markdown(text, case_analysis):
    images = _case_image_candidates(case_analysis)
    if not images:
        return ""
    allowed = _allowed_case_image_urls(case_analysis)
    if _markdown_image_urls(text) & allowed:
        return ""
    lines = ["", "", "## 案例图片参考"]
    for index, item in enumerate(images[:4], 1):
        url = item.get("url")
        if not url:
            continue
        label = item.get("title") or item.get("caption") or ("案例图片%s" % index)
        lines.append("![%s](%s)" % (label, url))
        caption = item.get("caption") or item.get("context") or ""
        if caption:
            lines.append("_%s_" % caption[:120])
    return "\n".join(lines) + "\n"


def _split_sentences(text):
    if not text:
        return []
    parts = re.split(r"[。！？!?；;\n\r]+", text)
    return [part.strip() for part in parts if part and part.strip()]


def _case_analysis(payload):
    cases = _case_items(payload)
    if not cases:
        return None

    if len(cases) > 1:
        analyses = []
        any_parse_ok = False
        for index, case in enumerate(cases[:3], 1):
            content = _case_content(case)
            sentences = _split_sentences(content)
            key_sentences = [
                sentence for sentence in sentences
                if any(keyword in sentence for keyword in ["问题", "任务", "项目", "企业", "数据", "场景", "需求", "案例", "学生", "实践", "工程"])
            ][:5]
            if not key_sentences:
                key_sentences = sentences[:3]
            parse_ok = bool(case.get("pdfParseOk", True)) and bool(content.strip())
            any_parse_ok = any_parse_ok or parse_ok
            analyses.append({
                "id": case.get("id"),
                "title": case.get("title") or ("参考案例%s" % index),
                "courseName": case.get("courseName"),
                "sourceName": case.get("sourceName"),
                "sourceUrl": case.get("sourceUrl"),
                "summary": case.get("summary") or "；".join(key_sentences[:2]),
                "coreSituation": key_sentences[0] if key_sentences else "",
                "keyProblems": key_sentences[1:4],
                "materialJson": case.get("materialJson") or "[]",
                "imageMaterials": _case_images(case),
                "structureJson": case.get("structureJson") or "",
                "pdfParseOk": parse_ok,
            })
        return {
            "id": ",".join(str(case.get("id")) for case in analyses if case.get("id")),
            "title": "多案例融合参考",
            "pdfParseOk": any_parse_ok,
            "parseStatus": "ok" if any_parse_ok else "failed",
            "cases": analyses,
            "summary": "；".join(item.get("summary") or item.get("title") or "" for item in analyses[:3]),
            "coreSituation": "；".join(item.get("coreSituation") or "" for item in analyses[:3]),
            "keyProblems": [problem for item in analyses for problem in (item.get("keyProblems") or [])][:8],
            "imageMaterials": [image for item in analyses for image in (item.get("imageMaterials") or [])][:4],
            "transferableActivities": [
                "比较多份优秀案例的导入情境，选择最贴合本课题的真实问题作为课堂导入",
                "把案例中的关键任务改写为小组探究、代码实践或课堂讨论任务",
                "在教案末尾列出案例中的图片、课件、视频或互动资源链接作为推荐素材",
            ],
            "suitableSections": ["课堂导入", "任务探究", "小组讨论", "随堂练习", "推荐素材"],
        }

    case = cases[0]
    content = _case_content(case)
    sentences = _split_sentences(content)
    keywords = ["问题", "任务", "项目", "企业", "数据", "场景", "需求", "案例", "学生", "实践", "工程"]
    key_sentences = [
        sentence
        for sentence in sentences
        if any(keyword in sentence for keyword in keywords)
    ][:6]
    if not key_sentences:
        key_sentences = sentences[:4]

    parse_ok = bool(case.get("pdfParseOk", True)) and bool(content.strip())
    return {
        "id": case.get("id"),
        "title": case.get("title") or "未命名案例",
        "category": case.get("category"),
        "difficulty": case.get("difficulty"),
        "courseName": case.get("courseName"),
        "pdfParseOk": parse_ok,
        "parseStatus": "ok" if parse_ok else "failed",
        "summary": "；".join(key_sentences[:3]) if key_sentences else "",
        "coreSituation": key_sentences[0] if key_sentences else "",
        "keyProblems": key_sentences[1:4],
        "imageMaterials": _case_images(case),
        "transferableActivities": [
            "导入环节引用案例情境，引出本节课核心问题",
            "任务探究环节将案例问题改写为小组分析或实践任务",
            "课堂练习环节使用案例中的数据、角色或约束条件",
        ],
        "suitableSections": ["课堂导入", "任务驱动", "小组讨论", "随堂练习"],
    }


def _case_analysis_block(case_analysis):
    if case_analysis and case_analysis.get("cases"):
        lines = ["已选择多份优秀教学案例，请融合其结构与素材，但不要照搬原文。"]
        for index, item in enumerate(case_analysis.get("cases") or [], 1):
            lines.extend([
                "案例%s：%s" % (index, item.get("title")),
                "来源：%s %s" % (item.get("sourceName") or "", item.get("sourceUrl") or ""),
                "主题/情境：%s" % (item.get("coreSituation") or item.get("summary") or ""),
                "关键问题：%s" % "；".join(item.get("keyProblems") or []),
                "素材链接JSON：%s" % (item.get("materialJson") or "[]"),
            ])
        lines.append(_image_instruction_block(case_analysis))
        lines.extend([
            "融合要求：每处使用案例内容的段落开头标注【案例参考】。",
            "素材要求：在教案中增加“推荐素材”小节，列出可用于课堂的图片、课件、视频或互动资源链接，并注明来源。",
        ])
        return "\n".join(lines)
    if not case_analysis:
        return "未选择教学案例。"
    if not case_analysis.get("pdfParseOk"):
        return (
            "已选择教学案例《%s》，但 PDF 正文解析失败。生成时只能参考案例标题、分类和课程信息，"
            "不得假造案例细节。"
            % case_analysis.get("title")
        )
    return "\n".join(
        [
            "案例标题：%s" % case_analysis.get("title"),
            "案例主题/情境：%s" % case_analysis.get("coreSituation"),
            "关键问题：%s" % "；".join(case_analysis.get("keyProblems") or []),
            "可迁移活动：%s" % "；".join(case_analysis.get("transferableActivities") or []),
            "适用环节：%s" % "、".join(case_analysis.get("suitableSections") or []),
            "要求：凡是融入该案例的段落或条目，必须在段首写明【案例参考】。",
        ]
    )


def _quiz_total(form):
    counts = form.get("typeCounts") or {}
    total = 0
    for value in counts.values():
        try:
            total += int(value)
        except Exception:
            pass
    return total


def _has_reflection_request(form):
    return bool(re.search(r"教学反思|课后反思|反思|复盘", str(form.get("extraRequirements") or "")))


def _selected_methods(form):
    return _as_list(form.get("selectedMethods") or form.get("methods"))


def _selected_activities(form):
    return _as_list(form.get("selectedActivities") or form.get("activities"))


def _excluded_sections(form):
    excluded = set(_as_list(form.get("excludedSections")))
    if "板书设计" not in _selected_activities(form):
        excluded.add("板书设计")
    if not _has_reflection_request(form):
        excluded.update(["教学反思", "教学效果评价", "改进方向", "课后反思"])
    return excluded


def _method_requirements(methods):
    rules = {
        "讲授演示法": "教学过程必须体现教师讲解、示范或操作演示。",
        "案例教学法": "教学过程必须在导入、任务或讨论中体现案例情境。",
        "项目驱动法": "教学过程必须输出明确项目任务、学生产出物和评价点。",
        "任务驱动法": "教学过程必须输出明确任务、学生产出物和评价点。",
        "探究式学习": "教学过程必须输出问题链和学生探究活动。",
        "合作学习": "教学过程必须输出小组分工、讨论或汇报环节。",
    }
    return [rules[method] for method in methods if method in rules]


def _plan_sections(form):
    sections = ["教学主题", "课时信息", "学情分析", "教学目标", "教学重难点", "教学准备", "教学过程"]
    if "板书设计" in set(_selected_activities(form)):
        sections.append("板书设计")
    sections.append("作业/课后任务")
    if _has_reflection_request(form):
        sections.append("教学反思")
    return sections


def _json_from_text(text):
    text = (text or "").strip()
    text = re.sub(r"^```(?:json)?\s*", "", text, flags=re.I)
    text = re.sub(r"```$", "", text).strip()
    first = text.find("{")
    last = text.rfind("}")
    if first >= 0 and last > first:
        text = text[first:last + 1]
    return json.loads(text)


def _quality(agent_type, text, form, citations, case_analysis=None):
    checks = []
    evidence_ids = {item.get("evidenceId") for item in citations or [] if item.get("evidenceId")}
    referenced_ids = _referenced_evidence_ids(text)

    if agent_type == "plan":
        required = ["教学主题", "课时信息", "学情分析", "教学目标", "教学重难点", "教学准备", "教学过程"]
        for item in required:
            checks.append({"name": item, "passed": item in text})
        checks.append({"name": "作业/课后任务", "passed": "作业" in text or "课后任务" in text})
        has_board = "板书设计" in text
        checks.append({"name": "板书设计按需生成", "passed": has_board if "板书设计" in _selected_activities(form) else not has_board})
        reflection_words = ["教学反思", "教学效果评价", "改进方向", "课后反思"]
        has_reflection = any(word in text for word in reflection_words)
        checks.append({"name": "课前教案不含默认反思", "passed": has_reflection if _has_reflection_request(form) else not has_reflection})
        if case_analysis:
            parse_ok = bool(case_analysis.get("pdfParseOk"))
            checks.append({"name": "案例解析", "passed": parse_ok, "actual": "ok" if parse_ok else "failed"})
            checks.append({"name": "案例融合度", "passed": (not parse_ok) or ("【案例参考】" in text), "actual": text.count("【案例参考】")})
        allowed_images = _allowed_case_image_urls(case_analysis)
        used_images = _markdown_image_urls(text)
        if allowed_images:
            checks.append({"name": "case image url whitelist", "passed": used_images.issubset(allowed_images), "actual": ",".join(sorted(used_images - allowed_images))})
            checks.append({"name": "case image inserted", "passed": len(used_images & allowed_images) > 0, "actual": len(used_images & allowed_images)})
        else:
            checks.append({"name": "no invented case image", "passed": len(used_images) == 0, "actual": ",".join(sorted(used_images))})
    elif agent_type in {"quiz", "quiz_optimize"}:
        total = _quiz_total(form)
        question_part = text.split("\n---\n")[0]
        count = len(re.findall(r"(?m)^\s*\d+\s*[.、．)）]", question_part))
        checks.append({"name": "题目区/答案区分离", "passed": "\n---" in text})
        checks.append({"name": "题目数量", "passed": total == 0 or count == total, "expected": total, "actual": count})
        checks.append({"name": "答案解析", "passed": "答案" in text or "解析" in text})
    elif agent_type in {"anim", "anim_repair", "anim_optimize"}:
        try:
            obj = _json_from_text(text)
            steps = obj.get("steps") if isinstance(obj, dict) else []
            template_type = obj.get("templateType") if isinstance(obj, dict) else None
            checks.extend([
                {"name": "合法 JSON", "passed": True},
                {"name": "模板类型", "passed": template_type in {"sort", "protocol", "stack", "queue", "tree", "graph", "concept"}, "actual": template_type},
                {"name": "步骤数量", "passed": isinstance(steps, list) and len(steps) >= 3, "actual": len(steps) if isinstance(steps, list) else 0},
                {"name": "动画提示", "passed": isinstance(steps, list) and any(step.get("stageCaption") or step.get("motion") for step in steps if isinstance(step, dict))},
            ])
            if template_type == "concept" and isinstance(steps, list):
                visual_count = sum(1 for step in steps if isinstance(step, dict) and step.get("visual"))
                checks.append({"name": "概念视觉原语", "passed": visual_count >= min(4, len(steps)), "actual": visual_count})
        except Exception as exc:
            checks.append({"name": "合法 JSON", "passed": False, "actual": str(exc)[:120]})
    elif agent_type == "coding":
        try:
            obj = _json_from_text(text)
            test_cases = obj.get("testCases") if isinstance(obj, dict) else []
            templates = obj.get("templates") if isinstance(obj, dict) else []
            required_langs = set(_as_list(form.get("languages")))
            template_langs = {str(item.get("language")) for item in templates if isinstance(item, dict)}
            checks.extend([
                {"name": "合法 JSON", "passed": True},
                {"name": "题目标题", "passed": bool(obj.get("title"))},
                {"name": "题目描述", "passed": bool(obj.get("description"))},
                {"name": "测试用例", "passed": isinstance(test_cases, list) and len(test_cases) >= 4, "actual": len(test_cases) if isinstance(test_cases, list) else 0},
                {"name": "隐藏用例", "passed": isinstance(test_cases, list) and any(not tc.get("isSample") for tc in test_cases if isinstance(tc, dict))},
                {"name": "语言模板", "passed": required_langs.issubset(template_langs), "expected": ",".join(sorted(required_langs)), "actual": ",".join(sorted(template_langs))},
            ])
        except Exception as exc:
            checks.append({"name": "合法 JSON", "passed": False, "actual": str(exc)[:120]})

    elif agent_type == "micro_video":
        try:
            obj = _json_from_text(text)
            scenes = obj.get("scenes") if isinstance(obj, dict) else []
            checks.extend([
                {"name": "合法 JSON", "passed": True},
                {"name": "微课标题", "passed": bool(obj.get("title")) if isinstance(obj, dict) else False},
                {"name": "分镜数量", "passed": isinstance(scenes, list) and len(scenes) >= 3, "actual": len(scenes) if isinstance(scenes, list) else 0},
                {"name": "旁白完整", "passed": isinstance(scenes, list) and all((s.get("narration") or "").strip() for s in scenes if isinstance(s, dict))},
                {"name": "画面说明", "passed": isinstance(scenes, list) and all((s.get("visual") or "").strip() for s in scenes if isinstance(s, dict))},
            ])
        except Exception as exc:
            checks.append({"name": "合法 JSON", "passed": False, "actual": str(exc)[:120]})

    if agent_type not in NO_RETRIEVAL_AGENT_TYPES:
        has_evidence = len(evidence_ids) > 0
        checks.append({"name": "检索证据", "passed": agent_type == "plan" or has_evidence, "actual": len(evidence_ids)})
        checks.append({"name": "证据引用", "passed": (not has_evidence) or len(referenced_ids) > 0, "actual": ",".join(sorted(referenced_ids))})
        checks.append({"name": "证据编号有效", "passed": referenced_ids.issubset(evidence_ids), "actual": ",".join(sorted(referenced_ids - evidence_ids))})
        if agent_type == "anim" and has_evidence:
            try:
                obj = _json_from_text(text)
                steps = obj.get("steps") if isinstance(obj, dict) else []
                step_refs = sum(1 for step in steps if isinstance(step, dict) and step.get("evidenceIds"))
                checks.append({"name": "课件步骤可追溯", "passed": step_refs > 0, "actual": step_refs})
            except Exception:
                checks.append({"name": "课件步骤可追溯", "passed": False, "actual": "invalid_json"})
    passed = sum(1 for check in checks if check.get("passed"))
    score = int(round(passed * 100 / max(len(checks), 1)))
    return {"score": score, "checks": checks}


def _anim_schema_block():
    return """
只允许输出一个合法 JSON 对象，不要 Markdown，不要代码围栏。templateType 只能是 sort、protocol、stack、queue、tree、graph、concept。

sort:
{"templateType":"sort","title":"课件标题","subtitle":"一句副标题","targetGroup":"适用对象","teachingGoal":"教学目标","initialData":[5,1,4,2,8],"steps":[{"title":"步骤","desc":"一句说明","stageCaption":"看图提示","motion":{"type":"compare","indexes":[0,1]},"array":[5,1,4,2,8],"highlight":[0,1],"swap":[],"sortedTailStart":null}]}

protocol:
{"templateType":"protocol","title":"课件标题","subtitle":"一句副标题","targetGroup":"适用对象","teachingGoal":"教学目标","actors":["客户端","服务器"],"steps":[{"title":"步骤","desc":"一句说明","stageCaption":"报文飞向服务器","motion":{"type":"send","from":"客户端","to":"服务器","value":"SYN"},"from":"客户端","to":"服务器","message":"报文内容","clientState":"客户端状态","serverState":"服务器状态","messageType":"request"}]}

stack:
{"templateType":"stack","title":"课件标题","subtitle":"一句副标题","targetGroup":"适用对象","teachingGoal":"教学目标","initialStack":[],"steps":[{"title":"步骤","desc":"一句说明","stageCaption":"元素从栈顶压入","motion":{"type":"push","value":"A"},"stack":["A"],"operation":"push","activeValue":"A","poppedValue":null}]}

queue:
{"templateType":"queue","title":"课件标题","subtitle":"一句副标题","targetGroup":"适用对象","teachingGoal":"教学目标","initialQueue":[],"steps":[{"title":"步骤","desc":"一句说明","stageCaption":"元素从队尾进入","motion":{"type":"enqueue","value":"A"},"queue":["A"],"operation":"enqueue","activeValue":"A","removedValue":null}]}

tree:
{"templateType":"tree","title":"课件标题","subtitle":"一句副标题","targetGroup":"适用对象","teachingGoal":"教学目标","root":{"value":7,"left":{"value":3},"right":{"value":10}},"steps":[{"title":"步骤","desc":"一句说明","stageCaption":"访问当前节点","motion":{"type":"visit","path":[7,3]},"currentNode":3,"path":[7,3],"visited":[7,3],"operation":"visit"}]}

graph:
{"templateType":"graph","title":"课件标题","subtitle":"一句副标题","targetGroup":"适用对象","teachingGoal":"教学目标","nodes":["A","B","C"],"edges":[{"from":"A","to":"B","directed":false}],"steps":[{"title":"步骤","desc":"一句说明","stageCaption":"访问 A 并扩展邻接点","motion":{"type":"visit","value":"A"},"activeNode":"A","visited":["A"],"frontier":["B"],"activeEdges":[{"from":"A","to":"B"}],"operation":"visit"}]}

concept:
{"templateType":"concept","title":"课件标题","subtitle":"一句副标题","targetGroup":"适用对象","teachingGoal":"教学目标","mainTerm":"核心概念","coreIdea":"一句核心思想","steps":[{"title":"步骤","desc":"一句说明","stageCaption":"图上短提示","motion":{"type":"observe"},"focus":"关键词","keyPoints":["要点1"],"visual":{"type":"highlight-card","mainValue":"核心值","label":"标签","tone":"info"}}]}

queue operation 只能是 init/enqueue/dequeue/peek/done；tree operation 只能是 init/visit/compare/go-left/go-right/backtrack/done；graph operation 只能是 init/visit/enqueue/dequeue/push/pop/relax/done。concept 的 visual 可用 nodes-chain、tree、branching、comparison、highlight-card、flow。concept 至少 5 步，至少 4 步带 visual。所有模板每一步尽量带 stageCaption 和 motion。desc 不要写长段文字。
"""


def _coding_schema_block():
    return """
只允许输出一个合法 JSON 对象，不要 Markdown，不要代码围栏。结构必须为:
{
  "title": "题目标题，不超过30字",
  "description": "Markdown 题面，包含题目背景、输入格式、输出格式、样例说明、数据范围",
  "difficulty": "easy|medium|hard",
  "languages": ["java"],
  "timeLimitMs": 5000,
  "memoryLimitKb": 262144,
  "testCases": [
    {"input": "输入", "expectedOutput": "输出", "isSample": 1, "score": 20}
  ],
  "templates": [
    {"language": "java", "starterCode": "学生初始代码", "referenceSolution": "参考解"}
  ]
}
要求：至少 2 个样例用例，至少 2 个隐藏用例；测试用例总分建议 100；每个所选语言都必须有 starterCode 和 referenceSolution；参考解必须能通过全部测试用例。
"""


def _micro_video_schema_block():
    return """
只允许输出一个合法 JSON 对象，不要 Markdown，不要代码围栏。结构必须为:
{
  "title": "微课标题",
  "summary": "80字内课程摘要",
  "durationMinutes": 5,
  "knowledgePoints": ["知识点1"],
  "renderStyle": "cinematic_teaching",
  "scenes": [
    {
      "index": 1,
      "title": "分镜标题",
      "durationSeconds": 35,
      "narration": "教师口播旁白，语言自然，适合配音",
      "visual": "画面构成说明，如标题、流程图、代码片段、重点标注",
      "subtitle": "屏幕字幕，控制在两行内",
      "materials": ["建议素材或图示"],
      "layoutType": "protocol|flow|comparison|code|timeline|concept|stack|queue|tree|graph|memory_table",
      "visualPlan": {
        "main": "核心画面对象",
        "points": ["屏幕要点1", "屏幕要点2"]
      },
      "animPayload": {"templateType": "protocol", "steps": []},
      "onScreenText": ["屏幕短句1", "屏幕短句2"],
      "motion": ["pan", "highlight", "subtitle"],
      "keyframePrompt": "16:9 cinematic educational keyframe prompt for optional AI image generation",
      "subtitleSegments": ["按配音节奏切开的短字幕1", "短字幕2"],
      "voiceStyle": "warm_female|clear_male|calm_teacher|bright_teacher"
    }
  ]
}
硬性要求：scenes 3到8幕；每幕 narration 60到180字；layoutType 必须选择一个最适合教学表达的模板；visualPlan 必须是可被程序绘制的结构化对象，不要只写形容词；计算机协议、栈、队列、树、图等主题可额外输出 animPayload，结构尽量复用互动课件 templateType/steps；onScreenText 每条不超过18字；subtitleSegments 每条不超过28字；keyframePrompt 只用于可选关键帧插图，不要生成数字人、真人出镜或不可控整段文生视频提示词。
"""


def _system_prompt(agent_type, form=None, has_case=False):
    form = form or {}
    if agent_type == "plan":
        sections = "、".join(_plan_sections(form))
        excluded = "、".join(sorted(_excluded_sections(form))) or "无"
        method_rules = "；".join(_method_requirements(_selected_methods(form))) or "按教师选择的方法组织课堂活动。"
        prompt = (
            "你是智慧教育平台中的教案设计智能体。必须生成课前可直接编辑和执行的 Markdown 教案，"
            "不要输出寒暄、说明或推理过程。"
            "只允许输出这些章节：%s。不得输出这些章节或同义内容：%s。"
            "教学方法必须真实改变教学过程：%s"
            "教学过程要写清时间安排、教师活动、学生活动和可执行任务，避免空泛套话。"
            % (sections, excluded, method_rules)
        )
        if has_case:
            prompt += (
                "如果提供了教学案例分析，必须把案例情境、问题或数据自然融入课堂导入、任务探究、讨论或练习。"
                "每一处使用案例内容的段落或条目，都必须以【案例参考】开头。不得整段复制案例原文。"
            )
        return prompt
    if agent_type == "quiz_optimize":
        return (
            "你是智慧教育平台中的习题优化智能体。必须保持原题型分布和总题数不变，"
            "优化题目质量、难度梯度和答案解析。只输出优化后的完整 Markdown 习题正文。"
        )
    if agent_type == "quiz":
        return (
            "你是智慧教育平台中的命题智能体。必须按教师配置生成 Markdown 习题。"
            "题目区和答案解析区必须用单独一行 --- 分隔，题型数量必须严格一致。不要输出寒暄。"
        )
    if agent_type == "anim_repair":
        return "你是互动课件 JSON 修复智能体。只输出修复后的合法 JSON 对象。\n" + _anim_schema_block()
    if agent_type == "anim_optimize":
        return "你是互动课件 JSON 优化智能体。保持 templateType 与主要结构稳定，只输出优化后的合法 JSON 对象。\n" + _anim_schema_block()
    if agent_type == "anim":
        return (
            "你是互动课件生成智能体。目标是让学生看见抽象概念的变化过程，只输出合法 JSON 对象。"
            "必须严格围绕教师配置中的核心概念生成，不能复用 schema 示例里的标题、节点或主题。"
            "如果用户输入是 TCP、排序、栈、队列、树、图等明确主题，必须选择对应模板，不要一律输出 concept 或链表样例。\n"
            + _anim_schema_block()
        )
    if agent_type == "micro_video":
        return "你是智慧教育平台的微课导演与教学脚本智能体。你的任务是把教师输入转成可渲染的图文讲解型微课脚本。只输出合法 JSON 对象，不要寒暄，不要 Markdown。\n" + _micro_video_schema_block()
    if agent_type == "coding":
        return "你是编程教育命题智能体。生成可直接入库的在线编程题，只输出合法 JSON 对象。\n" + _coding_schema_block()
    return "你是智慧教育平台中的备课智能体。"


def _user_prompt(
    agent_type,
    payload,
    citations,
    case_analysis=None,
    structured_tool_results=None,
):
    form = payload.get("form") or {}
    base = [
        "【教师配置】",
        _form_text(form),
    ]
    if agent_type not in NO_RETRIEVAL_AGENT_TYPES:
        base.extend([
            "",
            "【检索到的平台证据包】",
            _citation_block(citations),
            _evidence_rules(agent_type, citations),
        ])
        if structured_tool_results:
            base.extend([
                "",
                "【受控工具返回的结构化上下文】",
                json.dumps(structured_tool_results, ensure_ascii=False),
                "这些内容已经按当前登录教师的数据范围过滤；不得推测或扩展到其他教师、班级或学生。",
            ])
    if agent_type == "plan":
        base.extend([
            "",
            "【教案硬性要求】",
            "章节白名单：%s" % "、".join(_plan_sections(form)),
            "禁止章节：%s" % ("、".join(sorted(_excluded_sections(form))) or "无"),
            "教学方法落地要求：%s" % ("；".join(_method_requirements(_selected_methods(form))) or "按教师选择的方法组织课堂活动。"),
            "如果未检索到强相关平台上下文，不要编造引用，也不要强行套用无关课程资源。",
            "",
            "【教学案例分析】",
            _case_analysis_block(case_analysis),
            _image_instruction_block(case_analysis),
        ])
    elif agent_type == "quiz":
        base.extend([
            "",
            "【硬性要求】",
            "严格按照 typeCounts 中每种题型的数量命题；不要出现数量为 0 的题型；题目区与答案解析区分离。",
        ])
    elif agent_type == "quiz_optimize":
        base.extend(["", "【当前习题全文】", payload.get("sourceContent") or ""])
    elif agent_type == "anim":
        base.extend([
            "",
            "【互动课件要求】",
            "根据核心概念选择最合适模板：排序/数组比较交换用 sort；两方通信时序用 protocol；栈/LIFO 用 stack；队列/FIFO 用 queue；二叉树/BST/树遍历用 tree；图/BFS/DFS/最短路径/拓扑关系用 graph；其余抽象概念用 concept。",
            "输出 5 到 7 个步骤优先，动作必须能连续演示。不要用大段文字代替动画。",
            "输出的 title、mainTerm、steps、visual 都必须与教师配置中的核心概念一致；不得把其他示例主题（例如链表、线性表、TCP）当作默认答案。",
        ])
        if payload.get("sourceContent"):
            base.extend([
                "",
                "【完整 JSON 生成规范】",
                payload.get("sourceContent") or "",
            ])
    elif agent_type == "anim_repair":
        base.extend(["", "【需要修复的 JSON 或错误信息】", payload.get("sourceContent") or ""])
    elif agent_type == "anim_optimize":
        base.extend([
            "",
            "【优化目标】",
            str(form.get("optimizeInstruction") or form.get("optimizeAction") or "优化课件可视化表达"),
            "",
            "【当前课件 JSON】",
            payload.get("sourceContent") or "",
        ])
    elif agent_type == "coding":
        base.extend([
            "",
            "【编程题硬性要求】",
            "题面必须自洽，输入输出格式可执行；所有测试用例必须与题意、参考解一致；每个选择语言都要生成模板和参考解。",
            "如果教师需求较抽象，请自动补足一个适合课堂练习的具体任务场景。",
        ])
    elif agent_type == "micro_video":
        base.extend([
            "",
            "【微课生成硬性要求】",
            "生成可渲染导演稿，目标是后续用教学画面、镜头运动、字幕和配音合成 MP4。",
            "必须输出严格 JSON，不要包裹 ```json。",
            "每个 scene 都要包含 index/title/durationSeconds/narration/visual/subtitle/materials/layoutType/visualPlan/onScreenText/motion/keyframePrompt/subtitleSegments/voiceStyle。",
            "layoutType 只能从 protocol、flow、comparison、code、timeline、concept 中选择；计算机网络握手、请求响应优先用 protocol。",
            "visualPlan 要写成程序能画的结构：protocol 写 actors/messages，flow 写 steps，comparison 写左右栏，code 写 code/highlights，timeline 写 events，concept 写 main/points。",
            "如果主题能复用互动课件模板，可额外输出 animPayload，templateType 使用 protocol/stack/queue/tree/graph/concept，steps 保持短小。",
            "旁白要口语化、适合教师讲解；字幕要短；屏幕文字宁少勿多，避免把讲稿整段搬上画面。",
            "如果检索证据可用，把证据转化为教学内容，但不要在 JSON 中塞长段原文。",
        ])
    return "\n".join(base)


def _retrieval_options(payload):
    options = dict(payload.get("retrievalOptions") or {})
    if options.get("graphPolicy") == "off":
        options["mode"] = "off"
    if "topK" not in options:
        options["topK"] = 4 if payload.get("agentType") == "plan" else 6
    if payload.get("graphNodeId"):
        options["graphNodeId"] = payload.get("graphNodeId")
    if payload.get("courseId"):
        options["courseId"] = payload.get("courseId")
    if payload.get("courseName"):
        options["courseName"] = payload.get("courseName")
    return options


def _retrieve_context(payload, query, documents):
    options = _retrieval_options(payload)
    if options.get("mode") == "off":
        return [], "off"

    try:
        qdrant_items = QdrantPrepareIndex().retrieve(
            query,
            teacher_id=payload.get("teacherId"),
            options=options,
        )
        if qdrant_items is not None:
            return qdrant_items, "qdrant"
    except Exception:
        pass

    try:
        fallback = ContextRetriever(
            max_items=int(options.get("topK") or 6),
            allow_model_load=False,
            graph_policy=options.get("graphPolicy") or "auto",
        ).retrieve(
            query,
            documents,
            teacher_id=payload.get("teacherId"),
            options=options,
        )
    except Exception:
        fallback = []
    for item in fallback:
        item.setdefault("snippet", (item.get("content") or "")[:260])
        item.setdefault("reason", "fallback_keyword")
        item.setdefault("chunkId", "%s-%s" % (item.get("sourceType") or "context", item.get("sourceId") or ""))
    return fallback, "fallback_keyword"


def _collect_structured_tool_results(
    payload,
    query,
    tool_context,
    workflow_run=None,
):
    context_data = tool_context.context_data
    calls = []
    if context_data.get("resources"):
        calls.append((
            "retrieve_course_material",
            {"query": query, "topK": 5},
        ))
    if context_data.get("cases"):
        calls.append((
            "retrieve_teaching_case",
            {"query": query, "topK": 3},
        ))
    if context_data.get("graphNodes"):
        calls.append((
            "query_course_graph",
            {"query": query, "maxNodes": 8, "includeRelations": True},
        ))
    if any(
        isinstance(item, dict) and item.get("scope") == "class"
        for item in context_data.get("studentWeaknesses") or []
    ):
        calls.append((
            "query_student_weakness",
            {"scope": "class", "topK": 5},
        ))

    results = []
    for name, arguments in calls:
        tool_started = time.perf_counter()
        try:
            if workflow_run is not None:
                workflow_run.record_tool_call()
            output = DEFAULT_TOOL_REGISTRY.invoke(name, arguments, tool_context)
            tool_duration_ms = int((time.perf_counter() - tool_started) * 1000)
            if workflow_run is not None:
                workflow_run.record_tool(
                    name,
                    tool_duration_ms,
                    "success",
                )
            results.append({
                "tool": name,
                "status": "success",
                "output": output.to_dict(),
            })
        except ToolExecutionError as exc:
            tool_duration_ms = int((time.perf_counter() - tool_started) * 1000)
            if workflow_run is not None:
                workflow_run.record_tool(
                    name,
                    tool_duration_ms,
                    "failed",
                    exc.code.value,
                )
            results.append({
                "tool": name,
                "status": "failed",
                "error": exc.to_dict(),
            })
    return results


def run_prepare_agent(payload, api_key):
    agent_type = (
        payload.get("agentType") or "plan"
        if isinstance(payload, dict)
        else "unknown"
    )
    workflow_run = WORKFLOW_RUN_STORE.create(
        agent_type,
        trace_id=payload.get("traceId") if isinstance(payload, dict) else None,
        session_id=payload.get("sessionId") if isinstance(payload, dict) else None,
    )

    try:
        yield _workflow_event(workflow_run)
        yield from _execute_prepare_agent(
            payload,
            api_key,
            workflow_run,
        )
    except GeneratorExit:
        if not workflow_run.is_terminal and workflow_run.state != WorkflowState.WAITING_CONFIRMATION:
            try:
                workflow_run.transition(
                    WorkflowState.CANCELLED,
                    "client_disconnected",
                    FailureCategory.CANCELLED,
                )
            except WorkflowStateError:
                pass
        raise
    except Exception as exc:
        if not workflow_run.is_terminal:
            try:
                transition = workflow_run.transition(
                    WorkflowState.FAILED,
                    "unhandled_business_failure",
                    FailureCategory.BUSINESS_FAILURE,
                )
                yield _workflow_event(workflow_run, transition)
            except WorkflowStateError:
                pass
        yield _event(
            "error",
            message="Agent workflow failed during business processing.",
            failureCategory=FailureCategory.BUSINESS_FAILURE.value,
            requestId=workflow_run.request_id,
        )


def _execute_prepare_agent(payload, api_key, workflow_run):
    agent_type = workflow_run.agent_type
    supported = {"plan", "quiz", "quiz_optimize", "anim", "anim_repair", "anim_optimize", "coding", "micro_video"}
    if agent_type not in supported:
        transition = workflow_run.transition(
            WorkflowState.FAILED,
            "unsupported_agent_type",
            FailureCategory.INVALID_REQUEST,
        )
        yield _workflow_event(workflow_run, transition)
        yield _event(
            "error",
            message="Unsupported agentType: %s" % agent_type,
            failureCategory=FailureCategory.INVALID_REQUEST.value,
            requestId=workflow_run.request_id,
        )
        return

    try:
        tool_context = ToolContext.from_agent_payload(payload)
    except ToolExecutionError as exc:
        transition = workflow_run.transition(
            WorkflowState.FAILED,
            "invalid_tool_context",
            FailureCategory.INVALID_REQUEST,
        )
        yield _workflow_event(workflow_run, transition)
        yield _event(
            "error",
            message=str(exc),
            error=exc.to_dict(),
            failureCategory=FailureCategory.INVALID_REQUEST.value,
            requestId=workflow_run.request_id,
        )
        return
    workflow_run.bind_actor(tool_context.actor_id)

    transition = workflow_run.transition(
        WorkflowState.PLANNING,
        "request_validated",
    )
    yield _workflow_event(workflow_run, transition)

    yield _event("stage", name="理解需求", status="running")
    query = _query_for(payload)
    yield _event("stage", name="理解需求", status="done", detail=query[:160])

    case_analysis = None
    if agent_type == "plan" and _case_items(payload):
        yield _event("stage", name="案例分析", status="running")
        case_analysis = _case_analysis(payload)
        yield _event("case_analysis", item=case_analysis)
        if case_analysis and case_analysis.get("pdfParseOk"):
            detail = "已提炼案例：%s" % case_analysis.get("title")
        else:
            detail = "案例正文解析失败，生成时仅参考元信息"
        yield _event("stage", name="案例分析", status="done", detail=detail)

    citations = []
    structured_tool_results = []
    if agent_type not in NO_RETRIEVAL_AGENT_TYPES:
        transition = workflow_run.transition(
            WorkflowState.RETRIEVING,
            "retrieval_started",
        )
        yield _workflow_event(workflow_run, transition)
        yield _event("stage", name="检索知识图谱与资源库", status="running")
        retrieval_started = time.perf_counter()
        documents = build_documents(payload)
        citations, retrieval_engine = _retrieve_context(payload, query, documents)
        citations = _build_evidence_pack(agent_type, citations)
        citation_keys = (
            "sourceType", "sourceId", "title", "score", "snippet", "resourceType",
            "reason", "chunkId", "pdfParseOk", "parseStatus", "sourceKey",
            "evidenceId", "useFor", "documentId", "chunkIndex", "chunkCount",
            "courseId", "courseName", "sourceUrl", "sourceName",
            "metadataSchemaVersion", "contentHash", "chunkHash",
        )
        yield _event("citation", items=[{k: d.get(k) for k in citation_keys} for d in citations])
        retrieval_ms = int((time.perf_counter() - retrieval_started) * 1000)
        workflow_run.record_retrieval(
            retrieval_engine,
            len(citations),
            retrieval_ms,
            degraded=retrieval_engine == "fallback_keyword",
            error_type=(
                FailureCategory.RETRIEVAL_FAILURE.value
                if retrieval_engine == "fallback_keyword"
                else None
            ),
        )
        citation_detail = (
            "命中 %s 条强相关上下文（%s, %sms）" % (len(citations), retrieval_engine, retrieval_ms)
            if citations
            else "未检索到强相关资源（%s, %sms）" % (retrieval_engine, retrieval_ms)
        )
        yield _event("stage", name="检索知识图谱与资源库", status="done", detail=citation_detail)

        if retrieval_engine == "fallback_keyword":
            transition = workflow_run.transition(
                WorkflowState.DEGRADED,
                "vector_retrieval_unavailable_using_keyword_fallback",
                FailureCategory.RETRIEVAL_FAILURE,
            )
            yield _workflow_event(workflow_run, transition)
        transition = workflow_run.transition(
            WorkflowState.EXECUTING_TOOLS,
            "structured_tool_execution_started",
        )
        yield _workflow_event(workflow_run, transition)
        try:
            structured_tool_results = _collect_structured_tool_results(
                payload,
                query,
                tool_context,
                workflow_run=workflow_run,
            )
        except WorkflowLimitError as exc:
            transition = workflow_run.transition(
                WorkflowState.FAILED,
                "tool_call_limit_reached",
                FailureCategory.TOOL_FAILURE,
            )
            yield _workflow_event(workflow_run, transition)
            yield _event(
                "error",
                message=str(exc),
                failureCategory=FailureCategory.TOOL_FAILURE.value,
                requestId=workflow_run.request_id,
            )
            return
        if any(item.get("status") == "failed" for item in structured_tool_results):
            transition = workflow_run.transition(
                WorkflowState.DEGRADED,
                "one_or_more_tools_failed",
                FailureCategory.TOOL_FAILURE,
            )
            yield _workflow_event(workflow_run, transition)

    yield _event("stage", name="规划生成策略", status="running")
    yield _event("stage", name="规划生成策略", status="done")

    transition = workflow_run.transition(
        WorkflowState.GENERATING,
        "model_generation_started",
    )
    yield _workflow_event(workflow_run, transition)
    yield _event("stage", name="生成内容", status="running")
    full_text = []
    system_prompt = _system_prompt(
        agent_type,
        payload.get("form") or {},
        has_case=bool(case_analysis),
    )
    user_prompt = _user_prompt(
        agent_type,
        payload,
        citations,
        case_analysis=case_analysis,
        structured_tool_results=structured_tool_results,
    )
    generation_started = time.perf_counter()
    first_token_ms = None
    prompt_tokens = estimate_tokens(system_prompt) + estimate_tokens(user_prompt)
    while True:
        try:
            for chunk in stream_deepseek(
                api_key,
                system_prompt,
                user_prompt,
                max_tokens=7000 if agent_type == "plan" else 4200 if agent_type == "micro_video" else 6000,
                temperature=0.45 if agent_type in {"plan", "anim", "micro_video"} else 0.3,
            ):
                if first_token_ms is None:
                    first_token_ms = int(
                        (time.perf_counter() - generation_started) * 1000
                    )
                full_text.append(chunk)
                yield _event("content", delta=chunk)
            break
        except Exception as exc:
            can_retry = (
                not full_text
                and workflow_run.retry_count < workflow_run.limits.max_retries
            )
            if can_retry:
                workflow_run.record_retry()
                transition = workflow_run.transition(
                    WorkflowState.RETRYING,
                    "model_failed_before_first_chunk",
                    FailureCategory.MODEL_FAILURE,
                )
                yield _workflow_event(workflow_run, transition)
                transition = workflow_run.transition(
                    WorkflowState.GENERATING,
                    "model_retry_started",
                )
                yield _workflow_event(workflow_run, transition)
                continue

            transition = workflow_run.transition(
                WorkflowState.FAILED,
                (
                    "model_failed_after_partial_output"
                    if full_text
                    else "model_retry_limit_reached"
                ),
                FailureCategory.MODEL_FAILURE,
            )
            workflow_run.record_model(
                first_token_ms=first_token_ms,
                total_duration_ms=int(
                    (time.perf_counter() - generation_started) * 1000
                ),
                input_tokens=prompt_tokens * (workflow_run.retry_count + 1),
                output_tokens=estimate_tokens("".join(full_text)),
            )
            yield _workflow_event(workflow_run, transition)
            yield _event(
                "error",
                message="AI model service failed; please retry.",
                failureCategory=FailureCategory.MODEL_FAILURE.value,
                requestId=workflow_run.request_id,
            )
            return
    workflow_run.record_model(
        first_token_ms=first_token_ms,
        total_duration_ms=int(
            (time.perf_counter() - generation_started) * 1000
        ),
        input_tokens=prompt_tokens * (workflow_run.retry_count + 1),
        output_tokens=estimate_tokens("".join(full_text)),
    )
    if agent_type == "plan":
        image_delta = _missing_case_image_markdown("".join(full_text), case_analysis)
        if image_delta:
            full_text.append(image_delta)
            yield _event("content", delta=image_delta)
    yield _event("stage", name="生成内容", status="done")

    transition = workflow_run.transition(
        WorkflowState.CHECKING,
        "quality_check_started",
    )
    yield _workflow_event(workflow_run, transition)
    yield _event("stage", name="质量检查", status="running")
    quality = _quality(agent_type, "".join(full_text), payload.get("form") or {}, citations, case_analysis=case_analysis)
    yield _event("quality", report=quality)
    yield _event("stage", name="质量检查", status="done", detail="质量分 %s" % quality.get("score"))
    transition = workflow_run.transition(
        WorkflowState.WAITING_CONFIRMATION,
        "generated_content_requires_user_confirmation_before_save",
    )
    yield _workflow_event(workflow_run, transition)
    yield _event(
        "done",
        requestId=workflow_run.request_id,
        requiresConfirmation=True,
        workflow=workflow_run.snapshot(),
    )


def _workflow_event(workflow_run, transition=None):
    data = workflow_run.state_event(transition)
    data.pop("type", None)
    return _event("workflow_state", **data)
