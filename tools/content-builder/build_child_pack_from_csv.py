#!/usr/bin/env python3
"""Build the frozen 50-character child-pack-v1 candidate from CSV sources."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import shutil
from pathlib import Path


PACK_VERSION = "child-pack-v1"
ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SOURCE = ROOT / "content-source" / "v1"


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as file:
        return list(csv.DictReader(file))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--media-root", type=Path, default=None)
    parser.add_argument("--allow-draft", action="store_true")
    args = parser.parse_args()
    characters = read_csv(args.source / "characters.csv")
    questions = read_csv(args.source / "questions.csv")
    media = read_csv(args.source / "media.csv")
    reviews = read_csv(args.source / "reviews.csv")
    require(len(characters) == 50, f"characters.csv 必须是50字，实际 {len(characters)}")
    ids = [row["id"] for row in characters]
    require(len(set(ids)) == 50, "characters.csv 存在重复 id")
    chars = {row["character"] for row in characters}
    all_active = all(row["review_status"] == "ACTIVE" for row in characters)
    require(all_active or args.allow_draft, "存在未审核字符；候选包请使用 --allow-draft")
    require(len(questions) == 50, f"questions.csv 必须是50题，实际 {len(questions)}")
    characters_by_id = {row["id"]: row for row in characters}
    option_catalog = []
    compiled_questions_by_id: dict[str, list[dict]] = {character_id: [] for character_id in ids}
    question_variants = (
        ("LISTEN_CHOOSE_CHARACTER", "SOUND_TO_SHAPE"),
        ("CHARACTER_CHOOSE_IMAGE", "SHAPE_TO_MEANING"),
        ("CHARACTER_CHOOSE_AUDIO", "SHAPE_TO_SOUND"),
        ("LIFE_WORD_CONTEXT", "CONTEXT"),
    )
    for row in questions:
        character_id = row["character_id"]
        require(character_id in characters_by_id, f"题目引用未知 character_id: {character_id}")
        option_chars = [row["correct_character"], row["option_character_1"], row["option_character_2"], row["option_character_3"]]
        require(all(option in chars for option in option_chars), f"题目包含范围外汉字: {row['question_id']}")
        require(row["correct_character"] == characters_by_id[character_id]["character"], f"题目答案不匹配: {row['question_id']}")
        option_ids = [f"text_{next(item['id'] for item in characters if item['character'] == option)}" for option in option_chars]
        for variant, evidence in question_variants:
            compiled_questions_by_id[character_id].append({
                "id": f"{row['question_id']}_{variant.lower()}",
                "type": variant,
                "promptAudio": f"audio/characters/{character_id}_v1.mp3",
                "correctOptionId": option_ids[0],
                "optionIds": option_ids,
                "minLearnedCount": 0,
                "evidenceCategory": evidence,
            })
    for row in characters:
        option_catalog.append({"id": f"text_{row['id']}", "kind": "TEXT", "characterId": row["id"], "text": row["character"]})
    compiled_characters = []
    child_entries = []
    for order, row in enumerate(characters, start=1):
        character_id = row["id"]
        suffix = character_id.removeprefix("char_")
        status = row["review_status"]
        reviewed = status == "ACTIVE"
        words = [row["word_1"], row["word_2"]]
        compiled_characters.append({
            "id": character_id,
            "character": row["character"],
            "pinyin": row["pinyin"],
            "toneNumber": int(row["tone"]),
            "order": order,
            "meaningForChild": row["meaning_for_child"],
            "imageAsset": f"images/characters/{character_id}_main_v1.webp",
            "imageAlt": row["image_prompt"],
            "words": [{"text": word, "audioAsset": f"audio/words/{character_id}_{index}_v1.mp3"} for index, word in enumerate(words, start=1)],
            "sentence": {"text": row["sentence"], "audioAsset": f"audio/sentences/{character_id}_v1.mp3"},
            "audio": {"character": f"audio/characters/{character_id}_v1.mp3", "meaning": f"audio/meanings/meaning_{character_id}_v1.mp3"},
            "teachingPrompt": row["meaning_for_child"],
            "confusableRestrictions": [],
            "misconceptions": [],
            "questionSeeds": compiled_questions_by_id[character_id],
            "contentReview": {"textReviewed": reviewed, "assetReviewedByDeveloper": reviewed, "assetReviewedByParent": reviewed, "blockedReason": None if reviewed else "csv-draft"},
        })
        child_entries.append({
            "characterId": character_id,
            "meaningForChild": row["meaning_for_child"],
            "words": words,
            "sentence": row["sentence"],
            "learningGoal": f"理解“{row['character']}”的意思，并能在生活词语中认出它。",
            "imageRequest": row["image_prompt"],
            "audioRequest": row["audio_required"],
        })
    content = {
        "schemaVersion": 2,
        "contentVersion": PACK_VERSION,
        "layers": {"foundationVersion": "csv-child-pack-v1", "childContentVersion": PACK_VERSION, "experienceVersion": "child-pack-experience-v1"},
        "childLearningPack": {"schemaVersion": 1, "packVersion": PACK_VERSION, "characters": child_entries},
        "course": {"stageTestThreshold": 3, "badgeMilestones": []},
        "learningOrder": ids,
        "reviewOffsetsDays": [1, 3, 7, 14, 30, 60],
        "optionCatalog": option_catalog,
        "characters": compiled_characters,
    }
    args.output.mkdir(parents=True, exist_ok=True)
    media_root = args.media_root or (args.source / "generated")
    content_path = args.output / "content.json"
    pack_path = args.output / "pack.json"
    manifest_path = args.output / "manifest.json"
    content_path.write_text(json.dumps(content, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    package_status = "ACTIVE" if all_active else "CANDIDATE"
    pack_path.write_text(json.dumps({"packId": PACK_VERSION, "version": PACK_VERSION, "status": package_status, "characterCount": 50, "contentPath": "content.json", "manifestPath": "manifest.json"}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    resources = []
    for row in media:
        if row["character_id"] != "ALL":
            continue
        asset_template = row["asset_path"]
        if "{id}" in asset_template:
            continue
        source = media_root / asset_template
        target = args.output / asset_template
        if source.is_file():
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)
            payload = target.read_bytes()
            resources.append({"path": target.relative_to(args.output).as_posix(), "sha256": hashlib.sha256(payload).hexdigest(), "bytes": len(payload), "required": row["required"].lower() == "true"})
    for character in characters:
        cid = character["id"]
        for relative in (
            Path("images") / "characters" / f"{cid}_main_v1.webp",
            Path("audio") / "characters" / f"{cid}_v1.mp3",
            Path("audio") / "meanings" / f"meaning_{cid}_v1.mp3",
            Path("audio") / "words" / f"{cid}_1_v1.mp3",
            Path("audio") / "words" / f"{cid}_2_v1.mp3",
            Path("audio") / "sentences" / f"{cid}_v1.mp3",
        ):
            source = media_root / relative
            target = args.output / relative
            if not source.is_file():
                raise ValueError(f"缺少媒体资源: {source}")
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)
            payload = target.read_bytes()
            resources.append({"path": relative.as_posix(), "sha256": hashlib.sha256(payload).hexdigest(), "bytes": len(payload), "required": True})
    manifest_path.write_text(json.dumps({"manifestVersion": 1, "resources": resources}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Child pack written: {args.output} ({len(compiled_characters)} characters, status={package_status})")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, StopIteration) as exc:
        raise SystemExit(f"CHILD PACK ERROR: {exc}")
