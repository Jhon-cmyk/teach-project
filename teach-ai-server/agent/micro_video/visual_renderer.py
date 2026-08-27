import math
import os
import textwrap
from functools import lru_cache

import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFont


PALETTE = {
    "ink": "#122033",
    "muted": "#64748b",
    "paper": "#f8fbff",
    "line": "#d8e2ee",
    "blue": "#2563eb",
    "cyan": "#06b6d4",
    "green": "#16a34a",
    "amber": "#f59e0b",
    "red": "#ef4444",
    "dark": "#0f172a",
    "violet": "#7c3aed",
}


@lru_cache(maxsize=64)
def font(size, bold=False):
    candidates = [
        "C:/Windows/Fonts/msyhbd.ttc" if bold else "C:/Windows/Fonts/msyh.ttc",
        "C:/Windows/Fonts/simhei.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for path in candidates:
        if path and os.path.exists(path):
            return ImageFont.truetype(path, size=size)
    return ImageFont.load_default()


def render_frame(script, scene, scene_progress, global_progress, options=None, keyframe=None, size=(1280, 720)):
    options = options or {}
    base = _keyframe_backdrop(keyframe, size) if keyframe is not None else _background(size, int(scene.get("index") or 1))
    draw = ImageDraw.Draw(base, "RGBA")
    _draw_header(draw, script, scene, global_progress, size)

    layout = scene.get("layoutType") or "concept"
    if layout == "protocol":
        _draw_protocol(draw, scene, scene_progress, size)
    elif layout == "flow":
        _draw_flow(draw, scene, scene_progress, size)
    elif layout == "comparison":
        _draw_comparison(draw, scene, scene_progress, size)
    elif layout == "code":
        _draw_code(draw, scene, scene_progress, size)
    elif layout == "timeline":
        _draw_timeline(draw, scene, scene_progress, size)
    elif layout == "stack":
        _draw_stack(draw, scene, scene_progress, size)
    elif layout == "queue":
        _draw_queue(draw, scene, scene_progress, size)
    elif layout == "tree":
        _draw_tree(draw, scene, scene_progress, size)
    elif layout == "graph":
        _draw_graph(draw, scene, scene_progress, size)
    elif layout == "memory_table":
        _draw_memory_table(draw, scene, scene_progress, size)
    else:
        _draw_concept(draw, scene, scene_progress, size)

    if options.get("burnSubtitles", True):
        _draw_subtitle(draw, scene, scene_progress, size, options)
    _draw_progress(draw, global_progress, size)
    return base


def cover_image(script, first_scene, size=(1280, 720)):
    image = _background(size, 1)
    draw = ImageDraw.Draw(image, "RGBA")
    draw.rounded_rectangle((72, 108, 1208, 610), radius=24, fill=(255, 255, 255, 236), outline="#dce7f3", width=2)
    draw.text((122, 170), _fit(str(script.get("title") or "AI Micro Course"), 24), fill=PALETTE["ink"], font=font(58, True))
    summary = script.get("summary") or first_scene.get("subtitle") or first_scene.get("narration") or ""
    y = 280
    for line in _wrap(summary, 36)[:3]:
        draw.text((126, y), line, fill=PALETTE["muted"], font=font(28))
        y += 42
    draw.rounded_rectangle((126, 500, 448, 552), radius=26, fill=PALETTE["dark"])
    draw.text((156, 512), "AI 微课生成", fill="#ffffff", font=font(24, True))
    return image


def _background(size, index):
    w, h = size
    arr = np.zeros((h, w, 3), dtype=np.uint8)
    colors = [
        ((239, 248, 255), (229, 234, 246)),
        ((244, 250, 247), (229, 241, 233)),
        ((251, 247, 240), (238, 231, 220)),
        ((244, 245, 251), (232, 229, 243)),
    ]
    c1, c2 = colors[(index - 1) % len(colors)]
    for y in range(h):
        ratio = y / max(1, h - 1)
        arr[y, :, :] = [int(c1[i] * (1 - ratio) + c2[i] * ratio) for i in range(3)]
    image = Image.fromarray(arr, "RGB")
    draw = ImageDraw.Draw(image, "RGBA")
    draw.ellipse((-180, -220, 420, 360), fill=(37, 99, 235, 34))
    draw.ellipse((930, 430, 1450, 950), fill=(22, 163, 74, 30))
    for offset in range(-200, w + 200, 110):
        draw.line((offset, 0, offset - 280, h), fill=(255, 255, 255, 52), width=2)
    return image


def _keyframe_backdrop(keyframe, size):
    image = keyframe.resize(size).convert("RGB")
    image = ImageEnhance.Contrast(image).enhance(0.88)
    overlay = Image.new("RGBA", size, (8, 14, 28, 112))
    return Image.alpha_composite(image.convert("RGBA"), overlay).convert("RGB")


def _draw_header(draw, script, scene, progress, size):
    w, _ = size
    draw.rounded_rectangle((40, 26, w - 40, 84), radius=18, fill=(255, 255, 255, 222))
    draw.text((68, 42), _fit(str(script.get("title") or ""), 30), fill=PALETTE["ink"], font=font(24, True))
    draw.rounded_rectangle((w - 192, 38, w - 66, 72), radius=17, fill="#0f172a")
    draw.text((w - 164, 44), "Scene %02d" % int(scene.get("index") or 1), fill="#ffffff", font=font(18, True))


def _draw_progress(draw, progress, size):
    w, h = size
    draw.rounded_rectangle((72, h - 32, w - 72, h - 24), radius=4, fill=(203, 213, 225, 150))
    draw.rounded_rectangle((72, h - 32, 72 + int((w - 144) * _clamp(progress)), h - 24), radius=4, fill=PALETTE["blue"])


def _draw_protocol(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    messages = _normalize_messages(plan, scene)
    active_index = min(max(len(messages) - 1, 0), int(progress * max(1, len(messages))))
    phase = progress * max(1, len(messages)) - active_index

    draw.rounded_rectangle((70, 112, 1210, 594), radius=24, fill=(255, 255, 255, 232), outline="#dce7f3", width=2)
    draw.text((110, 142), _fit(str(scene.get("title") or ""), 26), fill=PALETTE["ink"], font=font(38, True))

    left = (250, 356)
    right = (1030, 356)
    _draw_endpoint(draw, left, "客户端", _actor_state(plan, 0, "SYN_SENT"), PALETTE["blue"])
    _draw_endpoint(draw, right, "服务器", _actor_state(plan, 1, "LISTEN"), PALETTE["green"])
    draw.line((left[0] + 118, left[1], right[0] - 118, right[1]), fill=(148, 163, 184, 140), width=6)

    for idx, msg in enumerate(messages):
        y = 250 + idx * 72
        alpha = 255 if idx <= active_index else 80
        start_left = _is_client(msg.get("from"))
        start_x = left[0] + 126 if start_left else right[0] - 126
        end_x = right[0] - 126 if start_left else left[0] + 126
        color = PALETTE["blue"] if start_left else PALETTE["green"]
        row_y = y if len(messages) > 1 else 330
        if idx < active_index:
            _draw_arrow(draw, start_x, row_y, end_x, row_y, color, alpha)
            _draw_message_card(draw, (start_x + end_x) / 2, row_y, msg, color, alpha)
        elif idx == active_index:
            moving_x = start_x + (end_x - start_x) * _ease(_clamp(phase))
            _draw_arrow(draw, start_x, row_y, moving_x, row_y, color, alpha)
            _draw_message_card(draw, moving_x, row_y, msg, color, alpha)

    summary = str(scene.get("subtitle") or scene.get("title") or "")
    draw.rounded_rectangle((118, 520, 590, 560), radius=18, fill=(37, 99, 235, 34), outline="#bfdbfe", width=2)
    draw.text((148, 529), _fit(summary, 24), fill=PALETTE["ink"], font=font(21, True))


def _draw_flow(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    steps = _normalize_steps(plan.get("steps") or scene.get("onScreenText") or [scene.get("title")])
    count = max(1, min(5, len(steps)))
    active = min(count - 1, int(progress * count))

    draw.rounded_rectangle((70, 112, 1210, 594), radius=24, fill=(255, 255, 255, 232), outline="#dce7f3", width=2)
    draw.text((110, 142), _fit(str(scene.get("title") or ""), 26), fill=PALETTE["ink"], font=font(38, True))
    x0, y0 = 145, 336
    gap = 28
    box_w = int((990 - gap * (count - 1)) / count)
    for idx, step in enumerate(steps[:count]):
        x = x0 + idx * (box_w + gap)
        is_active = idx == active
        is_done = idx < active
        color = PALETTE["blue"] if is_active else PALETTE["green"] if is_done else "#cbd5e1"
        fill = color if is_active else "#f8fafc"
        text_color = "#ffffff" if is_active else PALETTE["ink"]
        draw.rounded_rectangle((x, y0 - 76, x + box_w, y0 + 76), radius=18, fill=fill, outline=color, width=3)
        draw.text((x + 20, y0 - 56), "%02d" % (idx + 1), fill=text_color, font=font(22, True))
        yy = y0 - 18
        for line in _wrap(step, max(5, box_w // 28))[:3]:
            draw.text((x + 20, yy), line, fill=text_color, font=font(22, True))
            yy += 30
        if idx < count - 1:
            _draw_arrow(draw, x + box_w + 4, y0, x + box_w + gap - 6, y0, "#94a3b8", 230)


def _draw_comparison(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    left_title, left_items, right_title, right_items = _normalize_comparison(plan, scene)
    draw.text((100, 130), _fit(str(scene.get("title") or ""), 30), fill=PALETTE["ink"], font=font(40, True))
    _comparison_card(draw, 106, 216, 500, 330, left_title, left_items, PALETTE["red"], min(1, progress * 1.8))
    _comparison_card(draw, 674, 216, 500, 330, right_title, right_items, PALETTE["green"], min(1, max(0, progress * 1.8 - 0.3)))
    draw.rounded_rectangle((592, 338, 688, 392), radius=27, fill=PALETTE["dark"])
    draw.text((617, 350), "VS", fill="#ffffff", font=font(24, True))


def _draw_concept(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    title = _clean_text(plan.get("main") or scene.get("title"))
    points = _normalize_steps(plan.get("points") or scene.get("onScreenText") or [])
    draw.rounded_rectangle((74, 118, 1206, 590), radius=26, fill=(255, 255, 255, 232), outline="#dce7f3", width=2)
    draw.text((120, 160), _fit(title, 28), fill=PALETTE["ink"], font=font(46, True))
    draw.rounded_rectangle((120, 226, 120 + int(430 * max(0.25, progress)), 236), radius=5, fill=PALETTE["blue"])
    y = 286
    for idx, item in enumerate(points[:4]):
        alpha = 120 + int(135 * min(1, max(0, progress * 4 - idx)))
        draw.rounded_rectangle((124, y - 10, 1130, y + 56), radius=16, fill=(241, 245, 249, alpha))
        draw.ellipse((150, y + 10, 178, y + 38), fill=PALETTE["blue"])
        draw.text((202, y), _fit(item, 36), fill=PALETTE["ink"], font=font(30, True))
        y += 82


def _draw_code(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    code = str(plan.get("code") or "\n".join(_normalize_steps(scene.get("onScreenText") or [])))
    draw.rounded_rectangle((78, 116, 1202, 592), radius=24, fill=(15, 23, 42, 242))
    draw.text((120, 152), _fit(str(scene.get("title") or ""), 30), fill="#e2e8f0", font=font(36, True))
    y = 220
    for idx, line in enumerate(code.splitlines()[:9]):
        active = idx <= int(progress * 9)
        draw.rounded_rectangle((120, y - 5, 1144, y + 36), radius=8, fill=(37, 99, 235, 78) if active else (255, 255, 255, 12))
        draw.text((146, y), _fit(line, 68), fill="#f8fafc" if active else "#94a3b8", font=font(24))
        y += 43


def _draw_timeline(draw, scene, progress, size):
    events = _normalize_steps((scene.get("visualPlan") or {}).get("events") or scene.get("onScreenText") or [])
    draw.rounded_rectangle((78, 118, 1202, 590), radius=24, fill=(255, 255, 255, 232), outline="#dce7f3", width=2)
    draw.text((120, 154), _fit(str(scene.get("title") or ""), 30), fill=PALETTE["ink"], font=font(38, True))
    count = max(1, min(5, len(events)))
    y = 355
    draw.line((160, y, 1120, y), fill="#94a3b8", width=5)
    for idx, event in enumerate(events[:count]):
        x = 180 + idx * int(900 / max(1, count - 1)) if count > 1 else 640
        active = idx <= int(progress * count)
        color = PALETTE["blue"] if active else "#cbd5e1"
        draw.ellipse((x - 28, y - 28, x + 28, y + 28), fill=color)
        for line_idx, line in enumerate(_wrap(event, 8)[:2]):
            draw.text((x - 70, y + 48 + line_idx * 28), line, fill=PALETTE["ink"], font=font(21, True))


def _draw_stack(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    items = _normalize_steps(plan.get("items") or plan.get("stack") or scene.get("onScreenText") or [])
    if not items:
        items = ["A", "B", "C"]
    visible = max(1, min(len(items), int(math.ceil(progress * len(items)))))
    draw.rounded_rectangle((82, 114, 1198, 590), radius=24, fill=(255, 255, 255, 234), outline="#dce7f3", width=2)
    draw.text((120, 150), _fit(str(scene.get("title") or "Stack"), 28), fill=PALETTE["ink"], font=font(40, True))
    draw.text((908, 154), _fit(str(plan.get("operation") or "LIFO"), 16), fill=PALETTE["blue"], font=font(26, True))
    base_x, base_y, box_w, box_h = 452, 500, 376, 54
    for idx, item in enumerate(items[:visible]):
        y = base_y - idx * (box_h + 12)
        color = PALETTE["blue"] if idx == visible - 1 else "#e0f2fe"
        text_color = "#ffffff" if idx == visible - 1 else PALETTE["ink"]
        draw.rounded_rectangle((base_x, y, base_x + box_w, y + box_h), radius=12, fill=color, outline=PALETTE["blue"], width=2)
        draw.text((base_x + 28, y + 13), _fit(str(item), 26), fill=text_color, font=font(24, True))
    draw.line((base_x - 24, base_y + box_h + 8, base_x + box_w + 24, base_y + box_h + 8), fill=PALETTE["dark"], width=5)
    draw.text((base_x + box_w + 62, base_y - (visible - 1) * (box_h + 12) + 12), "top", fill=PALETTE["muted"], font=font(24, True))


def _draw_queue(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    items = _normalize_steps(plan.get("items") or plan.get("queue") or scene.get("onScreenText") or [])
    if not items:
        items = ["A", "B", "C"]
    visible = max(1, min(len(items), int(math.ceil(progress * len(items)))))
    draw.rounded_rectangle((78, 116, 1202, 590), radius=24, fill=(255, 255, 255, 234), outline="#dce7f3", width=2)
    draw.text((118, 154), _fit(str(scene.get("title") or "Queue"), 28), fill=PALETTE["ink"], font=font(40, True))
    draw.text((920, 156), _fit(str(plan.get("operation") or "FIFO"), 16), fill=PALETTE["green"], font=font(26, True))
    x0, y, box_w, box_h, gap = 170, 340, 132, 86, 18
    for idx, item in enumerate(items[:visible]):
        x = x0 + idx * (box_w + gap)
        color = PALETTE["green"] if idx == 0 else PALETTE["blue"] if idx == visible - 1 else "#eef2ff"
        fill = color if idx in {0, visible - 1} else "#f8fafc"
        text_color = "#ffffff" if idx in {0, visible - 1} else PALETTE["ink"]
        draw.rounded_rectangle((x, y, x + box_w, y + box_h), radius=16, fill=fill, outline=color, width=3)
        draw.text((x + 20, y + 28), _fit(str(item), 8), fill=text_color, font=font(24, True))
    draw.text((x0, y + 118), "front", fill=PALETTE["green"], font=font(22, True))
    draw.text((x0 + (visible - 1) * (box_w + gap), y - 42), "rear", fill=PALETTE["blue"], font=font(22, True))


def _draw_tree(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    nodes = _normalize_steps(plan.get("nodes") or scene.get("onScreenText") or [])
    if not nodes:
        nodes = ["root", "left", "right", "leaf"]
    active_count = max(1, min(len(nodes), int(math.ceil(progress * len(nodes)))))
    draw.rounded_rectangle((78, 116, 1202, 590), radius=24, fill=(255, 255, 255, 234), outline="#dce7f3", width=2)
    draw.text((116, 150), _fit(str(scene.get("title") or "Tree"), 30), fill=PALETTE["ink"], font=font(38, True))
    positions = [(640, 230), (430, 350), (850, 350), (320, 470), (540, 470), (760, 470), (980, 470)]
    for idx, pos in enumerate(positions[:min(len(nodes), len(positions))]):
        if idx > 0:
            parent = (idx - 1) // 2
            draw.line((positions[parent][0], positions[parent][1] + 30, pos[0], pos[1] - 30), fill="#94a3b8", width=4)
    for idx, (x, y) in enumerate(positions[:min(len(nodes), len(positions))]):
        active = idx < active_count
        color = PALETTE["blue"] if active else "#cbd5e1"
        draw.ellipse((x - 42, y - 42, x + 42, y + 42), fill=color, outline=PALETTE["dark"] if active else "#94a3b8", width=2)
        label = _fit(str(nodes[idx]), 7)
        tw = draw.textlength(label, font=font(21, True))
        draw.text((x - tw / 2, y - 13), label, fill="#ffffff" if active else PALETTE["ink"], font=font(21, True))


def _draw_graph(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    nodes = _normalize_steps(plan.get("nodes") or scene.get("onScreenText") or [])
    if not nodes:
        nodes = ["A", "B", "C", "D"]
    nodes = nodes[:7]
    active_count = max(1, min(len(nodes), int(math.ceil(progress * len(nodes)))))
    draw.rounded_rectangle((76, 114, 1204, 592), radius=24, fill=(255, 255, 255, 234), outline="#dce7f3", width=2)
    draw.text((116, 150), _fit(str(scene.get("title") or "Graph"), 30), fill=PALETTE["ink"], font=font(38, True))
    center = (640, 374)
    radius = 210
    positions = {}
    for idx, node in enumerate(nodes):
        angle = -math.pi / 2 + idx * 2 * math.pi / max(1, len(nodes))
        positions[node] = (center[0] + int(math.cos(angle) * radius), center[1] + int(math.sin(angle) * radius))
    edges = plan.get("edges") if isinstance(plan.get("edges"), list) else []
    if not edges:
        edges = [{"from": nodes[idx], "to": nodes[(idx + 1) % len(nodes)]} for idx in range(len(nodes))]
    for edge in edges[:10]:
        src = edge.get("from") if isinstance(edge, dict) else None
        dst = edge.get("to") if isinstance(edge, dict) else None
        if src in positions and dst in positions:
            draw.line((*positions[src], *positions[dst]), fill=(148, 163, 184, 150), width=4)
    for idx, node in enumerate(nodes):
        x, y = positions[node]
        active = idx < active_count
        color = PALETTE["violet"] if active else "#e2e8f0"
        draw.ellipse((x - 42, y - 42, x + 42, y + 42), fill=color, outline=PALETTE["violet"], width=3)
        label = _fit(str(node), 6)
        tw = draw.textlength(label, font=font(22, True))
        draw.text((x - tw / 2, y - 14), label, fill="#ffffff" if active else PALETTE["ink"], font=font(22, True))


def _draw_memory_table(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    rows = plan.get("rows") if isinstance(plan.get("rows"), list) else []
    if not rows:
        rows = [{"name": item, "value": ""} for item in _normalize_steps(scene.get("onScreenText") or [])[:5]]
    if not rows:
        rows = [{"name": "value", "value": "42"}, {"name": "index", "value": "0"}]
    reveal = max(1, min(len(rows), int(math.ceil(progress * len(rows)))))
    draw.rounded_rectangle((80, 114, 1200, 592), radius=24, fill=(255, 255, 255, 236), outline="#dce7f3", width=2)
    draw.text((118, 150), _fit(str(scene.get("title") or "Memory Table"), 30), fill=PALETTE["ink"], font=font(38, True))
    left, top, width = 180, 230, 920
    col_w = [250, 330, 260]
    headers = ["name", "value", "note"]
    x = left
    for idx, header in enumerate(headers):
        draw.rounded_rectangle((x, top, x + col_w[idx], top + 52), radius=8, fill=PALETTE["dark"])
        draw.text((x + 22, top + 13), header, fill="#ffffff", font=font(22, True))
        x += col_w[idx]
    for row_idx, row in enumerate(rows[:reveal]):
        y = top + 58 + row_idx * 56
        fill = "#eff6ff" if row_idx == reveal - 1 else "#f8fafc"
        x = left
        values = [
            row.get("name") if isinstance(row, dict) else str(row),
            row.get("value") if isinstance(row, dict) else "",
            row.get("note") if isinstance(row, dict) else "",
        ]
        for col_idx, value in enumerate(values):
            draw.rectangle((x, y, x + col_w[col_idx], y + 52), fill=fill, outline="#d8e2ee")
            draw.text((x + 18, y + 14), _fit(str(value or ""), 18), fill=PALETTE["ink"], font=font(21, True))
            x += col_w[col_idx]


def _draw_subtitle(draw, scene, progress, size, options=None):
    w, h = size
    options = options or {}
    timeline = scene.get("subtitleTimeline") if isinstance(scene.get("subtitleTimeline"), list) else []
    text = ""
    if timeline and options.get("absoluteSeconds") is not None:
        absolute = float(options.get("absoluteSeconds") or 0)
        for item in timeline:
            start = float(item.get("start") or 0)
            end = float(item.get("end") or 0)
            if start <= absolute < end:
                text = _clean_text(item.get("text") or "")
                break
        if not text:
            return
    else:
        segments = scene.get("subtitleSegments") or [scene.get("subtitle") or ""]
        if not segments:
            return
        idx = min(len(segments) - 1, int(progress * len(segments)))
        text = _clean_text(segments[idx])
    if not text:
        return
    lines = _wrap(text, 34)[:2]
    box_h = 22 + len(lines) * 34
    left, right = 238, w - 238
    top = h - 104 - box_h
    draw.rounded_rectangle((left, top, right, h - 102), radius=18, fill=(255, 255, 255, 218), outline="#bfdbfe", width=2)
    y = top + 12
    for line in lines:
        tw = draw.textlength(line, font=font(25, True))
        draw.text(((w - tw) / 2, y), line, fill=PALETTE["ink"], font=font(25, True))
        y += 34


def _draw_endpoint(draw, center, label, state, color):
    x, y = center
    draw.ellipse((x - 110, y - 110, x + 110, y + 110), fill=(255, 255, 255, 255), outline=color, width=6)
    draw.rounded_rectangle((x - 66, y - 46, x + 66, y + 38), radius=14, fill=color)
    draw.rectangle((x - 48, y - 20, x + 48, y + 20), fill="#ffffff")
    draw.rectangle((x - 28, y + 38, x + 28, y + 56), fill=color)
    label_width = draw.textlength(label, font=font(28, True))
    draw.text((x - label_width / 2, y + 80), label, fill=PALETTE["ink"], font=font(28, True))
    if state:
        state_text = _fit(state, 16)
        state_width = draw.textlength(state_text, font=font(18, True))
        draw.rounded_rectangle((x - state_width / 2 - 16, y + 118, x + state_width / 2 + 16, y + 150), radius=16, fill=color)
        draw.text((x - state_width / 2, y + 124), state_text, fill="#ffffff", font=font(18, True))


def _draw_arrow(draw, x1, y1, x2, y2, color, alpha=255):
    draw.line((x1, y1, x2, y2), fill=_rgba(color, alpha), width=7)
    direction = 1 if x2 >= x1 else -1
    draw.polygon([(x2, y2), (x2 - direction * 20, y2 - 12), (x2 - direction * 20, y2 + 12)], fill=_rgba(color, alpha))


def _draw_message_card(draw, x, y, msg, color, alpha=255):
    label = _fit(str(msg.get("label") or msg.get("content") or msg.get("message") or "Message"), 18)
    note = _fit(str(msg.get("note") or msg.get("desc") or ""), 20)
    width = max(190, int(draw.textlength(label, font=font(24, True))) + 58)
    draw.rounded_rectangle((x - width / 2, y - 31, x + width / 2, y + 31), radius=16, fill=(255, 255, 255, alpha), outline=_rgba(color, alpha), width=3)
    draw.text((x - width / 2 + 26, y - 17), label, fill=_rgba(PALETTE["ink"], alpha), font=font(24, True))
    if note:
        draw.text((x - width / 2 + 28, y + 36), note, fill=_rgba(PALETTE["muted"], alpha), font=font(18))


def _comparison_card(draw, x, y, w, h, title, items, color, reveal):
    alpha = 150 + int(85 * _clamp(reveal))
    draw.rounded_rectangle((x, y, x + w, y + h), radius=24, fill=(255, 255, 255, alpha), outline=_rgba(color, 230), width=3)
    draw.rounded_rectangle((x + 28, y + 28, x + 210, y + 72), radius=22, fill=color)
    draw.text((x + 50, y + 38), _fit(title, 10), fill="#ffffff", font=font(22, True))
    yy = y + 112
    for item in items[:4]:
        draw.ellipse((x + 38, yy + 8, x + 62, yy + 32), fill=color)
        for line in _wrap(item, 20)[:2]:
            draw.text((x + 82, yy), line, fill=PALETTE["ink"], font=font(24, True))
            yy += 30
        yy += 20


def _normalize_messages(plan, scene):
    messages = plan.get("messages") if isinstance(plan.get("messages"), list) else []
    result = []
    for item in messages:
        if isinstance(item, dict):
            result.append({
                "from": _clean_text(item.get("from") or ""),
                "to": _clean_text(item.get("to") or ""),
                "label": _clean_text(item.get("label") or item.get("content") or item.get("message") or item.get("title") or ""),
                "note": _clean_text(item.get("note") or item.get("description") or item.get("desc") or ""),
            })
        else:
            result.append({"from": "客户端", "to": "服务器", "label": _clean_text(item), "note": ""})
    if result:
        return result[:5]
    text = " ".join([str(scene.get("title") or ""), str(scene.get("visual") or ""), str(scene.get("narration") or "")]).lower()
    if "syn" in text or "ack" in text or "tcp" in text:
        return [
            {"from": "客户端", "to": "服务器", "label": "SYN", "note": "请求建立连接"},
            {"from": "服务器", "to": "客户端", "label": "SYN-ACK", "note": "确认并响应"},
            {"from": "客户端", "to": "服务器", "label": "ACK", "note": "连接建立"},
        ]
    return [{"from": "客户端", "to": "服务器", "label": "Request", "note": "发送请求"}]


def _actor_state(plan, index, fallback):
    actors = plan.get("actors") if isinstance(plan.get("actors"), list) else []
    if index < len(actors) and isinstance(actors[index], dict):
        return _clean_text(actors[index].get("state") or fallback)
    return fallback


def _normalize_steps(value):
    if value is None:
        return []
    if not isinstance(value, list):
        value = [value]
    result = []
    for item in value:
        if isinstance(item, dict):
            result.append(_clean_text(item.get("description") or item.get("content") or item.get("title") or item.get("step") or item))
        else:
            result.append(_clean_text(item))
    return [item for item in result if item]


def _normalize_comparison(plan, scene):
    left = plan.get("left") if isinstance(plan.get("left"), dict) else {}
    right = plan.get("right") if isinstance(plan.get("right"), dict) else {}
    left_title = _clean_text(plan.get("leftTitle") or left.get("title") or "问题")
    right_title = _clean_text(plan.get("rightTitle") or right.get("title") or "方案")
    left_items = _normalize_steps(plan.get("leftItems") or left.get("items") or left.get("points") or [left.get("description")])
    right_items = _normalize_steps(plan.get("rightItems") or right.get("items") or right.get("points") or [right.get("description")])
    if not left_items or not right_items:
        points = _normalize_steps(scene.get("onScreenText") or [scene.get("subtitle") or scene.get("title")])
        left_items = left_items or points[:1] or ["容易误建连接"]
        right_items = right_items or points[1:] or ["第三次确认避免误判"]
    return left_title, left_items, right_title, right_items


def _is_client(value):
    text = str(value or "").lower()
    return "client" in text or "客" in text or "瀹" in text or not ("server" in text or "服" in text or "鏈" in text)


def _clean_text(value):
    if value is None:
        return ""
    if isinstance(value, dict):
        value = value.get("description") or value.get("content") or value.get("title") or value.get("name") or value.get("step") or ""
    return str(value).replace("\r", " ").replace("\n", " ").strip()


def _wrap(text, width):
    lines = []
    for raw in _clean_text(text).splitlines():
        raw = raw.strip()
        if not raw:
            continue
        lines.extend(textwrap.wrap(raw, width=max(6, width), replace_whitespace=False) or [raw])
    return lines


def _fit(text, max_chars):
    text = _clean_text(text)
    return text if len(text) <= max_chars else text[:max(1, max_chars - 1)] + "…"


def _rgba(hex_color, alpha):
    value = hex_color.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (int(alpha),)


def _clamp(value):
    return max(0.0, min(1.0, float(value)))


def _ease(value):
    value = _clamp(value)
    return value * value * (3 - 2 * value)


# Refined visual template overrides. These definitions intentionally live at the
# end of the module so they replace the earlier lightweight template without
# touching script normalization or motion composition.
PALETTE.update({
    "ink": "#172033",
    "muted": "#667085",
    "paper": "#fbfcf8",
    "line": "#d8e0ea",
    "blue": "#2f6fdf",
    "cyan": "#14b8c8",
    "green": "#19a974",
    "amber": "#f2a33a",
    "red": "#e95757",
    "dark": "#101827",
    "violet": "#7c5cff",
    "surface": "#ffffff",
})


def cover_image(script, first_scene, size=(1280, 720)):
    image = _background(size, 1)
    draw = ImageDraw.Draw(image, "RGBA")
    _draw_panel(draw, (76, 104, 1204, 612), radius=34, fill=(255, 255, 255, 236))
    draw.rounded_rectangle((112, 144, 242, 178), radius=17, fill=PALETTE["dark"])
    draw.text((134, 151), "AI \u5fae\u8bfe", fill="#ffffff", font=font(18, True))
    draw.rounded_rectangle((112, 204, 122, 362), radius=5, fill=PALETTE["blue"])
    draw.rounded_rectangle((124, 204, 134, 322), radius=5, fill=PALETTE["amber"])
    draw.text((162, 196), _fit(str(script.get("title") or "AI Micro Course"), 24), fill=PALETTE["ink"], font=font(60, True))

    summary = script.get("summary") or first_scene.get("subtitle") or first_scene.get("narration") or ""
    y = 314
    for line in _wrap(summary, 36)[:3]:
        draw.text((164, y), line, fill=PALETTE["muted"], font=font(29))
        y += 44

    draw.rounded_rectangle((164, 512, 438, 564), radius=26, fill=PALETTE["blue"])
    draw.text((194, 525), "\u5fae\u8bfe\u89c6\u9891", fill="#ffffff", font=font(22, True))
    draw.line((840, 164, 1126, 164), fill=(47, 111, 223, 80), width=6)
    draw.line((880, 194, 1126, 194), fill=(242, 163, 58, 105), width=6)
    draw.line((920, 224, 1126, 224), fill=(25, 169, 116, 92), width=6)
    return image


def _background(size, index):
    return _background_cached(size, index).copy()


@lru_cache(maxsize=16)
def _background_cached(size, index):
    w, h = size
    themes = [
        ((250, 252, 248), (231, 239, 250), (216, 235, 247)),
        ((249, 252, 249), (224, 243, 235), (236, 232, 247)),
        ((255, 251, 244), (238, 231, 220), (226, 239, 248)),
        ((247, 249, 255), (234, 231, 245), (224, 243, 236)),
    ]
    c1, c2, c3 = [np.array(color, dtype=np.float32) for color in themes[(index - 1) % len(themes)]]
    x = np.linspace(0, 1, w, dtype=np.float32)[None, :, None]
    y = np.linspace(0, 1, h, dtype=np.float32)[:, None, None]
    diagonal = x * 0.42 + y * 0.58
    wave = (np.sin((x * 3.4 + y * 2.1 + index) * math.pi) + 1) * 0.035
    mixed = np.clip(diagonal + wave, 0, 1)
    base = c1 * (1 - mixed) + c2 * mixed
    accent = np.clip((x - 0.45), 0, 1) * 0.42
    arr = base * (1 - accent) + c3 * accent
    image = Image.fromarray(np.uint8(np.clip(arr, 0, 255)), "RGB")
    draw = ImageDraw.Draw(image, "RGBA")

    for offset in range(-260, w + 260, 118):
        draw.line((offset, 0, offset - 310, h), fill=(255, 255, 255, 48), width=2)
    for line_y in range(96, h, 96):
        draw.line((58, line_y, w - 58, line_y), fill=(23, 32, 51, 18), width=1)
    draw.polygon([(w - 430, 0), (w, 0), (w, 236), (w - 318, 164)], fill=(255, 255, 255, 54))
    draw.polygon([(0, h - 176), (276, h), (0, h)], fill=(47, 111, 223, 34))
    return image


def _keyframe_backdrop(keyframe, size):
    image = keyframe.resize(size).convert("RGB")
    image = ImageEnhance.Contrast(image).enhance(0.9)
    image = ImageEnhance.Brightness(image).enhance(0.96)
    overlay = Image.new("RGBA", size, (10, 16, 30, 92))
    return Image.alpha_composite(image.convert("RGBA"), overlay).convert("RGB")


def _draw_header(draw, script, scene, progress, size):
    w, _ = size
    _draw_panel(draw, (42, 28, w - 42, 88), radius=20, fill=(255, 255, 255, 224), shadow=False)
    draw.rounded_rectangle((68, 46, 116, 62), radius=8, fill=PALETTE["blue"])
    draw.rounded_rectangle((124, 46, 152, 62), radius=8, fill=PALETTE["amber"])
    draw.text((176, 43), _fit(str(script.get("title") or ""), 32), fill=PALETTE["ink"], font=font(24, True))
    label = "AI \u5fae\u8bfe"
    label_font = font(17, True)
    label_width = draw.textlength(label, font=label_font)
    draw.rounded_rectangle((w - label_width - 108, 42, w - 66, 74), radius=16, fill=(16, 24, 39, 232))
    draw.text((w - label_width - 88, 49), label, fill="#ffffff", font=label_font)


def _draw_progress(draw, progress, size):
    w, h = size
    left, right = 86, w - 86
    y = h - 31
    draw.rounded_rectangle((left, y, right, y + 8), radius=4, fill=(16, 24, 39, 54))
    fill_right = left + int((right - left) * _clamp(progress))
    draw.rounded_rectangle((left, y, fill_right, y + 8), radius=4, fill=PALETTE["blue"])
    if fill_right > left + 8:
        draw.ellipse((fill_right - 7, y - 4, fill_right + 9, y + 12), fill="#ffffff", outline=PALETTE["blue"], width=3)


def _draw_subtitle(draw, scene, progress, size, options=None):
    w, h = size
    options = options or {}
    timeline = scene.get("subtitleTimeline") if isinstance(scene.get("subtitleTimeline"), list) else []
    text = ""
    if timeline and options.get("absoluteSeconds") is not None:
        absolute = float(options.get("absoluteSeconds") or 0)
        for item in timeline:
            start = float(item.get("start") or 0)
            end = float(item.get("end") or 0)
            if start <= absolute < end:
                text = _clean_text(item.get("text") or "")
                break
        if not text:
            return
    else:
        segments = scene.get("subtitleSegments") or [scene.get("subtitle") or ""]
        if not segments:
            return
        idx = min(len(segments) - 1, int(progress * len(segments)))
        text = _clean_text(segments[idx])
    if not text:
        return

    lines = _wrap(text, 34)[:2]
    box_h = 28 + len(lines) * 36
    left, right = 206, w - 206
    bottom = h - 78
    top = bottom - box_h
    draw.rounded_rectangle((left + 8, top + 10, right + 8, bottom + 10), radius=22, fill=(15, 23, 42, 34))
    draw.rounded_rectangle((left, top, right, bottom), radius=22, fill=(15, 23, 42, 222), outline=(255, 255, 255, 70), width=1)
    draw.rounded_rectangle((left + 26, top + 18, left + 34, bottom - 18), radius=4, fill=PALETTE["amber"])
    y = top + 15
    for line in lines:
        tw = draw.textlength(line, font=font(25, True))
        draw.text(((w - tw) / 2 + 12, y), line, fill="#ffffff", font=font(25, True))
        y += 36


def _draw_panel(draw, bounds, radius=24, fill=(255, 255, 255, 232), outline=None, shadow=True):
    x1, y1, x2, y2 = bounds
    if shadow:
        draw.rounded_rectangle((x1 + 8, y1 + 10, x2 + 8, y2 + 10), radius=radius, fill=(16, 24, 39, 28))
    draw.rounded_rectangle(bounds, radius=radius, fill=fill, outline=outline or (255, 255, 255, 150), width=1)


def _fit(text, max_chars):
    text = _clean_text(text)
    return text if len(text) <= max_chars else text[:max(1, max_chars - 3)] + "..."


def _draw_scene_heading(draw, title, x, y, max_chars=28, dark=False):
    text_color = "#ffffff" if dark else PALETTE["ink"]
    muted = "#b8c2d2" if dark else PALETTE["muted"]
    draw.rounded_rectangle((x, y + 9, x + 14, y + 48), radius=7, fill=PALETTE["amber"])
    draw.text((x + 30, y), _fit(str(title or ""), max_chars), fill=text_color, font=font(38, True))
    draw.text((x + 32, y + 50), "\u6838\u5fc3\u8bb2\u89e3\u753b\u9762", fill=muted, font=font(17, True))


def _draw_concept(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    title = _clean_text(plan.get("main") or scene.get("title"))
    points = _normalize_steps(plan.get("points") or scene.get("onScreenText") or [])
    _draw_panel(draw, (72, 116, 1208, 590), radius=30, fill=(255, 255, 255, 234), outline=(210, 221, 235, 180))
    _draw_scene_heading(draw, title, 118, 154, 28)
    draw.rounded_rectangle((118, 244, 610, 256), radius=6, fill=(47, 111, 223, 46))
    draw.rounded_rectangle((118, 244, 118 + int(492 * max(0.18, progress)), 256), radius=6, fill=PALETTE["blue"])

    y = 300
    for idx, item in enumerate(points[:4]):
        reveal = min(1, max(0, progress * 4 - idx))
        alpha = 132 + int(96 * reveal)
        fill = (247, 249, 252, alpha)
        draw.rounded_rectangle((124, y - 10, 1128, y + 58), radius=18, fill=fill, outline=(216, 226, 238, 120), width=1)
        color = PALETTE["blue"] if idx % 2 == 0 else PALETTE["green"]
        draw.rounded_rectangle((148, y + 7, 184, y + 43), radius=18, fill=color)
        number = "%02d" % (idx + 1)
        tw = draw.textlength(number, font=font(16, True))
        draw.text((166 - tw / 2, y + 16), number, fill="#ffffff", font=font(16, True))
        draw.text((208, y + 4), _fit(item, 40), fill=PALETTE["ink"], font=font(28, True))
        y += 80


def _draw_flow(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    steps = _normalize_steps(plan.get("steps") or scene.get("onScreenText") or [scene.get("title")])
    count = max(1, min(5, len(steps)))
    active = min(count - 1, int(progress * count))

    _draw_panel(draw, (72, 116, 1208, 590), radius=30, fill=(255, 255, 255, 235), outline=(210, 221, 235, 180))
    _draw_scene_heading(draw, scene.get("title") or "", 118, 154, 28)
    x0, y0 = 122, 352
    gap = 22
    box_w = int((1036 - gap * (count - 1)) / count)
    for idx, step in enumerate(steps[:count]):
        x = x0 + idx * (box_w + gap)
        done = idx < active
        is_active = idx == active
        color = PALETTE["blue"] if is_active else PALETTE["green"] if done else "#c9d3df"
        fill = color if is_active else "#f8fafc"
        text_color = "#ffffff" if is_active else PALETTE["ink"]
        draw.rounded_rectangle((x + 5, y0 - 76 + 8, x + box_w + 5, y0 + 82 + 8), radius=20, fill=(16, 24, 39, 22))
        draw.rounded_rectangle((x, y0 - 76, x + box_w, y0 + 82), radius=20, fill=fill, outline=color, width=3)
        draw.rounded_rectangle((x + 18, y0 - 56, x + 62, y0 - 14), radius=21, fill="#ffffff" if is_active else color)
        draw.text((x + 29, y0 - 45), str(idx + 1), fill=color if is_active else "#ffffff", font=font(18, True))
        yy = y0 - 6
        for line in _wrap(step, max(5, box_w // 25))[:3]:
            draw.text((x + 20, yy), line, fill=text_color, font=font(21, True))
            yy += 29
        if idx < count - 1:
            mid_y = y0 + 4
            _draw_arrow(draw, x + box_w + 4, mid_y, x + box_w + gap - 7, mid_y, "#8ea0b6", 210)


def _draw_stack(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    items = _normalize_steps(plan.get("items") or plan.get("stack") or scene.get("onScreenText") or [])
    if not items:
        items = ["A", "B", "C"]
    visible = max(1, min(len(items), int(math.ceil(progress * len(items)))))

    _draw_panel(draw, (72, 116, 1208, 590), radius=30, fill=(255, 255, 255, 235), outline=(210, 221, 235, 180))
    _draw_scene_heading(draw, scene.get("title") or "Stack", 118, 154, 28)
    operation = _fit(str(plan.get("operation") or "LIFO"), 18)
    draw.rounded_rectangle((118, 250, 326, 296), radius=23, fill=(47, 111, 223, 28), outline=(47, 111, 223, 90), width=1)
    draw.text((146, 260), operation, fill=PALETTE["blue"], font=font(23, True))
    y = 326
    for idx, point in enumerate(items[:3]):
        color = PALETTE["blue"] if idx == 0 else PALETTE["green"] if idx == 1 else PALETTE["amber"]
        draw.rounded_rectangle((124, y, 164, y + 40), radius=20, fill=color)
        draw.text((186, y + 5), _fit(point, 22), fill=PALETTE["ink"], font=font(23, True))
        y += 62

    base_x, base_y, box_w, box_h = 596, 500, 360, 58
    draw.rounded_rectangle((base_x - 46, 216, base_x + box_w + 46, base_y + box_h + 32), radius=28, fill=(247, 249, 252, 220), outline=(216, 226, 238, 160), width=1)
    for idx, item in enumerate(items[:visible]):
        item_y = base_y - idx * (box_h + 14)
        is_top = idx == visible - 1
        color = PALETTE["blue"] if is_top else "#e7f0ff"
        text_color = "#ffffff" if is_top else PALETTE["ink"]
        draw.rounded_rectangle((base_x + 8, item_y + 8, base_x + box_w + 8, item_y + box_h + 8), radius=14, fill=(16, 24, 39, 24))
        draw.rounded_rectangle((base_x, item_y, base_x + box_w, item_y + box_h), radius=14, fill=color, outline=PALETTE["blue"], width=2)
        draw.text((base_x + 30, item_y + 15), _fit(str(item), 22), fill=text_color, font=font(24, True))
    draw.line((base_x - 30, base_y + box_h + 10, base_x + box_w + 30, base_y + box_h + 10), fill=PALETTE["dark"], width=5)
    top_y = base_y - (visible - 1) * (box_h + 14)
    draw.rounded_rectangle((base_x + box_w + 58, top_y + 10, base_x + box_w + 138, top_y + 46), radius=18, fill=PALETTE["dark"])
    draw.text((base_x + box_w + 80, top_y + 18), "top", fill="#ffffff", font=font(18, True))


def _draw_code(draw, scene, progress, size):
    plan = scene.get("visualPlan") or {}
    code = str(plan.get("code") or "\n".join(_normalize_steps(scene.get("onScreenText") or [])))
    _draw_panel(draw, (72, 116, 1208, 590), radius=30, fill=(16, 24, 39, 244), outline=(255, 255, 255, 46))
    _draw_scene_heading(draw, scene.get("title") or "", 118, 150, 28, dark=True)
    draw.rounded_rectangle((112, 224, 1168, 548), radius=20, fill=(5, 11, 22, 168), outline=(255, 255, 255, 34), width=1)
    draw.ellipse((136, 246, 150, 260), fill="#e95757")
    draw.ellipse((160, 246, 174, 260), fill="#f2a33a")
    draw.ellipse((184, 246, 198, 260), fill="#19a974")
    render_lines = []
    for raw in code.splitlines() or [code]:
        render_lines.extend(textwrap.wrap(raw, width=68, replace_whitespace=False) or [raw])
    y = 292
    for idx, line in enumerate(render_lines[:7]):
        active = idx <= int(progress * 7)
        fill = (47, 111, 223, 92) if active else (255, 255, 255, 10)
        draw.rounded_rectangle((132, y - 6, 1146, y + 36), radius=9, fill=fill)
        draw.text((154, y), _fit(line, 72), fill="#f8fafc" if active else "#94a3b8", font=font(23))
        y += 42
