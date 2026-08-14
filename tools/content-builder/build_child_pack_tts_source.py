#!/usr/bin/env python3
"""Create deterministic Edge-TTS work items for the 50-character CSV pack."""
from __future__ import annotations

import argparse
import csv
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--characters", type=Path, default=Path("content-source/v1/characters.csv"))
    parser.add_argument("--output", type=Path, default=Path("content-source/v1/generated/tts_source_all.csv"))
    args = parser.parse_args()
    with args.characters.open(encoding="utf-8", newline="") as file:
        rows = list(csv.DictReader(file))
    output = []
    for row in rows:
        cid, suffix = row["id"], row["id"].removeprefix("char_")
        entries = [
            (f"{cid}_character", row["character"], f"audio/characters/{cid}_v1.mp3"),
            (f"{cid}_meaning", row["meaning_for_child"], f"audio/meanings/meaning_{cid}_v1.mp3"),
            (f"{cid}_word1", row["word_1"], f"audio/words/{cid}_1_v1.mp3"),
            (f"{cid}_word2", row["word_2"], f"audio/words/{cid}_2_v1.mp3"),
            (f"{cid}_sentence", row["sentence"], f"audio/sentences/{cid}_v1.mp3"),
        ]
        output.extend({"id": item_id, "text": text, "filename": filename} for item_id, text, filename in entries)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=("id", "text", "filename"))
        writer.writeheader()
        writer.writerows(output)
    print(f"TTS source written: {args.output} ({len(output)} files)")


if __name__ == "__main__":
    main()
