import json
import re


LAYOUT_TYPES = {
    "cover",
    "protocol",
    "flow",
    "comparison",
    "code",
    "timeline",
    "concept",
    "stack",
    "queue",
    "tree",
    "graph",
    "memory_table",
}


def json_from_text(text):
    if isinstance(text, dict):
        return text
    text = (text or "").strip()
    text = re.sub(r"^```(?:json)?\s*", "", text, flags=re.I)
    text = re.sub(r"```$", "", text).strip()
    first = text.find("{")
    last = text.rfind("}")
    if first >= 0 and last > first:
        text = text[first:last + 1]
    return json.loads(text)


def split_subtitles(text, max_len=24):
    raw = re.split(r"([。！？!?；;，,])", str(text or ""))
    pieces = []
    current = ""
    for item in raw:
        if not item:
            continue
        current += item
        if item in "。！？!?；;，," or len(current) >= max_len:
            value = current.strip()
            if value:
                pieces.append(value)
            current = ""
    if current.strip():
        pieces.append(current.strip())
    if not pieces and text:
        pieces = [str(text).strip()]
    return pieces[:8]


def normalize_script(script_json, options=None):
    options = options or {}
    script = json_from_text(script_json)
    warnings = []
    title = script.get("title") or options.get("title") or "AI micro course"
    scenes = script.get("scenes") if isinstance(script.get("scenes"), list) else []
    if not scenes:
        raise ValueError("script scenes is empty")

    target_minutes = int(options.get("durationMinutes") or script.get("durationMinutes") or 5)
    target_minutes = max(3, min(8, target_minutes))
    default_duration = max(16, int(target_minutes * 60 / max(len(scenes), 1)))

    normalized = []
    for idx, scene in enumerate(scenes, 1):
        src = dict(scene or {})
        narration = _clean(src.get("narration") or src.get("subtitle") or src.get("title") or "")
        subtitle = _clean(src.get("subtitle") or narration)
        layout_type = _normalize_layout(src.get("layoutType"), src, title)
        if layout_type not in LAYOUT_TYPES:
            warnings.append("Unsupported layoutType '%s' normalized to concept at scene %s" % (layout_type, idx))
            layout_type = "concept"

        duration = int(float(src.get("durationSeconds") or default_duration))
        duration = max(10, min(60, duration))
        on_screen_text = _normalize_text_list(src.get("onScreenText"))
        if not on_screen_text:
            on_screen_text = split_subtitles(subtitle or narration, 18)[:4]
        on_screen_text = [_short_text(item, 28) for item in on_screen_text[:5]]

        subtitle_segments = _normalize_text_list(src.get("subtitleSegments"))
        if not subtitle_segments:
            subtitle_segments = split_subtitles(subtitle or narration)
        subtitle_segments = [_short_text(item, 34) for item in subtitle_segments[:8]]

        item = {
            "index": int(src.get("index") or idx),
            "title": _clean(src.get("title") or "\u77e5\u8bc6\u70b9 %02d" % idx),
            "durationSeconds": duration,
            "narration": narration,
            "visual": _clean(src.get("visual") or ""),
            "subtitle": subtitle,
            "materials": _normalize_text_list(src.get("materials")),
            "layoutType": layout_type,
            "visualPlan": _normalize_visual_plan(src.get("visualPlan"), layout_type, src, on_screen_text),
            "onScreenText": on_screen_text,
            "motion": src.get("motion") if isinstance(src.get("motion"), list) else ["pan", "highlight", "subtitle"],
            "keyframePrompt": src.get("keyframePrompt") or _keyframe_prompt(title, src, layout_type),
            "subtitleSegments": subtitle_segments,
            "voiceStyle": src.get("voiceStyle") or options.get("voiceId") or "default_female",
        }
        if isinstance(src.get("animPayload"), dict):
            item["animPayload"] = src.get("animPayload")
        normalized.append(item)

    script["title"] = title
    script["summary"] = script.get("summary") or ""
    script["durationMinutes"] = target_minutes
    script["scenes"] = normalized
    script["renderVersion"] = "micro-video-v3"
    return script, warnings


def _normalize_layout(value, scene, script_title):
    current = str(value or "").strip()
    text = " ".join([
        str(script_title or ""),
        str(scene.get("title") or ""),
        str(scene.get("visual") or ""),
        str(scene.get("narration") or ""),
        " ".join(_normalize_text_list(scene.get("materials"))),
    ]).lower()
    if any(key in text for key in ["为什么", "为什", "两次握手", "二次握手", "历史连接", "误判", "避免", "vs", "对比", "比较"]):
        return "comparison"
    if any(key in text for key in ["第一次", "第二次", "第三次", "syn", "ack", "tcp", "握手", "request", "response"]):
        return "protocol"
    if current in {"flow", "comparison", "code", "timeline", "stack", "queue", "tree", "graph", "memory_table"}:
        return current
    if any(key in text for key in ["lifo", "stack", "栈", "压栈", "出栈", "push", "pop"]):
        return "stack"
    if any(key in text for key in ["fifo", "queue", "队列", "入队", "出队", "enqueue", "dequeue"]):
        return "queue"
    if any(key in text for key in ["binary tree", "bst", "tree", "二叉树", "树遍历", "前序", "中序", "后序"]):
        return "tree"
    if any(key in text for key in ["graph", "bfs", "dfs", "shortest path", "topology", "拓扑", "图遍历", "最短路径"]):
        return "graph"
    if any(key in text for key in ["memory", "table", "array", "pointer", "heap", "stack frame", "内存", "数组", "指针", "变量表"]):
        return "memory_table"
    if any(key in text for key in ["状态变化", "状态变", "总结", "流程", "closed", "listen", "established"]):
        return "flow"
    if current:
        return current
    if any(key in text for key in ["流程", "步骤", "阶段", "过程", "flow", "状态"]):
        return "flow"
    if any(key in text for key in ["对比", "区别", "vs", "比较", "为什么"]):
        return "comparison"
    if any(key in text for key in ["代码", "伪代码", "class ", "public ", "def "]):
        return "code"
    if any(key in text for key in ["时间", "顺序", "timeline"]):
        return "timeline"
    return "concept"


def _normalize_visual_plan(plan, layout_type, scene, on_screen_text):
    plan = plan if isinstance(plan, dict) else {}
    if layout_type == "protocol":
        return {
            "actors": _normalize_actors(plan),
            "messages": _normalize_messages(plan, scene),
            "focus": _clean(plan.get("focus") or scene.get("title") or ""),
        }
    if layout_type == "flow":
        steps = _normalize_text_list(plan.get("steps") or on_screen_text or [scene.get("title")])
        return {"steps": steps[:5], "focusIndex": int(plan.get("focusIndex") or 0)}
    if layout_type == "comparison":
        left = plan.get("left") if isinstance(plan.get("left"), dict) else {}
        right = plan.get("right") if isinstance(plan.get("right"), dict) else {}
        left_items = _normalize_text_list(plan.get("leftItems") or left.get("items") or left.get("points") or left.get("description"))
        right_items = _normalize_text_list(plan.get("rightItems") or right.get("items") or right.get("points") or right.get("description"))
        if not left_items or not right_items:
            points = on_screen_text or split_subtitles(scene.get("subtitle") or scene.get("narration"), 20)
            left_items = left_items or points[:1] or ["旧连接可能被误判"]
            right_items = right_items or points[1:] or ["第三次确认避免误判"]
        return {
            "leftTitle": _clean(plan.get("leftTitle") or left.get("title") or "问题"),
            "leftItems": left_items[:4],
            "rightTitle": _clean(plan.get("rightTitle") or right.get("title") or "方案"),
            "rightItems": right_items[:4],
        }
    if layout_type == "code":
        return {
            "code": _clean(plan.get("code") or _extract_code(scene.get("visual")) or "\n".join(on_screen_text)),
            "highlights": _normalize_text_list(plan.get("highlights") or on_screen_text)[:4],
        }
    if layout_type == "timeline":
        return {"events": _normalize_text_list(plan.get("events") or on_screen_text or [scene.get("title")])[:5]}
    if layout_type == "stack":
        return {
            "items": _normalize_text_list(plan.get("items") or plan.get("stack") or on_screen_text or [scene.get("title")])[:6],
            "active": _clean(plan.get("active") or plan.get("activeValue") or ""),
            "operation": _clean(plan.get("operation") or "push/pop"),
        }
    if layout_type == "queue":
        return {
            "items": _normalize_text_list(plan.get("items") or plan.get("queue") or on_screen_text or [scene.get("title")])[:6],
            "active": _clean(plan.get("active") or plan.get("activeValue") or ""),
            "operation": _clean(plan.get("operation") or "enqueue/dequeue"),
        }
    if layout_type == "tree":
        return {
            "nodes": _normalize_text_list(plan.get("nodes") or plan.get("path") or on_screen_text or [scene.get("title")])[:7],
            "active": _clean(plan.get("active") or plan.get("currentNode") or ""),
            "path": _normalize_text_list(plan.get("path") or plan.get("visited") or [])[:7],
        }
    if layout_type == "graph":
        return {
            "nodes": _normalize_text_list(plan.get("nodes") or on_screen_text or ["A", "B", "C"])[:7],
            "edges": _normalize_edges(plan.get("edges")),
            "active": _clean(plan.get("active") or plan.get("activeNode") or ""),
            "frontier": _normalize_text_list(plan.get("frontier") or [])[:5],
        }
    if layout_type == "memory_table":
        return {
            "columns": _normalize_text_list(plan.get("columns") or ["name", "value"])[:4],
            "rows": _normalize_rows(plan.get("rows") or plan.get("variables") or on_screen_text),
            "active": _clean(plan.get("active") or plan.get("focus") or ""),
        }
    return {
        "main": _clean(plan.get("main") or scene.get("title")),
        "points": _normalize_text_list(plan.get("points") or on_screen_text)[:5],
        "hint": _clean(plan.get("hint") or scene.get("visual") or "")[:100],
    }


def _normalize_actors(plan):
    actors = plan.get("actors") if isinstance(plan.get("actors"), list) else []
    result = []
    for item in actors[:2]:
        if isinstance(item, dict):
            result.append({"name": _clean(item.get("name")), "state": _clean(item.get("state"))})
        else:
            result.append({"name": _clean(item), "state": ""})
    while len(result) < 2:
        result.append({"name": "客户端" if len(result) == 0 else "服务器", "state": "LISTEN" if len(result) == 1 else ""})
    return result


def _normalize_messages(plan, scene):
    messages = plan.get("messages") if isinstance(plan.get("messages"), list) else []
    result = []
    for item in messages[:5]:
        if isinstance(item, dict):
            result.append({
                "from": _clean(item.get("from") or "客户端"),
                "to": _clean(item.get("to") or "服务器"),
                "label": _clean(item.get("label") or item.get("content") or item.get("message") or item.get("title") or "Message"),
                "note": _clean(item.get("note") or item.get("description") or item.get("desc") or ""),
            })
        else:
            result.append({"from": "客户端", "to": "服务器", "label": _clean(item), "note": ""})
    if result:
        return result

    text = " ".join([str(scene.get("title") or ""), str(scene.get("visual") or ""), str(scene.get("narration") or "")]).lower()
    if "syn" in text or "ack" in text or "tcp" in text or "握手" in text:
        return [
            {"from": "客户端", "to": "服务器", "label": "SYN", "note": "请求建立连接"},
            {"from": "服务器", "to": "客户端", "label": "SYN-ACK", "note": "确认并响应"},
            {"from": "客户端", "to": "服务器", "label": "ACK", "note": "连接建立"},
        ]
    return [{"from": "客户端", "to": "服务器", "label": "Request", "note": "发送请求"}]


def _normalize_text_list(value):
    if value is None:
        return []
    if not isinstance(value, list):
        value = [value]
    result = []
    for item in value:
        if isinstance(item, dict):
            result.append(_clean(item.get("description") or item.get("content") or item.get("title") or item.get("name") or item.get("step") or item))
        else:
            for part in re.split(r"[,，、\n]+", str(item)):
                part = part.strip()
                if part:
                    result.append(part)
    return [item for item in result if item]


def _extract_code(text):
    match = re.search(r"```(?:\w+)?\s*([\s\S]+?)```", str(text or ""))
    if match:
        return match.group(1).strip()
    lines = [line.strip() for line in str(text or "").splitlines()]
    code_like = [line for line in lines if any(token in line for token in ["=", "()", "{", "}", "if ", "for ", "while "])]
    return "\n".join(code_like[:8])


def _normalize_edges(value):
    if not isinstance(value, list):
        return []
    result = []
    for item in value[:8]:
        if isinstance(item, dict):
            result.append({
                "from": _clean(item.get("from") or item.get("source") or ""),
                "to": _clean(item.get("to") or item.get("target") or ""),
            })
        elif isinstance(item, (list, tuple)) and len(item) >= 2:
            result.append({"from": _clean(item[0]), "to": _clean(item[1])})
        else:
            parts = re.split(r"[-→>]+", str(item))
            if len(parts) >= 2:
                result.append({"from": _clean(parts[0]), "to": _clean(parts[1])})
    return [edge for edge in result if edge.get("from") and edge.get("to")]


def _normalize_rows(value):
    if not isinstance(value, list):
        value = _normalize_text_list(value)
    rows = []
    for idx, item in enumerate(value[:6]):
        if isinstance(item, dict):
            rows.append({
                "name": _clean(item.get("name") or item.get("key") or item.get("variable") or ("var%s" % (idx + 1))),
                "value": _clean(item.get("value") or item.get("content") or item.get("description") or ""),
                "note": _clean(item.get("note") or item.get("type") or ""),
            })
        else:
            text = _clean(item)
            parts = re.split(r"[:=：]", text, maxsplit=1)
            rows.append({
                "name": _clean(parts[0]) if parts else ("var%s" % (idx + 1)),
                "value": _clean(parts[1]) if len(parts) > 1 else text,
                "note": "",
            })
    return rows or [{"name": "value", "value": _clean(value), "note": ""}]


def _keyframe_prompt(title, scene, layout_type):
    return (
        "cinematic educational keyframe, 16:9, clean teaching visual, "
        "topic: %s, scene: %s, layout: %s, visual: %s"
        % (title, scene.get("title") or "", layout_type, scene.get("visual") or "")
    )


def _clean(value):
    if value is None:
        return ""
    if isinstance(value, dict):
        value = value.get("description") or value.get("content") or value.get("title") or value.get("name") or value.get("step") or ""
    return str(value).replace("\r", " ").replace("\n", " ").strip()


def _short_text(value, max_len):
    value = _clean(value)
    return value if len(value) <= max_len else value[:max_len - 1] + "…"
