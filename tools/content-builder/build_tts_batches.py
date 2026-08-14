#!/usr/bin/env python3
"""Create deterministic, offline-only 30-character TTS production batch CSVs."""
from __future__ import annotations

import csv
from pathlib import Path, PurePosixPath, PureWindowsPath

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "content-source" / "starter-thirty-v1" / "generated" / "tts_source.csv"
OUTPUT = ROOT / "artifacts" / "tts-production" / "source"
FIELDS = ("id", "character_id", "type", "text", "output_file")
OUT_FIELDS = ("id", "text", "filename")
BATCHES = (
    ("一", "二", "三", "上", "下"), ("日", "月", "水", "火", "木"),
    ("天", "云", "雨", "风", "田"), ("手", "头", "耳", "眼", "牙"),
    ("牛", "羊", "鸟", "鱼", "马"),
)
CHARACTER_IDS = {"一":"char_u4e00","二":"char_u4e8c","三":"char_u4e09","上":"char_u4e0a","下":"char_u4e0b","日":"char_u65e5","月":"char_u6708","水":"char_u6c34","火":"char_u706b","木":"char_u6728","天":"char_u5929","云":"char_u4e91","雨":"char_u96e8","风":"char_u98ce","田":"char_u7530","手":"char_u624b","头":"char_u5934","耳":"char_u8033","眼":"char_u773c","牙":"char_u7259","牛":"char_u725b","羊":"char_u7f8a","鸟":"char_u9e1f","鱼":"char_u9c7c","马":"char_u9a6c"}


def safe_filename(value: str) -> str:
    path, windows = PurePosixPath(value), PureWindowsPath(value)
    if not value or "\\" in value or path.is_absolute() or windows.is_absolute() or windows.drive or any(part in ("", ".", "..") for part in path.parts) or path.suffix != ".mp3":
        raise ValueError(f"invalid output_file: {value!r}")
    return path.as_posix()


def load(source: Path = SOURCE) -> list[dict[str, str]]:
    with source.open(encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)
        if tuple(reader.fieldnames or ()) != FIELDS:
            raise ValueError(f"source fields must be exactly {FIELDS}")
        rows = list(reader)
    if len(rows) != 150:
        raise ValueError(f"expected 150 source rows, found {len(rows)}")
    ids, filenames = set(), set()
    for row in rows:
        row["id"] = row["id"].strip(); row["text"] = row["text"].strip(); row["character_id"] = row["character_id"].strip(); row["output_file"] = safe_filename(row["output_file"].strip())
        if not row["id"] or not row["text"]:
            raise ValueError("id and text must not be empty")
        if row["id"] in ids or row["output_file"] in filenames:
            raise ValueError("duplicate id or output_file")
        ids.add(row["id"]); filenames.add(row["output_file"])
    return rows


def build(rows: list[dict[str, str]]) -> list[list[dict[str, str]]]:
    batches = []
    for characters in BATCHES:
        expected = {CHARACTER_IDS[character] for character in characters}
        batch = [row for row in rows if row["character_id"] in expected]
        if {row["character_id"] for row in batch} != expected or len(batch) != 30:
            raise ValueError(f"batch {characters} must contain exactly 30 source rows")
        batches.append([{ "id": row["id"], "text": row["text"], "filename": row["output_file"] } for row in batch])
    if len({item["id"] for batch in batches for item in batch}) != 150:
        raise ValueError("batch IDs are not globally unique")
    return batches


def csv_text(rows: list[dict[str, str]]) -> str:
    import io
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=OUT_FIELDS, lineterminator="\n")
    writer.writeheader(); writer.writerows(rows)
    return stream.getvalue()


def write(output: Path, batches: list[list[dict[str, str]]]) -> None:
    output.mkdir(parents=True, exist_ok=True)
    all_rows = [row for batch in batches for row in batch]
    for index, batch in enumerate(batches, 1):
        (output / f"batch-{index:02d}.csv").write_text(csv_text(batch), encoding="utf-8")
    (output / "all-150.csv").write_text(csv_text(all_rows), encoding="utf-8")


def main() -> None:
    write(OUTPUT, build(load()))
    print("Wrote five deterministic 30-item source batches and all-150.csv")


if __name__ == "__main__":
    main()
