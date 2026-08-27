import math
import os
import re
import shutil
import subprocess
import json
import urllib.error
import urllib.parse
import urllib.request
import wave
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from time import sleep


MIN_VALID_MEAN_VOLUME_DB = -35.0
LEAD_SILENCE_SECONDS = 0.18
SEGMENT_PAUSE_SECONDS = 0.22
SCENE_PAUSE_SECONDS = 0.55
TAIL_SILENCE_SECONDS = 0.25
SILENCE_THRESHOLD = 180
TRIM_KEEP_SECONDS = 0.06


def estimate_narration_seconds(text):
    count = len(str(text or "").strip())
    if count <= 0:
        return 2.0
    return max(4.0, count / 4.2)


def compose_audio(script, out_dir, options, warnings):
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    provider = (os.getenv("MICRO_VIDEO_TTS_PROVIDER") or options.get("ttsProvider") or "aliyun").strip().lower()
    voice_id = options.get("voiceId") or os.getenv("MICRO_VIDEO_TTS_VOICE") or "xiaoyun"
    text_path = out_dir / "narration.txt"
    audio_path = out_dir / "narration.wav"
    units = _build_audio_units(script)
    text_path.write_text(_full_text(units), encoding="utf-8")

    if provider == "aliyun":
        try:
            audio_meta = _compose_aliyun_units(units, out_dir, audio_path, voice_id, options, warnings)
            provider_name = "aliyun"
        except Exception as exc:
            if (os.getenv("MICRO_VIDEO_TTS_FALLBACK") or "").strip().lower() != "sapi":
                raise
            warnings.append("Aliyun TTS failed; falling back to Windows SAPI: %s" % exc)
            audio_meta = _compose_windows_sapi_units(units, out_dir, audio_path, voice_id, warnings)
            provider_name = "windows_sapi"
    elif provider in {"sapi", "local", "windows"}:
        audio_meta = _compose_windows_sapi_units(units, out_dir, audio_path, voice_id, warnings)
        provider_name = "windows_sapi"
    elif provider == "command":
        rendered = _run_tts_command(os.getenv("MICRO_VIDEO_TTS_COMMAND") or "", text_path, audio_path, voice_id, warnings)
        provider_name = "command"
        if not rendered:
            raise RuntimeError("No usable command TTS audio was generated. Check MICRO_VIDEO_TTS_COMMAND.")
        audio_stats = inspect_wav_volume(audio_path)
        audio_meta = {
            "voiceId": voice_id,
            "resolvedVoiceName": voice_id,
            "timeline": _approximate_timeline(units, audio_stats.get("durationSeconds") or 0),
            "sceneDurations": {},
        }
    else:
        raise RuntimeError("Unsupported TTS provider: %s" % provider)

    if not audio_path.exists():
        raise RuntimeError("No usable local TTS audio was generated. Configure MICRO_VIDEO_TTS_PROVIDER or install a local voice.")

    audio_stats = inspect_wav_volume(audio_path)
    if not audio_stats.get("valid"):
        raise RuntimeError(
            "Generated TTS audio is too quiet: mean %.1f dB, peak %.1f dB. Check the local TTS voice/output device."
            % (audio_stats.get("meanVolumeDb", -120.0), audio_stats.get("peakVolumeDb", -120.0))
        )

    timeline = audio_meta.get("timeline") or _approximate_timeline(units, audio_stats.get("durationSeconds") or 0)
    scene_durations = _scene_durations_from_timeline(timeline, audio_stats.get("durationSeconds") or 0)
    return audio_path, {
        "provider": provider_name,
        "voiceId": voice_id,
        "status": "ok",
        **audio_meta,
        "timeline": timeline,
        "sceneDurations": scene_durations,
        **audio_stats,
    }


def inspect_wav_volume(path):
    with wave.open(str(path), "rb") as wav:
        channels = wav.getnchannels()
        sample_width = wav.getsampwidth()
        frame_count = wav.getnframes()
        raw = wav.readframes(frame_count)
        duration = frame_count / float(wav.getframerate() or 44100)
        sample_rate = wav.getframerate()
    if sample_width != 2 or not raw:
        return {
            "durationSeconds": round(duration if "duration" in locals() else 0, 2),
            "meanVolumeDb": -120.0,
            "peakVolumeDb": -120.0,
            "valid": False,
        }

    sample_count = len(raw) // 2
    if sample_count <= 0:
        return {"durationSeconds": round(duration, 2), "meanVolumeDb": -120.0, "peakVolumeDb": -120.0, "valid": False}

    total = 0.0
    peak = 0
    for idx in range(sample_count):
        value = int.from_bytes(raw[idx * 2: idx * 2 + 2], byteorder="little", signed=True)
        absolute = abs(value)
        peak = max(peak, absolute)
        total += value * value
    rms = math.sqrt(total / sample_count)
    mean_db = _dbfs(rms)
    peak_db = _dbfs(peak)
    return {
        "durationSeconds": round(duration, 2),
        "sampleRate": sample_rate,
        "channels": channels,
        "meanVolumeDb": round(mean_db, 1),
        "peakVolumeDb": round(peak_db, 1),
        "valid": mean_db >= MIN_VALID_MEAN_VOLUME_DB,
    }


def _build_audio_units(script):
    units = []
    for scene_idx, scene in enumerate(script.get("scenes") or [], 1):
        scene_index = int(scene.get("index") or scene_idx)
        subtitle_segments = _subtitle_segments(scene)
        narration = _clean(scene.get("narration") or scene.get("subtitle") or scene.get("title") or "")
        chunks = _split_narration(narration, max(1, len(subtitle_segments)))
        if not subtitle_segments:
            subtitle_segments = chunks or [_clean(scene.get("subtitle") or scene.get("title") or "")]
        while len(chunks) < len(subtitle_segments):
            chunks.append("")
        for idx, subtitle_text in enumerate(subtitle_segments):
            spoken = chunks[idx].strip() or subtitle_text
            if idx == 0:
                title = _clean(scene.get("title") or "")
                if title and title not in spoken:
                    spoken = title + ". " + spoken
                if False and title and title not in spoken:
                    spoken = "{}。{}".format(title, spoken)
                if False and title and title not in spoken:
                    spoken = "%s。%s" % (title, spoken)
            units.append({
                "sceneIndex": scene_index,
                "segmentIndex": idx + 1,
                "text": _clean(subtitle_text),
                "spokenText": _clean_spoken(spoken),
            })
    return [unit for unit in units if unit.get("spokenText") or unit.get("text")]


def _compose_windows_sapi_units(units, out_dir, audio_path, voice_id, warnings):
    parts_dir = Path(out_dir) / "_tts_parts"
    parts_dir.mkdir(parents=True, exist_ok=True)
    voice_name = _voice_name_for_id(voice_id)
    rate = _rate_for_voice(voice_id)
    clip_items = []
    resolved_voice_name = ""

    for idx, unit in enumerate(units, 1):
        text_path = parts_dir / ("part_%03d.txt" % idx)
        clip_path = parts_dir / ("part_%03d.wav" % idx)
        text_path.write_text(unit.get("spokenText") or unit.get("text") or "", encoding="utf-8")
        result = _run_windows_sapi_tts(text_path, clip_path, voice_name, rate, warnings)
        if not result or not clip_path.exists():
            raise RuntimeError("Windows SAPI TTS failed for segment %s" % idx)
        resolved_voice_name = result.get("resolvedVoiceName") or resolved_voice_name
        clip_items.append((unit, clip_path))

    timeline, scene_durations = _concat_wav_clips(clip_items, audio_path)
    _normalize_wav_for_delivery(audio_path, warnings)
    _cleanup_parts_dir(parts_dir, warnings)
    if voice_name != resolved_voice_name and resolved_voice_name:
        warnings.append("Requested local voice '%s' was not installed; used '%s'." % (voice_name, resolved_voice_name))
    if voice_id in {"default_male", "calm"} and resolved_voice_name == "Microsoft Huihui Desktop":
        warnings.append("Only Microsoft Huihui Desktop is installed for zh-CN, so '%s' uses the same local voice." % voice_id)
    return {
        "voiceId": voice_id,
        "resolvedVoiceName": resolved_voice_name or voice_name,
        "timeline": timeline,
        "sceneDurations": scene_durations,
    }


def _concat_wav_clips(clip_items, audio_path):
    params = None
    output_frames = []
    timeline = []
    scene_start = {}
    scene_end = {}
    cursor = LEAD_SILENCE_SECONDS

    for item_index, (unit, clip_path) in enumerate(clip_items):
        clip_params, frames, duration = _read_trimmed_wav_frames(clip_path)
        if params is None:
            params = clip_params
            output_frames.append(_silence_frames(LEAD_SILENCE_SECONDS, params))
        elif clip_params[:3] != params[:3]:
            raise RuntimeError("TTS clips use inconsistent WAV formats.")

        scene_index = int(unit.get("sceneIndex") or 1)
        scene_start.setdefault(scene_index, cursor)
        start = cursor
        output_frames.append(frames)
        cursor += duration
        end = cursor
        timeline.append({
            "sceneIndex": scene_index,
            "segmentIndex": int(unit.get("segmentIndex") or 1),
            "start": round(start, 3),
            "end": round(end, 3),
            "duration": round(max(0.001, end - start), 3),
            "text": unit.get("text") or "",
            "spokenText": unit.get("spokenText") or "",
        })
        scene_end[scene_index] = end

        is_last = item_index == len(clip_items) - 1
        next_scene = None if is_last else int(clip_items[item_index + 1][0].get("sceneIndex") or scene_index)
        pause = 0 if is_last else (SCENE_PAUSE_SECONDS if next_scene != scene_index else SEGMENT_PAUSE_SECONDS)
        if pause:
            output_frames.append(_silence_frames(pause, params))
            cursor += pause

    if params is None:
        params = (1, 2, 22050, "NONE", "not compressed")
    output_frames.append(_silence_frames(TAIL_SILENCE_SECONDS, params))
    cursor += TAIL_SILENCE_SECONDS
    if timeline:
        timeline[-1]["end"] = round(cursor, 3)
        timeline[-1]["duration"] = round(max(0.001, cursor - float(timeline[-1].get("start") or 0)), 3)

    with wave.open(str(audio_path), "wb") as wav:
        channels, sample_width, sample_rate, comptype, compname = params
        wav.setnchannels(channels)
        wav.setsampwidth(sample_width)
        wav.setframerate(sample_rate)
        wav.setcomptype(comptype, compname)
        wav.writeframes(b"".join(output_frames))

    scene_durations = {}
    sorted_scenes = sorted(scene_start)
    previous_scene_start = 0.0
    for idx, scene_index in enumerate(sorted_scenes):
        start = previous_scene_start
        if idx + 1 < len(sorted_scenes):
            next_start = scene_start[sorted_scenes[idx + 1]]
            end = next_start
        else:
            end = cursor
        scene_durations[str(scene_index)] = round(max(0.1, end - start), 3)
        previous_scene_start = end
    return timeline, scene_durations


def _read_trimmed_wav_frames(path):
    with wave.open(str(path), "rb") as wav:
        channels = wav.getnchannels()
        sample_width = wav.getsampwidth()
        sample_rate = wav.getframerate()
        comptype = wav.getcomptype()
        compname = wav.getcompname()
        raw = wav.readframes(wav.getnframes())
    if sample_width != 2 or not raw:
        duration = len(raw) / float(max(1, channels * sample_width * sample_rate))
        return (channels, sample_width, sample_rate, comptype, compname), raw, duration

    frame_width = channels * sample_width
    frame_count = len(raw) // frame_width
    keep_frames = int(TRIM_KEEP_SECONDS * sample_rate)
    first = 0
    last = max(0, frame_count - 1)
    while first < frame_count and _frame_peak(raw, first, frame_width, channels) < SILENCE_THRESHOLD:
        first += 1
    while last > first and _frame_peak(raw, last, frame_width, channels) < SILENCE_THRESHOLD:
        last -= 1
    first = max(0, first - keep_frames)
    last = min(frame_count - 1, last + keep_frames)
    trimmed = raw[first * frame_width:(last + 1) * frame_width]
    duration = len(trimmed) / float(max(1, frame_width * sample_rate))
    return (channels, sample_width, sample_rate, comptype, compname), trimmed, duration


def _frame_peak(raw, frame_index, frame_width, channels):
    base = frame_index * frame_width
    peak = 0
    for channel in range(channels):
        offset = base + channel * 2
        value = int.from_bytes(raw[offset:offset + 2], byteorder="little", signed=True)
        peak = max(peak, abs(value))
    return peak


def _silence_frames(seconds, params):
    channels, sample_width, sample_rate, _, _ = params
    frame_count = max(0, int(seconds * sample_rate))
    return b"\x00" * frame_count * channels * sample_width


def _normalize_wav_for_delivery(audio_path, warnings):
    ffmpeg = _ffmpeg_exe()
    if not ffmpeg:
        warnings.append("ffmpeg is not available for WAV normalization; raw SAPI audio was kept.")
        return
    temp_path = Path(audio_path).with_name(Path(audio_path).stem + ".normalized.wav")
    cmd = [
        ffmpeg,
        "-y",
        "-i",
        str(audio_path),
        "-af",
        "loudnorm=I=-18:TP=-1.5:LRA=11,aresample=44100",
        "-ar",
        "44100",
        "-ac",
        "1",
        str(temp_path),
    ]
    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if result.returncode == 0 and temp_path.exists():
        temp_path.replace(audio_path)
    else:
        warnings.append("WAV normalization failed: " + (result.stderr[-300:] or result.stdout[-300:]))
        try:
            temp_path.unlink()
        except OSError:
            pass


def _cleanup_parts_dir(parts_dir, warnings):
    try:
        shutil.rmtree(parts_dir)
    except OSError as exc:
        warnings.append("Temporary TTS parts could not be cleaned: %s" % exc)


def _full_text(units):
    lines = []
    current_scene = None
    for unit in units:
        scene_index = unit.get("sceneIndex")
        if scene_index != current_scene:
            if lines:
                lines.append("")
            current_scene = scene_index
        text = unit.get("spokenText") or unit.get("text") or ""
        if text:
            lines.append(text)
    return "\n".join(lines)


def _subtitle_segments(scene):
    segments = scene.get("subtitleSegments") if isinstance(scene.get("subtitleSegments"), list) else []
    result = [_clean(item) for item in segments if _clean(item)]
    if result:
        return result
    subtitle = _clean(scene.get("subtitle") or "")
    if subtitle:
        return _split_short_text(subtitle, 28)
    narration = _clean(scene.get("narration") or scene.get("title") or "")
    return _split_short_text(narration, 28)[:8]


def _split_narration(text, count):
    text = _clean(text)
    if not text:
        return []
    sentences = [item.strip() for item in re.split(r"(?<=[。！？!?；;])", text) if item.strip()]
    if not sentences:
        sentences = _split_short_text(text, max(32, int(len(text) / max(1, count))))
    if len(sentences) <= count:
        chunks = list(sentences)
    else:
        total_chars = sum(len(sentence) for sentence in sentences) or 1
        target_chars = max(1, total_chars / max(1, count))
        chunks = []
        current = ""
        remaining_chunks = count
        for idx, sentence in enumerate(sentences):
            remaining_sentences = len(sentences) - idx
            current = (current + sentence).strip()
            should_close = len(current) >= target_chars and remaining_sentences > remaining_chunks - 1
            must_close = remaining_sentences == remaining_chunks
            if len(chunks) < count - 1 and (should_close or must_close):
                chunks.append(current)
                current = ""
                remaining_chunks -= 1
        if current:
            chunks.append(current)
    while len(chunks) < count:
        chunks.append("")
    return chunks[:count]


def _split_short_text(text, max_len):
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
    return pieces or ([str(text).strip()] if str(text or "").strip() else [])


def _approximate_timeline(units, total_seconds):
    total_seconds = max(0.1, float(total_seconds or 0))
    weights = [max(1, len(unit.get("spokenText") or unit.get("text") or "")) for unit in units]
    total_weight = sum(weights) or 1
    cursor = 0.0
    timeline = []
    for unit, weight in zip(units, weights):
        duration = total_seconds * weight / total_weight
        start = cursor
        end = min(total_seconds, cursor + duration)
        timeline.append({
            "sceneIndex": int(unit.get("sceneIndex") or 1),
            "segmentIndex": int(unit.get("segmentIndex") or 1),
            "start": round(start, 3),
            "end": round(end, 3),
            "duration": round(max(0.001, end - start), 3),
            "text": unit.get("text") or "",
            "spokenText": unit.get("spokenText") or "",
        })
        cursor = end
    return timeline


def _scene_durations_from_timeline(timeline, total_seconds):
    if not timeline:
        return {}
    starts = {}
    ends = {}
    for item in timeline:
        scene_index = str(item.get("sceneIndex") or 1)
        starts[scene_index] = min(starts.get(scene_index, item.get("start", 0)), item.get("start", 0))
        ends[scene_index] = max(ends.get(scene_index, item.get("end", 0)), item.get("end", 0))
    result = {}
    ordered = sorted(starts, key=lambda value: int(value))
    previous = 0.0
    for idx, scene_index in enumerate(ordered):
        if idx + 1 < len(ordered):
            end = starts[ordered[idx + 1]]
        else:
            end = float(total_seconds or ends[scene_index])
        result[scene_index] = round(max(0.1, end - previous), 3)
        previous = end
    return result


def _compose_aliyun_units(units, out_dir, audio_path, voice_id, options, warnings):
    parts_dir = Path(out_dir) / "_tts_parts"
    parts_dir.mkdir(parents=True, exist_ok=True)
    resolved_voice = _aliyun_voice_for_id(voice_id)
    voice_profile = _aliyun_voice_profile_for_id(voice_id)
    max_workers = int(options.get("ttsWorkers") or os.getenv("MICRO_VIDEO_TTS_WORKERS") or 1)
    max_workers = max(1, min(8, max_workers, len(units) or 1))

    def render_unit(idx, unit):
        clip_path = parts_dir / ("part_%03d.wav" % idx)
        text = unit.get("spokenText") or unit.get("text") or ""
        _run_aliyun_tts(text, clip_path, resolved_voice, options, voice_profile)
        if not clip_path.exists():
            raise RuntimeError("Aliyun TTS did not create audio for segment %s" % idx)
        return idx, unit, clip_path

    results = []
    if max_workers <= 1 or len(units) <= 1:
        for idx, unit in enumerate(units, 1):
            results.append(render_unit(idx, unit))
    else:
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = [executor.submit(render_unit, idx, unit) for idx, unit in enumerate(units, 1)]
            for future in as_completed(futures):
                results.append(future.result())
    clip_items = [(unit, clip_path) for _, unit, clip_path in sorted(results, key=lambda item: item[0])]

    timeline, scene_durations = _concat_wav_clips(clip_items, audio_path)
    _normalize_wav_for_delivery(audio_path, warnings)
    _cleanup_parts_dir(parts_dir, warnings)
    return {
        "voiceId": voice_id,
        "resolvedVoiceName": resolved_voice,
        "speechRate": voice_profile.get("speechRate"),
        "pitchRate": voice_profile.get("pitchRate"),
        "volume": voice_profile.get("volume"),
        "timeline": timeline,
        "sceneDurations": scene_durations,
    }


def _run_aliyun_tts(text, audio_path, voice, options, voice_profile=None):
    voice_profile = voice_profile or {}
    aliyun_tts = options.get("aliyunTts") if isinstance(options.get("aliyunTts"), dict) else {}
    app_key = aliyun_tts.get("appKey") or os.getenv("MICRO_VIDEO_ALIYUN_APP_KEY") or os.getenv("ALIYUN_NLS_APP_KEY") or ""
    token = aliyun_tts.get("token") or os.getenv("MICRO_VIDEO_ALIYUN_TOKEN") or os.getenv("ALIYUN_NLS_TOKEN") or ""
    endpoint = (
        aliyun_tts.get("endpoint")
        or os.getenv("MICRO_VIDEO_ALIYUN_ENDPOINT")
        or "https://nls-gateway-cn-shanghai.aliyuncs.com/stream/v1/tts"
    )
    if not app_key or not token:
        raise RuntimeError("Aliyun TTS is not configured. Set MICRO_VIDEO_ALIYUN_APP_KEY and MICRO_VIDEO_ALIYUN_TOKEN.")

    params = {
        "appkey": app_key,
        "token": token,
        "text": str(text or "")[:280],
        "format": "wav",
        "sample_rate": int(options.get("sampleRate") or os.getenv("MICRO_VIDEO_ALIYUN_SAMPLE_RATE") or 16000),
        "voice": voice,
        "volume": int(options.get("volume") or os.getenv("MICRO_VIDEO_ALIYUN_VOLUME") or voice_profile.get("volume") or 80),
        "speech_rate": int(options.get("speechRate") or os.getenv("MICRO_VIDEO_ALIYUN_SPEECH_RATE") or voice_profile.get("speechRate") or 0),
        "pitch_rate": int(options.get("pitchRate") or os.getenv("MICRO_VIDEO_ALIYUN_PITCH_RATE") or voice_profile.get("pitchRate") or 0),
    }
    encoded = json.dumps(params, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        endpoint,
        data=encoded,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    max_retries = int(options.get("ttsRetries") or os.getenv("MICRO_VIDEO_TTS_RETRIES") or 3)
    for attempt in range(max_retries + 1):
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                body = response.read()
                content_type = response.headers.get("Content-Type", "")
            break
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="ignore")
            if _is_aliyun_rate_limited(exc.code, detail) and attempt < max_retries:
                sleep(min(8.0, 1.2 * (attempt + 1) ** 2))
                continue
            raise RuntimeError("Aliyun TTS HTTP %s: %s" % (exc.code, detail[:300])) from exc
        except urllib.error.URLError as exc:
            if attempt < max_retries:
                sleep(min(6.0, 0.8 * (attempt + 1) ** 2))
                continue
            raise RuntimeError("Aliyun TTS request failed: %s" % exc.reason) from exc

    if "json" in content_type.lower() or body[:1] == b"{":
        detail = body.decode("utf-8", errors="ignore")
        if _is_aliyun_rate_limited(400, detail):
            raise RuntimeError("Aliyun TTS was rate limited. Please retry later or keep MICRO_VIDEO_TTS_WORKERS=1: %s" % detail[:240])
        raise RuntimeError("Aliyun TTS returned an error: %s" % detail[:300])
    if not body:
        raise RuntimeError("Aliyun TTS returned empty audio.")
    Path(audio_path).write_bytes(body)


def _is_aliyun_rate_limited(status_code, detail):
    text = str(detail or "").upper()
    return status_code in {400, 429, 503} and ("TOO_MANY_REQUESTS" in text or "RATE" in text or "限流" in text)


def _run_windows_sapi_tts(text_path, audio_path, voice_name, rate, warnings):
    ps_path = Path(audio_path).with_suffix(".tts.ps1")
    ps_path.write_text(
        """
param(
  [string]$TextPath,
  [string]$OutputPath,
  [string]$VoiceName,
  [int]$Rate
)
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Speech
$text = Get-Content -LiteralPath $TextPath -Raw -Encoding UTF8
$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$synth.Rate = $Rate
$synth.Volume = 100
$voices = $synth.GetInstalledVoices() | ForEach-Object { $_.VoiceInfo.Name }
if ($VoiceName -and ($voices -contains $VoiceName)) {
  $synth.SelectVoice($VoiceName)
} elseif ($voices -contains "Microsoft Huihui Desktop") {
  $synth.SelectVoice("Microsoft Huihui Desktop")
}
$selected = $synth.Voice.Name
$synth.SetOutputToWaveFile($OutputPath)
$synth.Speak($text)
$synth.Dispose()
Write-Output $selected
""".strip(),
        encoding="utf-8-sig",
    )
    try:
        result = subprocess.run(
            [
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                str(ps_path),
                "-TextPath",
                str(text_path),
                "-OutputPath",
                str(audio_path),
                "-VoiceName",
                voice_name,
                "-Rate",
                str(rate),
            ],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=180,
        )
        if result.returncode != 0:
            warnings.append("Windows SAPI TTS failed: " + (result.stderr[-300:] or result.stdout[-300:]))
            return None
        return {"path": audio_path, "resolvedVoiceName": (result.stdout or "").strip().splitlines()[-1] if result.stdout.strip() else voice_name}
    finally:
        try:
            ps_path.unlink()
        except OSError:
            pass


def _run_tts_command(command, text_path, audio_path, voice_id, warnings):
    if not command:
        warnings.append("MICRO_VIDEO_TTS_COMMAND is empty.")
        return None
    try:
        rendered_command = (
            command
            .replace("{text}", str(text_path))
            .replace("{output}", str(audio_path))
            .replace("{voice}", str(voice_id))
        )
        result = subprocess.run(rendered_command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=180)
        if result.returncode == 0 and audio_path.exists():
            return audio_path
        warnings.append("TTS command failed: " + (result.stderr[-300:] or result.stdout[-300:]))
    except Exception as exc:
        warnings.append("TTS command error: %s" % exc)
    return None


def _voice_name_for_id(voice_id):
    mapping = {
        "default_female": "Microsoft Huihui Desktop",
        "default_male": "Microsoft Huihui Desktop",
        "calm": "Microsoft Huihui Desktop",
    }
    return mapping.get(str(voice_id or ""), str(voice_id or "Microsoft Huihui Desktop"))


def _aliyun_voice_for_id(voice_id):
    mapping = {
        "warm_female": "xiaoyun",
        "clear_male": "xiaogang",
        "calm_teacher": "ruoxi",
        "bright_teacher": "sijia",
        "default_female": "xiaoyun",
        "default_male": "xiaogang",
        "calm": "ruoxi",
    }
    return mapping.get(str(voice_id or ""), str(voice_id or "xiaoyun"))


def _aliyun_voice_profile_for_id(voice_id):
    mapping = {
        "warm_female": {"speechRate": -20, "pitchRate": 40, "volume": 82},
        "clear_male": {"speechRate": 20, "pitchRate": -120, "volume": 86},
        "calm_teacher": {"speechRate": -160, "pitchRate": -70, "volume": 82},
        "bright_teacher": {"speechRate": 150, "pitchRate": 120, "volume": 86},
        "default_female": {"speechRate": -20, "pitchRate": 40, "volume": 82},
        "default_male": {"speechRate": 20, "pitchRate": -120, "volume": 86},
        "calm": {"speechRate": -160, "pitchRate": -70, "volume": 82},
    }
    return mapping.get(str(voice_id or ""), {"speechRate": 0, "pitchRate": 0, "volume": 80})


def _rate_for_voice(voice_id):
    mapping = {
        "default_female": -1,
        "default_male": -1,
        "calm": -2,
    }
    return mapping.get(str(voice_id or ""), -1)


def _clean(value):
    if value is None:
        return ""
    return str(value).replace("\r", " ").replace("\n", " ").strip()


def _clean_spoken(value):
    text = _clean(value)
    text = re.sub(r"\s+", " ", text)
    return text


def _ffmpeg_exe():
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    try:
        import imageio_ffmpeg
        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:
        return None


def _dbfs(value):
    if value <= 0:
        return -120.0
    return 20.0 * math.log10(float(value) / 32768.0)
