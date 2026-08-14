#!/usr/bin/env python3
"""Generate auditable local MP3 files with Edge TTS (development only).

The Android app never imports this tool or calls a network TTS service.  Each
generated file has a stable provenance fingerprint.  Existing files are reused
only when their recorded fingerprint *and* SHA-256 still match the requested
text, voice and prosody settings; otherwise --force is required.
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
import tempfile
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath, PureWindowsPath
from typing import Any

import edge_tts
from mutagen.mp3 import MP3

ROOT = Path(__file__).resolve().parents[2]
CSV_FIELDS = ("id", "text", "filename")
RETRY_COUNT = 3


def args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--voice", required=True)
    parser.add_argument("--rate", default="-8%")
    parser.add_argument("--volume", default="+0%")
    parser.add_argument("--pitch", default="+0Hz")
    parser.add_argument("--force", action="store_true", help="Permit replacing a mismatched existing MP3")
    parser.add_argument("--normalize-existing", action="store_true", help="Normalize verified legacy MP3s locally without Edge TTS")
    parser.add_argument("--index", type=Path, help="Markdown index path")
    parser.add_argument("--manifest", type=Path, help="JSON manifest path")
    return parser.parse_args()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonical_filename(value: str) -> str:
    """Accept only a portable relative MP3 path below --output-dir."""
    if not value or "\\" in value:
        raise ValueError(f"filename must use a relative POSIX path: {value!r}")
    path = PurePosixPath(value)
    windows_path = PureWindowsPath(value)
    if path.is_absolute() or windows_path.is_absolute() or windows_path.drive or any(part in ("", ".", "..") for part in path.parts):
        raise ValueError(f"filename escapes output directory: {value!r}")
    if path.suffix.lower() != ".mp3":
        raise ValueError(f"filename must end in .mp3: {value!r}")
    return path.as_posix()


def load_rows(source: Path) -> list[dict[str, str]]:
    with source.open(encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)
        if tuple(reader.fieldnames or ()) != CSV_FIELDS:
            raise ValueError(f"CSV header must be exactly {CSV_FIELDS}")
        rows = list(reader)
    if not rows:
        raise ValueError("CSV must contain at least one row")
    ids: set[str] = set()
    filenames: set[str] = set()
    for row in rows:
        row["id"] = row["id"].strip()
        row["text"] = row["text"].strip()
        row["filename"] = canonical_filename(row["filename"].strip())
        if not row["id"] or not row["text"]:
            raise ValueError("CSV id and text must not be empty")
        if row["id"] in ids:
            raise ValueError(f"duplicate CSV id: {row['id']}")
        if row["filename"] in filenames:
            raise ValueError(f"duplicate CSV filename: {row['filename']}")
        ids.add(row["id"])
        filenames.add(row["filename"])
    return rows


def output_target(output_dir: Path, filename: str) -> Path:
    root = output_dir.resolve()
    target = (root / filename).resolve()
    if target != root and root not in target.parents:
        raise ValueError(f"output path escapes output directory: {filename!r}")
    return target


def portable_output_path(target: Path, output_dir: Path) -> str:
    """Use POSIX relative paths so reports are reproducible on Windows/Linux."""
    try:
        return target.resolve().relative_to(ROOT.resolve()).as_posix()
    except ValueError:
        return target.resolve().relative_to(output_dir.resolve().parent).as_posix()


def provenance(row: dict[str, str], options: argparse.Namespace) -> str:
    payload = {
        "id": row["id"], "text": row["text"], "filename": row["filename"],
        "voice": options.voice, "rate": options.rate, "volume": options.volume,
        "pitch": options.pitch, "edgeTtsVersion": edge_tts.__version__,
    }
    return hashlib.sha256(json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")).hexdigest()


def manifest_path(options: argparse.Namespace) -> Path:
    return options.manifest or options.output_dir / "audition_manifest.json"


def failed_items_path(options: argparse.Namespace) -> Path:
    return manifest_path(options).with_name("failed_items.json")


def atomic_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(f".{path.name}.tmp")
    temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def load_existing(options: argparse.Namespace) -> dict[str, dict[str, Any]]:
    path = manifest_path(options)
    if not path.is_file():
        return {}
    try:
        raw = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ValueError(f"invalid existing manifest: {path}: {exc}") from exc
    if not isinstance(raw, list):
        raise ValueError(f"existing manifest must be a JSON list: {path}")
    return {str(item.get("id")): item for item in raw if isinstance(item, dict) and item.get("id")}


def decoded_audio_is_not_silent(path: Path) -> bool:
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        raise RuntimeError("ffmpeg is required to verify decoded audio is not silent")
    result = subprocess.run(
        [ffmpeg, "-hide_banner", "-i", str(path), "-af", "volumedetect", "-f", "null", os.devnull],
        capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30, check=False,
    )
    match = re.search(r"mean_volume:\s*(-?[\d.]+) dB", result.stdout + result.stderr)
    if result.returncode != 0 or not match:
        raise RuntimeError(f"could not decode and measure MP3: {path}")
    return float(match.group(1)) > -90.0


def probe_mp3(path: Path) -> dict[str, Any]:
    ffprobe = shutil.which("ffprobe")
    if not ffprobe:
        raise RuntimeError("ffprobe is required to verify MP3 format")
    result = subprocess.run([ffprobe, "-v", "error", "-show_streams", "-show_format", "-of", "json", str(path)], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30, check=False)
    if result.returncode != 0:
        raise RuntimeError(f"ffprobe rejected MP3: {path}")
    return json.loads(result.stdout)


def validate_mp3(path: Path) -> int:
    if not path.is_file() or path.stat().st_size <= 1024:
        raise RuntimeError(f"MP3 is missing or too small: {path}")
    details = probe_mp3(path)
    stream = next((item for item in details.get("streams", []) if item.get("codec_type") == "audio" and item.get("codec_name") == "mp3"), None)
    duration_ms = round(MP3(path).info.length * 1000)
    bitrate = int((stream or {}).get("bit_rate") or details.get("format", {}).get("bit_rate") or 0)
    if not stream or duration_ms <= 300 or int(stream.get("channels", 0)) != 1 or int(stream.get("sample_rate", 0)) != 44100 or not 96000 <= bitrate <= 128000 or not decoded_audio_is_not_silent(path):
        raise RuntimeError(f"MP3 is invalid, too short, or silent: {path}")
    return duration_ms


def normalize_mp3(source: Path, target: Path) -> None:
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        raise RuntimeError("ffmpeg is required to normalize MP3")
    result = subprocess.run([ffmpeg, "-y", "-i", str(source), "-map", "0:a:0", "-vn", "-ac", "1", "-ar", "44100", "-c:a", "libmp3lame", "-b:a", "96k", str(target)], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60, check=False)
    if result.returncode != 0:
        raise RuntimeError(f"ffmpeg normalization failed: {source}")
    validate_mp3(target)


def legacy_metadata_matches(entry: dict[str, Any], row: dict[str, str], options: argparse.Namespace) -> bool:
    """One-time migration path for the v1 audition manifest without a fingerprint."""
    return all(entry.get(key) == value for key, value in {
        "id": row["id"], "text": row["text"], "voice": options.voice,
        "rate": options.rate, "volume": options.volume, "pitch": options.pitch,
    }.items())


def can_reuse(entry: dict[str, Any] | None, row: dict[str, str], target: Path, options: argparse.Namespace) -> bool:
    if not entry or not target.is_file():
        return False
    fingerprint = provenance(row, options)
    fingerprint_matches = entry.get("inputFingerprint") == fingerprint
    migratable_v1 = not entry.get("inputFingerprint") and legacy_metadata_matches(entry, row, options)
    if not (fingerprint_matches or migratable_v1):
        return False
    if entry.get("sha256") != sha256(target):
        return False
    try:
        validate_mp3(target)
        return True
    except RuntimeError:
        return False


def metadata_and_hash_match(entry: dict[str, Any] | None, row: dict[str, str], target: Path, options: argparse.Namespace) -> bool:
    if not entry or not target.is_file() or entry.get("sha256") != sha256(target):
        return False
    return entry.get("inputFingerprint") == provenance(row, options) or (not entry.get("inputFingerprint") and legacy_metadata_matches(entry, row, options))


async def synthesize(text: str, target: Path, options: argparse.Namespace) -> None:
    communicator = edge_tts.Communicate(text=text, voice=options.voice, rate=options.rate, volume=options.volume, pitch=options.pitch)
    await communicator.save(str(target))


def record(row: dict[str, str], target: Path, options: argparse.Namespace, status: str, generated_at: str | None) -> dict[str, Any]:
    return {
        "id": row["id"], "text": row["text"], "outputPath": portable_output_path(target, options.output_dir),
        "voice": options.voice, "rate": options.rate, "volume": options.volume, "pitch": options.pitch,
        "fileSize": target.stat().st_size, "durationMs": validate_mp3(target), "sha256": sha256(target),
        "inputFingerprint": provenance(row, options), "generationStatus": status,
        "generatedAt": generated_at, "edgeTtsVersion": edge_tts.__version__,
    }


async def generate_one(row: dict[str, str], options: argparse.Namespace, existing: dict[str, Any] | None) -> dict[str, Any]:
    target = output_target(options.output_dir, row["filename"])
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.exists() and can_reuse(existing, row, target, options):
        return record(row, target, options, "REUSED_VERIFIED", existing.get("generatedAt"))
    if target.exists() and options.normalize_existing and metadata_and_hash_match(existing, row, target, options):
        temporary = target.with_name(f".{target.name}.normalized.tmp.mp3")
        try:
            normalize_mp3(target, temporary)
            temporary.replace(target)
            return record(row, target, options, "NORMALIZED_EXISTING", existing.get("generatedAt"))
        finally:
            temporary.unlink(missing_ok=True)
    if target.exists() and not options.force:
        return {"id": row["id"], "text": row["text"], "outputPath": portable_output_path(target, options.output_dir), "voice": options.voice, "rate": options.rate, "volume": options.volume, "pitch": options.pitch, "inputFingerprint": provenance(row, options), "generationStatus": "FAILED", "generatedAt": None, "error": "existing file provenance does not match; rerun with --force to replace it"}

    raw = target.with_name(f".{target.name}.edge-tts.raw.mp3")
    temporary = target.with_name(f".{target.name}.edge-tts.normalized.tmp.mp3")
    last_error = "unknown error"
    for attempt in range(1, RETRY_COUNT + 1):
        try:
            raw.unlink(missing_ok=True)
            temporary.unlink(missing_ok=True)
            await synthesize(row["text"], raw, options)
            normalize_mp3(raw, temporary)
            temporary.replace(target)  # Atomic replacement; retain prior file on failures.
            raw.unlink(missing_ok=True)
            return record(row, target, options, "GENERATED", datetime.now(timezone.utc).isoformat())
        except Exception as exc:
            last_error = f"attempt {attempt}: {type(exc).__name__}: {exc}"
            raw.unlink(missing_ok=True)
            temporary.unlink(missing_ok=True)
            if attempt < RETRY_COUNT:
                await asyncio.sleep(attempt)
    return {"id": row["id"], "text": row["text"], "outputPath": portable_output_path(target, options.output_dir), "voice": options.voice, "rate": options.rate, "volume": options.volume, "pitch": options.pitch, "inputFingerprint": provenance(row, options), "generationStatus": "FAILED", "generatedAt": None, "error": last_error}


def write_reports(items: list[dict[str, Any]], options: argparse.Namespace) -> None:
    manifest = manifest_path(options)
    index = options.index or options.output_dir / "AUDITION_INDEX.md"
    atomic_json(manifest, items)
    lines = ["# Edge TTS 试听包", "", "以下项目必须由真人试听确认：单字声调；‘一’是否为 yī；‘一个’与‘一天’的自然连读；4—7岁儿童语速；声音温和清楚；首尾是否截断；是否有噪声或异常停顿。", "", "| 文件 | 状态 | 大小 | 时长(ms) | SHA-256 |", "|---|---:|---:|---:|---|"]
    for item in items:
        lines.append(f"| {item['outputPath']} | {item['generationStatus']} | {item.get('fileSize', '')} | {item.get('durationMs', '')} | {item.get('sha256', '')} |")
    temporary = index.with_name(f".{index.name}.tmp")
    temporary.write_text("\n".join(lines) + "\n", encoding="utf-8")
    temporary.replace(index)


async def main() -> int:
    options = args()
    options.output_dir = options.output_dir.resolve()
    rows = load_rows(options.input)
    existing = load_existing(options)
    items = []
    for row in rows:  # Serial requests, with a polite gap. CI never calls this path.
        items.append(await generate_one(row, options, existing.get(row["id"])))
        await asyncio.sleep(0.4)
    failures = [item for item in items if item["generationStatus"] == "FAILED"]
    if failures:
        atomic_json(failed_items_path(options), failures)
        return 1
    write_reports(items, options)
    failed_items_path(options).unlink(missing_ok=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
