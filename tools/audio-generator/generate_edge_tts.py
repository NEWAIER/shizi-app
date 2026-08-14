#!/usr/bin/env python3
"""Generate and audit real MP3 audition files with edge-tts.

This is a development-only tool. It never belongs in the Android runtime and it
never writes placeholder files: failed output is deleted and the process exits 1.
"""
from __future__ import annotations

import argparse
import asyncio
import csv
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

import edge_tts
from mutagen.mp3 import MP3


def args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--voice", required=True)
    parser.add_argument("--rate", default="-8%")
    parser.add_argument("--volume", default="+0%")
    parser.add_argument("--pitch", default="+0Hz")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--index", type=Path, help="Markdown index path")
    parser.add_argument("--manifest", type=Path, help="JSON manifest path")
    return parser.parse_args()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def non_silent_mp3(path: Path) -> bool:
    # A real encoded MP3 must parse and contain frame payload beyond its header.
    header = path.read_bytes()[:4]
    has_id3 = header[:3] == b"ID3"
    # MPEG audio frames start with eleven 1 bits. Edge may emit FFF3/FFF2 rather
    # than only FFFB, so accept every valid Layer III frame sync variant.
    has_mpeg_frame = len(header) >= 2 and header[0] == 0xFF and (header[1] & 0xE0) == 0xE0
    return path.stat().st_size > 1024 and (has_id3 or has_mpeg_frame)


def decoded_audio_is_not_silent(path: Path) -> bool:
    """Decode the file and reject audio with no meaningful PCM volume.

    ffmpeg is a development-machine prerequisite only.  It is never bundled in
    the Android app; edge-tts output is still a local MP3 asset.
    """
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        raise RuntimeError("ffmpeg is required to verify decoded audio is not silent")
    result = subprocess.run(
        [ffmpeg, "-hide_banner", "-i", str(path), "-af", "volumedetect", "-f", "null", os.devnull],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=30,
        check=False,
    )
    output = result.stdout + result.stderr
    match = re.search(r"mean_volume:\s*(-?[\d.]+) dB", output)
    if result.returncode != 0 or not match:
        raise RuntimeError(f"could not measure decoded audio volume: {path}")
    return float(match.group(1)) > -90.0


async def synthesize(text: str, target: Path, voice: str, rate: str, volume: str, pitch: str) -> None:
    communicator = edge_tts.Communicate(text=text, voice=voice, rate=rate, volume=volume, pitch=pitch)
    await communicator.save(str(target))


async def generate_one(row: dict[str, str], options: argparse.Namespace) -> dict[str, object]:
    target = options.output_dir / row["filename"]
    target.parent.mkdir(parents=True, exist_ok=True)
    generated_at = datetime.now(timezone.utc).isoformat()
    if target.exists() and not options.force:
        try:
            audio = MP3(target)
            if non_silent_mp3(target) and audio.info.length > 0.3:
                return report(row, target, options, "EXISTING_VALID", generated_at)
        except Exception:
            pass
        target.unlink(missing_ok=True)
    last_error = None
    for attempt in range(1, 4):
        try:
            target.unlink(missing_ok=True)
            await synthesize(row["text"], target, options.voice, options.rate, options.volume, options.pitch)
            return report(row, target, options, "GENERATED", generated_at)
        except Exception as exc:  # keep the original error in the manifest
            last_error = f"attempt {attempt}: {type(exc).__name__}: {exc}"
            target.unlink(missing_ok=True)
            if attempt < 3:
                await asyncio.sleep(attempt)
    return {"id": row["id"], "text": row["text"], "outputPath": str(target), "voice": options.voice, "rate": options.rate, "volume": options.volume, "pitch": options.pitch, "generationStatus": "FAILED", "error": last_error, "generatedAt": generated_at}


def report(row: dict[str, str], target: Path, options: argparse.Namespace, status: str, generated_at: str) -> dict[str, object]:
    audio = MP3(target)
    if not non_silent_mp3(target) or not decoded_audio_is_not_silent(target):
        target.unlink(missing_ok=True)
        raise RuntimeError(f"not a non-silent MP3: {target}")
    duration_ms = round(audio.info.length * 1000)
    if duration_ms <= 300:
        target.unlink(missing_ok=True)
        raise RuntimeError(f"duration is too short: {target}")
    return {"id": row["id"], "text": row["text"], "outputPath": str(target), "voice": options.voice, "rate": options.rate, "volume": options.volume, "pitch": options.pitch, "fileSize": target.stat().st_size, "durationMs": duration_ms, "sha256": sha256(target), "generationStatus": status, "generatedAt": generated_at, "edgeTtsVersion": edge_tts.__version__}


def write_reports(items: list[dict[str, object]], options: argparse.Namespace) -> None:
    manifest = options.manifest or options.output_dir / "audition_manifest.json"
    index = options.index or options.output_dir / "AUDITION_INDEX.md"
    manifest.parent.mkdir(parents=True, exist_ok=True)
    manifest.write_text(json.dumps(items, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lines = ["# Edge TTS 试听包", "", "以下项目必须由真人试听确认：单字声调；‘一’是否为 yī；‘一个’与‘一天’的自然连读；4—7岁儿童语速；声音温和清楚；首尾是否截断；是否有噪声或异常停顿。", "", "| 文件 | 状态 | 大小 | 时长(ms) | SHA-256 |", "|---|---:|---:|---:|---|"]
    for item in items:
        lines.append(f"| {Path(str(item['outputPath'])).name} | {item['generationStatus']} | {item.get('fileSize', '')} | {item.get('durationMs', '')} | {item.get('sha256', '')} |")
    index.write_text("\n".join(lines) + "\n", encoding="utf-8")


async def main() -> int:
    options = args()
    with options.input.open(encoding="utf-8", newline="") as file:
        rows = list(csv.DictReader(file))
    required = {"id", "text", "filename"}
    if not rows or set(rows[0]) != required:
        raise ValueError(f"CSV header must be exactly {sorted(required)}")
    items = []
    for row in rows:  # serial requests, with a small polite gap
        items.append(await generate_one(row, options))
        await asyncio.sleep(0.4)
    write_reports(items, options)
    failures = [item for item in items if item["generationStatus"] == "FAILED"]
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
