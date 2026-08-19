"""Split the 300-character media worklists into six deterministic 50-character batches."""
from __future__ import annotations

import csv
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "content-source" / "child-pack-v2-300"
OUTPUT = SOURCE / "generated"


def read(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as file:
        return list(csv.DictReader(file))


def write(path: Path, rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def main() -> None:
    characters = read(SOURCE / "characters.csv")
    media = read(SOURCE / "media.csv")
    questions = read(SOURCE / "questions.csv")
    tts = read(SOURCE / "generated" / "tts_source_all.csv")
    if len(characters) != 300 or len(questions) != 300 or len(media) != 1800:
        raise SystemExit("expected 300 characters, 300 questions, and 1800 media requirements")
    if len(tts) != 1500:
        raise SystemExit("expected 1500 TTS work items")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for batch_index in range(6):
        batch_ids = {row["id"] for row in characters[batch_index * 50:(batch_index + 1) * 50]}
        write(OUTPUT / f"batch-{batch_index + 1:02d}-characters.csv", [row for row in characters if row["id"] in batch_ids])
        write(OUTPUT / f"batch-{batch_index + 1:02d}-questions.csv", [row for row in questions if row["character_id"] in batch_ids])
        write(OUTPUT / f"batch-{batch_index + 1:02d}-media.csv", [row for row in media if row["character_id"] in batch_ids])
        tts_ids = {row["id"] for row in characters[batch_index * 50:(batch_index + 1) * 50]}
        write(OUTPUT / f"batch-{batch_index + 1:02d}-tts.csv", [row for row in tts if any(row["id"].startswith(character_id + "_") for character_id in tts_ids)])
    print("Wrote six 50-character media batches")


if __name__ == "__main__":
    main()
