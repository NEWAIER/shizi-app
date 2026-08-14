#!/usr/bin/env python3
"""Merge per-voice edge-tts manifests into the required audition root report."""
from __future__ import annotations
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / "artifacts" / "tts-audition"

def main() -> None:
    items = []
    for manifest in sorted(ROOT.glob("zh-CN-*/audition_manifest.json")):
        items.extend(json.loads(manifest.read_text(encoding="utf-8")))
    (ROOT / "audition_manifest.json").write_text(json.dumps(items, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lines = ["# Edge TTS 试听包", "", "人工试听必检：单字声调；‘一’是否为规范 yī；‘一个’与‘一天’是否自然连读；语速是否适合4—7岁；声音是否温和清楚；首尾是否截断；是否有噪声或异常停顿。", "", "| Voice | 文件 | 状态 | 大小 | 时长(ms) | SHA-256 |", "|---|---|---|---:|---:|---|"]
    for item in items:
        lines.append(f"| {item['voice']} | {Path(item['outputPath']).name} | {item['generationStatus']} | {item.get('fileSize', '')} | {item.get('durationMs', '')} | {item.get('sha256', '')} |")
    (ROOT / "AUDITION_INDEX.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {len(items)} records to {ROOT}")

if __name__ == "__main__": main()
