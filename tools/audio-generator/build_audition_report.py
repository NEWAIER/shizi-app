#!/usr/bin/env python3
"""Build or verify a stable combined Edge TTS audition report."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2] / "artifacts" / "tts-audition"


def outputs() -> tuple[str, str]:
    items: list[dict[str, Any]] = []
    for manifest in sorted(ROOT.glob("zh-CN-*/audition_manifest.json")):
        items.extend(json.loads(manifest.read_text(encoding="utf-8")))
    items.sort(key=lambda item: (str(item["voice"]), str(item["id"])))
    manifest_text = json.dumps(items, ensure_ascii=False, indent=2) + "\n"
    lines = ["# Edge TTS 试听包", "", "人工试听必检：单字声调；‘一’是否为规范 yī；‘一个’与‘一天’是否自然连读；语速是否适合4—7岁；声音是否温和清楚；首尾是否截断；是否有噪声或异常停顿。", "", "| Voice | 文件 | 状态 | 大小 | 时长(ms) | SHA-256 |", "|---|---|---|---:|---:|---|"]
    for item in items:
        lines.append(f"| {item['voice']} | {Path(item['outputPath']).name} | {item['generationStatus']} | {item.get('fileSize', '')} | {item.get('durationMs', '')} | {item.get('sha256', '')} |")
    return manifest_text, "\n".join(lines) + "\n"


def atomic_write(path: Path, text: str) -> None:
    temporary = path.with_name(f".{path.name}.tmp")
    temporary.write_text(text, encoding="utf-8")
    temporary.replace(path)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Verify reports without writing")
    options = parser.parse_args()
    manifest_text, index_text = outputs()
    manifest, index = ROOT / "audition_manifest.json", ROOT / "AUDITION_INDEX.md"
    if options.check:
        if not manifest.is_file() or not index.is_file() or manifest.read_text(encoding="utf-8") != manifest_text or index.read_text(encoding="utf-8") != index_text:
            print("Audition combined reports are stale; run build_audition_report.py", flush=True)
            return 1
        print("Audition combined reports are current")
        return 0
    atomic_write(manifest, manifest_text)
    atomic_write(index, index_text)
    print(f"Wrote {len(json.loads(manifest_text))} records to {ROOT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
