#!/usr/bin/env python3
"""Generate real MP3 files with Azure Speech only when credentials are explicitly supplied.

No placeholder file is ever produced: each failed request exits non-zero and removes
the partial target. Set AZURE_SPEECH_KEY and AZURE_SPEECH_REGION before using.
"""
from __future__ import annotations

import csv, os, sys, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "content-source" / "starter-thirty-v1" / "generated" / "tts_source.csv"
PACK = ROOT / "app" / "src" / "main" / "assets" / "content" / "packs" / "starter-thirty-v1" / "2.0.0"


def main() -> None:
    key, region = os.getenv("AZURE_SPEECH_KEY"), os.getenv("AZURE_SPEECH_REGION")
    if not key or not region:
        sys.exit("AZURE_SPEECH_KEY and AZURE_SPEECH_REGION are required; no files created.")
    with SOURCE.open(encoding="utf-8", newline="") as file:
        for row in csv.DictReader(file):
            target = PACK / row["output_file"]
            target.parent.mkdir(parents=True, exist_ok=True)
            ssml = f'<speak version="1.0" xml:lang="zh-CN"><voice name="zh-CN-XiaoyiNeural"><prosody rate="-8%">{row["text"]}</prosody></voice></speak>'.encode("utf-8")
            request = urllib.request.Request(f"https://{region}.tts.speech.microsoft.com/cognitiveservices/v1", data=ssml, method="POST", headers={"Ocp-Apim-Subscription-Key": key, "Content-Type": "application/ssml+xml", "X-Microsoft-OutputFormat": "audio-48khz-96kbitrate-mono-mp3", "User-Agent": "shizi-content-builder"})
            try:
                with urllib.request.urlopen(request, timeout=60) as response:
                    data = response.read()
                if not data.startswith(b"ID3") and not data.startswith(b"\xff\xfb"):
                    raise RuntimeError("Azure response was not an MP3")
                target.write_bytes(data)
            except Exception:
                target.unlink(missing_ok=True)
                raise
            print(target.relative_to(ROOT))


if __name__ == "__main__":
    main()
