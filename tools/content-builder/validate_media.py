#!/usr/bin/env python3
"""Validate candidate media by probing and decoding, never by file headers alone."""
from __future__ import annotations

import csv
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path, PurePosixPath, PureWindowsPath
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "content-source" / "starter-thirty-v1" / "generated" / "media_requirements.csv"
PACK = ROOT / "app" / "src" / "main" / "assets" / "content" / "packs" / "starter-thirty-v1" / "2.0.0"


class MediaValidationError(RuntimeError):
    pass


def safe_asset_path(value: str) -> Path:
    if not value or "\\" in value:
        raise MediaValidationError(f"asset path must be a relative POSIX path: {value!r}")
    path = PurePosixPath(value)
    windows_path = PureWindowsPath(value)
    if path.is_absolute() or windows_path.is_absolute() or windows_path.drive or any(part in ("", ".", "..") for part in path.parts):
        raise MediaValidationError(f"asset path escapes pack: {value!r}")
    return Path(*path.parts)


def executable(name: str) -> str:
    path = shutil.which(name)
    if not path:
        raise MediaValidationError(f"{name} is required for real media validation")
    return path


def ffprobe(path: Path) -> dict[str, Any]:
    result = subprocess.run(
        [executable("ffprobe"), "-v", "error", "-show_streams", "-show_format", "-of", "json", str(path)],
        capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30, check=False,
    )
    if result.returncode != 0:
        raise MediaValidationError(f"ffprobe rejected {path}: {result.stderr.strip()}")
    try:
        return json.loads(result.stdout)
    except json.JSONDecodeError as exc:
        raise MediaValidationError(f"ffprobe returned invalid JSON for {path}") from exc


def decode(path: Path) -> None:
    result = subprocess.run(
        [executable("ffmpeg"), "-v", "error", "-i", str(path), "-f", "null", os.devnull],
        capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30, check=False,
    )
    if result.returncode != 0:
        raise MediaValidationError(f"ffmpeg decode rejected {path}: {result.stderr.strip()}")


def positive_number(value: Any) -> bool:
    try:
        return float(value) > 0
    except (TypeError, ValueError):
        return False


def validate_file(path: Path) -> None:
    if not path.is_file() or path.stat().st_size <= 0:
        raise MediaValidationError(f"missing media: {path}")
    details = ffprobe(path)
    streams = details.get("streams", [])
    suffix = path.suffix.lower()
    if suffix == ".mp3":
        audio = next((stream for stream in streams if stream.get("codec_type") == "audio" and stream.get("codec_name") == "mp3"), None)
        if not audio or not positive_number(details.get("format", {}).get("duration")):
            raise MediaValidationError(f"not a decodable MP3: {path}")
    elif suffix == ".webp":
        image = next((stream for stream in streams if stream.get("codec_type") == "video" and stream.get("codec_name") == "webp"), None)
        if not image or not positive_number(image.get("width")) or not positive_number(image.get("height")):
            raise MediaValidationError(f"not a decodable WebP: {path}")
    else:
        raise MediaValidationError(f"unsupported candidate media type: {path}")
    decode(path)


def main() -> None:
    if not SOURCE.exists():
        sys.exit("Run build_starter_thirty.py first.")
    errors: list[str] = []
    with SOURCE.open(encoding="utf-8", newline="") as file:
        for row in csv.DictReader(file):
            try:
                validate_file(PACK / safe_asset_path(row["asset_path"]))
            except MediaValidationError as exc:
                errors.append(str(exc))
    if errors:
        sys.exit("Candidate media validation failed:\n" + "\n".join(errors))
    print("Candidate media ffprobe and decode validation: OK")


if __name__ == "__main__":
    main()
