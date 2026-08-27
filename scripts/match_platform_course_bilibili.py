#!/usr/bin/env python3
"""Match platform courses to Bilibili videos and generate chapter repair SQL."""

from __future__ import annotations

import argparse
import html
import json
import os
import re
import subprocess
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass
from difflib import SequenceMatcher
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "outputs" / "platform_course_bilibili"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
)

MANUAL_CHAPTER_OVERRIDES: dict[int, list[tuple[str, str]]] = {
    13: [
        ("后端技术栈总览：Spring、Docker、Redis、MySQL、Kubernetes", "https://www.bilibili.com/video/BV1fZTi69EGR?p=1"),
        ("Docker环境下常用应用部署：MySQL、Redis、RabbitMQ", "https://www.bilibili.com/video/BV1dm411R7Vj?p=1"),
        ("Docker + SpringCloud 微服务部署", "https://www.bilibili.com/video/BV1s5411j7zq?p=1"),
    ],
    19: [
        ("Python入门半小时，剩下靠AI", "https://www.bilibili.com/video/BV1xHn9z8EPX?p=1"),
        ("Cursor 保姆级教程：用 AI 写 Python 代码", "https://www.bilibili.com/video/BV1qyAqzZEGg?p=1"),
        ("黑马程序员 Python + AI 零基础入门", "https://www.bilibili.com/video/BV1sHU9BmEne?p=1"),
    ],
    26: [
        ("2025徐老师Vue3全家桶课程+大型项目实战", "https://www.bilibili.com/video/BV1BPAjzvEbG?p=1"),
        ("Vue3极简2025版教程", "https://www.bilibili.com/video/BV13tjqzmEDZ?p=1"),
        ("尚硅谷Vue3入门到实战", "https://www.bilibili.com/video/BV1Za4y1r7KE?p=1"),
    ],
    32: [
        ("2025徐老师Vue3能源综合管理平台项目实战", "https://www.bilibili.com/video/BV1AnkWBLEvA?p=1"),
        ("Vue3+Vite+Element-Plus商城后台管理系统", "https://www.bilibili.com/video/BV1bE15BiEcb?p=1"),
        ("前端Vue3实战项目：网易云音乐APP开发", "https://www.bilibili.com/video/BV17ZzLBTE5C?p=1"),
    ],
    38: [
        ("零基础人工智能入门：深度学习+PyTorch", "https://www.bilibili.com/video/BV1K14y1c75e?p=1"),
        ("PyTorch深度学习快速入门教程", "https://www.bilibili.com/video/BV1hE411t7RN?p=1"),
        ("黑马程序员神经网络与深度学习课程", "https://www.bilibili.com/video/BV1c5yrBcEEX?p=1"),
    ],
    42: [
        ("人工智能导论4小时期末速成课", "https://www.bilibili.com/video/BV1qCkbYsEQs?p=1"),
        ("人工智能导论3小时期末速成", "https://www.bilibili.com/video/BV11tDPB7EQz?p=1"),
        ("神经网络与深度学习基础", "https://www.bilibili.com/video/BV1c5yrBcEEX?p=1"),
    ],
    52: [
        ("10分钟速成Java", "https://www.bilibili.com/video/BV1Ee411H7mT?p=1"),
        ("Java语言程序设计3小时期末速成课", "https://www.bilibili.com/video/BV1yw411E7Db?p=1"),
        ("黑马程序员Java零基础视频教程", "https://www.bilibili.com/video/BV17F411T7Ao?p=1"),
    ],
}


@dataclass
class Course:
    id: int
    name: str
    cover_img: str


def clean_text(value: str) -> str:
    value = html.unescape(value or "")
    value = re.sub(r"<[^>]+>", "", value)
    value = re.sub(r"\s+", " ", value)
    return value.strip()


def console_text(value: str) -> str:
    return value.encode("gbk", errors="replace").decode("gbk")


def cover_key(url: str) -> str:
    path = urllib.parse.urlparse((url or "").replace("//", "https://", 1)).path
    return Path(path).name.lower()


def request_json(url: str) -> dict[str, Any]:
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Referer": "https://www.bilibili.com/",
            "Accept": "application/json,text/plain,*/*",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            "Origin": "https://www.bilibili.com",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception:
        env = os.environ.copy()
        env["BILI_URL"] = url
        ps = (
            "$ProgressPreference='SilentlyContinue';"
            "$headers=@{"
            "'User-Agent'='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36';"
            "'Referer'='https://www.bilibili.com/';"
            "'Accept'='application/json,text/plain,*/*';"
            "'Accept-Language'='zh-CN,zh;q=0.9,en;q=0.8'"
            "};"
            "(Invoke-WebRequest -Uri $env:BILI_URL -Headers $headers -TimeoutSec 25).Content"
        )
        completed = subprocess.run(
            ["powershell", "-NoProfile", "-Command", ps],
            capture_output=True,
            text=True,
            encoding="utf-8",
            env=env,
            check=True,
        )
        return json.loads(completed.stdout)


def compact_keyword(keyword: str, max_len: int = 48) -> str:
    value = clean_text(keyword)
    value = re.sub(r"【[^】]*】", " ", value)
    value = re.sub(r"\([^)]*\)|（[^）]*）", " ", value)
    value = re.sub(r"[!！,，。:：;；|｜_《》“”\"'、]+", " ", value)
    value = re.sub(r"\s+", " ", value).strip()
    return value[:max_len].strip() or clean_text(keyword)[:max_len].strip()


def mysql_environment() -> dict[str, str]:
    password = os.getenv("DB_PASSWORD")
    if not password:
        raise RuntimeError(
            "DB_PASSWORD must be set before running the course matching script."
        )

    environment = os.environ.copy()
    environment["MYSQL_PWD"] = password
    return environment


def mysql_query(mysql: str, database: str, sql: str) -> str:
    args = [
        mysql,
        "--host=127.0.0.1",
        "--port=3306",
        "--user=root",
        f"--database={database}",
        "--default-character-set=utf8mb4",
        "--batch",
        "--raw",
        "--skip-column-names",
        f"--execute={sql}",
    ]
    completed = subprocess.run(
        args,
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
        env=mysql_environment(),
    )
    return completed.stdout


def load_courses(mysql: str, database: str) -> list[Course]:
    sql = (
        "SELECT id, REPLACE(REPLACE(name, CHAR(13), ' '), CHAR(10), ' ') AS name, coverImg "
        "FROM course "
        "WHERE sourceType='platform' AND type='video' AND isDelete=0 "
        "ORDER BY id"
    )
    rows: list[Course] = []
    for line in mysql_query(mysql, database, sql).splitlines():
        if not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        rows.append(Course(id=int(parts[0]), name=parts[1].strip(), cover_img=parts[2].strip()))
    return rows


def search_bilibili(keyword: str) -> list[dict[str, Any]]:
    candidates = [keyword, compact_keyword(keyword), compact_keyword(keyword, 28), compact_keyword(keyword, 18)]
    last_error: Exception | None = None
    for candidate in dict.fromkeys(item for item in candidates if item):
        encoded = urllib.parse.quote(candidate)
        url = f"https://api.bilibili.com/x/web-interface/search/type?search_type=video&keyword={encoded}"
        for attempt in range(3):
            try:
                data = request_json(url)
                if data.get("code") != 0:
                    raise RuntimeError(data.get("message"))
                return [
                    item
                    for item in data.get("data", {}).get("result", [])
                    if item.get("type") == "video" and item.get("bvid")
                ]
            except Exception as exc:
                last_error = exc
                time.sleep(2 + attempt * 4)
    raise RuntimeError(f"Bilibili search failed for {keyword}: {last_error}")


def fetch_pages(bvid: str) -> list[dict[str, Any]]:
    data = request_json(f"https://api.bilibili.com/x/web-interface/view?bvid={urllib.parse.quote(bvid)}")
    if data.get("code") != 0:
        raise RuntimeError(f"Bilibili view failed for {bvid}: {data.get('message')}")
    return data.get("data", {}).get("pages", []) or []


def score_result(course: Course, item: dict[str, Any]) -> float:
    item_title = clean_text(str(item.get("title", "")))
    title_score = SequenceMatcher(None, course.name.lower(), item_title.lower()).ratio()
    same_cover = cover_key(course.cover_img) and cover_key(course.cover_img) == cover_key(str(item.get("pic", "")))
    author_bonus = 0.08 if any(token in course.name for token in ("黑马", "尚硅谷", "韩顺平", "千锋", "动力节点")) and any(
        token in str(item.get("author", "")) for token in ("黑马", "尚硅谷", "韩顺平", "千锋", "动力节点")
    ) else 0
    play_bonus = min(float(item.get("play") or 0) / 50_000_000, 0.06)
    return title_score + (0.75 if same_cover else 0) + author_bonus + play_bonus


def choose_best(course: Course, results: list[dict[str, Any]]) -> dict[str, Any]:
    if not results:
        raise RuntimeError(f"No Bilibili results for course {course.id} {course.name}")
    return max(results, key=lambda item: score_result(course, item))


def page_url(bvid: str, page: int) -> str:
    return f"https://www.bilibili.com/video/{bvid}?p={page}"


def bvid_from_url(url: str) -> str:
    match = re.search(r"/video/(BV[a-zA-Z0-9]+)", url)
    return match.group(1) if match else ""


def manual_chapters(course_id: int) -> list[dict[str, Any]] | None:
    override = MANUAL_CHAPTER_OVERRIDES.get(course_id)
    if not override:
        return None
    return [
        {
            "title": title[:100],
            "videoUrl": url,
            "sortOrder": index,
            "bvid": bvid_from_url(url),
            "sourceTitle": title,
        }
        for index, (title, url) in enumerate(override, start=1)
    ]


def build_chapters(course: Course, best: dict[str, Any], results: list[dict[str, Any]]) -> list[dict[str, Any]]:
    override = manual_chapters(course.id)
    if override:
        return override

    bvid = str(best["bvid"])
    pages = fetch_pages(bvid)
    chapters: list[dict[str, Any]] = []

    for idx, page in enumerate(pages[:3], start=1):
        part = clean_text(str(page.get("part") or f"P{idx}"))
        chapters.append(
            {
                "title": part[:100],
                "videoUrl": page_url(bvid, idx),
                "sortOrder": idx,
                "bvid": bvid,
                "sourceTitle": clean_text(str(best.get("title", ""))),
            }
        )

    if len(chapters) < 3:
        seen = {bvid}
        for item in results:
            other_bvid = str(item.get("bvid") or "")
            if not other_bvid or other_bvid in seen:
                continue
            seen.add(other_bvid)
            chapters.append(
                {
                    "title": clean_text(str(item.get("title", "")))[:100],
                    "videoUrl": page_url(other_bvid, 1),
                    "sortOrder": len(chapters) + 1,
                    "bvid": other_bvid,
                    "sourceTitle": clean_text(str(item.get("title", ""))),
                }
            )
            if len(chapters) == 3:
                break

    while len(chapters) < 3:
        chapters.append(
            {
                "title": f"{course.name} P{len(chapters) + 1}"[:100],
                "videoUrl": page_url(bvid, min(len(chapters) + 1, max(len(pages), 1))),
                "sortOrder": len(chapters) + 1,
                "bvid": bvid,
                "sourceTitle": clean_text(str(best.get("title", ""))),
            }
        )
    return chapters[:3]


def apply_manual_overrides(matches: list[dict[str, Any]]) -> None:
    for item in matches:
        override = manual_chapters(int(item["courseId"]))
        if override:
            item["chapters"] = override


def sql_quote(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def generate_sql(matches: list[dict[str, Any]]) -> str:
    lines = [
        "-- Generated by scripts/match_platform_course_bilibili.py",
        "-- Stores Bilibili page URLs in course_chapter.video_url.",
        "START TRANSACTION;",
    ]
    for item in matches:
        course_id = int(item["courseId"])
        chapters = item["chapters"]
        lines.append(f"UPDATE course_chapter SET is_delete = 1, update_time = NOW() WHERE course_id = {course_id};")
        for chapter in chapters:
            lines.append(
                "INSERT INTO course_chapter "
                "(course_id, title, video_url, sort_order, create_time, update_time, is_delete) VALUES "
                f"({course_id}, {sql_quote(chapter['title'])}, {sql_quote(chapter['videoUrl'])}, "
                f"{int(chapter['sortOrder'])}, NOW(), NOW(), 0);"
            )
        lines.append(
            f"UPDATE course SET videoUrl = {sql_quote(chapters[0]['videoUrl'])}, updateTime = NOW() "
            f"WHERE id = {course_id};"
        )
    lines.append("COMMIT;")
    return "\n".join(lines) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mysql", default="mysql")
    parser.add_argument("--database", default="teach_platform")
    parser.add_argument("--apply", action="store_true", help="Execute the generated SQL against the local database.")
    args = parser.parse_args()

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    courses = load_courses(args.mysql, args.database)
    json_path = OUT_DIR / "matches.json"
    sql_path = OUT_DIR / "replace_with_bilibili_urls.sql"
    matches: list[dict[str, Any]] = []
    if json_path.exists():
        matches = json.loads(json_path.read_text(encoding="utf-8"))
        apply_manual_overrides(matches)
    matched_course_ids = {int(item["courseId"]) for item in matches}

    for index, course in enumerate(courses, start=1):
        if course.id in matched_course_ids:
            print(f"[{index}/{len(courses)}] course {course.id}: cached")
            continue
        results = search_bilibili(course.name)
        best = choose_best(course, results)
        chapters = build_chapters(course, best, results)
        match = {
            "courseId": course.id,
            "courseName": course.name,
            "courseCover": course.cover_img,
            "matchedBvid": best["bvid"],
            "matchedTitle": clean_text(str(best.get("title", ""))),
            "matchedAuthor": best.get("author"),
            "matchedCover": str(best.get("pic", "")),
            "matchScore": round(score_result(course, best), 4),
            "sourceUrl": f"https://www.bilibili.com/video/{best['bvid']}",
            "chapters": chapters,
        }
        matches.append(match)
        matched_course_ids.add(course.id)
        json_path.write_text(json.dumps(matches, ensure_ascii=False, indent=2), encoding="utf-8")
        sql_path.write_text(generate_sql(matches), encoding="utf-8")
        print(f"[{index}/{len(courses)}] course {course.id}: {console_text(course.name)} -> {best['bvid']}")
        time.sleep(0.8)

    json_path.write_text(json.dumps(matches, ensure_ascii=False, indent=2), encoding="utf-8")
    sql_path.write_text(generate_sql(matches), encoding="utf-8")
    print(f"Wrote {json_path}")
    print(f"Wrote {sql_path}")

    if args.apply:
        subprocess.run(
            [
                args.mysql,
                "--host=127.0.0.1",
                "--port=3306",
                "--user=root",
                f"--database={args.database}",
                "--default-character-set=utf8mb4",
            ],
            input=sql_path.read_bytes(),
            check=True,
            cwd=ROOT,
            env=mysql_environment(),
        )
        print("Applied generated SQL.")


if __name__ == "__main__":
    main()
