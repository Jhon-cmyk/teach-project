import json
import math
from pathlib import Path

from .audio_composer import compose_audio
from .keyframe_provider import load_keyframe
from .motion_composer import compose_video
from .script_normalizer import normalize_script
from .visual_renderer import cover_image


ROOT_DIR = Path(__file__).resolve().parents[2]
OUTPUT_ROOT = ROOT_DIR / "static" / "videos" / "micro_course"


def render_micro_video(payload, base_url):
    task_id = str(payload.get("taskId") or "preview")
    options = dict(payload.get("options") or {})
    warnings = []
    script, normalize_warnings = normalize_script(payload.get("scriptJson") or "{}", options)
    warnings.extend(normalize_warnings)

    out_dir = OUTPUT_ROOT / task_id
    out_dir.mkdir(parents=True, exist_ok=True)

    scenes = script.get("scenes") or []
    first_scene = scenes[0]
    cover_path = out_dir / "cover.jpg"
    cover_image(script, first_scene).save(cover_path, quality=92)

    keyframes = {}
    use_keyframes = bool(options.get("useAiKeyframes")) or options.get("qualityMode") == "keyframe"
    if use_keyframes:
        for scene in scenes:
            image = load_keyframe(scene, out_dir, warnings)
            if image is not None:
                keyframes[int(scene.get("index") or 1)] = image

    audio_path, audio_meta = compose_audio(script, out_dir, options, warnings)
    _apply_audio_timeline(script, audio_meta)

    normalized_script_path = out_dir / "script.normalized.json"
    normalized_script_path.write_text(json.dumps(script, ensure_ascii=False, indent=2), encoding="utf-8")

    subtitle_path = out_dir / "subtitle.srt"
    subtitle_path.write_text(_build_srt(audio_meta.get("timeline") or [], scenes), encoding="utf-8")

    video_path, video_meta = compose_video(script, out_dir, audio_path, keyframes, {
        "qualityMode": options.get("qualityMode") or "standard",
        "burnSubtitles": bool(options.get("burnSubtitles", options.get("subtitlesEnabled", True))),
    }, warnings)

    audio_seconds = float(audio_meta.get("durationSeconds") or 0)
    video_seconds = float(video_meta.get("durationSeconds") or sum(float(scene.get("durationSeconds") or 0) for scene in scenes))
    duration_seconds = int(math.ceil(max(audio_seconds, video_seconds)))
    render_stats = {
        "sceneCount": len(scenes),
        "durationSeconds": duration_seconds,
        "qualityMode": options.get("qualityMode") or "standard",
        "keyframeCount": len(keyframes),
        "audio": audio_meta,
        "video": video_meta,
        "sync": {
            "status": "timeline_aligned",
            "audioSeconds": round(audio_seconds, 2),
            "videoSeconds": round(video_seconds, 2),
        },
    }

    base = base_url.rstrip("/")
    rel = f"/static/videos/micro_course/{task_id}"
    return {
        "status": "succeeded",
        "title": script.get("title") or options.get("title") or "AI micro course",
        "durationSeconds": duration_seconds,
        "videoUrl": f"{base}{rel}/{video_path.name}",
        "coverUrl": f"{base}{rel}/{cover_path.name}",
        "subtitleUrl": f"{base}{rel}/{subtitle_path.name}",
        "audioUrl": f"{base}{rel}/{audio_path.name}",
        "warnings": warnings,
        "renderStats": render_stats,
        "scriptJson": json.dumps(script, ensure_ascii=False),
    }


def _apply_audio_timeline(script, audio_meta):
    scene_durations = audio_meta.get("sceneDurations") or {}
    timeline = audio_meta.get("timeline") or []
    timeline_by_scene = {}
    for item in timeline:
        scene_index = str(item.get("sceneIndex") or 1)
        timeline_by_scene.setdefault(scene_index, []).append(item)

    for scene in script.get("scenes") or []:
        scene_index = str(int(scene.get("index") or 1))
        duration = scene_durations.get(scene_index)
        if duration:
            scene["durationSeconds"] = round(float(duration), 3)
        scene["subtitleTimeline"] = timeline_by_scene.get(scene_index, [])


def _build_srt(timeline, scenes):
    if not timeline:
        return _build_fallback_srt(scenes)
    lines = []
    for index, item in enumerate(timeline, 1):
        text = str(item.get("text") or "").strip()
        if not text:
            continue
        lines.extend([
            str(index),
            "%s --> %s" % (_format_srt_time(float(item.get("start") or 0)), _format_srt_time(float(item.get("end") or 0))),
            text,
            "",
        ])
    return "\n".join(lines)


def _build_fallback_srt(scenes):
    lines = []
    cursor = 0.0
    index = 1
    for scene in scenes:
        duration = float(scene.get("durationSeconds") or 10)
        segments = scene.get("subtitleSegments") or [scene.get("subtitle") or scene.get("narration") or ""]
        segment_duration = duration / max(1, len(segments))
        for segment in segments:
            start = cursor
            end = min(cursor + segment_duration, cursor + duration)
            lines.extend([
                str(index),
                "%s --> %s" % (_format_srt_time(start), _format_srt_time(end)),
                str(segment).strip(),
                "",
            ])
            cursor = end
            index += 1
        cursor = round(cursor, 3)
    return "\n".join(lines)


def _format_srt_time(seconds):
    seconds = max(0.0, float(seconds))
    total_ms = int(round(seconds * 1000))
    ms = total_ms % 1000
    total = total_ms // 1000
    hh = total // 3600
    mm = (total % 3600) // 60
    ss = total % 60
    return f"{hh:02d}:{mm:02d}:{ss:02d},{ms:03d}"
