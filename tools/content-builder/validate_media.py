#!/usr/bin/env python3
"""Strict offline validation for candidate MP3/WebP media.

Headers alone are never trusted. Every item is probed and fully decoded with
FFmpeg; WebP colour mode is additionally inspected with Pillow.
"""
from __future__ import annotations

import csv
import io
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path, PurePosixPath, PureWindowsPath
from typing import Any

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "content-source" / "starter-thirty-v1" / "generated" / "media_requirements.csv"
PACK = ROOT / "app" / "src" / "main" / "assets" / "content" / "packs" / "starter-thirty-v1" / "2.0.0"
MAX_WEBP_BYTES = 500 * 1024
MIN_DURATION_SECONDS = 0.3


class MediaValidationError(RuntimeError):
    pass


def safe_asset_path(value: str) -> Path:
    if not value or "\\" in value:
        raise MediaValidationError(f"asset path must be a relative POSIX path: {value!r}")
    path, windows_path = PurePosixPath(value), PureWindowsPath(value)
    if path.is_absolute() or windows_path.is_absolute() or windows_path.drive or any(part in ("", ".", "..") for part in path.parts):
        raise MediaValidationError(f"asset path escapes pack: {value!r}")
    return Path(*path.parts)


def executable(name: str) -> str:
    value = shutil.which(name)
    if not value:
        raise MediaValidationError(f"{name} is required for real media validation")
    return value


def run(command: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30, check=False)


def ffprobe(path: Path) -> dict[str, Any]:
    result = run([executable("ffprobe"), "-v", "error", "-show_streams", "-show_format", "-of", "json", str(path)])
    if result.returncode != 0:
        raise MediaValidationError(f"ffprobe rejected {path}: {result.stderr.strip()}")
    try:
        return json.loads(result.stdout)
    except json.JSONDecodeError as exc:
        raise MediaValidationError(f"ffprobe returned invalid JSON for {path}") from exc


def decode(path: Path) -> None:
    result = run([executable("ffmpeg"), "-v", "error", "-i", str(path), "-f", "null", os.devnull])
    if result.returncode != 0:
        raise MediaValidationError(f"ffmpeg decode rejected {path}: {result.stderr.strip()}")


def number(value: Any, name: str, path: Path) -> float:
    try:
        return float(value)
    except (TypeError, ValueError) as exc:
        raise MediaValidationError(f"missing {name} in media metadata: {path}") from exc


def mean_volume_db(path: Path) -> float:
    result = run([executable("ffmpeg"), "-hide_banner", "-i", str(path), "-af", "volumedetect", "-f", "null", os.devnull])
    match = re.search(r"mean_volume:\s*(-?[\d.]+) dB", result.stdout + result.stderr)
    if result.returncode != 0 or not match:
        raise MediaValidationError(f"could not measure decoded audio volume: {path}")
    return float(match.group(1))


def audio_stream(details: dict[str, Any], path: Path) -> dict[str, Any]:
    stream = next((item for item in details.get("streams", []) if item.get("codec_type") == "audio"), None)
    if not stream or stream.get("codec_name") != "mp3":
        raise MediaValidationError(f"not an MP3 audio stream: {path}")
    return stream


def validate_mp3(path: Path) -> None:
    if not path.is_file() or path.stat().st_size <= 1024:
        raise MediaValidationError(f"missing or too-small MP3: {path}")
    details = ffprobe(path)
    stream = audio_stream(details, path)
    duration = number(details.get("format", {}).get("duration"), "duration", path)
    bitrate = number(stream.get("bit_rate") or details.get("format", {}).get("bit_rate"), "bit_rate", path)
    if duration <= MIN_DURATION_SECONDS:
        raise MediaValidationError(f"MP3 duration must exceed {MIN_DURATION_SECONDS}s: {path}")
    if int(stream.get("channels", 0)) != 1:
        raise MediaValidationError(f"MP3 must be mono: {path}")
    if int(stream.get("sample_rate", 0)) != 44100:
        raise MediaValidationError(f"MP3 must be 44100Hz: {path}")
    if not 96000 <= bitrate <= 128000:
        raise MediaValidationError(f"MP3 bitrate must be 96-128kbps: {path}")
    decode(path)
    if mean_volume_db(path) <= -90.0:
        raise MediaValidationError(f"MP3 is silent: {path}")


def validate_webp(path: Path) -> None:
    if not path.is_file() or path.stat().st_size <= 0:
        raise MediaValidationError(f"missing WebP: {path}")
    if path.stat().st_size > MAX_WEBP_BYTES:
        raise MediaValidationError(f"WebP exceeds {MAX_WEBP_BYTES} bytes: {path}")
    details = ffprobe(path)
    stream = next((item for item in details.get("streams", []) if item.get("codec_type") == "video" and item.get("codec_name") == "webp"), None)
    if not stream:
        raise MediaValidationError(f"not a WebP video stream: {path}")
    if int(stream.get("width", 0)) != 1024 or int(stream.get("height", 0)) != 1024:
        raise MediaValidationError(f"WebP must be 1024x1024: {path}")
    decode(path)
    try:
        with Image.open(io.BytesIO(path.read_bytes())) as image:
            image.load()
            if image.format != "WEBP" or image.size != (1024, 1024) or image.mode not in ("RGB", "RGBA"):
                raise MediaValidationError(f"WebP must decode as 1024x1024 RGB/RGBA: {path}")
    except MediaValidationError:
        raise
    except Exception as exc:
        raise MediaValidationError(f"Pillow rejected WebP: {path}") from exc


def validate_file(path: Path) -> None:
    if path.suffix.lower() == ".mp3":
        validate_mp3(path)
    elif path.suffix.lower() == ".webp":
        validate_webp(path)
    else:
        raise MediaValidationError(f"unsupported candidate media type: {path}")


def requirements() -> list[str]:
    with SOURCE.open(encoding="utf-8", newline="") as file:
        return [row["asset_path"] for row in csv.DictReader(file)]


def candidate_files() -> set[str]:
    if not PACK.exists():
        return set()
    return {path.relative_to(PACK).as_posix() for folder in (PACK / "audio", PACK / "images") if folder.is_dir() for path in folder.rglob("*") if path.is_file()}


def main() -> None:
    if not SOURCE.exists():
        sys.exit("Run build_starter_thirty.py first.")
    errors: list[str] = []
    listed = requirements()
    duplicates = sorted({item for item in listed if listed.count(item) > 1})
    if duplicates:
        errors.extend(f"duplicate resource reference: {item}" for item in duplicates)
    expected: set[str] = set()
    for value in listed:
        try:
            relative = safe_asset_path(value).as_posix()
            expected.add(relative)
            validate_file(PACK / relative)
        except MediaValidationError as exc:
            errors.append(str(exc))
    unused = sorted(candidate_files() - expected)
    errors.extend(f"unreferenced resource: {item}" for item in unused)
    if errors:
        sys.exit("Candidate media validation failed:\n" + "\n".join(errors))
    print("Candidate media ffprobe/decode/specification validation: OK")


if __name__ == "__main__":
    main()
