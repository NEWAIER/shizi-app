#!/usr/bin/env python3
"""Build PR-03A production manifests from frozen CSV sources.

This tool intentionally does not create media. It produces deterministic work lists
for real-image and real-audio production, then validates imported files in PR-03B.
"""
from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "content-source" / "starter-thirty-v1"
OUT = SOURCE / "generated"
BASE_IMAGE_PROMPT = (
    "儿童识字APP教学插画，统一现代扁平绘本风格，1024×1024正方形，浅米色纯净背景，"
    "主体居中占画面约70%，轮廓清楚，颜色柔和，适合4—7岁儿童，一眼可识别，"
    "画面只有一个明确主旨，无文字、无拼音、无数字、无水印、无Logo、无边框、无复杂装饰。"
)


def rows(name: str) -> list[dict[str, str]]:
    with (SOURCE / name).open("r", encoding="utf-8", newline="") as file:
        return list(csv.DictReader(file))


def write_csv(name: str, fieldnames: list[str], values: list[dict[str, str]]) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    with (OUT / name).open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(values)


def main() -> None:
    characters = rows("characters.csv")
    readings = {row["character_id"]: row for row in rows("readings.csv")}
    words = rows("words.csv")
    sentences = {row["character_id"]: row for row in rows("sentences.csv")}
    assert len(characters) == 25, "PR-03A requires exactly 25 new characters"
    assert len({row["id"] for row in characters}) == 25, "duplicate character ID"
    assert [int(row["order"]) for row in characters] == list(range(6, 31)), "unexpected frozen order"
    assert all(row["id"] in readings and row["id"] in sentences for row in characters), "missing reading or sentence"
    assert all(sum(word["character_id"] == row["id"] for word in words) == 3 for row in characters), "each character needs 3 words"

    tts, prompts, media, checklist = [], [], [], []
    word_by_character: dict[str, list[dict[str, str]]] = {}
    for word in words:
        word_by_character.setdefault(word["character_id"], []).append(word)
    for character in characters:
        cid = character["id"]
        suffix = cid.removeprefix("char_")
        audio_rows = [
            ("character", character["character"], f"audio/characters/{cid}_v1.mp3"),
            ("meaning", character["meaning_for_child"], f"audio/meanings/meaning_{suffix}_v1.mp3"),
            *[("word", word["text"], f"audio/words/word_{suffix}_{word['position']}_v1.mp3") for word in word_by_character[cid]],
            ("sentence", sentences[cid]["text"], f"audio/sentences/sentence_{suffix}_v1.mp3"),
        ]
        for kind, text, output_file in audio_rows:
            tts.append({"id": output_file.rsplit("/", 1)[-1].removesuffix(".mp3"), "character_id": cid, "type": kind, "text": text, "output_file": output_file})
            media.append({"asset_path": output_file, "character_id": cid, "usage": kind, "required_format": "MP3 mono 44.1kHz 96-128kbps", "source": "TTS or adult self-recording", "license_status": "SELF_GENERATED_PENDING_REVIEW"})
        image_path = f"images/characters/{cid}_main_v1.webp"
        prompts.append({"character_id": cid, "character": character["character"], "output_file": image_path, "prompt": f"{BASE_IMAGE_PROMPT}\n本张图片主题：{character['image_theme']}"})
        media.append({"asset_path": image_path, "character_id": cid, "usage": "main image / image option", "required_format": "WebP sRGB 1024x1024 <=500KB", "source": "AI generated or self-created", "license_status": "SELF_GENERATED_PENDING_REVIEW"})
        checklist.append({"character_id": cid, "character": character["character"], "check": "字形、拼音、声调、释义、词句、主图、题目唯一性、音频完整性", "parent_result": "PENDING_REAL_PARENT_REVIEW", "notes": ""})
    write_csv("tts_source.csv", ["id", "character_id", "type", "text", "output_file"], tts)
    write_csv("image_prompts.csv", ["character_id", "character", "output_file", "prompt"], prompts)
    write_csv("media_requirements.csv", ["asset_path", "character_id", "usage", "required_format", "source", "license_status"], media)
    write_csv("parent_review_checklist.csv", ["character_id", "character", "check", "parent_result", "notes"], checklist)
    print(f"Generated {len(tts)} TTS rows, {len(prompts)} image prompts, and {len(media)} media requirements in {OUT}")


if __name__ == "__main__":
    main()
