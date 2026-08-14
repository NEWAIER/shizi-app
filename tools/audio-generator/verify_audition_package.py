#!/usr/bin/env python3
"""Verify the committed 24-file Edge TTS audition package without networking."""
from __future__ import annotations

import hashlib
import importlib.util
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
AUDITION = ROOT / "artifacts" / "tts-audition"
VOICES = ("zh-CN-XiaoyiNeural", "zh-CN-XiaoxiaoNeural")


def sha256(path: Path) -> str:
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    return digest


def validator():
    spec = importlib.util.spec_from_file_location("strict_media_validator", ROOT / "tools" / "content-builder" / "validate_media.py")
    module = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(module)
    return module


def load(path: Path) -> list[dict]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(raw, list):
        raise ValueError(f"manifest must be a list: {path}")
    return raw


def main() -> int:
    errors: list[str] = []
    all_records: list[dict] = []
    strict = validator()
    for voice in VOICES:
        folder = AUDITION / voice
        records = load(folder / "audition_manifest.json") if (folder / "audition_manifest.json").is_file() else []
        mp3s = sorted(folder.glob("*.mp3")) if folder.is_dir() else []
        numbered = [path for path in mp3s if re.fullmatch(r"\d{2}_.+\.mp3", path.name)]
        if len(records) != 12 or len(numbered) != 12 or len(mp3s) != 12:
            errors.append(f"{voice}: expected exactly 12 formal MP3 entries, records, and files")
        names = {Path(str(item.get("outputPath", ""))).name for item in records}
        if names != {path.name for path in numbered}:
            errors.append(f"{voice}: manifest/file mismatch or orphan MP3")
        for item in records:
            target = ROOT / str(item.get("outputPath", ""))
            if item.get("generationStatus") == "FAILED":
                errors.append(f"{voice}: FAILED item {item.get('id')}")
            if not target.is_file() or item.get("sha256") != sha256(target):
                errors.append(f"{voice}: SHA-256 mismatch for {item.get('id')}")
                continue
            try:
                strict.validate_mp3(target)
            except Exception as exc:
                errors.append(f"{voice}: invalid MP3 {target.name}: {exc}")
        all_records.extend(records)
    root_records = load(AUDITION / "audition_manifest.json") if (AUDITION / "audition_manifest.json").is_file() else []
    root_keys = {(item.get("voice"), item.get("id")) for item in root_records}
    local_keys = {(item.get("voice"), item.get("id")) for item in all_records}
    if len(all_records) != 24 or len(root_records) != 24 or root_keys != local_keys:
        errors.append("combined manifest is not a one-to-one set of 24 voice/id records")
    if errors:
        print("Audition package verification failed:\n" + "\n".join(errors))
        return 1
    print("Audition package verification: OK (2 voices, 24 normalized MP3 files)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
