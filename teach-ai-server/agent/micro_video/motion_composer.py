import shutil
import subprocess
import math
from pathlib import Path

import cv2
import numpy as np

from .visual_renderer import render_frame


def ffmpeg_exe():
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    try:
        import imageio_ffmpeg
        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:
        return None


def compose_video(script, out_dir, audio_path, keyframes, options, warnings):
    out_dir = Path(out_dir)
    raw_video_path = out_dir / "micro_course_raw.mp4"
    video_path = out_dir / "micro_course.mp4"
    quality_mode = options.get("qualityMode") or "standard"
    fps = 18 if quality_mode == "keyframe" else 12 if quality_mode == "fast" else 15
    size = (1280, 720)
    scenes = script.get("scenes") or []
    total_duration = sum(float(scene.get("durationSeconds") or 0) for scene in scenes) or 1.0
    total_frames = 0

    writer = cv2.VideoWriter(str(raw_video_path), cv2.VideoWriter_fourcc(*"mp4v"), fps, size)
    if not writer.isOpened():
        raise RuntimeError("cannot open video writer")

    try:
        elapsed = 0.0
        for scene in scenes:
            duration = float(scene.get("durationSeconds") or 10)
            frame_count = max(1, int(math.ceil(duration * fps)))
            keyframe = keyframes.get(int(scene.get("index") or 1)) if keyframes else None
            for frame_index in range(frame_count):
                local = frame_index / max(1, frame_count - 1)
                global_progress = (elapsed + local * duration) / total_duration
                frame_options = dict(options)
                frame_options["absoluteSeconds"] = elapsed + local * duration
                frame_options["sceneElapsedSeconds"] = local * duration
                image = render_frame(script, scene, _ease(local), global_progress, options=frame_options, keyframe=keyframe, size=size)
                image = _camera_motion(image, local, scene, options)
                frame = cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR)
                writer.write(frame)
                total_frames += 1
            elapsed += duration
    finally:
        writer.release()

    used_ffmpeg = _mux_browser_mp4(raw_video_path, audio_path, video_path, fps, warnings)
    if used_ffmpeg:
        try:
            raw_video_path.unlink()
        except OSError:
            pass
    else:
        raw_video_path.replace(video_path)
    duration = _probe_duration(video_path) or (total_frames / float(fps or 1))
    bitrate = int(video_path.stat().st_size * 8 / max(1.0, duration)) if video_path.exists() else 0
    return video_path, {
        "fps": fps,
        "frames": total_frames,
        "codec": "h264+aac" if used_ffmpeg else "mp4v",
        "bitrate": bitrate,
        "durationSeconds": round(duration, 2),
        "templateVersion": "micro-video-template-v3",
    }


def _camera_motion(image, progress, scene, options=None):
    options = options or {}
    if options.get("qualityMode") == "fast":
        return image
    motions = scene.get("motion") if isinstance(scene.get("motion"), list) else []
    if "pan" not in motions and "zoom" not in motions:
        return image
    w, h = image.size
    zoom = 1.0 + 0.025 * progress
    scaled = image.resize((int(w * zoom), int(h * zoom)))
    max_x = scaled.width - w
    max_y = scaled.height - h
    x = int(max_x * (0.25 + 0.5 * progress))
    y = int(max_y * 0.45)
    return scaled.crop((x, y, x + w, y + h))


def _mux_browser_mp4(raw_video_path, audio_path, video_path, fps, warnings):
    ffmpeg = ffmpeg_exe()
    if not ffmpeg:
        warnings.append("ffmpeg is not available; video was saved without AAC muxing.")
        return False
    cmd = [
        ffmpeg,
        "-y",
        "-r",
        str(fps),
        "-i",
        str(raw_video_path),
        "-i",
        str(audio_path),
        "-c:v",
        "libx264",
        "-pix_fmt",
        "yuv420p",
        "-profile:v",
        "baseline",
        "-level",
        "3.1",
        "-c:a",
        "aac",
        "-b:a",
        "128k",
        "-af",
        "loudnorm=I=-16:TP=-1.5:LRA=11,aresample=44100",
        "-ar",
        "44100",
        "-ac",
        "1",
        "-shortest",
        "-movflags",
        "+faststart",
        "-preset",
        "superfast",
        "-crf",
        "20",
        "-b:v",
        "2200k",
        "-maxrate",
        "2600k",
        "-bufsize",
        "5200k",
        str(video_path),
    ]
    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if result.returncode != 0:
        raise RuntimeError("ffmpeg mux failed: " + (result.stderr[-600:] or result.stdout[-600:]))
    return True


def _probe_duration(path):
    ffprobe = shutil.which("ffprobe")
    if not ffprobe:
        return None
    result = subprocess.run(
        [
            ffprobe,
            "-v",
            "error",
            "-show_entries",
            "format=duration",
            "-of",
            "default=noprint_wrappers=1:nokey=1",
            str(path),
        ],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    if result.returncode != 0:
        return None
    try:
        return float((result.stdout or "").strip())
    except ValueError:
        return None


def _ease(value):
    value = max(0.0, min(1.0, float(value)))
    return value * value * (3 - 2 * value)
