#!/usr/bin/env python3
"""Build PR-03A production manifests from frozen CSV sources.

This tool intentionally does not create media. It produces deterministic work lists
for real-image and real-audio production, then validates imported files in PR-03B.
"""
from __future__ import annotations

import csv
import argparse
import json
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


QUESTION_TYPES = ("LISTEN_CHOOSE_CHARACTER", "CHARACTER_CHOOSE_IMAGE", "CHARACTER_CHOOSE_AUDIO", "SHAPE_RECOGNITION", "LIFE_WORD_CONTEXT")
EVIDENCE = {"LISTEN_CHOOSE_CHARACTER": "AUDIO_RECOGNITION", "CHARACTER_CHOOSE_IMAGE": "IMAGE_RECOGNITION", "CHARACTER_CHOOSE_AUDIO": "AUDIO_MATCH", "SHAPE_RECOGNITION": "SHAPE_RECOGNITION", "LIFE_WORD_CONTEXT": "LIFE_WORD_CONTEXT"}


def write_question_sources(characters: list[dict[str, str]], words: list[dict[str, str]]) -> None:
    """Create reviewable, fixed question sources. This never creates media."""
    option_fields = ["option_id", "kind", "character_id", "text", "asset_path", "review_status"]
    question_fields = ["question_id", "character_id", "question_type", "prompt_audio", "correct_option_id", "option_ids", "min_learned_count", "evidence_category", "requires_new_image", "text_review_status", "parent_review_status"]
    review_fields = ["question_id", "option_id", "distractor_reason", "ambiguity_risk", "review_status", "notes"]
    image_fields = ["asset_path", "character_id", "usage", "shared_template", "source_question_ids", "requires_new_image", "review_status"]
    options, questions, reviews, images = [], [], [], []
    ids = [row["id"] for row in characters]
    by_character = {row["id"]: [] for row in characters}
    for word in words: by_character[word["character_id"]].append(word)
    for index, character in enumerate(characters):
        cid, char = character["id"], character["character"]
        suffix = cid.removeprefix("char_")
        options.extend([
            {"option_id": f"text_{cid}", "kind": "TEXT", "character_id": cid, "text": char, "asset_path": "", "review_status": "PENDING"},
            {"option_id": f"image_{cid}", "kind": "IMAGE", "character_id": cid, "text": char, "asset_path": f"images/characters/{cid}_main_v1.webp", "review_status": "PENDING"},
            {"option_id": f"audio_{cid}", "kind": "AUDIO", "character_id": cid, "text": char, "asset_path": f"audio/characters/{cid}_v1.mp3", "review_status": "PENDING"},
        ])
        for word in by_character[cid]:
            options.append({"option_id": f"word_{cid}_{word['position']}", "kind": "TEXT", "character_id": cid, "text": word["text"], "asset_path": "", "review_status": "PENDING"})
        template = "quantity-one-two-three" if char in ("一", "二", "三") else "position-up-down" if char in ("上", "下") else "character-main"
        images.append({"asset_path": f"images/characters/{cid}_main_v1.webp", "character_id": cid, "usage": "main image and image-option", "shared_template": template, "source_question_ids": f"q_{cid}_character_choose_image", "requires_new_image": "false", "review_status": "PENDING"})
    for index, character in enumerate(characters):
        cid, suffix = character["id"], character["id"].removeprefix("char_")
        candidates = [ids[(index + offset) % len(ids)] for offset in (0, -1, -2, 1)]
        for question_type in QUESTION_TYPES:
            if question_type == "CHARACTER_CHOOSE_IMAGE":
                option_ids, correct = [f"image_{value}" for value in candidates], f"image_{cid}"
            elif question_type == "CHARACTER_CHOOSE_AUDIO":
                option_ids, correct = [f"audio_{value}" for value in candidates], f"audio_{cid}"
            elif question_type == "LIFE_WORD_CONTEXT":
                word_ids = [f"word_{value}_1" for value in candidates]
                option_ids, correct = word_ids, f"word_{cid}_1"
            else:
                option_ids, correct = [f"text_{value}" for value in candidates], f"text_{cid}"
            qid = f"q_{cid}_{question_type.lower()}"
            questions.append({"question_id": qid, "character_id": cid, "question_type": question_type, "prompt_audio": f"audio/characters/{cid}_v1.mp3", "correct_option_id": correct, "option_ids": json.dumps(option_ids, ensure_ascii=False, separators=(",", ":")), "min_learned_count": str(index + 1), "evidence_category": EVIDENCE[question_type], "requires_new_image": "false", "text_review_status": "PENDING", "parent_review_status": "PENDING"})
            for option_id in option_ids:
                if option_id != correct:
                    reviews.append({"question_id": qid, "option_id": option_id, "distractor_reason": "优先采用当前字之前已引入的字；首批不足时使用相邻基础字。", "ambiguity_risk": "LOW", "review_status": "PENDING", "notes": "人工审核时确认不造成字形或语义歧义。"})
    source_write("options.csv", option_fields, options)
    source_write("questions.csv", question_fields, questions)
    source_write("distractor_review.csv", review_fields, reviews)
    source_write("question_image_requirements.csv", image_fields, images)


def source_write(name: str, fieldnames: list[str], values: list[dict[str, str]]) -> None:
    with (SOURCE / name).open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames, lineterminator="\n")
        writer.writeheader(); writer.writerows(values)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write-question-sources", action="store_true")
    arguments = parser.parse_args()
    characters = rows("characters.csv")
    readings = {row["character_id"]: row for row in rows("readings.csv")}
    words = rows("words.csv")
    sentences = {row["character_id"]: row for row in rows("sentences.csv")}
    assert len(characters) == 25, "PR-03A requires exactly 25 new characters"
    assert len({row["id"] for row in characters}) == 25, "duplicate character ID"
    assert [int(row["order"]) for row in characters] == list(range(6, 31)), "unexpected frozen order"
    assert all(row["id"] in readings and row["id"] in sentences for row in characters), "missing reading or sentence"
    assert all(sum(word["character_id"] == row["id"] for word in words) == 3 for row in characters), "each character needs 3 words"
    if arguments.write_question_sources:
        write_question_sources(characters, words)

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
    if (SOURCE / "options.csv").exists():
        options = rows("options.csv"); questions = rows("questions.csv"); image_requirements = rows("question_image_requirements.csv")
        write_csv("optionCatalog.csv", list(options[0]), options)
        write_csv("questionSeeds.csv", list(questions[0]), questions)
        write_csv("question_image_requirements.csv", list(image_requirements[0]), image_requirements)
    print(f"Generated {len(tts)} TTS rows, {len(prompts)} image prompts, and {len(media)} media requirements in {OUT}")


if __name__ == "__main__":
    main()
